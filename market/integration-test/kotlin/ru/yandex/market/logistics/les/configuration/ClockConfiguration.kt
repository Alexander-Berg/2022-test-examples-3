package ru.yandex.market.logistics.les.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.common.util.date.TestableClock

@Configuration
open class ClockConfiguration {
    @Bean
    open fun clock() = TestableClock()
}
