package org.http4k.connect.anthropic.action

import dev.forkhandles.result4k.map
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Role
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.Temperature
import org.http4k.ai.model.UserPrompt
import org.http4k.ai.util.toSseSequence
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.anthropic.AnthropicAIAction
import org.http4k.connect.anthropic.AnthropicAIMoshi
import org.http4k.connect.anthropic.ErrorCode
import org.http4k.connect.anthropic.Thinking
import org.http4k.connect.anthropic.ToolChoice
import org.http4k.connect.anthropic.action.MessageGenerationEvent.Ping
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@Http4kConnectAction
@JsonSerializable
@ConsistentCopyVisibility
data class MessageCompletionStream internal constructor(
    val model: ModelName,
    val messages: List<Message>,
    val max_tokens: MaxTokens,
    val metadata: Metadata? = null,
    val stop_sequences: List<String> = emptyList(),
    val system: List<Content.Text>? = null,
    val temperature: Temperature? = null,
    val tool_choice: ToolChoice? = null,
    val tools: List<Tool> = emptyList(),
    val top_k: Int? = null,
    val top_p: Double? = null,
    val thinking: Thinking? = null,
    val cache_control: CacheControl? = null,
    val output_config: OutputConfig? = null,
    val container: ContainerSpec? = null,
    val inference_geo: InferenceGeo? = null,
    val service_tier: ServiceTier? = null,
    val stream: Boolean,
) : AnthropicAIAction<Sequence<MessageGenerationEvent>> {
    constructor(
        model: ModelName,
        prompt: UserPrompt,
        max_tokens: MaxTokens,
        metadata: Metadata? = null,
        stop_sequences: List<String> = emptyList(),
        system: List<Content.Text>? = null,
        temperature: Temperature? = null,
        tool_choice: ToolChoice? = null,
        tools: List<Tool> = emptyList(),
        top_k: Int? = null,
        top_p: Double? = null,
        thinking: Thinking? = null,
        cache_control: CacheControl? = null,
        output_config: OutputConfig? = null,
        container: ContainerSpec? = null,
        inference_geo: InferenceGeo? = null,
        service_tier: ServiceTier? = null,
    ) : this(
        model,
        listOf(
            Message(
                Role.User, listOf(Content.Text(prompt.value))
            )
        ),
        max_tokens,
        metadata,
        stop_sequences,
        system,
        temperature,
        tool_choice,
        tools,
        top_k,
        top_p,
        thinking,
        cache_control,
        output_config,
        container,
        inference_geo,
        service_tier,
        true
    )

    constructor(
        model: ModelName,
        messages: List<Message>,
        max_tokens: MaxTokens,
        metadata: Metadata? = null,
        stop_sequences: List<String> = emptyList(),
        system: List<Content.Text>? = null,
        temperature: Temperature? = null,
        tool_choice: ToolChoice? = null,
        tools: List<Tool> = emptyList(),
        top_k: Int? = null,
        top_p: Double? = null,
        thinking: Thinking? = null,
        cache_control: CacheControl? = null,
        output_config: OutputConfig? = null,
        container: ContainerSpec? = null,
        inference_geo: InferenceGeo? = null,
        service_tier: ServiceTier? = null,
    ) : this(
        model,
        messages,
        max_tokens,
        metadata,
        stop_sequences,
        system,
        temperature,
        tool_choice,
        tools,
        top_k,
        top_p,
        thinking,
        cache_control,
        output_config,
        container,
        inference_geo,
        service_tier,
        true
    )

    constructor(model: ModelName, prompt: UserPrompt, max_tokens: MaxTokens) :
        this(model, prompt, max_tokens, null)

    constructor(model: ModelName, messages: List<Message>, max_tokens: MaxTokens) :
        this(model, messages, max_tokens, null)

    override fun toRequest() =
        Request(POST, "/v1/messages").with(AnthropicAIMoshi.autoBody<MessageCompletionStream>().toLens() of this)

    override fun toResult(response: Response) =
        toSseSequence(response, AnthropicAIMoshi)
            .map { it.filterNot { it is Ping } }
}

@JsonSerializable
@Polymorphic("type")
sealed class MessageGenerationEvent {

    @JsonSerializable
    @PolymorphicLabel("message_start")
    data class StartMessage(val message: MessageCompletionResponse) : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("content_block_start")
    data class StartBlock(val index: Long, val content_block: DeltaContent) : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("content_block_delta")
    data class Delta(val index: Long, val delta: DeltaContent) : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("content_block_stop")
    data class Stop(val index: Long) : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("error")
    data class Error(val error: ErrorDetail) : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("message_stop")
    data object StopMessage : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("ping")
    data object Ping : MessageGenerationEvent()

    @JsonSerializable
    @PolymorphicLabel("message_delta")
    data class MessageDelta(val delta: MessageDeltaContent) : MessageGenerationEvent()

    @JsonSerializable
    @JsonDefaultValue
    data class Unknown(val raw: String) : MessageGenerationEvent()
}

@JsonSerializable
data class ErrorDetail(val type: ErrorCode, val message: String)

@JsonSerializable
data class MessageDeltaContent(
    val stop_reason: StopReason?,
    val stop_sequence: String?,
    val usage: Usage?
)

@JsonSerializable
@Polymorphic("type")
sealed class DeltaContent {
    @JsonSerializable
    @PolymorphicLabel("text")
    data class Text(val text: String) : DeltaContent()

    @JsonSerializable
    @PolymorphicLabel("text_delta")
    data class TextDelta(val text: String) : DeltaContent()

    @JsonSerializable
    @PolymorphicLabel("input_json_delta")
    data class Json(val partial_json: String) : DeltaContent()

    @JsonSerializable
    @PolymorphicLabel("thinking_delta")
    data class ThinkingDelta(val thinking: String, val estimated_tokens: Int? = null) : DeltaContent()

    @JsonSerializable
    @PolymorphicLabel("signature_delta")
    data class SignatureDelta(val signature: String) : DeltaContent()

    @JsonSerializable
    @JsonDefaultValue
    data class Unknown(val raw: String) : DeltaContent()
}
