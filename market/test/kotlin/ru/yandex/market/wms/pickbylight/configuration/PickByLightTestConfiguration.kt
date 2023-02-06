package ru.yandex.market.wms.pickbylight.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import ru.yandex.market.wms.common.spring.utils.uuid.TimeBasedGenerator
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles
import ru.yandex.market.wms.trace.Module
import ru.yandex.market.wms.trace.log.RequestTraceLog
import ru.yandex.market.wms.trace.log.RequestTraceLogBase
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Configuration
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
@Import(TimeBasedGenerator::class)
class PickByLightTestConfiguration {

    @Bean
    fun requestTraceLog(applicationModuleName: Module): RequestTraceLog =
        RequestTraceLogBase("$applicationModuleName-trace")

    @Bean
    fun clock(): Clock = Clock.fixed(Instant.parse("2020-04-01T12:34:56.789Z"), ZoneOffset.UTC)
}
