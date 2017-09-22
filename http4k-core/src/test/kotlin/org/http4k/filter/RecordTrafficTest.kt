package org.http4k.filter

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then

// 1. record and cache data
// 2. record and play back requests

fun main(args: Array<String>) {
    val next: HttpHandler = {
        Response(Status.OK).body("hello")
    }

    val disk = SimpleCachingFrom.Disk().then(next)

    val memory = SimpleCachingFrom.Memory { true }.then(next)

    val request = Request(Method.GET, "/asdasd/foo?asdas=asdasd").body("hello").header("asdas", "asdsad")
    println(disk(request))
    println(disk(request))

    println(memory(request))
    println(memory(request))

    CacheTrafficTo.Disk()
}