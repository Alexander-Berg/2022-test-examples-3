package ru.yandex.market.logistics.calendaring.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Configuration
open class ClockTestConfig {

    @Bean
    open fun timeZone(): TimeZone {
        val defaultTimeZone: TimeZone = TimeZone.getTimeZone("UTC")
        TimeZone.setDefault(defaultTimeZone)
        return defaultTimeZone
    }

    @Bean
    open fun clock(timeZone: TimeZone): Clock {
        val now = LocalDateTime.of(2021, 5, 11, 12, 0)
        return Clock.fixed(now.toInstant(timeZone.toZoneId().rules.getOffset(now)), timeZone.toZoneId())
    }
}
