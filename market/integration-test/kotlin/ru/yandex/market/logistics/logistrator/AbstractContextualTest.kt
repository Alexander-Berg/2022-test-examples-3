package ru.yandex.market.logistics.logistrator

import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.context.WebApplicationContext
import ru.yandex.market.application.properties.utils.Environments
import ru.yandex.market.logistics.dbqueue.DbQueueService
import ru.yandex.market.logistics.logistrator.configuration.IntegrationTestConfiguration
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener
import java.time.Clock

@ExtendWith(
    SpringExtension::class,
    SoftAssertionsExtension::class,
)
@SpringBootTest(
    classes = [IntegrationTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["spring.config.name=integration-test"],
)
@AutoConfigureMockMvc
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    ResetDatabaseTestExecutionListener::class,
    TransactionalTestExecutionListener::class,
    DbUnitTestExecutionListener::class,
    MockitoTestExecutionListener::class,
    ResetMocksTestExecutionListener::class,
    WithSecurityContextTestExecutionListener::class,
)
@CleanDatabase
@DbUnitConfiguration(dataSetLoader = ReplacementDataSetLoader::class)
@ContextConfiguration
@ActiveProfiles(Environments.INTEGRATION_TEST)
@TestPropertySource("classpath:application-integration-test.properties")
abstract class AbstractContextualTest {

    @InjectSoftAssertions
    protected lateinit var softly: SoftAssertions

    @Autowired
    protected lateinit var context: WebApplicationContext

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var dbQueueService: DbQueueService

    @Autowired
    protected lateinit var lmsClient: LMSClient

    @Autowired
    protected lateinit var clock: Clock
}
