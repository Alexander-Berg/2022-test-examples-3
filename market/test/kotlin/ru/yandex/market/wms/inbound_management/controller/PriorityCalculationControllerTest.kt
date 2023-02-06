package ru.yandex.market.wms.inbound_management.controller

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.inbound_management.controller.InboundManagementRequestPath.Companion.CALCULATE_PRIORITIES_PATH
import kotlin.test.Test

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PriorityCalculationControllerTest(context: WebApplicationContext) : IntegrationTest() {
    private val webTestClient = MockMvcWebTestClient.bindToApplicationContext(context).build()

    @Test
    @DatabaseSetups(
        DatabaseSetup("/service/before.xml", type = DatabaseOperation.INSERT),
        DatabaseSetup("/service/sku_oos.xml", type = DatabaseOperation.INSERT)
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            "/db/receipts-to-priorities/calculated.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),
    )
    fun calculate() {
        webTestClient.put()
            .uri(CALCULATE_PRIORITIES_PATH)
            .exchange()
            .expectStatus().isOk
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/1-row.xml"),
        DatabaseSetup("/service/before.xml"),
        DatabaseSetup("/service/sku_oos.xml")
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            "/db/receipts-to-priorities/calculated.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        ),
    )
    fun recalculate() {
        webTestClient.put()
            .uri(CALCULATE_PRIORITIES_PATH)
            .exchange()
            .expectStatus().isOk
    }
}
