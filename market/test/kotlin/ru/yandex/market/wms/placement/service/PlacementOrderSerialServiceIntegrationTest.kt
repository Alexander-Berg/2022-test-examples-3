package ru.yandex.market.wms.placement.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.wms.common.model.enums.NSqlConfigKey
import ru.yandex.market.wms.common.model.enums.NSqlConfigKey.YM_PLACEMENT_CONSTRAINTS
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory
import ru.yandex.market.wms.constraints.core.domain.RuleRestrictionType
import ru.yandex.market.wms.constraints.core.domain.SkuConstraintsViolation
import ru.yandex.market.wms.constraints.core.request.SkuInfo
import ru.yandex.market.wms.constraints.core.response.CheckByLocAndSkuResponse
import ru.yandex.market.wms.placement.config.PlacementIntegrationTest
import ru.yandex.market.wms.placement.dao.model.PlacementOrderStatus
import ru.yandex.market.wms.placement.dao.model.PlacementOrderType
import ru.yandex.market.wms.placement.exception.ConstraintViolationException
import ru.yandex.market.wms.placement.exception.ForbiddenPlacementLocException
import ru.yandex.market.wms.placement.exception.SerialsAlreadyPlacedException
import ru.yandex.market.wms.placement.exception.SerialsMismatchedException
import ru.yandex.market.wms.placement.model.dto.IdInfo
import ru.yandex.market.wms.placement.model.dto.PlacementOrderContent
import java.math.BigDecimal

