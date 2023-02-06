package ru.yandex.market.wms.constraints.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles
import ru.yandex.market.wms.trace.Module
import ru.yandex.market.wms.trace.log.RequestTraceLog
import ru.yandex.market.wms.trace.log.RequestTraceLogBase
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset


@Configuration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
class ConstraintsTestConfig {

    @Bean
    fun requestTraceLog(applicationModuleName: Module): RequestTraceLog {
        return RequestTraceLogBase("$applicationModuleName-trace")
    }

    @Bean
    fun clock(clock: TestableClock): Clock = clock

    @Bean
    fun clock(): TestableClock {
        val clock = TestableClock()
        clock.setFixed(Instant.parse("2022-03-01T12:34:56.789Z"), ZoneOffset.UTC)
        return clock
    }
}
