package ru.yandex.market.logistics.les

import com.amazon.sqs.javamessaging.SQSConnectionFactory
import java.time.Instant
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
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
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.les.configuration.IntegrationTestConfiguration

@ExtendWith(
    SpringExtension::class,
    SoftAssertionsExtension::class,
)
@SpringBootTest(
    classes = [IntegrationTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["spring.config.name=integration-test"]
)
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    TransactionalTestExecutionListener::class,
    MockitoTestExecutionListener::class,
    ResetMocksTestExecutionListener::class
)
@ActiveProfiles(profiles = ["integration-test"])
abstract class AbstractContextualIntegrationTest {
    @Autowired
    protected lateinit var clock: TestableClock

    @MockBean
    lateinit var jmsTemplate: JmsTemplate

    @MockBean
    lateinit var sqsConnectionFactory: SQSConnectionFactory

    @BeforeEach
    fun setup() {
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }
}

