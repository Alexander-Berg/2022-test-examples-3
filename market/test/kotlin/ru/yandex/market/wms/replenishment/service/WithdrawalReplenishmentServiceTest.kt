package ru.yandex.market.wms.replenishment.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.MessageHeaders
import ru.yandex.market.wms.common.model.dto.ReplenishmentCreateProblemOrdersRequestDto
import ru.yandex.market.wms.common.model.dto.ReplenishmentCreateTasksRequestDto
import ru.yandex.market.wms.common.model.enums.ReplenishmentType
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig
import ru.yandex.market.wms.common.spring.enums.replenishment.ProblemType
import ru.yandex.market.wms.common.spring.servicebus.model.dto.ProblemOrderDto
import ru.yandex.market.wms.replenishment.config.OrderReplenishmentTestConfig

@SpringBootTest(classes = [IntegrationTestConfig::class, OrderReplenishmentTestConfig::class])
class WithdrawalReplenishmentServiceTest : IntegrationTest() {
    @Autowired
    private lateinit var replenishmentAsyncService: ReplenishmentAsyncService

    private fun doReplenishment() {
        replenishmentAsyncService.receiveReplenishmentTaskRequest(
            ReplenishmentCreateTasksRequestDto(ReplenishmentType.WITHDRAWAL), MessageHeaders(emptyMap()),
        )
    }

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/1/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one order one item`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/1.1/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/1.1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `several orders one item`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/1.2/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/1.2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one order several items`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/2/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one order one item one holds`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/2.1/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/2.1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one order no items with required hold`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/2.2/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/2.2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one order one item several holds`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/2.3/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/2.3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one order several items several accepted holds`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/2.4/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/2.4/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one order not enough items`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/2.5/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/2.5/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one order one item one holds but it has exclusive`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/3/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one non hold order one item`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/3.1/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/3.1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `several non hold orders one item`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/3.2/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/3.2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one non hold order several items`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/3.3/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/3.3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `one non hold order not enough items`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/4/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/4/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `problem actualization`() = doReplenishment()

    @Test
    @DatabaseSetup("/service/withdrawal/create-tasks/5/before.xml")
    @ExpectedDatabase(
        value = "/service/withdrawal/create-tasks/5/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `problem add and replenish`() {
        replenishmentAsyncService.receiveReplenishmentProblemsAddRequest(
            ReplenishmentCreateProblemOrdersRequestDto(
                listOf(
                    ProblemOrderDto.builder()
                        .orderKey("00000002")
                        .qty(1)
                        .sku("SKU02")
                        .storerKey("STORER2")
                        .orderLineNumber("00001")
                        .type(ProblemType.OUT_OF_PICKING_STOCK)
                        .build()
                )
            ),
            MessageHeaders(emptyMap()),
        )
        doReplenishment()
    }



    @Test
    fun `empty test`() = doReplenishment()
}
