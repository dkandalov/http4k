package org.http4k.connect.anthropic.action

import org.http4k.ai.model.ModelName
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.Paged
import org.http4k.connect.PagedAction
import org.http4k.connect.anthropic.AnthropicAIAction
import org.http4k.connect.anthropic.AnthropicAIMoshi
import org.http4k.connect.kClass
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@Http4kConnectAction
data class GetModels(
    val limit: Int? = null,
    val after_id: String? = null,
    val before_id: String? = null
) : NonNullAutoMarshalledAction<Models>(kClass(), AnthropicAIMoshi), AnthropicAIAction<Models>,
    PagedAction<String, ModelInfo, Models, GetModels> {
    override fun next(token: String) = copy(after_id = token)

    override fun toRequest() = Request(GET, "/v1/models")
        .queries("limit" to limit?.toString(), "after_id" to after_id, "before_id" to before_id)
}

@Http4kConnectAction
data class GetModel(val model: ModelName) :
    NonNullAutoMarshalledAction<ModelInfo>(kClass(), AnthropicAIMoshi), AnthropicAIAction<ModelInfo> {
    override fun toRequest() = Request(GET, "/v1/models/${model.value}")
}

@JsonSerializable
data class Models(
    val `data`: List<ModelInfo>,
    val has_more: Boolean = false,
    val first_id: String? = null,
    val last_id: String? = null
) : Paged<String, ModelInfo> {
    override fun token() = last_id?.takeIf { has_more }
    override val items get() = `data`
}

@JsonSerializable
data class ModelInfo(
    val id: ModelName,
    val display_name: String,
    val created_at: Instant,
    val max_input_tokens: Int? = null,
    val max_tokens: Int? = null,
    val capabilities: ModelCapabilities? = null,
    val type: String = "model"
)

@JsonSerializable
data class ModelCapabilities(
    val batch: CapabilitySupport? = null,
    val citations: CapabilitySupport? = null,
    val code_execution: CapabilitySupport? = null,
    val image_input: CapabilitySupport? = null,
    val pdf_input: CapabilitySupport? = null,
    val structured_outputs: CapabilitySupport? = null,
    val context_management: ContextManagementCapability? = null,
    val effort: EffortCapability? = null,
    val thinking: ThinkingCapability? = null
)

@JsonSerializable
data class CapabilitySupport(val supported: Boolean)

@JsonSerializable
data class ContextManagementCapability(
    val supported: Boolean,
    val clear_tool_uses_20250919: CapabilitySupport? = null,
    val clear_thinking_20251015: CapabilitySupport? = null,
    val compact_20260112: CapabilitySupport? = null
)

@JsonSerializable
data class EffortCapability(
    val supported: Boolean,
    val low: CapabilitySupport? = null,
    val medium: CapabilitySupport? = null,
    val high: CapabilitySupport? = null,
    val xhigh: CapabilitySupport? = null,
    val max: CapabilitySupport? = null
)

@JsonSerializable
data class ThinkingCapability(val supported: Boolean, val types: ThinkingTypes? = null)

@JsonSerializable
data class ThinkingTypes(
    val adaptive: CapabilitySupport? = null,
    val enabled: CapabilitySupport? = null
)
