package com.turbomates.openapi

import java.util.Locale
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class OpenApiKType(val original: KType) {
    private val projectionTypes: Map<String, KType> = buildGenericTypes(original)
    private fun buildGenericTypes(type: KType): Map<String, KType> {
        val types = mutableMapOf<String, KType>()
        type.jvmErasure.typeParameters.forEachIndexed { index, kTypeParameter ->
            types[kTypeParameter.name] = type.arguments[index].type!!
        }

        return types
    }

    fun type(): Type {
        return buildType(original)
    }

    fun objectType(name: String): Type.Object {
        if (original.isCollection() || original.isPrimitive()) {
            throw InvalidTypeForOpenApiType(original.javaType.typeName, Type.Object::class.simpleName!!)
        }
        return buildType(name, original) as Type.Object
    }

    fun getArgumentProjectionType(type: KType): OpenApiKType {
        if (projectionTypes.containsKey(type.toString())) {
            return OpenApiKType(projectionTypes.getValue(type.toString()))
        }
        return OpenApiKType(type)
    }

    private fun buildType(name: String, type: KType): Type {
        if (type.isCollection() || type.isPrimitive() || type.isEnum()) {
            return buildType(type)
        }

        val kclass = type.classifier as? KClass<*>
        if (kclass != null && kclass.isValue) {
            return buildType(type.jvmErasure.memberProperties.first().returnType)
        }
        val descriptions = mutableListOf<Property>()
        type.jvmErasure.memberProperties.forEach { property ->
            var memberType = property.returnType
            // ToDo think about parametrization of this option
            if (!property.isLateinit) {
                descriptions.add(Property(property.name, buildType(memberType)))
            }
        }
        return Type.Object(name, descriptions, returnType = type.jvmErasure.qualifiedName, nullable = type.isMarkedNullable)
    }

    private fun buildType(memberType: KType): Type {
        return when {
            memberType.isCollection() -> {
                var collectionType = if (memberType.arguments.isEmpty()) {
                    memberType.jvmErasure.supertypes.first {
                        it.isSubtypeOf(typeOf<Set<*>>()) || it.isSubtypeOf(typeOf<List<*>>())
                    }.arguments.first().type!!
                } else {
                    memberType.arguments.first().type!!
                }
                if (projectionTypes.containsKey(collectionType.toString())) {
                    collectionType = projectionTypes.getValue(collectionType.toString())
                }
                when {
                    collectionType.isPrimitive() -> Type.Array(collectionType.openApiType, nullable = memberType.isMarkedNullable)
                    collectionType.isEnum() -> {
                        Type.Array(buildType(collectionType), nullable = memberType.isMarkedNullable)
                    }
                    else -> Type.Array(
                        buildType(collectionType.jvmErasure.simpleName!!, collectionType),
                        nullable = memberType.isMarkedNullable
                    )
                }
            }
            memberType.isMap() -> {
                val argType = memberType.arguments[0].type!!
                val firstType = projectionTypes.getOrDefault(argType.toString(), argType)
                val argSecondType = memberType.arguments[1].type!!
                val secondType = projectionTypes.getOrDefault(argSecondType.toString(), argSecondType)
                Type.Object(
                    "map",
                    properties = listOf(
                        Property(
                            firstType.jvmErasure.simpleName!!,
                            buildType(secondType)
                        )
                    ),
                    nullable = memberType.isMarkedNullable
                )
            }
            memberType.isEnum() -> {
                val values = memberType.jvmErasure.java.enumConstants
                Type.String(values.map { it.toString() }, nullable = memberType.isMarkedNullable)
            }
            memberType.isPrimitive() ->
                memberType.openApiType
            else -> {
                var projectionType = projectionTypes.getOrDefault(memberType.toString(), memberType)
                buildType(projectionType.jvmErasure.simpleName!!, projectionType)
            }
        }
    }

    private fun KType.isPrimitive(): Boolean {
        return javaClass.isPrimitive ||
                isSubtypeOf(typeOf<String?>()) ||
                isSubtypeOf(typeOf<Int?>()) ||
                isSubtypeOf(typeOf<Float?>()) ||
                isSubtypeOf(typeOf<Double?>()) ||
                isSubtypeOf(typeOf<Boolean?>()) ||
                isSubtypeOf(typeOf<UUID?>())
    }

    private fun KType.isCollection(): Boolean {
        return isSubtypeOf(typeOf<Collection<*>?>())
    }

    private fun KType.isMap(): Boolean {
        return isSubtypeOf(typeOf<Map<*, *>>())
    }

    private fun KType.isEnum(): Boolean {
        return this.javaClass.isEnum || isSubtypeOf(typeOf<Enum<*>?>())
    }


    private val KType.openApiType: Type
        get() {
            return when {
                isSubtypeOf(typeOf<String?>()) -> Type.String(nullable = isMarkedNullable)
                isSubtypeOf(typeOf<Locale?>()) -> Type.String(nullable = isMarkedNullable)
                isSubtypeOf(typeOf<UUID?>()) -> Type.String(nullable = isMarkedNullable)
                isSubtypeOf(typeOf<Int?>()) -> Type.Number(isMarkedNullable)
                isSubtypeOf(typeOf<Float?>()) -> Type.Number(isMarkedNullable)
                isSubtypeOf(typeOf<Boolean?>()) -> Type.Boolean(isMarkedNullable)
                isSubtypeOf(typeOf<Double?>()) -> Type.Number(isMarkedNullable)
                else -> throw UnhandledTypeException(jvmErasure.simpleName!!)
            }
        }
}

val KType.openApiKType: OpenApiKType
    get() = OpenApiKType(this)

class UnhandledTypeException(type: String) : Exception("unhandled type $type")
class InvalidTypeForOpenApiType(type: String, openApiType: String) : Exception("Invalid $type to build $openApiType")
