package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.parse
import java.io.File
import java.util.*

interface TrafficStorage {
    operator fun get(req: Request): Response?
    operator fun set(req: Request, resp: Response)

    companion object {

        /**
         * Write selected traffic in a tree to the passed base folder (using base64 encoded requests as a key). Repeated traffic is returned
         * from the stored version. DOES NOT PROVIDE ANY INVALIDATION OR OTHER CACHE-CONTROL SEMANTICS.
         */
        object Disk {
            operator fun invoke(baseDir: String = ".", mode: RecordMode = RecordMode.All): TrafficStorage =
                object : TrafficStorage {
                    override fun get(req: Request): Response? {
                        val file = File(req.toFolder(baseDir), "response.txt")
                        return if (file.exists()) Response.parse(String(file.readBytes())) else null
                    }

                    override fun set(req: Request, resp: Response) {
                        val requestFolder = req.toFolder(baseDir)
                        requestFolder.mkdirs()
                        mode.store(File(requestFolder, "request.txt"), req)
                        mode.store(File(requestFolder, "response.txt"), resp)
                    }
                }

            enum class RecordMode(private vararg val record: RecordMode) {
                None(), RequestOnly(), ResponseOnly(), All(RequestOnly, ResponseOnly);

                fun store(file: File, r: org.http4k.core.Request) {
                    if (record.contains(RequestOnly)) r.writeTo(file)
                }

                fun store(file: File, r: org.http4k.core.Response) {
                    if (record.contains(ResponseOnly)) r.writeTo(file)
                }

                private fun HttpMessage.writeTo(file: File) {
                    file.createNewFile()
                    file.writeBytes(toString().toByteArray())
                }
            }
        }

        /**
         * Provide a simple in-memory store (using base64 encoded requests as a key). Useful for Testing, but
         * DOES NOT PROVIDE ANY INVALIDATION OR OTHER CACHE-CONTROL SEMANTICS.
         */
        fun Memory(): TrafficStorage {
            val cache = linkedMapOf<String, org.http4k.core.Response>()
            return object : TrafficStorage {
                override fun get(req: Request) = cache[req.identify()]

                override fun set(req: Request, resp: Response) {
                    cache[req.identify()] = resp
                }
            }
        }
    }
}

private fun Request.toFolder(baseDir: String): File = File(File(if (baseDir.isEmpty()) "." else baseDir, uri.path), this.identify())

private fun HttpMessage.identify() = String(Base64.getEncoder().encode(toString().toByteArray()))

object RecordTraffic {
    fun to(store: TrafficStorage) = Filter { next ->
        {
            store[it]?.let { it } ?: next(it).apply { store[it] = this }
        }
    }
}