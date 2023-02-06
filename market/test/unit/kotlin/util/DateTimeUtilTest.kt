package ru.yandex.market.logistics.calendaring.util

import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.calendaring.base.SoftAssertionSupport
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTimeUtilTest : SoftAssertionSupport() {

    @Test
    fun roundUpDateTimeTest() {
        val dateTime = ZonedDateTime.of(2021, 5, 17, 1, 1, 1, 1, ZoneId.of("UTC"))
        val roundUpDateTime = DateTimeUtil.roundUpDateTime(dateTime, 30)
        val expected = ZonedDateTime.of(2021, 5, 17, 1, 30, 0, 0, ZoneId.of("UTC")).toInstant()
        softly.assertThat(roundUpDateTime.toInstant()).isEqualTo(expected)
    }


    @Test
    fun roundUpDateTimeWhenMinuteZeroTest() {
        val dateTime = ZonedDateTime.of(2021, 5, 17, 1, 0, 1, 1, ZoneId.of("UTC"))
        val roundUpDateTime = DateTimeUtil.roundUpDateTime(dateTime, 30)
        val expected = ZonedDateTime.of(2021, 5, 17, 1, 0, 0, 0, ZoneId.of("UTC")).toInstant()
        softly.assertThat(roundUpDateTime.toInstant()).isEqualTo(expected)
    }

}
