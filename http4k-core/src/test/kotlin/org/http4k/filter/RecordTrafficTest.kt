package org.http4k.filter

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then


fun main(args: Array<String>) {
    val next: HttpHandler = {
        Response(Status.OK).body("hello")
    }

    val disk = TrafficCachingProxy.Disk().then(next)

    val memory = TrafficCachingProxy.Memory().then(next)

    val request = Request(Method.GET, "/asdasd/foo?asdas=asdasd").body("hello").header("asdas", "asdsad")
    println(disk(request))
    println(disk(request))

    println(memory(request))
    println(memory(request))
}