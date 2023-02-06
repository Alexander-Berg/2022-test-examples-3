package ru.yandex.market.integration.npd

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Configuration
open class FunctionalTestConfig {

    @Bean
    open fun clock(): Clock {
        return Clock.fixed(
            OffsetDateTime.parse("2022-05-17T17:30:00+03:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant(),
            ZoneId.of("Europe/Moscow")
        )
    }
}
