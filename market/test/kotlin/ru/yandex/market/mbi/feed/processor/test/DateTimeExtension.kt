package ru.yandex.market.mbi.feed.processor.test

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
private const val MOSCOW_TIME_ZONE_STR: String = "Europe/Moscow"
private val MOSCOW_TIME_ZONE: ZoneId = ZoneId.of(MOSCOW_TIME_ZONE_STR)

fun toInstant(year: Int, month: Int, dayOfMonth: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): Instant {
    val dateTime: LocalDateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second)
    return dateTime.atZone(MOSCOW_TIME_ZONE).toInstant()
}
