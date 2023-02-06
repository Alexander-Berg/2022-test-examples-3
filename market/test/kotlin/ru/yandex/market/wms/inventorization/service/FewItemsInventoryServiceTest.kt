package ru.yandex.market.wms.inventorization.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.inventorization.core.model.FewItemsInventoryTaskRequest
import ru.yandex.market.wms.inventorization.dao.BalanceDao
import ru.yandex.market.wms.inventorization.dao.InventoryTaskDao
import ru.yandex.market.wms.inventorization.dao.PerformanceInventCycleDao
import ru.yandex.market.wms.inventorization.entity.PerformanceInventCycle
import ru.yandex.market.wms.inventorization.entity.SkuItemWithFewItems
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants
import java.time.LocalDateTime

class FewItemsInventoryServiceTest(
    @Autowired private val inventoryTaskDao: InventoryTaskDao,
    @Autowired private val securityDataProvider: ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider
) : IntegrationTest() {

    private val jmsTemplate = Mockito.mock(JmsTemplate::class.java)
    private val balanceDao = Mockito.mock(BalanceDao::class.java)
    private val performanceInventCycleDao: PerformanceInventCycleDao =
        Mockito.mock(PerformanceInventCycleDao::class.java)

    private lateinit var fewItemsInventoryService: FewItemsInventoryService

    private val performanceInventCycle = PerformanceInventCycle(
        id = 1,
        startTime = LocalDateTime.parse("2020-03-01T00:00:00"),
        endTime = LocalDateTime.parse("2020-06-01T00:00:00")
    )

    @BeforeEach
    fun setUp() {
        Mockito.`when`(performanceInventCycleDao.getCurrentInventCycle()).thenReturn(performanceInventCycle)
        fewItemsInventoryService = FewItemsInventoryService(
            inventoryTaskDao, jmsTemplate, balanceDao, securityDataProvider, performanceInventCycleDao
        )
    }

    @AfterEach
    fun tearDown() {
        Mockito.reset(jmsTemplate, performanceInventCycleDao)
    }

    @Test
    @ExpectedDatabase(
        value = "/few-items-inventory/create-task/no-inventory-query/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "wmwhseConnection"
    )
    fun testCreateFewItemsInventoryTaskCreatesTaskDetailWhenInventoryQueryDoesntPresent() {
        val request = FewItemsInventoryTaskRequest(
            loc = "C4-10-0001",
            sku = "ROV0000000000000000001",
            user = "anonymousUser"
        )
        fewItemsInventoryService.createFewItemsInventoryTask(request)
    }

    @Test
    @DatabaseSetup(
        value = ["/few-items-inventory/create-task/inventory-query-presents/before.xml"],
        connection = "wmwhseConnection"
    )
    @ExpectedDatabase(
        value = "/few-items-inventory/create-task/inventory-query-presents/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "wmwhseConnection"
    )
    fun testCreateFewItemsInventoryTaskCreatesTaskDetailWhenInventoryQueryPresents() {
        val request = FewItemsInventoryTaskRequest(
            loc = "C4-10-0001",
            sku = "ROV0000000000000000001",
            user = "anonymousUser"
        )
        fewItemsInventoryService.createFewItemsInventoryTask(request)
    }

    @Test
    fun testCreateFewItemsInventoryTaskCreatesTaskPushesMessageWhenThereIsTask() {
        Mockito.`when`(
            balanceDao.getSkuItemsWithFewItems(
                qty = 1,
                cycle = performanceInventCycle,
                limit = 5000,
                serialKeyOffset = 0
            )
        ).thenReturn(
            listOf(
                SkuItemWithFewItems(
                    serialKey = 1,
                    sku = "ROV0000000000000000001",
                    loc = "C4-10-0001",
                )
            )
        )

        val count: Int = fewItemsInventoryService.createFewItemsInventoryTasks(qty = 1)

        assertAll(
            { Assertions.assertEquals(1, count) },
            {
                Mockito.verify(jmsTemplate, Mockito.times(1))
                    .convertAndSend(
                        Mockito.eq(QueueNameConstants.FEW_ITEMS_INVENTORY_TASK),
                        Mockito.any<FewItemsInventoryTaskRequest>()
                    )
            },
            {
                Mockito.verify(jmsTemplate, Mockito.times(1))
                    .convertAndSend(
                        Mockito.eq(QueueNameConstants.FEW_ITEMS_INVENTORY_TASK),
                        Mockito.eq(
                            FewItemsInventoryTaskRequest(
                                loc = "C4-10-0001",
                                sku = "ROV0000000000000000001",
                                user = "anonymousUser"
                            )
                        )
                    )
            }
        )
    }

    @Test
    fun testCreateFewItemsInventoryTaskCreatesTaskDoesntPusheMessageWhenThereIsNoTask() {
        val count: Int = fewItemsInventoryService.createFewItemsInventoryTasks(qty = 1)

        assertAll(
            { Assertions.assertEquals(0, count) },
            {
                Mockito.verify(jmsTemplate, Mockito.times(0))
                    .convertAndSend(
                        Mockito.eq(QueueNameConstants.FEW_ITEMS_INVENTORY_TASK),
                        Mockito.any<FewItemsInventoryTaskRequest>()
                    )
            }
        )
    }
}
