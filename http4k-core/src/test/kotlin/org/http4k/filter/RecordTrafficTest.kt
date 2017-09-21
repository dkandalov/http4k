package org.http4k.filter

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.TrafficStorage.Companion


fun main(args: Array<String>) {
    val next: HttpHandler = {
        Response(Status.OK).body("hello")
    }
    val disk = RecordTraffic.to(Companion.Disk()).then(next)

    val request = Request(Method.GET, "/asdasd/foo?asdas=asdasd").body("hello").header("asdas", "asdsad")

    val memory = RecordTraffic.to(TrafficStorage.Memory()).then(next)
    println(disk(request))
    println(disk(request))

    println(memory(request))
    println(memory(request))
}