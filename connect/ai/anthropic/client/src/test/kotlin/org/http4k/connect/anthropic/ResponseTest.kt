package org.http4k.connect.anthropic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.model.StopReason
import org.http4k.connect.anthropic.action.BashCodeExecutionContent
import org.http4k.connect.anthropic.action.Citation
import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.MessageCompletionResponse
import org.http4k.connect.anthropic.action.RefusalCategory
import org.http4k.connect.anthropic.action.WebSearchFailed
import org.http4k.connect.anthropic.action.WebSearchResults
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class ResponseTest {

    @Test
    fun `a refusal reports why it was refused`() {
        val response = AnthropicAIMoshi.asA(
            """{"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5","content":[],
               "stop_reason":"refusal","stop_sequence":null,
               "stop_details":{"type":"refusal","category":"cyber","explanation":"nope"},
               "usage":{"input_tokens":1,"output_tokens":1}}""",
            MessageCompletionResponse::class
        )

        assertThat(response.stop_reason, equalTo(StopReason.refusal))
        assertThat(response.stop_details?.category, equalTo(RefusalCategory.cyber))
    }

    @Test
    fun `usage reports the cache breakdown and server tool calls`() {
        val response = AnthropicAIMoshi.asA(
            """{"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5","content":[],
               "stop_reason":"end_turn","stop_sequence":null,
               "usage":{"input_tokens":10,"output_tokens":5,
                        "cache_creation":{"ephemeral_5m_input_tokens":1024,"ephemeral_1h_input_tokens":0},
                        "output_tokens_details":{"thinking_tokens":320},
                        "server_tool_use":{"web_search_requests":2,"web_fetch_requests":0}}}""",
            MessageCompletionResponse::class
        )

        assertThat(response.usage.cache_creation?.ephemeral_5m_input_tokens, equalTo(1024))
        assertThat(response.usage.output_tokens_details?.thinking_tokens, equalTo(320))
        assertThat(response.usage.server_tool_use?.web_search_requests, equalTo(2))
    }

    @Test
    fun `web search results are readable`() {
        val block = AnthropicAIMoshi.asA(
            """{"type":"web_search_tool_result","tool_use_id":"srvtoolu_1",
                "content":[{"type":"web_search_result","url":"https://example.com","title":"Example"}]}""",
            Content::class
        ) as Content.WebSearchToolResult

        assertThat((block.content as WebSearchResults).results.single().url, equalTo(Uri.of("https://example.com")))
    }

    @Test
    fun `a failed web search reports its error code`() {
        val block = AnthropicAIMoshi.asA(
            """{"type":"web_search_tool_result","tool_use_id":"srvtoolu_1",
                "content":{"type":"web_search_tool_result_error","error_code":"max_uses_exceeded"}}""",
            Content::class
        ) as Content.WebSearchToolResult

        assertThat((block.content as WebSearchFailed).error_code, equalTo(ErrorCode.of("max_uses_exceeded")))
    }

    @Test
    fun `code execution output is readable`() {
        val block = AnthropicAIMoshi.asA(
            """{"type":"bash_code_execution_tool_result","tool_use_id":"srvtoolu_2",
                "content":{"type":"bash_code_execution_result","stdout":"5.5\n","stderr":"","return_code":0}}""",
            Content::class
        ) as Content.BashCodeExecutionToolResult

        assertThat((block.content as BashCodeExecutionContent.Executed).stdout, equalTo("5.5\n"))
    }

    @Test
    fun `a cited answer reports where it came from`() {
        val block = AnthropicAIMoshi.asA(
            """{"type":"text","text":"The grass is green.","citations":[
                {"type":"char_location","cited_text":"The grass is green. ","document_index":0,
                 "document_title":"My Document","start_char_index":0,"end_char_index":20}]}""",
            Content::class
        ) as Content.Text

        val citation = block.citations!!.single() as Citation.CharLocation
        assertThat(citation.document_title, equalTo("My Document"))
        assertThat(citation.end_char_index, equalTo(20))
    }
}
