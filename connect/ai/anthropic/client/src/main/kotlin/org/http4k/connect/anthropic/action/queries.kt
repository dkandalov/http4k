package org.http4k.connect.anthropic.action

import org.http4k.core.Request

internal fun Request.queries(vararg params: Pair<String, String?>) =
    params.fold(this) { request, (name, value) -> value?.let { request.query(name, it) } ?: request }
