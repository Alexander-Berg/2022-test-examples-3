package ru.yandex.market.logistics.les

import com.amazon.sqs.javamessaging.SQSConnectionFactory
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest
import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.market.application.properties.utils.Environments
import ru.yandex.market.logistics.les.client.configuration.properties.RedrivePolicy
import ru.yandex.market.logistics.les.client.configuration.properties.SqsProperties
import ru.yandex.market.logistics.les.configuration.LargeTestConfiguration
import ru.yandex.market.logistics.les.repository.ydb.YdbEntityRepository
import ru.yandex.market.logistics.les.repository.ydb.YdbEventRepository
import ru.yandex.market.logistics.les.service.FlagService
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener

@ExtendWith(
    SpringExtension::class,
    SoftAssertionsExtension::class,
)
@SpringBootTest(
    classes = [LargeTestConfiguration::class],
    webEnvironment = MOCK,
    properties = ["spring.config.name=large-test"]
)
@AutoConfigureMockMvc
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    ResetDatabaseTestExecutionListener::class,
    TransactionalTestExecutionListener::class,
    DbUnitTestExecutionListener::class,
    MockitoTestExecutionListener::class,
    ResetMocksTestExecutionListener::class
)
@CleanDatabase
@ActiveProfiles(profiles = [Environments.INTEGRATION_TEST])
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader::class)
abstract class AbstractLargeContextualTest {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @SpyBean
    lateinit var jmsTemplate: JmsTemplate

    @Autowired
    lateinit var client: AmazonSQS

    @Autowired
    lateinit var lesSqsProperties: SqsProperties

    @Autowired
    lateinit var sqsConnectionFactory: SQSConnectionFactory

    @MockBean
    lateinit var ydbEventRepository: YdbEventRepository

    @MockBean
    lateinit var ydbEntityRepository: YdbEntityRepository

    @SpyBean
    lateinit var flagService: FlagService

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    protected fun createQueues(client: AmazonSQS, lesSqsProperties: SqsProperties) {
        lesSqsProperties.queues.forEach { (_, queueName) -> createQueueWithDlq(client, lesSqsProperties, queueName) }
    }

    private fun createQueueWithDlq(client: AmazonSQS, lesSqsProperties: SqsProperties, queueName: String) {
        val dlqName = "${queueName}_dlq"
        val dlqUrl = client.createQueue(dlqName).queueUrl
        val queueUrl = client.createQueue(queueName).queueUrl

        val dlqAttrs = client.getQueueAttributes(
            GetQueueAttributesRequest(dlqUrl)
                .withAttributeNames("QueueArn"))

        val dlqArn = dlqAttrs.attributes["QueueArn"]

        val request = SetQueueAttributesRequest()
            .withQueueUrl(queueUrl)
            .addAttributesEntry("RedrivePolicy", RedrivePolicy(dlqArn!!, lesSqsProperties.maxReceiveCount).toString())

        client.setQueueAttributes(request)
    }

}
