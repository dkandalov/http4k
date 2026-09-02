package org.http4k.connect.anthropic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.UserPrompt
import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.DeltaContent
import org.http4k.connect.anthropic.action.MessageCompletion
import org.junit.jupiter.api.Test

class ThinkingTest {

    @Test
    fun `thinking content is readable from a response`() {
        assertThat(
            AnthropicAIMoshi.asA(
                """{"type":"thinking","thinking":"working through it","signature":"sig"}""",
                Content::class
            ),
            equalTo(Content.Thinking("working through it", "sig") as Content)
        )
        assertThat(
            AnthropicAIMoshi.asA("""{"type":"redacted_thinking","data":"EvwBCkYYAiJA"}""", Content::class),
            equalTo(Content.RedactedThinking("EvwBCkYYAiJA") as Content)
        )
    }

    @Test
    fun `thinking deltas are modelled rather than passed through as unknown`() {
        assertThat(
            AnthropicAIMoshi.asA(
                """{"type":"thinking_delta","thinking":"1071 = 2 x 462 + 147"}""",
                DeltaContent::class
            ),
            !isA<DeltaContent.Unknown>()
        )
        assertThat(
            AnthropicAIMoshi.asA("""{"type":"signature_delta","signature":"EqQBCgIYAhIM"}""", DeltaContent::class),
            !isA<DeltaContent.Unknown>()
        )
    }

    @Test
    fun `a request can configure thinking`() {
        fun thinkingIn(thinking: Thinking) = AnthropicAIMoshi.asFormatString(
            MessageCompletion(
                AnthropicModels.Claude_Opus_5, UserPrompt.of("why?"), MaxTokens.of(10), thinking = thinking
            )
        )

        assertThat(
            thinkingIn(Thinking.Adaptive(ThinkingDisplay.summarized)),
            containsSubstring(""""thinking":{"type":"adaptive","display":"summarized"}""")
        )
        assertThat(thinkingIn(Thinking.Disabled), containsSubstring(""""thinking":{"type":"disabled"}"""))
    }
}
