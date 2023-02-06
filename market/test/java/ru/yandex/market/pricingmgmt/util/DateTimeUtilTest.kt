package ru.yandex.market.pricingmgmt.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.OffsetDateTime

internal class DateTimeUtilTest {
    @ParameterizedTest
    @CsvSource(value = [",,UTC", "2022-05-19T14:29:20+03:00,1652959760,Europe/Moscow", "2022-05-19T11:29:20Z,1652959760,UTC"])
    fun getOffsetDateTimeFromEpochSecondTest(expected: String?, timestamp: Long?, timeZone: String){
        assertEquals(expected?.let { OffsetDateTime.parse(it) },
            DateTimeUtil.getOffsetDateTimeFromEpochSecond(timestamp, timeZone)
        )
    }
}
