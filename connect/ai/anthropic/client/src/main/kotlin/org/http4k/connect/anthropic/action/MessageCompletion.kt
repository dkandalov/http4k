package org.http4k.connect.anthropic.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.ResponseId
import org.http4k.ai.model.Role
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.Temperature
import org.http4k.ai.model.UserPrompt
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.anthropic.AnthropicAIAction
import org.http4k.connect.anthropic.AnthropicAIMoshi
import org.http4k.connect.anthropic.Thinking
import org.http4k.connect.anthropic.ToolChoice
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
@ConsistentCopyVisibility
data class MessageCompletion internal constructor(
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
    val stream: Boolean
) : AnthropicAIAction<MessageCompletionResponse> {
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
        listOf(Message(Role.User, listOf(Content.Text(prompt.value)))),
        max_tokens, metadata, stop_sequences, system, temperature, tool_choice, tools, top_k, top_p, thinking, cache_control, output_config, container, inference_geo, service_tier, false
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
        max_tokens, metadata, stop_sequences, system, temperature, tool_choice, tools, top_k, top_p, thinking, cache_control, output_config, container, inference_geo, service_tier, false
    )

    constructor(model: ModelName, prompt: UserPrompt, max_tokens: MaxTokens) :
        this(model, prompt, max_tokens, null)

    constructor(model: ModelName, messages: List<Message>, max_tokens: MaxTokens) :
        this(model, messages, max_tokens, null)

    override fun toRequest() =
        Request(POST, "/v1/messages").with(AnthropicAIMoshi.autoBody<MessageCompletion>().toLens() of this)

    override fun toResult(response: Response) = when {
        response.status.successful -> Success(
            AnthropicAIMoshi.autoBody<MessageCompletionResponse>().toLens()(response)
        )

        else -> Failure(asRemoteFailure(response))
    }
}

@JsonSerializable
data class MessageCompletionResponse(
    val id: ResponseId,
    val role: Role,
    val content: List<Content>,
    val model: ModelName,
    val stop_reason: StopReason?,
    val stop_sequence: String?,
    val usage: Usage,
    val stop_details: RefusalDetails? = null,
    val container: Container? = null,
    val type: String = "message"
)
