package org.http4k.connect.anthropic.action

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.connect.anthropic.FileId
import org.http4k.connect.anthropic.SkillId
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class Source {
    @JsonSerializable
    @PolymorphicLabel("base64")
    data class Base64(val data: Base64Blob, val media_type: MimeType) : Source()

    @JsonSerializable
    @PolymorphicLabel("url")
    data class Url(val url: Uri) : Source()

    @JsonSerializable
    @PolymorphicLabel("file")
    data class File(val file_id: FileId) : Source()
}

@JsonSerializable
@Polymorphic("type")
sealed class DocumentSource {
    @JsonSerializable
    @PolymorphicLabel("base64")
    data class Base64(val data: Base64Blob, val media_type: MimeType) : DocumentSource()

    @JsonSerializable
    @PolymorphicLabel("text")
    data class Text(val data: String, val media_type: MimeType) : DocumentSource()

    @JsonSerializable
    @PolymorphicLabel("content")
    data class Blocks(val content: List<Content.Text>) : DocumentSource()

    @JsonSerializable
    @PolymorphicLabel("url")
    data class Url(val url: Uri) : DocumentSource()

    @JsonSerializable
    @PolymorphicLabel("file")
    data class File(val file_id: FileId) : DocumentSource()
}

@JsonSerializable
data class ImageTransformations(val oversized_image: OversizedImage? = null)

enum class OversizedImage {
    downsize, error
}

@JsonSerializable
@Polymorphic("type")
sealed class Citation {
    @JsonSerializable
    @PolymorphicLabel("char_location")
    data class CharLocation(
        val cited_text: String,
        val document_index: Int,
        val start_char_index: Int,
        val end_char_index: Int,
        val document_title: String? = null,
        val file_id: FileId? = null
    ) : Citation()

    @JsonSerializable
    @PolymorphicLabel("page_location")
    data class PageLocation(
        val cited_text: String,
        val document_index: Int,
        val start_page_number: Int,
        val end_page_number: Int,
        val document_title: String? = null,
        val file_id: FileId? = null
    ) : Citation()

    @JsonSerializable
    @PolymorphicLabel("content_block_location")
    data class ContentBlockLocation(
        val cited_text: String,
        val document_index: Int,
        val start_block_index: Int,
        val end_block_index: Int,
        val document_title: String? = null,
        val file_id: FileId? = null
    ) : Citation()

    @JsonSerializable
    @PolymorphicLabel("web_search_result_location")
    data class WebSearchResultLocation(
        val cited_text: String,
        val encrypted_index: String,
        val url: Uri,
        val title: String? = null
    ) : Citation()

    @JsonSerializable
    @PolymorphicLabel("search_result_location")
    data class SearchResultLocation(
        val cited_text: String,
        val search_result_index: Int,
        val start_block_index: Int,
        val end_block_index: Int,
        val source: Uri,
        val title: String? = null
    ) : Citation()
}

sealed interface ContainerSpec

class ContainerId private constructor(value: String) : StringValue(value), ContainerSpec {
    companion object : NonBlankStringValueFactory<ContainerId>(::ContainerId)
}

@JsonSerializable
data class ContainerParams(val skills: List<Skill> = emptyList()) : ContainerSpec

@JsonSerializable
data class Skill(val skill_id: SkillId, val version: String? = null, val type: String = "anthropic")

class InferenceGeo private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<InferenceGeo>(::InferenceGeo) {
        val us = of("us")
        val global = of("global")
    }
}

enum class ServiceTier {
    auto, standard_only
}

class UsageServiceTier private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<UsageServiceTier>(::UsageServiceTier) {
        val standard = of("standard")
        val priority = of("priority")
        val batch = of("batch")
    }
}
