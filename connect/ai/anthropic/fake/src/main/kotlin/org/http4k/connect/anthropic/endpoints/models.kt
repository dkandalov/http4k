package org.http4k.connect.anthropic.endpoints

import org.http4k.connect.anthropic.AnthropicAIMoshi.autoBody
import org.http4k.connect.anthropic.action.ModelInfo
import org.http4k.connect.anthropic.action.Models
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.routes

fun models(models: Storage<ModelInfo>) = routes(
    "/v1/models" bind GET to {
        Response(OK).with(
            autoBody<Models>().toLens() of Models(models.keySet().sorted().mapNotNull(models::get))
        )
    },
    "/v1/models/{id}" bind GET to { request ->
        models[Path.of("id")(request)]
            ?.let { Response(OK).with(autoBody<ModelInfo>().toLens() of it) }
            ?: Response(NOT_FOUND)
    }
)
