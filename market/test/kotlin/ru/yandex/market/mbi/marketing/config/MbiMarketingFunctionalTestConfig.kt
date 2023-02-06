package ru.yandex.market.mbi.marketing.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import liquibase.integration.spring.SpringLiquibase
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import ru.yandex.common.cache.memcached.client.MemCachedClientFactory
import ru.yandex.market.common.test.mockito.MemCachedClientFactoryMock
import java.time.Clock
import java.time.Instant
import javax.annotation.PostConstruct
import javax.sql.DataSource

@Configuration
@Profile("functionalTest")
@Import(SpringApplicationConfig::class)
open class MbiMarketingFunctionalTestConfig {

    @PostConstruct
    open fun setSystemProperties() {
        System.setProperty("org.jooq.no-logo", "true")
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    open fun embeddedPostgres(): EmbeddedPostgres {
        return EmbeddedPostgres.builder()
            .start();
    }

    @Bean
    open fun liquibase(
        dataSource: DataSource,
        @Value("\${liquibase.changelog.unittest}") changeLog: String
    ): SpringLiquibase {
        val liquibase = SpringLiquibase()
        liquibase.dataSource = dataSource
        liquibase.setShouldRun(true)
        liquibase.changeLog = changeLog
        return liquibase
    }

    @Bean
    open fun clock(): Clock {
        val clock = mock(Clock::class.java)
        Mockito.`when`(clock.instant()).thenReturn(Instant.now())
        return clock
    }

    @Bean
    open fun memCachedClientFactory(): MemCachedClientFactory {
        return MemCachedClientFactoryMock()
    }

    @Bean(name = ["mbiMock"], initMethod = "start", destroyMethod = "stop")
    open fun mbiMock(): WireMockServer {
        return newWireMockServer()
    }

    open fun newWireMockServer(): WireMockServer {
        return WireMockServer(
            WireMockConfiguration()
                .dynamicPort()
                .notifier(ConsoleNotifier(true))
        )
    }

}
