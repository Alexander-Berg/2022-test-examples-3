package ru.yandex.market.logistics.calendaring.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import ru.yandex.market.logistics.calendaring.config.idm.IdmRoleSecurityConfigurationAdapter
import ru.yandex.market.logistics.test.integration.db.LiquibaseTestConfiguration
import ru.yandex.misc.db.embedded.ImportEmbeddedPg

@Configuration
@ImportEmbeddedPg
@Import(
    TestDataSourceConfiguration::class,
    LiquibaseTestConfiguration::class,
    DbUnitTestConfiguration::class,
    DbQueueProducersConfig::class,
    ClockTestConfig::class,
    JpaConfig::class,
    AppInfoConfig::class,
    MockConfig::class,
    GeobaseTimezoneConfig::class,
    RetryTemplateConfig::class,
    RestResponseEntityExceptionHandler::class,
    IdmRoleSecurityConfigurationAdapter::class,
    ThymeleafConfig::class
)
@ComponentScan(
    "ru.yandex.market.logistics.calendaring.controller",
    "ru.yandex.market.logistics.calendaring.service",
    "ru.yandex.market.logistics.calendaring.repository",
    "ru.yandex.market.logistics.calendaring.extension.converter",
    "ru.yandex.market.logistics.calendaring.solomon.repository",
    "ru.yandex.market.logistics.calendaring.solomon.metrics",
    "ru.yandex.market.logistics.calendaring.solomon.controller",
    "ru.yandex.market.logistics.calendaring.monitoring",
    "ru.yandex.market.logistics.calendaring.report",
    "ru.yandex.market.logistics.calendaring.meta",
    "ru.yandex.market.logistics.calendaring.booking",
    "ru.yandex.market.logistics.calendaring.dbqueue.producer",
)
@ActiveProfiles("integration_test")
open class IntegrationTestConfig
