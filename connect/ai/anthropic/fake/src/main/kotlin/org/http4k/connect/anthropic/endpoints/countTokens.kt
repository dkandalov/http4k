package org.http4k.connect.anthropic.endpoints

import org.http4k.connect.anthropic.AnthropicAIMoshi.autoBody
import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.CountTokens
import org.http4k.connect.anthropic.action.TokenCount
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.bind

fun countTokens() = "/v1/messages/count_tokens" bind POST to { request ->
    val counted = autoBody<CountTokens>().toLens()(request)
        .messages
        .flatMap { it.content }
        .filterIsInstance<Content.Text>()
        .sumOf { it.text.split(" ").size }

    Response(OK).with(autoBody<TokenCount>().toLens() of TokenCount(counted))
}
