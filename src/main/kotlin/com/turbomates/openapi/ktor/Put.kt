@file:Suppress("unused", "OPT_IN_USAGE")

package com.turbomates.openapi.ktor

import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.locations.locations
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.method
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import kotlin.reflect.typeOf

inline fun <reified TResponse : Any> Route.put(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.() -> TResponse
): Route {
    val route = method(HttpMethod.Put) {
        handle {
            call.respond(body())
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Put,
        response = typeOf<TResponse>()
    )
    return route
}

inline fun <reified TResponse : Any> Route.put(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.() -> TResponse
): Route {
    val route = route(path, HttpMethod.Put) {
        handle {
            call.respond(body())
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Put,
        response = typeOf<TResponse>()
    )
    return route
}

inline fun <reified TResponse : Any, reified TBody : Any> Route.put(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TBody) -> TResponse
): Route {
    val route = route(path, HttpMethod.Put) {
        handle {
            call.respond(body(call.receive()))
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Put,
        response = typeOf<TResponse>(),
        body = typeOf<TBody>()
    )
    return route
}

inline fun <reified TResponse : Any, reified TParams : Any> Route.emptyPut(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TParams) -> TResponse
): Route {
    val route = route(path, HttpMethod.Put) {
        handle {
            call.respond(body(locations.resolve(TParams::class, call)))
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Put,
        response = typeOf<TResponse>(),
        pathParams = if (route.buildFullPath().containsPathParameters()) typeOf<TParams>() else null,
        queryParams = if (!route.buildFullPath().containsPathParameters()) typeOf<TParams>() else null
    )
    return route
}

inline fun <reified TResponse : Any, reified TBody : Any, reified TParams : Any> Route.put(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TBody, TParams) -> TResponse
): Route {
    val route = route(path, HttpMethod.Put) {
        handle {
            call.respond(body(call.receive(), locations.resolve(TParams::class, call)))
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Put,
        response = typeOf<TResponse>(),
        body = typeOf<TBody>(),
        pathParams = if (route.buildFullPath().containsPathParameters()) typeOf<TParams>() else null,
        queryParams = if (!route.buildFullPath().containsPathParameters()) typeOf<TParams>() else null
    )
    return route
}

inline fun <reified TResponse : Any, reified TBody : Any, reified TQuery : Any, reified TPath : Any> Route.put(
    path: String,
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(TBody, TPath, TQuery) -> TResponse
): Route {
    val route = route(path, HttpMethod.Put) {
        handle {
            call.respond(
                body(
                    call.receive(),
                    locations.resolve(TPath::class, call),
                    locations.resolve(TQuery::class, call)
                )
            )
        }
    }
    openApi.addToPath(
        route.buildFullPath(),
        HttpMethod.Put,
        response = typeOf<TResponse>(),
        body = typeOf<TBody>(),
        queryParams = typeOf<TQuery>(),
        pathParams = typeOf<TPath>()
    )
    return route
}
