package ru.yandex.market.logistics.les.client.configuration

import java.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.context.TestPropertySource
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import ru.yandex.market.logistics.les.client.component.sqs.SqsRequestTraceTskvLogger
import ru.yandex.market.logistics.les.client.component.sqs.TraceableMessagePostProcessor
import ru.yandex.market.logistics.les.client.configuration.properties.ProxyProperties
import ru.yandex.market.logistics.les.client.configuration.properties.TraceProperties
import ru.yandex.market.logistics.les.client.configuration.sqs.SqsConfiguration


@EnableScheduling
@Configuration
@Import(
    SqsConfiguration::class,
)
@ComponentScan(
    "ru.yandex.market.logistics.les.client.producer",
    "ru.yandex.market.logistics.les.client.component",
    "ru.yandex.market.logistics.les.client.configuration.properties"
)
@ConfigurationPropertiesScan("ru.yandex")
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
    open fun clock(): Clock = Clock.systemDefaultZone()

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
}
