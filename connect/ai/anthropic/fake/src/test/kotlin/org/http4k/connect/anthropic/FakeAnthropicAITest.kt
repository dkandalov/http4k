package org.http4k.connect.anthropic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.MaxTokens
import org.http4k.connect.anthropic.action.BatchRequest
import org.http4k.connect.anthropic.action.BatchResult
import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.Message
import org.http4k.connect.anthropic.action.MessageCompletion
import org.http4k.connect.anthropic.action.MessageGenerationEvent.Delta
import org.http4k.connect.anthropic.action.MessageGenerationEvent.MessageDelta
import org.http4k.connect.anthropic.action.MessageGenerationEvent.StartBlock
import org.http4k.connect.anthropic.action.MessageGenerationEvent.StartMessage
import org.http4k.connect.anthropic.action.MessageGenerationEvent.Stop
import org.http4k.connect.anthropic.action.MessageGenerationEvent.StopMessage
import org.http4k.connect.successValue
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.filter.debug
import org.junit.jupiter.api.Test

class FakeAnthropicAITest : AnthropicAIContract {
    override val anthropicAi = AnthropicAI.Http(
        ApiKey.of("hello"),
        ApiVersion._2023_06_01,
        FakeAnthropicAI().debug()
    )

    @Test
    fun `a streamed response has the event structure a real one has`() {
        val events = anthropicAi.messageCompletionStream(
            AnthropicModels.Claude_Sonnet_5,
            listOf(Message.User(Content.Text("hello"))),
            MaxTokens.of(10)
        ).successValue().toList()

        assertThat(
            events.map { it::class },
            equalTo(
                listOf(
                    StartMessage::class,
                    StartBlock::class,
                    Delta::class,
                    Stop::class,
                    MessageDelta::class,
                    StopMessage::class
                )
            )
        )
    }

    @Test
    fun `can round trip a message batch`() {
        val batch = anthropicAi.createMessageBatch(
            listOf(
                BatchRequest(
                    CustomId.of("first"),
                    MessageCompletion(
                        AnthropicModels.Claude_Sonnet_5,
                        listOf(Message.User(Content.Text("hello"))),
                        MaxTokens.of(10)
                    )
                )
            )
        ).successValue()

        assertThat(batch.processing_status, equalTo(ProcessingStatus.ended))
        assertThat(batch.request_counts.succeeded, equalTo(1))

        assertThat(anthropicAi.getMessageBatch(batch.id).successValue(), equalTo(batch))
        assertThat(anthropicAi.listMessageBatches().successValue().data, equalTo(listOf(batch)))

        val results = anthropicAi.getMessageBatchResults(batch.id).successValue().toList()
        assertThat(results.map { it.custom_id }, equalTo(listOf(CustomId.of("first"))))
        assertThat(results.first().result is BatchResult.Succeeded, equalTo(true))

        assertThat(
            anthropicAi.cancelMessageBatch(batch.id).successValue().processing_status,
            equalTo(ProcessingStatus.canceling)
        )

        assertThat(anthropicAi.deleteMessageBatch(batch.id).successValue().id, equalTo(batch.id))
        assertThat(anthropicAi.listMessageBatches().successValue().data, equalTo(emptyList()))
    }

    @Test
    fun `can round trip a file`() {
        val uploaded = anthropicAi.uploadFile(
            FileName.of("hello.txt"),
            "hello world".byteInputStream(),
            TEXT_PLAIN
        ).successValue()

        assertThat(uploaded.filename, equalTo(FileName.of("hello.txt")))
        assertThat(uploaded.size_bytes, equalTo(11L))
        assertThat(uploaded.downloadable, equalTo(false))

        assertThat(anthropicAi.getFile(uploaded.id).successValue(), equalTo(uploaded))
        assertThat(anthropicAi.listFiles().successValue().data, equalTo(listOf(uploaded)))

        assertThat(
            anthropicAi.downloadFile(uploaded.id).successValue().reader().readText(),
            equalTo("hello world")
        )

        assertThat(anthropicAi.deleteFile(uploaded.id).successValue().id, equalTo(uploaded.id))
        assertThat(anthropicAi.listFiles().successValue().data, equalTo(emptyList()))
    }
}
