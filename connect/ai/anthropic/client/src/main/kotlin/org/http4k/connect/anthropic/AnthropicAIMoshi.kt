package org.http4k.connect.anthropic

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Buffer
import org.http4k.ai.util.withAiMappings
import org.http4k.connect.anthropic.action.ContainerId
import org.http4k.connect.anthropic.action.ContainerParams
import org.http4k.connect.anthropic.action.ContainerSpec
import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.DeltaContent
import org.http4k.connect.anthropic.action.InferenceGeo
import org.http4k.connect.anthropic.action.MessageGenerationEvent
import org.http4k.connect.anthropic.action.Tool
import org.http4k.connect.anthropic.action.UsageServiceTier
import org.http4k.connect.anthropic.action.WebSearchContent
import org.http4k.connect.anthropic.action.WebSearchFailed
import org.http4k.connect.anthropic.action.WebSearchResult
import org.http4k.connect.anthropic.action.WebSearchResults
import org.http4k.connect.model.Timestamp
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory
import java.lang.reflect.Type

object AnthropicAIMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(Content.Unknown::class.java, RawJsonAdapter({ it.raw }, Content::Unknown))
        .add(DeltaContent.Unknown::class.java, RawJsonAdapter({ it.raw }, DeltaContent::Unknown))
        .add(MessageGenerationEvent.Unknown::class.java, RawJsonAdapter({ it.raw }, MessageGenerationEvent::Unknown))
        .add(ToolAdapterFactory)
        .add(WebSearchContentAdapterFactory)
        .add(ContainerSpecAdapterFactory)
        .add(AnthropicAIJsonAdapterFactory)
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAiMappings()
        .value(ModelType)
        .value(ContainerId)
        .value(FileId)
        .value(FileName)
        .value(SkillId)
        .value(MessageBatchId)
        .value(CustomId)
        .value(ProcessingStatus)
        .value(ToolType)
        .value(ErrorCode)
        .value(InferenceGeo)
        .value(UsageServiceTier)
        .value(ToolUseId)
        .value(Timestamp)
        .value(UserId)
        .done()
)

@KotshiJsonAdapterFactory
object AnthropicAIJsonAdapterFactory : JsonAdapter.Factory by KotshiAnthropicAIJsonAdapterFactory

private class RawJsonAdapter<T : Any>(
    private val raw: (T) -> String,
    private val wrap: (String) -> T
) : JsonAdapter<T>() {
    override fun fromJson(reader: JsonReader) = wrap(reader.nextSource().use { it.readUtf8() })

    override fun toJson(writer: JsonWriter, value: T?) {
        value?.let { writer.value(Buffer().writeUtf8(raw(it))) } ?: writer.nullValue()
    }
}

private object ToolAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != Tool::class.java) return null
        val user = moshi.adapter(Tool.User::class.java)
        val builtIn = moshi.adapter(Tool.BuiltIn::class.java)

        return object : JsonAdapter<Tool>() {
            override fun fromJson(reader: JsonReader): Tool? = when (reader.declaredType()) {
                null, "custom" -> user.fromJson(reader)
                else -> builtIn.fromJson(reader)
            }

            override fun toJson(writer: JsonWriter, value: Tool?) = moshi.writeByType(writer, value)
        }.nullSafe()
    }

    private fun JsonReader.declaredType() = peekJson().use {
        it.setFailOnUnknown(false)
        (it.readJsonValue() as? Map<*, *>)?.get("type") as? String
    }
}

private object WebSearchContentAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != WebSearchContent::class.java) return null

        val results = moshi.adapter<List<WebSearchResult>>(
            Types.newParameterizedType(List::class.java, WebSearchResult::class.java)
        )
        val failed = moshi.adapter(WebSearchFailed::class.java)

        return object : JsonAdapter<WebSearchContent>() {
            override fun fromJson(reader: JsonReader) = when (reader.peek()) {
                JsonReader.Token.BEGIN_ARRAY -> WebSearchResults(results.fromJson(reader)!!)
                else -> failed.fromJson(reader)
            }

            override fun toJson(writer: JsonWriter, value: WebSearchContent?) {
                when (value) {
                    is WebSearchResults -> results.toJson(writer, value.results)
                    is WebSearchFailed -> failed.toJson(writer, value)
                    null -> writer.nullValue()
                }
            }
        }.nullSafe()
    }
}

private object ContainerSpecAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != ContainerSpec::class.java) return null

        val id = moshi.adapter(ContainerId::class.java)
        val params = moshi.adapter(ContainerParams::class.java)

        return object : JsonAdapter<ContainerSpec>() {
            override fun fromJson(reader: JsonReader) = when (reader.peek()) {
                JsonReader.Token.STRING -> id.fromJson(reader)
                else -> params.fromJson(reader)
            }

            override fun toJson(writer: JsonWriter, value: ContainerSpec?) = moshi.writeByType(writer, value)
        }.nullSafe()
    }
}

@Suppress("UNCHECKED_CAST")
private fun Moshi.writeByType(writer: JsonWriter, value: Any?) {
    value?.let { (adapter(it.javaClass) as JsonAdapter<Any>).toJson(writer, it) } ?: writer.nullValue()
}
