package org.http4k.connect.anthropic.action

import org.http4k.ai.model.ModelName
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.anthropic.AnthropicAIAction
import org.http4k.connect.anthropic.AnthropicAIMoshi
import org.http4k.connect.anthropic.Thinking
import org.http4k.connect.anthropic.ToolChoice
import org.http4k.connect.kClass
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CountTokens(
    val model: ModelName,
    val messages: List<Message>,
    val system: List<Content.Text>? = null,
    val tools: List<Tool> = emptyList(),
    val tool_choice: ToolChoice? = null,
    val thinking: Thinking? = null,
    val output_config: OutputConfig? = null,
    val cache_control: CacheControl? = null
) : NonNullAutoMarshalledAction<TokenCount>(kClass(), AnthropicAIMoshi), AnthropicAIAction<TokenCount> {
    override fun toRequest() = Request(POST, "/v1/messages/count_tokens")
        .with(AnthropicAIMoshi.autoBody<CountTokens>().toLens() of this)
}

@JsonSerializable
data class TokenCount(val input_tokens: Int, val context_management: CountTokensContextManagement? = null)

@JsonSerializable
data class CountTokensContextManagement(val original_input_tokens: Int)
