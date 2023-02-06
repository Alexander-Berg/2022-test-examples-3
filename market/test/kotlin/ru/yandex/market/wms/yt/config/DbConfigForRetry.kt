package ru.yandex.market.wms.yt.config

import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.retry.annotation.EnableRetry
import org.springframework.test.context.ActiveProfiles
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.spring.config.CommonConfig
import ru.yandex.market.wms.common.spring.config.HttpClientConfiguration
import ru.yandex.market.wms.common.spring.config.LoggingConfiguration
import ru.yandex.market.wms.common.spring.config.TraceLogConfiguration
import ru.yandex.market.wms.common.spring.controller.HealthCheckController
import ru.yandex.market.wms.common.spring.dao.implementation.CounterDao
import ru.yandex.market.wms.common.spring.service.CounterService
import ru.yandex.market.wms.common.spring.solomon.SolomonPushClient
import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao
import ru.yandex.market.wms.shared.libs.configproperties.dao.NSqlConfigDao
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles
import ru.yandex.market.wms.shared.libs.env.conifg.service.WmsInfoService
import ru.yandex.market.wms.yt.dao.SpringSnapshotDao
import ru.yandex.market.wms.yt.dao.YtTaskDao
import ru.yandex.market.wms.yt.provider.TestUserProvider
import java.time.Clock

@ActiveProfiles(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
@EnableRetry
@TestConfiguration
@PropertySource("classpath:solomon-test.properties")
@SpringBootApplication(
    scanBasePackages = [
        "ru.yandex.market.wms.common.spring.service.health.ping",
    ],
    exclude = [JdbcTemplateAutoConfiguration::class]
)
@Import(
    TraceLogConfiguration::class,
    HealthCheckController::class,
    LoggingConfiguration::class,
    HttpClientConfiguration::class,
    ru.yandex.market.wms.shared.libs.env.conifg.service.WmsInfoService::class,
    CommonConfig::class,
    SolomonPushClient::class,
    CounterService::class,
    CounterDao::class,
    NSqlConfigDao::class
)
open class DbConfigForRetry {
    @Bean("mockJdbc")
    fun namedParameterJdbcTemplate(): NamedParameterJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate::class.java)

    @Bean
    fun jdbcTemplate(): JdbcTemplate = Mockito.mock(JdbcTemplate::class.java)

    @Bean("securityDataProvider")
    fun securityDataProvider() = TestUserProvider()

    @Bean
    @Primary
    fun nSqlConfigDao(
        @Qualifier("securityDataProvider") securityDataProvider: TestUserProvider,
        namedParameterJdbcTemplate: NamedParameterJdbcTemplate?,
        clock: Clock?
    ): GlobalConfigurationDao =
        NSqlConfigDao(
            securityDataProvider,
            namedParameterJdbcTemplate,
            clock
        )

    @Bean
    @Primary
    open fun testDbConfigService(nSqlConfigDao: NSqlConfigDao) = DbConfigService(nSqlConfigDao)

    @Bean
    @Primary
    fun globalConfigurationDao(nSqlConfigDao: NSqlConfigDao): GlobalConfigurationDao = nSqlConfigDao

    @Bean
    @Primary
    fun ytTaskDao(@Qualifier("mockJdbc") namedParameterJdbcTemplate: NamedParameterJdbcTemplate, clock: Clock) =
        YtTaskDao(namedParameterJdbcTemplate, clock)

    @Bean
    @Primary
    fun springSnapshotDao(@Qualifier("mockJdbc") namedParameterJdbcTemplate: NamedParameterJdbcTemplate) =
        SpringSnapshotDao(namedParameterJdbcTemplate)
}
