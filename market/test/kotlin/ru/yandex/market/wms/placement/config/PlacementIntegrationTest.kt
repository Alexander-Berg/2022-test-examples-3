package ru.yandex.market.wms.placement.config

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import org.intellij.lang.annotations.Language
import org.skyscreamer.jsonassert.JSONParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.support.DirtiesContextTestExecutionListener
import org.springframework.test.context.transaction.TransactionalTestExecutionListener
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.wms.common.spring.BaseTest
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader
import ru.yandex.market.wms.constraints.client.ConstraintsClient
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles

@SpringBootTest(classes = [DbConfig::class, PlacementTestConfig::class])
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
    databaseConnection = ["placementConnection", "archiveConnection"]
)
open class PlacementIntegrationTest : BaseTest() {

    protected val mapper = jsonMapper {
        addModule(kotlinModule())
        addModule(JavaTimeModule())
    }

    @MockBean
    @Autowired
    protected lateinit var coreClient: CoreClient

    @MockBean
    @Autowired
    protected lateinit var constraintsClient: ConstraintsClient

    @Autowired
    protected lateinit var mockMvc: MockMvc

    protected open fun json(@Language("json5") json5: String): String {
        return JSONParser.parseJSON(json5).toString()
    }

    protected fun json(vararg entry: Pair<String, Any?>): String =
        json(mapOf(*entry))

    private fun json(entries: Map<String, Any?>): String =
        mapper.writeValueAsString(entries)
}
