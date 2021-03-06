package org.http4k.client

import org.http4k.core.Body
import java.io.InputStream
import java.nio.ByteBuffer

sealed class ResponseBodyMode : (InputStream) -> Body {

    object Memory : ResponseBodyMode() {
        override fun invoke(stream: InputStream): Body = stream.use { Body(ByteBuffer.wrap(it.readBytes())) }
    }

    object Stream : ResponseBodyMode() {
        override fun invoke(stream: InputStream): Body = Body(stream)
    }

}