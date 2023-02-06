package ru.yandex.market.wms.placement.scheduler

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyCollection
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory
import ru.yandex.market.wms.placement.config.PlacementIntegrationTest
import ru.yandex.market.wms.placement.service.CoreService
import ru.yandex.market.wms.placement.service.PlacementOrderService
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset

class PlacementJobsTest(
    @Autowired private val scheduledJobs: ScheduledJobs,
    @Autowired private val testableClock: TestableClock,
) : PlacementIntegrationTest() {

    @MockBean
    @Autowired
    private lateinit var coreService: CoreService

    @SpyBean
    @Autowired
    private lateinit var placementOrderService: PlacementOrderService

    @BeforeEach
    fun clean() {
        reset(coreService)
    }

    @DatabaseSetup("/job/placement-archive/before-placement.xml")
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "/job/placement-archive/after-placement.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "/job/placement-archive/after-archive.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "archiveConnection"
        )
    )
    @Test
    fun `Archive only expired and finished placement orders`() {
        testableClock.setFixed(Instant.parse("2021-10-18T12:00:00Z"), ZoneOffset.UTC)
        scheduledJobs.executeArchive()
    }

    @DatabaseSetup("/job/clean-prepare-placement-orders/before.xml")
    @ExpectedDatabase(
        value = "/job/clean-prepare-placement-orders/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun `Clean old placement orders in prepare status`() {
        testableClock.setFixed(Instant.parse("2021-10-18T12:00:00Z"), ZoneOffset.UTC)
        scheduledJobs.executeCleanPrepareOrders()
    }

    @DatabaseSetup("/job/synchronize-orders/before.xml")
    @ExpectedDatabase(
        value = "/job/synchronize-orders/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun `synchronize orders`() {
        setupGetSerialInventoriesBySerialNumbers(
            mapOf("" to listOf("100", "101"), "RCP100" to listOf("102"))
        )

        testableClock.setFixed(Instant.parse("2021-10-18T12:00:00Z"), ZoneOffset.UTC)
        scheduledJobs.executeSynchronizeOrders()
        verify(placementOrderService).synchronizeOrder(99, "scheduler")
        verifyNoMoreInteractions(placementOrderService)
    }

    private fun setupGetSerialInventoriesBySerialNumbers(serialInventories: List<SerialInventory>) {
        `when`(coreService.getAllSerialInventoriesBySerialNumbers(anyCollection()))
            .thenReturn(serialInventories)
    }

    private fun setupGetSerialInventoriesBySerialNumbers(serialNumbersById: Map<String, List<String>>) {
        setupGetSerialInventoriesBySerialNumbers(
            serialNumbersById.flatMap { (id, serialNumbers) ->
                buildSerialInventories(id = id, serialNumbers = serialNumbers)
            }
        )
    }

    private fun buildSerialInventories(
        lot: String = "", loc: String = "PLACEMENT", id: String = "", serialNumbers: List<String>
    ): List<SerialInventory> {
        return serialNumbers.map {
            SerialInventory.builder()
                .serialNumber(it)
                .lot(lot)
                .loc(loc)
                .id(id)
                .quantity(BigDecimal.ONE)
                .build()
        }
    }
}
