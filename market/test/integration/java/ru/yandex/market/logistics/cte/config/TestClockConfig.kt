package ru.yandex.market.logistics.cte.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Configuration
class TestClockConfig {
    @Bean
    @Primary
    fun clock(): Clock {
        return Clock.fixed(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneId.of("Europe/Moscow"))
    }
}
