package ru.yandex.market.logistics.mqm.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.common.util.date.TestableClock

@Configuration
class ClockConfiguration {
    @Bean
    fun clock() = TestableClock()
}
