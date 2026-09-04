package org.http4k.connect.anthropic.endpoints

import org.http4k.connect.anthropic.AnthropicAIMoshi.autoBody
import org.http4k.connect.anthropic.FileId
import org.http4k.connect.anthropic.FileName
import org.http4k.connect.anthropic.StoredFile
import org.http4k.connect.anthropic.action.DeletedFile
import org.http4k.connect.anthropic.action.FileMetadata
import org.http4k.connect.anthropic.action.Files
import org.http4k.connect.model.MimeType
import org.http4k.connect.storage.Storage
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.MultipartFormBody
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock

fun files(files: Storage<StoredFile>, clock: Clock) = routes(
    "/v1/files" bind POST to { request ->
        val form = MultipartFormBody.from(request)
        val part = form.file("file")!!
        val content = part.content.readBytes()
        val id = FileId.of("file_" + content.contentHashCode().toString().replace("-", ""))

        val metadata = FileMetadata(
            id,
            FileName.of(part.filename),
            MimeType.of(part.contentType),
            content.size.toLong(),
            clock.instant(),
            expires_at = form.fieldValue("expires_in_seconds")?.let { clock.instant().plusSeconds(it.toLong()) }
        )

        files[id.value] = StoredFile(metadata, content)

        Response(OK).with(autoBody<FileMetadata>().toLens() of metadata)
    },

    "/v1/files" bind GET to {
        Response(OK).with(autoBody<Files>().toLens() of Files(files.all().map { it.metadata }))
    },

    "/v1/files/{id}/content" bind GET to { request ->
        files.withId(request) { _, it ->
            Response(OK).with(CONTENT_TYPE of OCTET_STREAM).body(it.content.inputStream(), it.content.size.toLong())
        }
    },

    "/v1/files/{id}" bind GET to { request ->
        files.withId(request) { _, it -> Response(OK).with(autoBody<FileMetadata>().toLens() of it.metadata) }
    },

    "/v1/files/{id}" bind DELETE to { request ->
        files.withId(request) { key, _ ->
            files.remove(key)
            Response(OK).with(autoBody<DeletedFile>().toLens() of DeletedFile(FileId.of(key)))
        }
    }
)
