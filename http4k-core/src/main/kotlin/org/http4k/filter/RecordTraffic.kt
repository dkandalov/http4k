package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import org.http4k.core.then
import org.http4k.filter.TrafficStore.RecordMode
import java.io.File
import java.util.*

object TrafficCache {
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

object TrafficStore {

    // replace with predicate...
    enum class RecordMode(private vararg val record: RecordMode) {
        None(), RequestOnly(), ResponseOnly(), All(RequestOnly, ResponseOnly);

        fun store(file: File, r: org.http4k.core.Request) {
            if (record.contains(RequestOnly)) r.writeTo(file)
        }

        fun store(file: File, r: Response) {
            if (record.contains(ResponseOnly)) r.writeTo(file)
        }

        private fun HttpMessage.writeTo(file: File) {
            file.createNewFile()
            file.writeBytes(toString().toByteArray())
        }
    }

    object Disk {
        operator fun invoke(baseDir: String = ".", mode: RecordMode = RecordMode.All) = Filter { next ->
            {
                val requestFolder = it.toFolder(baseDir)
                requestFolder.mkdirs()
                mode.store(File(requestFolder, "request.txt"), it)
                next(it).apply {
                    mode.store(File(requestFolder, "response.txt"), this)
                }
            }
        }
    }

    object Memory {
        operator fun invoke(cache: MutableMap<Request, Response>) = Filter { next ->
            {
                next(it).apply {
                    cache[it] = this
                }
            }
        }
    }
}

object TrafficCachingProxy {
    fun Disk(baseDir: String = ".", mode: RecordMode = RecordMode.All): Filter =
        TrafficCache.Disk(baseDir).then(TrafficStore.Disk(baseDir, mode))

    fun Memory(): Filter {
        val cache = linkedMapOf<Request, Response>()
        return TrafficCache.Memory(cache).then(TrafficStore.Memory(cache))
    }
}

private fun Request.toFolder(baseDir: String): File = File(File(if (baseDir.isEmpty()) "." else baseDir, uri.path), this.identify())

private fun HttpMessage.identify() = String(Base64.getEncoder().encode(toString().toByteArray()))
