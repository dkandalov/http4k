package org.http4k.aws

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.junit.Test
import java.nio.ByteBuffer

class AwsCanonicalRequestTest {

    @Test
    fun `creates canonical version of simple request`() {
        val canonical = AwsCanonicalRequest.of(Request(Method.GET, "http://www.google.com/a/b").query("foo", "bar").header("abc", "def"))
        canonical.value.shouldMatch(equalTo("""GET
/a/b
foo=bar
abc:def

abc
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"""))
    }

    @Test
    fun `normalises path`() {
        val canonical = AwsCanonicalRequest.of(Request(Method.GET, "http://www.google.com/a:b:c/d e/f"))
        canonical.value.shouldMatch(equalTo("""GET
/a%3Ab%3Ac/d+e/f




e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"""))
    }

    @Test
    fun `normalises empty path`() {
        val canonical = AwsCanonicalRequest.of(Request(Method.GET, "http://www.google.com"))
        canonical.value.shouldMatch(equalTo("""GET
/




e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"""))
    }

    @Test
    fun `generates payload hash of binary body`() {
        val image = this::class.java.getResourceAsStream("/test.png").readBytes()
        val canonical = AwsCanonicalRequest.of(Request(Method.GET, "http://www.google.com").body(Body(ByteBuffer.wrap(image))))
        canonical.value.shouldMatch(equalTo("""GET
/




0fa4d114b9fbb132f096d727713aab9ea8d415b69c86053b6d2c819c4eb95db6"""))
    }
}