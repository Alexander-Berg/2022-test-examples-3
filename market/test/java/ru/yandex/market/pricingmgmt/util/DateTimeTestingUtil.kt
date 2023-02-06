package ru.yandex.market.pricingmgmt.util

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

object DateTimeTestingUtil {

    private val SYSTEM_ZONE: ZoneId = ZoneId.systemDefault()

    private fun createOffsetDateTime(dateTime: LocalDateTime): OffsetDateTime {
        if (dateTime.toEpochSecond(ZoneOffset.UTC) < 0) {
            throw IllegalArgumentException("Timezone conversion is broken for timestamps prior to Epoch Start")
        }

        return ZonedDateTime.of(dateTime, SYSTEM_ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC)
    }

    fun createOffsetDateTime(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int) =
        createOffsetDateTime(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second))

    private fun createJsonDateTime(dateTime: LocalDateTime): String =
        createOffsetDateTime(dateTime).format(DateTimeFormatter.ISO_DATE_TIME)

    fun createJsonDateTime(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int) =
        createJsonDateTime(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second))

    fun createDateFromOffsetDateTime(time: OffsetDateTime): Date = Date.from(time.toInstant())

    fun createDateFromLocalDateTime(dateTime: LocalDateTime): Date =
        createDateFromOffsetDateTime(createOffsetDateTime(dateTime))
}
