package ru.yandex.market.mboui.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class CookieUtilsTest {
    @Test
    fun test() {
        val sessionId = CookieUtils.parseSessionId("a=234; Session_id=asgfasdf2352.sdgs:sdg; b=sdgsdg")
        Assertions.assertEquals(sessionId, "asgfasdf2352.sdgs:sdg")
    }
}
