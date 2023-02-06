package ru.yandex.market.logistics.les

import com.amazon.sqs.javamessaging.SQSConnectionFactory
import com.amazonaws.services.sqs.AmazonSQS
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader
import com.yandex.ydb.table.values.PrimitiveValue
import java.time.Instant
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import org.springframework.test.web.servlet.MockMvc
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.application.properties.utils.Environments
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.les.configuration.IntegrationTestConfiguration
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener
import ru.yandex.market.ydb.integration.Ydb
import ru.yandex.market.ydb.integration.YdbTemplate

@ExtendWith(
    SpringExtension::class,
    SoftAssertionsExtension::class,
)
@SpringBootTest(
    classes = [IntegrationTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["spring.config.name=integration-test"]
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
abstract class AbstractContextualTest {
    @Autowired
    protected lateinit var clock: TestableClock

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var sqsClient: AmazonSQS

    @MockBean
    lateinit var jmsTemplate: JmsTemplate

    @MockBean
    lateinit var sqsConnectionFactory: SQSConnectionFactory

    @MockBean
    lateinit var ydbTemplate: YdbTemplate

    @Ydb
    @MockBean
    lateinit var ydbObjectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    protected fun Long.toUint64() = PrimitiveValue.uint64(this)
}
