package org.http4k.connect.anthropic

import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.ModelName
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.anthropic.action.ModelInfo
import org.http4k.connect.anthropic.endpoints.countTokens
import org.http4k.connect.anthropic.endpoints.files
import org.http4k.connect.anthropic.endpoints.messageBatches
import org.http4k.connect.anthropic.endpoints.messageCompletion
import org.http4k.connect.anthropic.endpoints.models
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header
import org.http4k.routing.routes
import java.time.Clock

class FakeAnthropicAI(
    completionGenerators: Map<ModelName, MessageContentGenerator> = emptyMap(),
    val models: Storage<ModelInfo> = DEFAULT_ANTHROPIC_MODELS,
    val batches: Storage<StoredBatch> = Storage.InMemory(),
    val files: Storage<StoredFile> = Storage.InMemory(),
    clock: Clock = Clock.systemUTC()
) : ChaoticHttpHandler() {

    override val app =
        ServerFilters.ApiKeyAuth(Header.required("x-api-key"), { true })
            .then(
                routes(
                    messageCompletion(completionGenerators),
                    countTokens(),
                    models(models),
                    messageBatches(batches, completionGenerators, clock),
                    files(files, clock)
                )
            )

    /**
     * Convenience function to get AnthropicAI client
     */
    fun client() = AnthropicAI.Http(
        ApiKey.of("key"),
        ApiVersion._2023_06_01,
        this
    )
}

fun main() {
    FakeAnthropicAI().start()
}
