package org.http4k.connect.anthropic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.model.ToolName
import org.http4k.connect.anthropic.action.Tool
import org.http4k.connect.anthropic.action.bash
import org.http4k.connect.anthropic.action.webSearch
import org.junit.jupiter.api.Test

class ToolTest {

    @Test
    fun `a built-in tool carries only the fields that are its own`() {
        assertThat(
            AnthropicAIMoshi.asFormatString(Tool.bash()),
            equalTo("""{"type":"bash_20250124","name":"bash"}""")
        )
        assertThat(
            AnthropicAIMoshi.asFormatString(Tool.webSearch(maxUses = 3, allowedDomains = listOf("example.com"))),
            equalTo(
                """{"type":"web_search_20260318","name":"web_search","max_uses":3,"allowed_domains":["example.com"]}"""
            )
        )
    }

    @Test
    fun `a user tool declares itself as custom`() {
        assertThat(
            AnthropicAIMoshi.asFormatString(
                Tool.User(ToolName.of("get_weather"), mapOf("type" to "object"), "Looks up the weather")
            ),
            equalTo(
                """{"name":"get_weather","input_schema":{"type":"object"},"description":"Looks up the weather","type":"custom"}"""
            )
        )
    }

    @Test
    fun `tools read back, including one this client does not know`() {
        assertThat(
            AnthropicAIMoshi.asA("""{"type":"bash_20250124","name":"bash"}""", Tool::class),
            equalTo(Tool.bash() as Tool)
        )
        assertThat(
            AnthropicAIMoshi.asA("""{"type":"brand_new_20270101","name":"brand_new"}""", Tool::class),
            equalTo(Tool.BuiltIn(ToolType.of("brand_new_20270101"), ToolName.of("brand_new")) as Tool)
        )
        assertThat(
            AnthropicAIMoshi.asA(
                """{"name":"get_weather","input_schema":{},"type":"custom"}""", Tool::class
            ),
            equalTo(Tool.User(ToolName.of("get_weather"), emptyMap()) as Tool)
        )
    }
}
