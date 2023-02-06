package ru.yandex.market.logistics.les.client

import com.amazon.sqs.javamessaging.SQSConnectionFactory
import com.amazonaws.services.sqs.AmazonSQS
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import ru.yandex.market.logistics.les.client.configuration.LargeTestConfiguration
import ru.yandex.market.logistics.les.client.configuration.properties.SqsProperties

@ExtendWith(
    SpringExtension::class,
    SoftAssertionsExtension::class,
)
@SpringBootTest(
    classes = [LargeTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["spring.config.name=large-test"]
)
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    TransactionalTestExecutionListener::class,
    MockitoTestExecutionListener::class,
    ResetMocksTestExecutionListener::class
)
@ActiveProfiles(profiles = ["integration-test"])
abstract class AbstractContextualTest {

    @Autowired
    lateinit var jmsTemplate: JmsTemplate

    @SpyBean
    lateinit var client: AmazonSQS

    @Autowired
    lateinit var lesSqsProperties: SqsProperties

    @Autowired
    lateinit var sqsConnectionFactory: SQSConnectionFactory
}
