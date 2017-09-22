package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import org.http4k.core.then
import java.io.File
import java.util.*

object ServeCachedTrafficFrom {
    object Disk {
        operator fun invoke(baseDir: String = ".") = Filter { next ->
            {
                val file = File(it.toFolder(baseDir), "response.txt")
                if (file.exists()) Response.parse(String(file.readBytes())) else next(it)
            }
        }
    }

    object Memory {
        operator fun invoke(cache: Map<Request, Response>) = Filter { next ->
            {
                cache[it] ?: next(it)
            }
        }
    }
}

object CacheTrafficTo {

    object Disk {
        operator fun invoke(baseDir: String = ".", predicate: (HttpMessage) -> Boolean = { true }) = Filter { next ->
            {
                val requestFolder = it.toFolder(baseDir)
                requestFolder.mkdirs()

                if (predicate(it)) it.writeTo(File(requestFolder, "request.txt"))

                next(it).apply {
                    if (predicate(this)) this.writeTo(File(requestFolder, "request.txt"))
                }
            }
        }

        private fun HttpMessage.writeTo(file: File) {
            file.createNewFile()
            file.writeBytes(toString().toByteArray())
        }
    }

    object Memory {
        operator fun invoke(cache: MutableMap<Request, Response>, shouldSave: (HttpMessage) -> Boolean = { true }) = Filter { next ->
            { req: Request ->
                next(req).apply {
                    if (shouldSave(req) || shouldSave(this)) cache[req] = this
                }
            }
        }
    }
}

object SimpleCachingFrom {
    fun Disk(baseDir: String = ".", shouldSave: (HttpMessage) -> Boolean = { true }): Filter =
        ServeCachedTrafficFrom.Disk(baseDir).then(CacheTrafficTo.Disk(baseDir, shouldSave))

    fun Memory(shouldSave: (HttpMessage) -> Boolean = { true }): Filter {
        val cache = linkedMapOf<Request, Response>()
        return ServeCachedTrafficFrom.Memory(cache).then(CacheTrafficTo.Memory(cache, shouldSave))
    }
}

private fun Request.toFolder(baseDir: String) =
    File(File(if (baseDir.isEmpty()) "." else baseDir, uri.path), String(Base64.getEncoder().encode(toString().toByteArray())))
