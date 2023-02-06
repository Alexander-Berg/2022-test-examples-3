package ru.yandex.market.logistics.cte.base

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import ru.yandex.market.logistics.cte.config.CacheConfiguration
import ru.yandex.market.logistics.cte.config.DataSourceConfig
import ru.yandex.market.logistics.cte.config.DbQueueLibraryConfig
import ru.yandex.market.logistics.cte.config.DbUnitConfig
import ru.yandex.market.logistics.cte.config.DefaultTmsDataSourceConfig
import ru.yandex.market.logistics.cte.config.JacksonConfiguration
import ru.yandex.market.logistics.cte.config.JpaConfig
import ru.yandex.market.logistics.cte.config.LiquibaseConfig
import ru.yandex.market.logistics.cte.config.MockConfiguration
import ru.yandex.market.logistics.cte.config.PostgreSQLContainerConfig
import ru.yandex.market.logistics.cte.config.ProtoClientConfiguration
import ru.yandex.market.logistics.cte.config.QueueShardConfiguration
import ru.yandex.market.logistics.cte.config.RestResponseEntityExceptionHandler
import ru.yandex.market.logistics.cte.config.ServiceCenterStatusFlowConfig
import ru.yandex.market.logistics.cte.config.TestClockConfig

@Configuration
@Import(
    PostgreSQLContainerConfig::class,
    DataSourceConfig::class,
    LiquibaseConfig::class,
    DbUnitConfig::class,
    DefaultTmsDataSourceConfig::class,
    JacksonConfiguration::class,
    JpaConfig::class,
    ProtoClientConfiguration::class,
    MockConfiguration::class,
    QueueShardConfiguration::class,
    TestClockConfig::class,
    DbQueueLibraryConfig::class,
    RestResponseEntityExceptionHandler::class,
    CacheConfiguration::class,
    ServiceCenterStatusFlowConfig::class
)
@EnableWebMvc
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(
    "ru.yandex.market.logistics.cte.service",
    "ru.yandex.market.logistics.cte.repo",
    "ru.yandex.market.logistics.cte.controller",
    "ru.yandex.market.logistics.cte.converters",
    "ru.yandex.market.logistics.cte.dbqueue.*",
    "ru.yandex.market.logistics.cte.dbqueue.property",
    "ru.yandex.market.logistics.cte.dbqueue.task"
)
@EnableAutoConfiguration(exclude = [SecurityAutoConfiguration::class])
class SecurityTestConfig
