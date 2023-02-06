package ru.yandex.market.transferact.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import ru.yandex.market.javaframework.main.config.TraceConfiguration
import ru.yandex.market.transferact.configuration.DomainConfiguration
import ru.yandex.market.transferact.configuration.RepositoryConfiguration
import ru.yandex.market.transferact.configuration.ServiceConfiguration
import ru.yandex.market.transferact.configuration.WebMvcConfiguration
import ru.yandex.market.transferact.configuration.dbqueue.DbQueueConfiguration

@Import(
    TraceConfiguration::class,
    EmbeddedPostgresConfiguration::class,
    RepositoryConfiguration::class,
    DomainConfiguration::class,
    ServiceConfiguration::class,
    WebMvcConfiguration::class,
    DbQueueConfiguration::class,
    MockIntegrationTestConfiguration::class,
    MockLesClientConfiguration::class,
    MockTvmConfiguration::class
)
@ComponentScan("ru.yandex.market.tpl.common.db.test")
open class TestConfiguration
