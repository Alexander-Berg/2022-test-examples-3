package ru.yandex.market.logistics.les.configuration

import org.slf4j.Logger
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.context.TestPropertySource
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.logistics.les.client.component.sqs.SqsRequestTraceTskvLogger
import ru.yandex.market.logistics.les.client.component.sqs.TraceableMessagePostProcessor
import ru.yandex.market.logistics.les.client.configuration.properties.SqsProperties
import ru.yandex.market.logistics.les.client.configuration.properties.TraceProperties
import ru.yandex.market.logistics.les.client.configuration.sqs.SqsConfiguration
import ru.yandex.market.logistics.les.client.producer.LesProducer
import ru.yandex.market.logistics.les.configuration.datasource.LiquibaseConfiguration
import ru.yandex.market.logistics.les.configuration.objectmapper.CryptoStringConfiguration
import ru.yandex.market.logistics.les.configuration.objectmapper.ObjectMapperConfiguration
import ru.yandex.market.logistics.les.configuration.ydb.MetricRegistryConfiguration
import ru.yandex.market.logistics.les.configuration.ydb.StorageConfiguration
import ru.yandex.market.logistics.les.configuration.ydb.YdbConfiguration
import ru.yandex.market.logistics.les.configuration.ydb.YdbIntegrationConfigurationLocal
import ru.yandex.market.logistics.les.entity.ydb.EventDaoConverter
import ru.yandex.market.logistics.les.mapper.component.AesCipherUtil
import ru.yandex.market.logistics.les.service.RoutingService
import ru.yandex.market.logistics.les.service.StringCipherUtil
import ru.yandex.market.logistics.les.service.YdbEventSavingService
import ru.yandex.market.logistics.les.service.queue.producer.DbEventProducer
import ru.yandex.market.logistics.les.service.sqs.InternalLesProducer
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres
import ru.yandex.market.ydb.integration.BaseYdbProperties
import ru.yandex.market.ydb.integration.YdbTemplate

@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Configuration
@EnableZonkyEmbeddedPostgres
@Import(
    ClockConfiguration::class,
    DbUnitTestConfiguration::class,
    LiquibaseConfiguration::class,
    MetricRegistryConfiguration::class,
    YdbConfiguration::class,
    RepositoryConfiguration::class,
    StorageConfiguration::class,
    DbQueueConfig::class,
    CacheConfiguration::class,
    EventDaoConverter::class,
    SecurityConfiguration::class,
    SqsConfiguration::class,
    ObjectMapperConfiguration::class,
    CryptoStringConfiguration::class,
    SqsProperties::class,
    YdbIntegrationConfigurationLocal::class,
    ExecutorConfiguration::class,
)
@MockBean(
    Logger::class,
    YdbTemplate::class
)
@SpyBean(
    TestableClock::class,
    TraceProperties::class,
    BaseYdbProperties::class,
    SqsProperties::class,
    TraceableMessagePostProcessor::class,
    SqsRequestTraceTskvLogger::class,
    AesCipherUtil::class,
    RoutingService::class,
    YdbEventSavingService::class,
    LesProducer::class,
    DbEventProducer::class,
    InternalLesProducer::class,
    StringCipherUtil::class,
)
@ComponentScan(
    "ru.yandex.market.logistics.les.converter",
    "ru.yandex.market.logistics.les.service",
    "ru.yandex.market.logistics.les.admin",
    "ru.yandex.market.logistics.les.entity.ydb",
    "ru.yandex.market.logistics.les.repository.ydb",
    "ru.yandex.market.logistics.les.client.producer",
    "ru.yandex.market.logistics.les.client.component",
)
@ConfigurationPropertiesScan("ru.yandex")
@EnableAutoConfiguration(exclude = [SecurityAutoConfiguration::class])
@TestPropertySource("classpath:integration-test.properties")
open class IntegrationTestConfiguration
