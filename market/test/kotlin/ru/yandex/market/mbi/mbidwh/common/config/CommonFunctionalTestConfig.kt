package ru.yandex.market.mbi.mbidwh.common.config

import org.mockito.kotlin.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 19.04.2022
 */
@Configuration
open class CommonFunctionalTestConfig {

    @Bean
    @Profile("!localYt")
    open fun readWriteClient(): YtClientProxy = mock {}

    @Bean
    @Profile("!localYt")
    open fun readOnlyClient(): YtClientProxySource = mock {}

    @Bean
    open fun testClock(): Clock = Clock.fixed(
        LocalDate.of(2021, 10, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
        ZoneId.systemDefault()
    )

    @Bean
    open fun clock(): Clock = mock { }
}