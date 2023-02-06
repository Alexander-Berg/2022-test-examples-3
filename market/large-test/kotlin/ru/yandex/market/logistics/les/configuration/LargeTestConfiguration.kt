package ru.yandex.market.logistics.les.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.context.TestPropertySource
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import ru.yandex.alice.paskills.common.ydb.YdbClient
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.logistics.les.client.component.sqs.SqsRequestTraceTskvLogger
import ru.yandex.market.logistics.les.client.component.sqs.TraceableMessagePostProcessor
import ru.yandex.market.logistics.les.client.configuration.properties.ProxyProperties
import ru.yandex.market.logistics.les.client.configuration.properties.TraceProperties
import ru.yandex.market.logistics.les.client.configuration.sqs.SqsConfiguration
import ru.yandex.market.logistics.les.configuration.datasource.LiquibaseConfiguration
import ru.yandex.market.logistics.les.configuration.objectmapper.CryptoStringConfiguration
import ru.yandex.market.logistics.les.configuration.objectmapper.ObjectMapperConfiguration
import ru.yandex.market.logistics.les.configuration.ydb.StorageConfiguration
import ru.yandex.market.logistics.les.configuration.ydb.YdbIntegrationConfigurationLocal
import ru.yandex.market.logistics.les.entity.ydb.EventDaoConverter
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration
import ru.yandex.market.logistics.test.integration.db.container.EnablePostgreSQLContainer
import ru.yandex.market.ydb.integration.BaseYdbProperties
import ru.yandex.market.ydb.integration.Ydb
import ru.yandex.market.ydb.integration.YdbTemplate

@EnableScheduling
@EnableWebMvc
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Configuration
@EnablePostgreSQLContainer
@Import(
    DbUnitTestConfiguration::class,
    LiquibaseConfiguration::class,
    RepositoryConfiguration::class,
    DbQueueConfig::class,
    SqsConfiguration::class,
    CacheConfiguration::class,
    EventDaoConverter::class,
    ObjectMapperConfiguration::class,
    CryptoStringConfiguration::class,
    StorageConfiguration::class,
    YdbIntegrationConfigurationLocal::class,
    ExecutorConfiguration::class
)
@MockBean(
    YdbTemplate::class,
    YdbClient::class,
)
@SpyBean(
    TestableClock::class,
    BaseYdbProperties::class,
)
@ComponentScan(
    "ru.yandex.market.common.trace",
    "ru.yandex.market.logistics.les.sqs",
    "ru.yandex.market.logistics.les.controller",
    "ru.yandex.market.logistics.les.repository.ydb",
    "ru.yandex.market.logistics.les.service",
    "ru.yandex.market.logistics.les.queue",
    "ru.yandex.market.logistics.les.client.producer",
    "ru.yandex.market.logistics.les.client.component",
    "ru.yandex.market.logistics.les.client.configuration.properties"
)
@ConfigurationPropertiesScan("ru.yandex")
@EnableAutoConfiguration(exclude = [SecurityAutoConfiguration::class])
@TestPropertySource("classpath:large-test.properties")
open class LargeTestConfiguration {

    open class KGenericContainer(str: String) : GenericContainer<KGenericContainer>(str)

    companion object {
        const val SQS_PORT = 9324

        @Bean
        fun sqsContainer(): KGenericContainer {
            return KGenericContainer("graze/sqs-local:multiarch")
                .withEnv("HOSTNAME", DockerClientFactory.instance().dockerHostIpAddress())
                .withClasspathResourceMapping("elastic-mq.conf", "/var/lib/elasticmq/config.conf", BindMode.READ_ONLY)
                .withExposedPorts(SQS_PORT)
                .waitingFor(Wait.forHttp("/").forStatusCode(404))
        }
    }

    @Bean
    open fun traceableMessagePostProcessor(clock: Clock): TraceableMessagePostProcessor = TraceableMessagePostProcessor(clock)

    @Bean
    open fun sqsRequestTraceTskvLogger(properties: TraceProperties, clock: Clock): SqsRequestTraceTskvLogger = SqsRequestTraceTskvLogger(properties, clock, LoggerFactory.getLogger(LargeTestConfiguration::class.java))

    @Bean
    @Primary
    open fun sqsProxyProperties(sqsContainer: KGenericContainer): ProxyProperties {
        sqsContainer.start()

        return ProxyProperties(
            host = DockerClientFactory.instance().dockerHostIpAddress(),
            port = sqsContainer.getMappedPort(SQS_PORT)
        )
    }

    @Bean
    open fun clock(): Clock = TestableClock()

    @Ydb
    @MockBean
    lateinit var ydbObjectMapper: ObjectMapper
}
