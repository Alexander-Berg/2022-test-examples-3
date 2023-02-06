package ru.yandex.market.pricingmgmt.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class TestMockConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    open fun blackboxApiWireMockServer(): WireMockServer {
        return WireMockServer(
            WireMockConfiguration()
                .dynamicPort()
                .notifier(Slf4jNotifier(false))
        )
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    open fun papiWireMockServer(): WireMockServer {
        return WireMockServer(
            WireMockConfiguration()
                .dynamicPort()
                .notifier(Slf4jNotifier(false))
        )
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    open fun promoB2BWireMockServer(): WireMockServer {
        return WireMockServer(
            WireMockConfiguration()
                .dynamicPort()
                .notifier(Slf4jNotifier(false))
        )
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    open fun dataCampWireMockServer(): WireMockServer {
        return WireMockServer(
            WireMockConfiguration()
                .dynamicPort()
                .notifier(Slf4jNotifier(false))
        )
    }
}
