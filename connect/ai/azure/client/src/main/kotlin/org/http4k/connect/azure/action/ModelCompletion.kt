package org.http4k.connect.azure.action

import org.http4k.ai.util.toSseSequence
import org.http4k.connect.azure.AzureAIAction
import org.http4k.connect.azure.AzureAIMoshi
import org.http4k.core.Response

interface ModelCompletion : AzureAIAction<Sequence<CompletionResponse>> {
    val stream: Boolean
    fun content(): List<Message>
    override fun toResult(response: Response) = toSseSequence(response, AzureAIMoshi, "[DONE]")
}
