package ru.yandex.market.wms.constraints.config

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.support.DirtiesContextTestExecutionListener
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.wms.common.spring.BaseTest
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader
import ru.yandex.market.wms.constraints.scheduler.ScheduledJobs
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles

@SpringBootTest(classes = [DbConfig::class, ConstraintsTestConfig::class])
@ActiveProfiles(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
@Transactional
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    DirtiesContextTestExecutionListener::class,
    TransactionalTestExecutionListener::class,
    DbUnitTestExecutionListener::class
)
@AutoConfigureMockMvc
@DbUnitConfiguration(
    dataSetLoader = NullableColumnsDataSetLoader::class,
    databaseConnection = ["constraintsConnection"]
)
open class ConstraintsIntegrationTest : BaseTest() {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @MockBean
    @Autowired
    protected lateinit var coreClient: CoreClient

    protected val mapper = jsonMapper {
        addModule(kotlinModule())
        addModule(JavaTimeModule())
    }

    companion object {
        @Autowired
        private lateinit var scheduledAnnotationBeanPostProcessor: ScheduledAnnotationBeanPostProcessor

        @Autowired
        private lateinit var scheduledJobs: ScheduledJobs

        @BeforeAll
        fun beforeAll() {
            // stop scheduled tasks in tests
            scheduledAnnotationBeanPostProcessor.postProcessBeforeDestruction(scheduledJobs, "scheduledJobs")
        }
    }
}
