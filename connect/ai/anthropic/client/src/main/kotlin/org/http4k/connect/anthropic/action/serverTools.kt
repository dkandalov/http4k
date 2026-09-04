package org.http4k.connect.anthropic.action

import org.http4k.ai.model.ToolName
import org.http4k.connect.anthropic.ErrorCode
import org.http4k.connect.anthropic.FileId
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel
import java.time.Instant

sealed interface WebSearchContent

@JsonSerializable
data class WebSearchResults(val results: List<WebSearchResult>) : WebSearchContent

@JsonSerializable
data class WebSearchFailed(val error_code: ErrorCode, val type: String = "web_search_tool_result_error") :
    WebSearchContent

@JsonSerializable
data class WebSearchResult(
    val url: Uri,
    val title: String? = null,
    val page_age: String? = null,
    val encrypted_content: String? = null,
    val type: String = "web_search_result"
)

@JsonSerializable
@Polymorphic("type")
sealed class WebFetchContent {
    @JsonSerializable
    @PolymorphicLabel("web_fetch_result")
    data class Fetched(
        val url: Uri,
        val retrieved_at: Instant? = null,
        val content: Content? = null
    ) : WebFetchContent()

    @JsonSerializable
    @PolymorphicLabel("web_fetch_tool_result_error")
    data class Failed(val error_code: ErrorCode) : WebFetchContent()
}

@JsonSerializable
@Polymorphic("type")
sealed class CodeExecutionContent {
    @JsonSerializable
    @PolymorphicLabel("code_execution_result")
    data class Executed(
        val stdout: String? = null,
        val stderr: String? = null,
        val return_code: Int? = null,
        val content: List<CodeExecutionOutput> = emptyList()
    ) : CodeExecutionContent()

    @JsonSerializable
    @PolymorphicLabel("code_execution_tool_result_error")
    data class Failed(val error_code: ErrorCode) : CodeExecutionContent()
}

@JsonSerializable
data class CodeExecutionOutput(val file_id: FileId, val type: String = "code_execution_output")

@JsonSerializable
@Polymorphic("type")
sealed class BashCodeExecutionContent {
    @JsonSerializable
    @PolymorphicLabel("bash_code_execution_result")
    data class Executed(
        val stdout: String? = null,
        val stderr: String? = null,
        val return_code: Int? = null,
        val content: List<BashCodeExecutionOutput> = emptyList()
    ) : BashCodeExecutionContent()

    @JsonSerializable
    @PolymorphicLabel("bash_code_execution_tool_result_error")
    data class Failed(val error_code: ErrorCode) : BashCodeExecutionContent()
}

@JsonSerializable
data class BashCodeExecutionOutput(val file_id: FileId, val type: String = "bash_code_execution_output")

@JsonSerializable
@Polymorphic("type")
sealed class TextEditorCodeExecutionContent {
    @JsonSerializable
    @PolymorphicLabel("text_editor_code_execution_view_result")
    data class Viewed(
        val content: String,
        val file_type: String? = null,
        val num_lines: Int? = null,
        val start_line: Int? = null,
        val total_lines: Int? = null
    ) : TextEditorCodeExecutionContent()

    @JsonSerializable
    @PolymorphicLabel("text_editor_code_execution_create_result")
    data class Created(val is_file_update: Boolean) : TextEditorCodeExecutionContent()

    @JsonSerializable
    @PolymorphicLabel("text_editor_code_execution_str_replace_result")
    data class Replaced(
        val lines: List<String> = emptyList(),
        val old_start: Int? = null,
        val old_lines: Int? = null,
        val new_start: Int? = null,
        val new_lines: Int? = null
    ) : TextEditorCodeExecutionContent()

    @JsonSerializable
    @PolymorphicLabel("text_editor_code_execution_tool_result_error")
    data class Failed(val error_code: ErrorCode, val error_message: String? = null) :
        TextEditorCodeExecutionContent()
}

@JsonSerializable
@Polymorphic("type")
sealed class ToolSearchContent {
    @JsonSerializable
    @PolymorphicLabel("tool_search_tool_search_result")
    data class Found(val tool_references: List<ToolReference> = emptyList()) : ToolSearchContent()

    @JsonSerializable
    @PolymorphicLabel("tool_search_tool_result_error")
    data class Failed(val error_code: ErrorCode, val error_message: String? = null) : ToolSearchContent()
}

@JsonSerializable
data class ToolReference(val tool_name: ToolName, val type: String = "tool_reference")
