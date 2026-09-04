package org.http4k.connect.anthropic.endpoints

import org.http4k.ai.model.ModelName
import org.http4k.ai.model.ResponseId
import org.http4k.ai.model.Role
import org.http4k.ai.model.StopReason
import org.http4k.connect.anthropic.AnthropicAIMoshi
import org.http4k.connect.anthropic.AnthropicAIMoshi.autoBody
import org.http4k.connect.anthropic.LoremIpsum
import org.http4k.connect.anthropic.MessageContentGenerator
import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.DeltaContent
import org.http4k.connect.anthropic.action.MessageCompletion
import org.http4k.connect.anthropic.action.MessageCompletionResponse
import org.http4k.connect.anthropic.action.MessageDeltaContent
import org.http4k.connect.anthropic.action.MessageGenerationEvent
import org.http4k.connect.anthropic.action.Usage
import org.http4k.connect.anthropic.end_turn
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.bind
import org.http4k.sse.SseMessage

fun messageCompletion(completionGenerators: Map<ModelName, MessageContentGenerator>) =
    "/v1/messages" bind Method.POST to { request ->
        val messageRequest = autoBody<MessageCompletion>().toLens()(request)

        val content = (completionGenerators[messageRequest.model] ?: MessageContentGenerator.LoremIpsum())(
            messageRequest.messages
        )

        val response = MessageCompletionResponse(
            ResponseId.of(messageRequest.hashCode().toString()),
            Role.Assistant,
            content,
            messageRequest.model,
            StopReason.end_turn,
            null,
            Usage(1, 1, 1, 1)
        )

        when {
            messageRequest.stream -> Response(OK)
                .with(CONTENT_TYPE of TEXT_EVENT_STREAM.withNoDirectives())
                .body(streamOf(response).joinToString("").byteInputStream())

            else -> Response(OK).with(autoBody<MessageCompletionResponse>().toLens() of response)
        }
    }

private fun streamOf(response: MessageCompletionResponse) =
    (listOf(MessageGenerationEvent.StartMessage(response.copy(content = emptyList()))) +
        response.content.flatMapIndexed { index, content ->
            listOfNotNull(
                MessageGenerationEvent.StartBlock(index.toLong(), DeltaContent.Text("")),
                (content as? Content.Text)?.let {
                    MessageGenerationEvent.Delta(index.toLong(), DeltaContent.TextDelta(it.text))
                },
                MessageGenerationEvent.Stop(index.toLong())
            )
        } +
        listOf(
            MessageGenerationEvent.MessageDelta(
                MessageDeltaContent(StopReason.end_turn, null, response.usage)
            ),
            MessageGenerationEvent.StopMessage
        ))
        .map { SseMessage.Data(AnthropicAIMoshi.asFormatString(it)).toMessage() }
