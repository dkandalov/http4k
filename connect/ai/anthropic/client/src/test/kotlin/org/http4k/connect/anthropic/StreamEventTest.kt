package org.http4k.connect.anthropic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.model.MaxTokens
import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.Message
import org.http4k.connect.anthropic.action.MessageCompletionStream
import org.http4k.connect.anthropic.action.MessageGenerationEvent.Error
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class StreamEventTest {

    private val action = MessageCompletionStream(
        AnthropicModels.Claude_Sonnet_5,
        listOf(Message.User(Content.Text("hello"))),
        MaxTokens.of(10)
    )

    private fun eventsFrom(body: String) =
        action.toResult(Response(OK).body(body)).valueOrNull()!!.toList()

    @Test
    fun `an error event exposes its type and message`() {
        val events = eventsFrom(
            """
            event: error
            data: {"type": "error", "error": {"type": "overloaded_error", "message": "Overloaded"}}

            """.trimIndent()
        )

        assertThat(events.size, equalTo(1))
        assertThat((events[0] as Error).error.type, equalTo(ErrorCode.of("overloaded_error")))
        assertThat((events[0] as Error).error.message, equalTo("Overloaded"))
    }

    @Test
    fun `a payload split over several data lines is reassembled`() {
        val events = eventsFrom(
            """
            event: error
            data: {"type": "error", "error":
            data: {"type": "overloaded_error", "message": "Overloaded"}}

            """.trimIndent()
        )

        assertThat((events.single() as Error).error.message, equalTo("Overloaded"))
    }
}
