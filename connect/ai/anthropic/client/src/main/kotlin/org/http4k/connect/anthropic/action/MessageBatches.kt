package org.http4k.connect.anthropic.action

import org.http4k.ai.util.toJsonLinesSequence
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.Paged
import org.http4k.connect.PagedAction
import org.http4k.connect.anthropic.AnthropicAIAction
import org.http4k.connect.anthropic.AnthropicAIMoshi
import org.http4k.connect.anthropic.CustomId
import org.http4k.connect.anthropic.MessageBatchId
import org.http4k.connect.anthropic.ProcessingStatus
import org.http4k.connect.kClass
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel
import java.time.Instant

@Http4kConnectAction
@JsonSerializable
data class CreateMessageBatch(val requests: List<BatchRequest>) : NonNullAutoMarshalledAction<MessageBatch>(kClass(), AnthropicAIMoshi), AnthropicAIAction<MessageBatch> {
    override fun toRequest() = Request(POST, "/v1/messages/batches")
        .with(AnthropicAIMoshi.autoBody<CreateMessageBatch>().toLens() of this)
}

@Http4kConnectAction
data class GetMessageBatch(val id: MessageBatchId) : NonNullAutoMarshalledAction<MessageBatch>(kClass(), AnthropicAIMoshi), AnthropicAIAction<MessageBatch> {
    override fun toRequest() = Request(GET, "/v1/messages/batches/${id.value}")
}

@Http4kConnectAction
data class CancelMessageBatch(val id: MessageBatchId) : NonNullAutoMarshalledAction<MessageBatch>(kClass(), AnthropicAIMoshi), AnthropicAIAction<MessageBatch> {
    override fun toRequest() = Request(POST, "/v1/messages/batches/${id.value}/cancel")
}

@Http4kConnectAction
data class DeleteMessageBatch(val id: MessageBatchId) : NonNullAutoMarshalledAction<DeletedMessageBatch>(kClass(), AnthropicAIMoshi), AnthropicAIAction<DeletedMessageBatch> {
    override fun toRequest() = Request(DELETE, "/v1/messages/batches/${id.value}")
}

@Http4kConnectAction
data class ListMessageBatches(
    val limit: Int? = null,
    val after_id: String? = null,
    val before_id: String? = null
) : NonNullAutoMarshalledAction<MessageBatches>(kClass(), AnthropicAIMoshi), AnthropicAIAction<MessageBatches>,
    PagedAction<String, MessageBatch, MessageBatches, ListMessageBatches> {
    override fun next(token: String) = copy(after_id = token)

    override fun toRequest() = Request(GET, "/v1/messages/batches")
        .queries("limit" to limit?.toString(), "after_id" to after_id, "before_id" to before_id)
}

@Http4kConnectAction
data class GetMessageBatchResults(val id: MessageBatchId) :
    AnthropicAIAction<Sequence<MessageBatchResponse>> {
    override fun toRequest() = Request(GET, "/v1/messages/batches/${id.value}/results")

    override fun toResult(response: Response) = toJsonLinesSequence(response, AnthropicAIMoshi)
}

@JsonSerializable
data class BatchRequest(val custom_id: CustomId, val params: MessageCompletion)

@JsonSerializable
data class MessageBatch(
    val id: MessageBatchId,
    val processing_status: ProcessingStatus,
    val request_counts: RequestCounts,
    val created_at: Instant,
    val expires_at: Instant,
    val results_url: Uri? = null,
    val ended_at: Instant? = null,
    val archived_at: Instant? = null,
    val cancel_initiated_at: Instant? = null,
    val type: String = "message_batch"
)

@JsonSerializable
data class MessageBatches(
    val `data`: List<MessageBatch>,
    val has_more: Boolean = false,
    val first_id: String? = null,
    val last_id: String? = null
) : Paged<String, MessageBatch> {
    override fun token() = last_id?.takeIf { has_more }
    override val items get() = `data`
}

@JsonSerializable
data class DeletedMessageBatch(val id: MessageBatchId, val type: String = "message_batch_deleted")

@JsonSerializable
data class RequestCounts(
    val processing: Int = 0,
    val succeeded: Int = 0,
    val errored: Int = 0,
    val canceled: Int = 0,
    val expired: Int = 0
)

@JsonSerializable
data class MessageBatchResponse(val custom_id: CustomId, val result: BatchResult)

@JsonSerializable
@Polymorphic("type")
sealed class BatchResult {
    @JsonSerializable
    @PolymorphicLabel("succeeded")
    data class Succeeded(val message: MessageCompletionResponse) : BatchResult()

    @JsonSerializable
    @PolymorphicLabel("errored")
    data class Errored(val error: Map<String, Any> = emptyMap()) : BatchResult()

    @JsonSerializable
    @PolymorphicLabel("canceled")
    data object Canceled : BatchResult()

    @JsonSerializable
    @PolymorphicLabel("expired")
    data object Expired : BatchResult()
}
