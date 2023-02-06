package ru.yandex.market.logistics.calendaring.filter

import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.calendaring.base.SoftAssertionSupport

class RequestLoggingFilterTest : SoftAssertionSupport() {
    @Test
    fun testRegex() {
        val ans = "abobakekabobalel"
        val testString = "abobakekX-Ya-Service-Ticket  :  \"deleteMe\"abobalel"
        softly.assertThat(testString.replace(RequestLoggingFilter.serviceTicketRegex, "")).isEqualTo(ans)
        val testStringNoWs = "abobakekX-Ya-Service-Ticket:\"deleteMe\"abobalel"
        softly.assertThat(testStringNoWs.replace(RequestLoggingFilter.serviceTicketRegex, "")).isEqualTo(ans)
        val testStringLowerCase = "abobakekx-ya-service-Ticket:\"deleteMe\"abobalel"
        softly.assertThat(testStringLowerCase.replace(RequestLoggingFilter.serviceTicketRegex, "")).isEqualTo(ans)
    }
}
