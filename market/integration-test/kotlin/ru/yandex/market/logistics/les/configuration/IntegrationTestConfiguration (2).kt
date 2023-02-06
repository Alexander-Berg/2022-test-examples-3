package ru.yandex.market.logistics.les.configuration

import com.amazonaws.services.sqs.AmazonSQS
import org.slf4j.Logger
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.logistics.les.client.component.sqs.SqsRequestTraceTskvLogger
import ru.yandex.market.logistics.les.client.component.sqs.TraceableMessagePostProcessor
import ru.yandex.market.logistics.les.client.configuration.properties.SqsProperties
import ru.yandex.market.logistics.les.client.configuration.properties.TraceProperties
import ru.yandex.market.logistics.les.client.configuration.sqs.SqsConfiguration

@Configuration
@Import(
    SqsConfiguration::class,
)
@MockBean(
    Logger::class,
    AmazonSQS::class
)
@SpyBean(
    TestableClock::class,
    TraceProperties::class,
    TraceableMessagePostProcessor::class,
    SqsRequestTraceTskvLogger::class,
    SqsProperties::class,
)
@ComponentScan(
    "ru.yandex.market.logistics.les.client.producer",
    "ru.yandex.market.logistics.les.client.component",
    "ru.yandex.market.logistics.les.client.configuration.properties"
)
@ConfigurationPropertiesScan("ru.yandex")
@TestPropertySource("classpath:integration-test.properties")
open class IntegrationTestConfiguration
