package org.http4k.ai.util

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.format.AutoMarshalling
import org.http4k.lens.Header
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence
import java.io.InputStream

inline fun <reified T : Any> Action<Result4k<Sequence<T>, RemoteFailure>>.toSseSequence(
    response: Response,
    autoMarshalling: AutoMarshalling,
    stopSignal: String? = null
) = when {
    response.status.successful -> Success(
        response.bodyAsSequenceOf<T>(autoMarshalling) { it.body.stream.sseData(stopSignal) }
    )

    else -> Failure(asRemoteFailure(response))
}

inline fun <reified T : Any> Action<Result4k<Sequence<T>, RemoteFailure>>.toJsonLinesSequence(
    response: Response,
    autoMarshalling: AutoMarshalling
) = when {
    response.status.successful -> Success(
        response.bodyAsSequenceOf<T>(autoMarshalling) {
            it.body.stream.bufferedReader().lineSequence().filter(String::isNotBlank)
        }
    )

    else -> Failure(asRemoteFailure(response))
}

@PublishedApi
internal fun InputStream.sseData(stopSignal: String?) = sequence {
    use { stream ->
        for (data in stream.chunkedSseSequence().mapNotNull(SseMessage::dataOrNull)) {
            if (data == stopSignal) break
            yield(data)
        }
    }
}

private fun SseMessage.dataOrNull() = when (this) {
    is SseMessage.Data -> data
    is SseMessage.Event -> data
    else -> null
}

@PublishedApi
internal inline fun <reified T : Any> Response.bodyAsSequenceOf(
    autoMarshalling: AutoMarshalling,
    chunks: (Response) -> Sequence<String>
): Sequence<T> = when {
    Header.CONTENT_TYPE(this)?.equalsIgnoringDirectives(ContentType.APPLICATION_JSON) == true ->
        sequenceOf(autoMarshalling.asA(bodyString()))

    else -> chunks(this).map { autoMarshalling.asA(it, T::class) }
}