class PlacementOrderSerialServiceIntegrationTest(
    @Autowired private val serialService: PlacementOrderSerialService
) : PlacementIntegrationTest() {

    @MockBean
    @Autowired
    private lateinit var coreService: CoreService

    @MockBean
    @Autowired
    private lateinit var dbConfigService: DbConfigService

    @BeforeEach
    fun cleanAndSetupDefaults() {
        reset(coreClient, coreService, constraintsClient)

        whenever(constraintsClient.checkByLocAndSku(anyString(), anyList()))
            .thenReturn(CheckByLocAndSkuResponse(true, emptyList()))
    }

    @Test
    @DatabaseSetup("/service/placement-order-serial/place-serials/part-of-id/before.xml")
    @ExpectedDatabase(
        value = "/service/placement-order-serial/place-serials/part-of-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `placeSerials - successfully placed part of id`() {
        setupAllSerialInventoriesBySerialNumbers(
            setOf("100", "101"),
            buildSerialInventories(id = "123", serialNumbers = listOf("100", "101"))
        )

        val expectedOrderContent = PlacementOrderContent(
            orderKey = 10, status = PlacementOrderStatus.IN_PROGRESS, orderType = PlacementOrderType.OPTIMIZATION,
            idList = listOf("123"), idInfoList = listOf(IdInfo("123", null, 3)), activeId = "123",
            uitCount = 3, uitPlacedCount = 2
        )
        val content = serialService.placeSerials(10, PlacementOrderType.OPTIMIZATION, setOf("100", "101"), "1-01")
        assertEquals(expectedOrderContent, content)
    }

    @Test
    @DatabaseSetup("/service/placement-order-serial/place-serials/full-id/before.xml")
    @ExpectedDatabase(
        value = "/service/placement-order-serial/place-serials/full-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `placeSerials - successfully placed full id`() {
        setupAllSerialInventoriesBySerialNumbers(
            setOf("100", "101"),
            buildSerialInventories(id = "123", serialNumbers = listOf("100", "101"))
        )

        whenever(dbConfigService.getConfig(Mockito.eq(NSqlConfigKey.YM_MORNING_CUTOFF_TIME), anyString()))
            .thenReturn("00:00:00")
        whenever(dbConfigService.getConfig(Mockito.eq(NSqlConfigKey.SHIFT_START_TIME), anyString()))
            .thenReturn("00:00:00")

        val expectedOrderContent = PlacementOrderContent(
            orderKey = 10, status = PlacementOrderStatus.IN_PROGRESS, orderType = PlacementOrderType.PLACEMENT,
            idList = listOf("123"), idInfoList = listOf(IdInfo("123", null, 3)), activeId = null,
            uitCount = 3, uitPlacedCount = 3
        )
        val content = serialService.placeSerials(10, PlacementOrderType.PLACEMENT, setOf("100", "101"), "1-01")
        assertEquals(expectedOrderContent, content)
    }

    @Test
    @DatabaseSetup("/service/placement-order-serial/place-serials/from-multiple-ids/before.xml")
    @ExpectedDatabase(
        value = "/service/placement-order-serial/place-serials/from-multiple-ids/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `placeSerials - successfully placed from multiple ids`() {
        val serialInventories = mutableListOf<SerialInventory>()
        serialInventories += buildSerialInventories(id = "123", serialNumbers = listOf("100"))
        serialInventories += buildSerialInventories(id = "124", serialNumbers = listOf("101"))

        setupAllSerialInventoriesBySerialNumbers(setOf("100", "101"), serialInventories)

        whenever(dbConfigService.getConfig(Mockito.eq(NSqlConfigKey.YM_MORNING_CUTOFF_TIME), anyString()))
            .thenReturn("00:00:00")
        whenever(dbConfigService.getConfig(Mockito.eq(NSqlConfigKey.SHIFT_START_TIME), anyString()))
            .thenReturn("00:00:00")

        val expectedOrderContent = PlacementOrderContent(
            orderKey = 10, status = PlacementOrderStatus.IN_PROGRESS, orderType = PlacementOrderType.PLACEMENT,
            idList = listOf("123", "124"),
            idInfoList = listOf("123", "124").map { IdInfo(it, null, 2) },
            activeId = null,
            uitCount = 4, uitPlacedCount = 3
        )
        val content = serialService.placeSerials(10, PlacementOrderType.PLACEMENT, setOf("100", "101"), "1-01")
        assertEquals(expectedOrderContent, content)
    }

    @Test
    @DatabaseSetup("/service/placement-order-serial/place-serials/already-placed/before.xml")
    fun `placeSerials - error because serials already placed`() {
        setupAllSerialInventoriesBySerialNumbers(
            setOf("100", "101"),
            buildSerialInventories(loc = "1-01", id = "123", serialNumbers = listOf("100", "101"))
        )

        assertThrows<SerialsAlreadyPlacedException> {
            serialService.placeSerials(10, PlacementOrderType.PLACEMENT, setOf("100", "101"), "1-01")
        }
    }

    @Test
    @DatabaseSetup("/service/placement-order-serial/place-serials/mismatched/before.xml")
    fun `placeSerials - error because serials mismatched with balances`() {
        val serialInventories = mutableListOf<SerialInventory>()
        serialInventories += buildSerialInventories(id = "123", serialNumbers = listOf("100"))
        serialInventories += buildSerialInventories(id = "124", serialNumbers = listOf("101"))

        setupAllSerialInventoriesBySerialNumbers(setOf("100", "101"), serialInventories)

        assertThrows<SerialsMismatchedException> {
            serialService.placeSerials(10, PlacementOrderType.PLACEMENT, setOf("100", "101"), "1-01")
        }
    }

    @Test
    @DatabaseSetup("/service/placement-order-serial/place-serials/cargotype-constraints/before.xml")
    fun `placeSerials - error because cargo type constraints are violated`() {
        val serialInventories =
            buildSerialInventories(id =  "123", serialNumbers = listOf("100", "101"), sku = "ROV123", storerKey = "465852")
        setupAllSerialInventoriesBySerialNumbers(setOf("100", "101"), serialInventories)

        whenever(dbConfigService.getConfigAsBoolean(YM_PLACEMENT_CONSTRAINTS))
            .thenReturn(true)
        whenever(constraintsClient.checkByLocAndSku(anyString(), anyList()))
            .thenReturn(
                CheckByLocAndSkuResponse(
                    false,
                    listOf(SkuConstraintsViolation("ROV123", "465852", RuleRestrictionType.CARGO_TYPE))
                )
            )

        val exception = assertThrows<ConstraintViolationException> {
            serialService.placeSerials(10, PlacementOrderType.PLACEMENT, setOf("100", "101"), "1-01")
        }

        assertEquals(
            "<div>100: Карготипы</div><div>101: Карготипы</div>",
            exception.wmsErrorData()["reasons"]
        )
        verify(constraintsClient)
            .checkByLocAndSku("1-01", listOf(SkuInfo("ROV123", "465852", 2)))
    }

    @Test
    @DatabaseSetup("/service/placement-order-serial/place-serials/forbidden-loc/before.xml")
    fun `placeSerials - error because loc in list of forbidden for placement`() {
        whenever(dbConfigService.getConfigAsStringList(NSqlConfigKey.YM_PLACEMENT_FORBIDDEN_LOCS))
            .thenReturn(listOf("LOST"))

        setupAllSerialInventoriesBySerialNumbers(
            setOf("100"), buildSerialInventories(id = "123", serialNumbers = listOf("100"))
        )

        assertThrows<ForbiddenPlacementLocException> {
            serialService.placeSerials(10, PlacementOrderType.OPTIMIZATION, setOf("100"), "LOST")
        }
    }

    private fun setupAllSerialInventoriesBySerialNumbers(serials: Set<String>, inventories: List<SerialInventory>) {
        whenever(coreService.getAllSerialInventoriesBySerialNumbers(serials))
            .thenReturn(inventories)
    }

    private fun buildSerialInventories(
        lot: String = "", loc: String = "PLACEMENT", id: String = "", serialNumbers: List<String>,
        sku: String = "", storerKey: String = ""
    ): List<SerialInventory> =
        serialNumbers.map {
            SerialInventory.builder()
                .sku("ROV123")
                .storerKey("465852")
                .serialNumber(it)
                .lot(lot)
                .loc(loc)
                .id(id)
                .sku(sku)
                .storerKey(storerKey)
                .quantity(BigDecimal.ONE)
                .build()
        }

}
