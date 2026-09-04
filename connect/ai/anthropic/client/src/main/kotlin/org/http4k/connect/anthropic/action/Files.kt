package org.http4k.connect.anthropic.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.Paged
import org.http4k.connect.PagedAction
import org.http4k.connect.anthropic.AnthropicAIAction
import org.http4k.connect.anthropic.AnthropicAIMoshi
import org.http4k.connect.anthropic.FileId
import org.http4k.connect.anthropic.FileName
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.kClass
import org.http4k.connect.model.MimeType
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.MultipartFormWithBoundary
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.MultipartFormBody
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.MultipartFormFile
import se.ansman.kotshi.JsonSerializable
import java.io.InputStream
import java.time.Instant

@Http4kConnectAction
data class UploadFile(
    val filename: FileName,
    val content: InputStream,
    val contentType: ContentType = OCTET_STREAM,
    val expires_in_seconds: Long? = null
) : NonNullAutoMarshalledAction<FileMetadata>(kClass(), AnthropicAIMoshi), AnthropicAIAction<FileMetadata> {
    override fun toRequest(): Request {
        val body = (MultipartFormBody() + ("file" to MultipartFormFile(filename.value, contentType, content)))
            .let { if (expires_in_seconds == null) it else it + ("expires_in_seconds" to expires_in_seconds.toString()) }

        return Request(POST, "/v1/files")
            .with(CONTENT_TYPE of MultipartFormWithBoundary(body.boundary))
            .body(body)
    }
}

@Http4kConnectAction
data class GetFile(val id: FileId) :
    NonNullAutoMarshalledAction<FileMetadata>(kClass(), AnthropicAIMoshi), AnthropicAIAction<FileMetadata> {
    override fun toRequest() = Request(GET, "/v1/files/${id.value}")
}

@Http4kConnectAction
data class DownloadFile(val id: FileId) : AnthropicAIAction<InputStream> {
    override fun toRequest() = Request(GET, "/v1/files/${id.value}/content")

    override fun toResult(response: Response) = when {
        response.status.successful -> Success(response.body.stream)
        else -> Failure(asRemoteFailure(response))
    }
}

@Http4kConnectAction
data class DeleteFile(val id: FileId) :
    NonNullAutoMarshalledAction<DeletedFile>(kClass(), AnthropicAIMoshi), AnthropicAIAction<DeletedFile> {
    override fun toRequest() = Request(DELETE, "/v1/files/${id.value}")
}

@Http4kConnectAction
data class ListFiles(
    val limit: Int? = null,
    val page: String? = null,
    val ids: List<FileId> = emptyList(),
    val scope_id: String? = null
) : NonNullAutoMarshalledAction<Files>(kClass(), AnthropicAIMoshi), AnthropicAIAction<Files>,
    PagedAction<String, FileMetadata, Files, ListFiles> {
    override fun next(token: String) = copy(page = token)

    override fun toRequest() = ids.fold(
        Request(GET, "/v1/files")
            .queries("limit" to limit?.toString(), "page" to page, "scope_id" to scope_id)
    ) { request, id -> request.query("ids[]", id.value) }
}

@JsonSerializable
data class FileMetadata(
    val id: FileId,
    val filename: FileName,
    val mime_type: MimeType,
    val size_bytes: Long,
    val created_at: Instant,
    val downloadable: Boolean = false,
    val expires_at: Instant? = null,
    val scope: FileScope? = null,
    val type: String = "file"
)

@JsonSerializable
data class FileScope(val type: String, val id: String)

@JsonSerializable
data class Files(val `data`: List<FileMetadata>, val next_page: String? = null) : Paged<String, FileMetadata> {
    override fun token() = next_page
    override val items get() = `data`
}

@JsonSerializable
data class DeletedFile(val id: FileId, val type: String = "file_deleted")
