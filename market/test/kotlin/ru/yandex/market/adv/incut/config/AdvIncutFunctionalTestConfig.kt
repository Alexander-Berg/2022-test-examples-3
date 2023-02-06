package ru.yandex.market.adv.incut.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.yandex.common.cache.memcached.client.MemCachedClientFactory
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.market.adv.incut.integration.saas.service.IncutSaasLogbrokerEvent
import ru.yandex.market.adv.incut.service.file.UuidFactory
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client
import ru.yandex.market.common.test.mockito.MemCachedClientFactoryMock
import ru.yandex.market.logbroker.LogbrokerEventPublisher
import java.time.Clock
import java.time.Instant
import javax.annotation.PostConstruct

@Configuration
@Profile("functionalTest")
open class AdvIncutFunctionalTestConfig {

    @PostConstruct
    open fun setSystemProperties() {
        System.setProperty("org.jooq.no-logo", "true")
    }

    @Bean
    open fun clock(): Clock {
        val clock = Mockito.mock(Clock::class.java)
        Mockito.`when`(clock.instant()).thenReturn(Instant.now())
        return clock
    }

    @Bean
    open fun uuidFactory(): UuidFactory = Mockito.mock(UuidFactory::class.java)

    @Bean
    open fun mdsS3IncutClient(): MdsS3Client {
        return Mockito.mock(MdsS3Client::class.java)
    }

    @Bean
    open fun memCachedClientFactory(): MemCachedClientFactory {
        return MemCachedClientFactoryMock()
    }

    @Bean(name = ["vendorPartnerMock"], initMethod = "start", destroyMethod = "stop")
    open fun vendorPartnerMock(): WireMockServer {
        return newWireMockServer()
    }

    @Bean(name = ["mediaAdvIncutSearchMock"], initMethod = "start", destroyMethod = "stop")
    open fun mediaAdvIncutSearchMock(): WireMockServer {
        return newWireMockServer()
    }

    @Bean
    open fun incutSaasLogbrokerEventPublisher()
        : LogbrokerEventPublisher<IncutSaasLogbrokerEvent> {
        return Mockito.mock(LogbrokerEventPublisher::class.java) as LogbrokerEventPublisher<IncutSaasLogbrokerEvent>
    }

    open fun newWireMockServer(): WireMockServer {
        return WireMockServer(
            WireMockConfiguration()
                .dynamicPort()
                .notifier(ConsoleNotifier(true))
        )
    }

    @Bean
    open fun ytHahnClient(): Yt {
        return Mockito.mock(Yt::class.java)
    }

    @Bean
    open fun ytNamedParameterJdbcTemplate(): NamedParameterJdbcTemplate {
        return Mockito.mock(NamedParameterJdbcTemplate::class.java)
    }
}
