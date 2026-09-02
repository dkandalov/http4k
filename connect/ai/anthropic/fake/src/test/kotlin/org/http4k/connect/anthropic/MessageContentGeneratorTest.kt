package org.http4k.connect.anthropic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.Content.Text
import org.http4k.connect.anthropic.action.Message
import org.junit.jupiter.api.Test

class MessageContentGeneratorTest {

    private val input = listOf(Message.User(Text("foobar")))

    @Test
    fun `reverse input`() {
        assertThat(MessageContentGenerator.ReverseInput(input), equalTo(listOf(Text("raboof") as Content)))
    }

    @Test
    fun `echo reflects a block the client does not model`() {
        val raw = """{"type":"future_block","note":"whatever"}"""

        assertThat(
            MessageContentGenerator.Echo(listOf(Message.User(Content.Unknown(raw)))),
            equalTo(listOf(Text(raw) as Content))
        )
    }
}
