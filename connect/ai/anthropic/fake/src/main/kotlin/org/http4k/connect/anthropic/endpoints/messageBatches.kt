package org.http4k.connect.anthropic.endpoints

import org.http4k.ai.model.ModelName
import org.http4k.ai.model.ResponseId
import org.http4k.ai.model.Role
import org.http4k.ai.model.StopReason
import org.http4k.connect.anthropic.AnthropicAIMoshi
import org.http4k.connect.anthropic.AnthropicAIMoshi.autoBody
import org.http4k.connect.anthropic.LoremIpsum
import org.http4k.connect.anthropic.MessageBatchId
import org.http4k.connect.anthropic.MessageContentGenerator
import org.http4k.connect.anthropic.ProcessingStatus
import org.http4k.connect.anthropic.StoredBatch
import org.http4k.connect.anthropic.action.BatchResult
import org.http4k.connect.anthropic.action.CreateMessageBatch
import org.http4k.connect.anthropic.action.DeletedMessageBatch
import org.http4k.connect.anthropic.action.MessageBatch
import org.http4k.connect.anthropic.action.MessageBatchResponse
import org.http4k.connect.anthropic.action.MessageBatches
import org.http4k.connect.anthropic.action.MessageCompletionResponse
import org.http4k.connect.anthropic.action.RequestCounts
import org.http4k.connect.anthropic.action.Usage
import org.http4k.connect.anthropic.end_turn
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock
import java.time.Duration

fun messageBatches(
    batches: Storage<StoredBatch>,
    completionGenerators: Map<ModelName, MessageContentGenerator>,
    clock: Clock
) = routes(
    "/v1/messages/batches" bind POST to { request ->
        val batch = newBatch(autoBody<CreateMessageBatch>().toLens()(request), completionGenerators, clock)
        batches[batch.batch.id.value] = batch
        Response(OK).with(autoBody<MessageBatch>().toLens() of batch.batch)
    },

    "/v1/messages/batches" bind GET to {
        Response(OK).with(autoBody<MessageBatches>().toLens() of MessageBatches(batches.all().map { it.batch }))
    },

    "/v1/messages/batches/{id}/results" bind GET to { request ->
        batches.withId(request) { _, stored ->
            Response(OK).body(stored.responses.joinToString("\n") { AnthropicAIMoshi.asFormatString(it) })
        }
    },

    "/v1/messages/batches/{id}/cancel" bind POST to { request ->
        batches.withId(request) { key, stored ->
            val cancelled = stored.batch.copy(
                processing_status = ProcessingStatus.canceling,
                cancel_initiated_at = clock.instant()
            )
            batches[key] = stored.copy(batch = cancelled)
            Response(OK).with(autoBody<MessageBatch>().toLens() of cancelled)
        }
    },

    "/v1/messages/batches/{id}" bind GET to { request ->
        batches.withId(request) { _, it -> Response(OK).with(autoBody<MessageBatch>().toLens() of it.batch) }
    },

    "/v1/messages/batches/{id}" bind DELETE to { request ->
        batches.withId(request) { key, _ ->
            batches.remove(key)
            Response(OK).with(autoBody<DeletedMessageBatch>().toLens() of DeletedMessageBatch(MessageBatchId.of(key)))
        }
    }
)

private fun newBatch(
    request: CreateMessageBatch,
    completionGenerators: Map<ModelName, MessageContentGenerator>,
    clock: Clock
): StoredBatch {
    val id = MessageBatchId.of("msgbatch_" + request.hashCode().toString().replace("-", ""))

    val responses = request.requests.map {
        MessageBatchResponse(
            it.custom_id,
            BatchResult.Succeeded(
                MessageCompletionResponse(
                    ResponseId.of(it.custom_id.value),
                    Role.Assistant,
                    (completionGenerators[it.params.model] ?: MessageContentGenerator.LoremIpsum())(it.params.messages),
                    it.params.model,
                    StopReason.end_turn,
                    null,
                    Usage(1, 1, 1, 1)
                )
            )
        )
    }

    return StoredBatch(
        MessageBatch(
            id,
            ProcessingStatus.ended,
            RequestCounts(succeeded = responses.size),
            clock.instant(),
            clock.instant().plus(Duration.ofDays(29)),
            Uri.of("https://api.anthropic.com/v1/messages/batches/${id.value}/results"),
            ended_at = clock.instant()
        ),
        responses
    )
}
