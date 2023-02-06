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
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.wms.common.model.enums.NSqlConfigKey
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
import ru.yandex.market.wms.placement.exception.IdAlreadyLostException
import ru.yandex.market.wms.placement.exception.IdAlreadyPlacedException
import ru.yandex.market.wms.placement.exception.IdNotAddedException
import ru.yandex.market.wms.placement.exception.IdPlacedMultipleLocException
import ru.yandex.market.wms.placement.exception.PlacementOrderNotFoundException
import ru.yandex.market.wms.placement.exception.SerialNotInPlacementLocException
import ru.yandex.market.wms.placement.model.dto.IdInfo
import ru.yandex.market.wms.placement.model.dto.PlacementOrderContent
import java.math.BigDecimal

class PlacementOrderIdServiceIntegrationTest(
    @Autowired private val placementOrderIdService: PlacementOrderIdService
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

        whenever((constraintsClient.checkByLocAndSku(anyString(), anyList())))
            .thenReturn(CheckByLocAndSkuResponse(true, emptyList()))
    }

    @Test
    @DatabaseSetup("/service/placement-order-id/place-id/ok/before.xml")
    @ExpectedDatabase(
        "/service/placement-order-id/place-id/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `placeId - successfully`() {
        whenever(dbConfigService.getConfig(NSqlConfigKey.YM_MORNING_CUTOFF_TIME, "00:00:00")).thenReturn("00:00:00")
        whenever(dbConfigService.getConfig(NSqlConfigKey.SHIFT_START_TIME, "00:00:00")).thenReturn("00:00:00")

        val expectedOrderContent = PlacementOrderContent(
            orderKey = 1,
            status = PlacementOrderStatus.IN_PROGRESS,
            orderType = PlacementOrderType.PLACEMENT,
            idList = listOf("123", "124"),
            idInfoList = listOf(IdInfo("123", null, 2), IdInfo("124", null, 1)),
            activeId = null,
            uitCount = 3,
            uitPlacedCount = 2
        )

        setupSerialInventoriesById(loc = "PLACEMENT", id = "123", serialNumbers = listOf("0000000100", "0000000101"))

        // another type of order
        assertThrows<PlacementOrderNotFoundException> {
            placementOrderIdService.placeId(1, PlacementOrderType.OPTIMIZATION, "123", "1-01")
        }

        val content = placementOrderIdService.placeId(1, PlacementOrderType.PLACEMENT, "123", "1-01")

        assertEquals(expectedOrderContent, content)
    }

    @Test
    @DatabaseSetup("/service/placement-order-id/place-id/id-not-added/before.xml")
    fun `placeId - id not added`() {
        assertThrows<IdNotAddedException> {
            placementOrderIdService.placeId(1, PlacementOrderType.PLACEMENT, "PLT101", "1-01")
        }
    }

    @Test
    @DatabaseSetup("/service/placement-order-id/place-id/not-in-placement-loc/before.xml")
    fun `placeId  - id not in placement loc`() {
        setupSerialInventoriesById(loc = "LOC", id = "123", serialNumbers = listOf("0000000100"))

        val exception = assertThrows<SerialNotInPlacementLocException> {
            placementOrderIdService.placeId(1, PlacementOrderType.PLACEMENT, "123", "1-01")
        }
        assertEquals("0000000100", exception.wmsErrorData()["uit"])
        assertEquals("LOC", exception.wmsErrorData()["loc"])
    }

    @Test
    @DatabaseSetup("/service/placement-order-id/place-id/serials-mismatched/before.xml")
    @ExpectedDatabase(
        "/service/placement-order-id/place-id/serials-mismatched/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `placeId - serials are mismatched in serialinventory and placement DB`() {
        // в балансах другой серийник
        // серийник в задании будет удален, новый добавлен
        setupSerialInventoriesById(loc = "PLACEMENT", id = "123", serialNumbers = listOf("0000000101"))
        placementOrderIdService.placeId(1, PlacementOrderType.OPTIMIZATION, "123", "1-01")
    }

    @Test
    @DatabaseSetup("/service/placement-order-id/place-id/already-placed/before.xml")
    @ExpectedDatabase(
        "/service/placement-order-id/place-id/already-placed/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `placeId - id had been already placed`() {
        setupSerialInventoriesById(loc = "1-01", id = "123", serialNumbers = listOf("0000000100", "0000000101"))

        assertThrows<IdAlreadyPlacedException> {
            placementOrderIdService.placeId(1, PlacementOrderType.OPTIMIZATION, "123", "1-01")
        }
    }

    @Test
    @DatabaseSetup("/service/placement-order-id/place-id/placed-multiple-locs/before.xml")
    @ExpectedDatabase(
        "/service/placement-order-id/place-id/placed-multiple-locs/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `placeId - id had been already placed in multiple locs`() {
        setupSerialInventoriesById(
            id = "123",
            serialAndLoc = listOf(Pair("0000000100", "1-01"), Pair("0000000101", "1-02"))
        )

        assertThrows<IdPlacedMultipleLocException> {
            placementOrderIdService.placeId(1, PlacementOrderType.PLACEMENT, "123", "1-01")
        }
    }

    @Test
    @DatabaseSetup("/service/placement-order-id/place-id/lost/before.xml")
    @ExpectedDatabase(
        "/service/placement-order-id/place-id/lost/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `placeId - id had been already lost`() {
        assertThrows<IdAlreadyLostException> {
            placementOrderIdService.placeId(1, PlacementOrderType.PLACEMENT, "123", "1-01")
        }
    }

    @Test
    @DatabaseSetup("/service/placement-order-id/place-id/dimension-constraints/before.xml")
    fun `placeId - error because dimension constraints are violated`() {
        val serialInventories =
            generateSerialInventories(listOf("100", "101"), "123", "ROV123", "465852")
        setupSerialInventoriesById("123", serialInventories)

        whenever(dbConfigService.getConfigAsBoolean(NSqlConfigKey.YM_PLACEMENT_CONSTRAINTS))
            .thenReturn(true)
        whenever(constraintsClient.checkByLocAndSku(anyString(), anyList()))
            .thenReturn(
                CheckByLocAndSkuResponse(
                    false,
                    listOf(
                        SkuConstraintsViolation("ROV123", "465852", RuleRestrictionType.DIMENSION),
                        SkuConstraintsViolation("ROV123", "465852", RuleRestrictionType.CARGO_TYPE)
                    )
                )
            )

        val exception = assertThrows<ConstraintViolationException> {
            placementOrderIdService.placeId(10, PlacementOrderType.PLACEMENT, "123", "1-01")
        }

        assertEquals(
            "<div>100: ВГХ, Карготипы</div><div>101: ВГХ, Карготипы</div>",
            exception.wmsErrorData()["reasons"]
        )
        verify(constraintsClient)
            .checkByLocAndSku("1-01", listOf(SkuInfo("ROV123", "465852", 2)))
    }

    @Test
    @DatabaseSetup("/service/placement-order-id/place-id/forbidden-loc/before.xml")
    fun `placeId - error because loc in list of forbidden for placement`() {
        whenever(dbConfigService.getConfigAsStringList(NSqlConfigKey.YM_PLACEMENT_FORBIDDEN_LOCS))
            .thenReturn(listOf("LOST"))

        setupSerialInventoriesById(loc = "PLACEMENT", id = "123", serialNumbers = listOf("0000000100", "0000000101"))

        assertThrows<ForbiddenPlacementLocException> {
            placementOrderIdService.placeId(1, PlacementOrderType.PLACEMENT, "123", "LOST")
        }
    }

    private fun generateSerialInventories(serialNumbers: List<String>, id: String, sku: String, storerKey:String) =
        buildSerialInventories(id = id, serialNumbers = serialNumbers, sku = sku, storerKey = storerKey)

    private fun setupSerialInventoriesById(
        lot: String = "", loc: String = "", id: String = "", serialNumbers: List<String>
    ) {
        whenever(coreService.getSerialInventoriesById(id))
            .thenReturn(buildSerialInventories(lot, loc, id, serialNumbers))
    }

    private fun setupSerialInventoriesById(id: String, serialInventories: List<SerialInventory>) {
        whenever(coreService.getSerialInventoriesById(id)).thenReturn(serialInventories)
    }

    private fun setupSerialInventoriesById(
        lot: String = "", id: String, serialAndLoc: List<Pair<String, String>>
    ) {
        val serialInventories = serialAndLoc.map {
            SerialInventory.builder()
                .serialNumber(it.first)
                .lot(lot)
                .loc(it.second)
                .id(id)
                .quantity(BigDecimal.ONE)
                .build()
        }
        whenever(coreService.getSerialInventoriesById(id)).thenReturn(serialInventories)
    }

    private fun buildSerialInventories(
        lot: String = "", loc: String = "PLACEMENT", id: String = "", serialNumbers: List<String>,
        sku: String = "", storerKey: String = ""
    ): List<SerialInventory> {
        return serialNumbers.map {
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
}
