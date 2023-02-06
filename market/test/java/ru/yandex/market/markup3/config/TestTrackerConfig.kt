package ru.yandex.market.markup3.config

import com.nhaarman.mockitokotlin2.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import ru.yandex.startrek.client.Session

@TestConfiguration
open class TestTrackerConfig {
    @Bean
    open fun trackerSession(): Session {
        return mock()
    }
}
