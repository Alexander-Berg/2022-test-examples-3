package ru.yandex.market.mboui.s3

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class RequestPathParserTest {

    @Test
    fun index() {
        val parser = RequestPathParser("debug/index.html")
        Assertions.assertEquals(parser.canonicalPath, "/debug/index.html")
        Assertions.assertEquals(parser.appName, "debug")
        Assertions.assertEquals(parser.s3Prefix, "index.html")
    }
}
