package ru.yandex.market.logistics.calendaring.base

import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi
import ru.yandex.market.logistics.calendaring.config.IntegrationTestConfig
import ru.yandex.market.logistics.calendaring.util.HibernateQueriesExecutionListener
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener
import ru.yandex.misc.db.embedded.ActivateEmbeddedPg
import java.util.*
import javax.servlet.Filter


@WebAppConfiguration
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension::class)
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    ResetDatabaseTestExecutionListener::class,
    DbUnitTestExecutionListener::class,
    MockitoTestExecutionListener::class,
    ResetMocksTestExecutionListener::class,
    HibernateQueriesExecutionListener::class
)
@ActiveProfiles(ActivateEmbeddedPg.EMBEDDED_PG)
@CleanDatabase
@DbUnitConfiguration
@TestPropertySource("classpath:application-test.properties")
abstract class IntegrationTest : SoftAssertionSupport() {

    protected var mockMvc: MockMvc? = null

    @Autowired
    private val wac: WebApplicationContext? = null

    @Autowired
    private val springSecurityFilterChain: Filter? = null

    @BeforeEach
    open fun setupMockMvc() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .addFilter<DefaultMockMvcBuilder>(springSecurityFilterChain)
            .build()
    }

    @Autowired
    protected var lmsClient: LMSClient? = null

    @Autowired
    protected var timeZone: TimeZone? = null

    @Autowired
    protected var ffwfClientApi: FulfillmentWorkflowClientApi? = null
}
