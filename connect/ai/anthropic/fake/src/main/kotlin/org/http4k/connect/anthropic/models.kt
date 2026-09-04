package org.http4k.connect.anthropic

import org.http4k.connect.anthropic.action.CapabilitySupport
import org.http4k.connect.anthropic.action.FileMetadata
import org.http4k.connect.anthropic.action.MessageBatch
import org.http4k.connect.anthropic.action.MessageBatchResponse
import org.http4k.connect.anthropic.action.ModelCapabilities
import org.http4k.connect.anthropic.action.ModelInfo
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import java.time.Instant

val DEFAULT_ANTHROPIC_MODELS = Storage.InMemory<ModelInfo>().apply {
    listOf(
        AnthropicModels.Claude_Opus_5 to "Claude Opus 5",
        AnthropicModels.Claude_Sonnet_5 to "Claude Sonnet 5",
        AnthropicModels.Claude_Haiku_4_5 to "Claude Haiku 4.5"
    ).forEach { (model, displayName) ->
        set(
            model.value,
            ModelInfo(
                model,
                displayName,
                Instant.parse("2026-01-01T00:00:00Z"),
                1000000,
                128000,
                ModelCapabilities(
                    image_input = CapabilitySupport(true),
                    structured_outputs = CapabilitySupport(true)
                )
            )
        )
    }
}

data class StoredBatch(
    val batch: MessageBatch,
    val responses: List<MessageBatchResponse>
)

data class StoredFile(
    val metadata: FileMetadata,
    val content: ByteArray
)
