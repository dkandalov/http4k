package org.http4k.connect.anthropic.endpoints

import org.http4k.connect.storage.Storage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.lens.Path

private val id = Path.of("id")

internal fun <T : Any> Storage<T>.all() = keySet().sorted().mapNotNull(::get)

internal fun <T : Any> Storage<T>.withId(request: Request, fn: (String, T) -> Response) =
    id(request).let { key -> get(key)?.let { fn(key, it) } ?: Response(NOT_FOUND) }
