package org.http4k.connect.anthropic.action

import org.http4k.ai.model.ToolName
import org.http4k.connect.anthropic.ToolType

fun Tool.Companion.bash() = Tool.BuiltIn(ToolType.of("bash_20250124"), ToolName.of("bash"))

fun Tool.Companion.memory() = Tool.BuiltIn(ToolType.of("memory_20250818"), ToolName.of("memory"))

fun Tool.Companion.codeExecution() =
    Tool.BuiltIn(ToolType.of("code_execution_20260521"), ToolName.of("code_execution"))

fun Tool.Companion.textEditor(maxCharacters: Int? = null) =
    Tool.BuiltIn(
        ToolType.of("text_editor_20250728"),
        ToolName.of("str_replace_based_edit_tool"),
        max_characters = maxCharacters
    )

fun Tool.Companion.webSearch(
    maxUses: Int? = null,
    allowedDomains: List<String>? = null,
    blockedDomains: List<String>? = null,
    userLocation: UserLocation? = null,
    responseInclusion: String? = null
) = Tool.BuiltIn(
    ToolType.of("web_search_20260318"),
    ToolName.of("web_search"),
    max_uses = maxUses,
    allowed_domains = allowedDomains,
    blocked_domains = blockedDomains,
    user_location = userLocation,
    response_inclusion = responseInclusion
)

fun Tool.Companion.webFetch(
    maxUses: Int? = null,
    maxContentTokens: Int? = null,
    citations: Citations? = null,
    allowedDomains: List<String>? = null,
    blockedDomains: List<String>? = null,
    useCache: Boolean? = null,
    responseInclusion: String? = null
) = Tool.BuiltIn(
    ToolType.of("web_fetch_20260318"),
    ToolName.of("web_fetch"),
    max_uses = maxUses,
    max_content_tokens = maxContentTokens,
    citations = citations,
    allowed_domains = allowedDomains,
    blocked_domains = blockedDomains,
    use_cache = useCache,
    response_inclusion = responseInclusion
)

fun Tool.Companion.toolSearchRegex() =
    Tool.BuiltIn(ToolType.of("tool_search_tool_regex_20251119"), ToolName.of("tool_search_tool_regex"))

fun Tool.Companion.toolSearchBm25() =
    Tool.BuiltIn(ToolType.of("tool_search_tool_bm25_20251119"), ToolName.of("tool_search_tool_bm25"))

fun Tool.Companion.computerToolset(configs: List<ToolConfig>? = null) =
    Tool.BuiltIn(ToolType.of("computer_toolset_20260801"), configs = configs)

fun Tool.Companion.browserToolset(configs: List<ToolConfig>? = null) =
    Tool.BuiltIn(ToolType.of("browser_toolset_20260801"), configs = configs)
