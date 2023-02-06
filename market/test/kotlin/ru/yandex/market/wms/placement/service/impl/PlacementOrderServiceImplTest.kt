@file:Suppress("UnnecessaryVariable")

package ru.yandex.market.wms.placement.service.impl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import ru.yandex.market.wms.common.model.enums.ItrnSourceType
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.common.spring.dao.entity.LotLocId
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory
import ru.yandex.market.wms.common.spring.dao.implementation.SerialInventoryDao
import ru.yandex.market.wms.common.spring.service.actiontracking.ActionTrackingService
import ru.yandex.market.wms.core.base.response.GetMostPopulatedZoneResponse
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.placement.dao.PlacementDetailsDao
import ru.yandex.market.wms.placement.dao.PlacementOrderCheckDao
import ru.yandex.market.wms.placement.dao.PlacementOrderDao
import ru.yandex.market.wms.placement.dao.PlacementOrderIdDao
import ru.yandex.market.wms.placement.dao.PlacementOrderSerialNumberDao
import ru.yandex.market.wms.placement.dao.model.NOT_FULLY_PLACED_STATUSES
import ru.yandex.market.wms.placement.dao.model.OrderKeyAndType
import ru.yandex.market.wms.placement.dao.model.PlacementItemStatus
import ru.yandex.market.wms.placement.dao.model.PlacementOrder
import ru.yandex.market.wms.placement.dao.model.PlacementOrderIdState
import ru.yandex.market.wms.placement.dao.model.PlacementOrderStatus
import ru.yandex.market.wms.placement.dao.model.PlacementOrderType
import ru.yandex.market.wms.placement.exception.IdHasFakeUitException
import ru.yandex.market.wms.placement.metrics.SolomonMetricManager
import ru.yandex.market.wms.placement.service.CoreService
import ru.yandex.market.wms.placement.service.IdNestingService
import ru.yandex.market.wms.placement.service.PlacementBalancesService
import ru.yandex.market.wms.placement.service.PlacementLogService
import ru.yandex.market.wms.placement.service.PlacementOrderCheckService
import ru.yandex.market.wms.placement.service.PlacementOrderContentService
import ru.yandex.market.wms.placement.service.PlacementOrderStatusService
import ru.yandex.market.wms.placement.service.RowRecommendationService
import java.math.BigDecimal
import java.time.LocalDateTime

internal class PlacementOrderServiceImplTest {
    @InjectMocks
    lateinit var service: PlacementOrderServiceImpl

    @Mock
    lateinit var orderDao: PlacementOrderDao

    @Mock
    lateinit var actionTrackingService: ActionTrackingService

    @Mock
    lateinit var placementOrderIdDao: PlacementOrderIdDao

    @Mock
    lateinit var placementOrderSerialNumberDao: PlacementOrderSerialNumberDao

    @Mock
    lateinit var placementOrderDetailDao: PlacementDetailsDao

    @Mock
    lateinit var coreService: CoreService

    @Mock
    lateinit var coreClient: CoreClient

    @Mock
    lateinit var placementOrderContentService: PlacementOrderContentService

    @Mock
    lateinit var userProvider: ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider

    @Mock
    lateinit var statusService: PlacementOrderStatusService

    @Mock
    lateinit var balancesService: PlacementBalancesService

    @Mock
    lateinit var metricManager: SolomonMetricManager

    @Mock
    lateinit var placementLogService: PlacementLogService

    @Mock
    lateinit var idNestingService: IdNestingService

    @Mock
    lateinit var dbConfigService: DbConfigService

    @Mock
    lateinit var placementOrderCheckService: PlacementOrderCheckService

    @Mock
    lateinit var placementOrderCheckDao: PlacementOrderCheckDao

    @Mock
    lateinit var scanningOperationLog: ScanningOperationLog

    @Mock
    lateinit var serialInventoryDao: SerialInventoryDao

    @Mock
    lateinit var recommendationService: RowRecommendationService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(coreClient.getMostPopulatedZoneByContainer(anyString()))
            .thenReturn(GetMostPopulatedZoneResponse(null))

        whenever(userProvider.user).thenReturn("test")

        whenever(orderDao.getPlacementOrderByKeyAndUser(anyInt(), anyString()))
            .thenReturn(testPlacementInProgressOrder)
        whenever(placementOrderContentService.getOrCreatePlacementOrder(PlacementOrderType.PLACEMENT))
            .thenReturn(testPlacementInProgressOrder)
    }

    @Test
    fun `Move to lost, when ID was placed in another order`() {
        val ids = listOf(
            PlacementOrderIdState("123", PlacementItemStatus.NOT_PLACED, "PARENT_ID")
        )
        setUpReturnIdsByOrderKey(ids, 1)

        whenever(placementOrderContentService.getNotPlacedSerialsById("123", 1))
            .thenReturn(emptyList())

        service.moveNotPlacedToLost(1)

        verifyNoMoreInteractions(coreService)
    }

    @Test
    fun `Move to lost, when ID was partly placed in another order`() {
        val ids = listOf(
            PlacementOrderIdState("123", PlacementItemStatus.PARTLY_PLACED, "PARENT_ID")
        )
        setUpReturnIdsByOrderKey(ids, 1)

        whenever(placementOrderContentService.getNotPlacedSerialsById("123", 1))
            .thenReturn(emptyList())

        service.moveNotPlacedToLost(1)

        verify(placementOrderContentService, times(1))
            .cleanAndFixStatusForId("123", 1)
        verifyNoInteractions(coreService)
    }

    @Test
    fun `Move to lost, nothing to move in balances of placement order`() {
        val ids = listOf(
            PlacementOrderIdState("123", PlacementItemStatus.PARTLY_PLACED, "PARENT_ID")
        )
        setUpReturnIdsByOrderKey(ids, 1)

        val notMovedSerials = listOf("uit1", "uit2", "uit3")
        whenever(placementOrderContentService.getNotPlacedSerialsById("123", 1))
            .thenReturn(notMovedSerials)

        whenever(serialInventoryDao.getByLocAndId(anyString(), anyString())).thenReturn(emptyList())
        whenever(coreService.moveIdToLost(anyString(), anyInt(), any())).thenReturn(emptyList())

        service.moveNotPlacedToLost(1)

        verify(placementOrderContentService, times(1))
            .moveToLost(emptyList(), "123", 1)
        verify(placementOrderContentService, times(1))
            .cleanAndFixStatusForId("123", 1)
        verify(coreService)
            .moveIdToLost("123", 1, ItrnSourceType.PLACEMENT_LOST)
    }

    @Test
    fun `Move to lost, nothing to move in balances of optimization order`() {
        whenever(
            orderDao.getPlacementOrderByKeyAndUser(anyInt(), anyString())
        ).thenReturn(testOptimizationInProgressOrder)

        val orderKey = testOptimizationInProgressOrder.placementOrderKey
        val id = PlacementOrderIdState("123", PlacementItemStatus.PARTLY_PLACED, "PARENT_ID")
        setUpReturnIdsByOrderKey(listOf(id), orderKey)

        val notMovedSerials = listOf("uit1", "uit2", "uit3")
        whenever(placementOrderContentService.getNotPlacedSerialsById("123", orderKey))
            .thenReturn(notMovedSerials)

        whenever(serialInventoryDao.getByLocAndId(anyString(), anyString())).thenReturn(emptyList())
        whenever(coreService.moveIdToLost(anyString(), anyInt(), any())).thenReturn(emptyList())

        service.moveNotPlacedToLost(orderKey)

        verify(placementOrderContentService)
            .moveToLost(emptyList(), "123", orderKey)
        verify(placementOrderContentService)
            .cleanAndFixStatusForId("123", orderKey)
        verify(coreService)
            .moveIdToLost("123", orderKey, ItrnSourceType.OPTIMIZATION_LOST)
    }

    @Test
    fun `Move to lost, only part was moved in balances`() {
        val ids = listOf(
            PlacementOrderIdState("123", PlacementItemStatus.PARTLY_PLACED, "PARENT_ID")
        )
        setUpReturnIdsByOrderKey(ids, 1)

        val notMovedSerials = listOf("uit1", "uit2", "uit3")
        whenever(placementOrderContentService.getNotPlacedSerialsById("123", 1))
            .thenReturn(notMovedSerials)

        val movedToLost = listOf("uit1", "uit2")
        whenever(
            coreService.moveIdToLost(eq("123"), eq(1), any())
        )
            .thenReturn(movedToLost.map { SerialInventory.builder().serialNumber(it).build() })

        service.moveNotPlacedToLost(1)

        verify(coreService, times(1))
            .moveIdToLost("123", 1, ItrnSourceType.PLACEMENT_LOST)
        verify(placementOrderContentService, times(1))
            .moveToLost(movedToLost, "123", 1)
        verify(placementOrderContentService, times(1))
            .cleanAndFixStatusForId("123", 1)
    }

    @Test
    fun `Move to lost, all uits was moved in balances`() {
        val ids = listOf(
            PlacementOrderIdState("123", PlacementItemStatus.PARTLY_PLACED, "PARENT_ID")
        )
        setUpReturnIdsByOrderKey(ids, 1)

        val notMovedSerials = listOf("uit1", "uit2", "uit3")
        whenever(placementOrderContentService.getNotPlacedSerialsById("123", 1))
            .thenReturn(notMovedSerials)

        val movedToLost = notMovedSerials
        whenever(coreService.moveIdToLost(eq("123"), eq(1), any()))
            .thenReturn(movedToLost.map { SerialInventory.builder().serialNumber(it).build() })

        service.moveNotPlacedToLost(1)

        verify(coreService, times(1))
            .moveIdToLost("123", 1, ItrnSourceType.PLACEMENT_LOST)
        verify(placementOrderContentService, times(1))
            .moveToLost(movedToLost, "123", 1)
    }

    @Test
    fun `Move to lost, whole NOT_PLACED ID was moved in balances`() {
        val ids = listOf(
            PlacementOrderIdState("123", PlacementItemStatus.NOT_PLACED, "PARENT_ID")
        )
        setUpReturnIdsByOrderKey(ids, 1)

        val notMovedSerials = listOf("uit1", "uit2", "uit3")
        whenever(placementOrderContentService.getNotPlacedSerialsById("123", 1))
            .thenReturn(notMovedSerials)

        val movedToLost = notMovedSerials
        whenever(coreService.moveIdToLost(eq("123"), eq(1), any()))
            .thenReturn(movedToLost.map { SerialInventory.builder().serialNumber(it).build() })

        service.moveNotPlacedToLost(1)

        verify(coreService, times(1))
            .moveIdToLost("123", 1, ItrnSourceType.PLACEMENT_LOST)
        verify(placementOrderContentService, times(1))
            .moveToLost("123", 1)
    }

    @Test
    fun `Move to lost, PARTLY_PLACED ID was moved in balances`() {
        val ids = listOf(
            PlacementOrderIdState("123", PlacementItemStatus.PARTLY_PLACED, "PARENT_ID")
        )
        setUpReturnIdsByOrderKey(ids, 1)

        val notMovedSerials = listOf("uit1", "uit2", "uit3")
        whenever(placementOrderContentService.getNotPlacedSerialsById("123", 1))
            .thenReturn(notMovedSerials)

        val movedToLost = notMovedSerials
        whenever(coreService.moveIdToLost(eq("123"), eq(1), any()))
            .thenReturn(movedToLost.map { SerialInventory.builder().serialNumber(it).build() })

        service.moveNotPlacedToLost(1)

        verify(coreService, times(1))
            .moveIdToLost("123", 1, ItrnSourceType.PLACEMENT_LOST)
        verify(placementOrderContentService, times(1))
            .moveToLost(movedToLost, "123", 1)
    }

    @Test
    fun `Move to lost from order with check, PARTLY_PLACED ID`() {
        val ids = listOf(
            PlacementOrderIdState("123", PlacementItemStatus.PARTLY_PLACED, "PARENT_ID")
        )
        setUpReturnIdsByOrderKey(ids, 1)
        whenever(orderDao.getPlacementOrderByKeyAndUser(eq(1), any()))
            .thenReturn(testPlacementInProgressOrder.copy(withCheck = true))

        val notMovedSerials = listOf("uit1", "uit2", "uit3")
        whenever(placementOrderContentService.getNotPlacedSerialsById("123", 1))
            .thenReturn(notMovedSerials)

        val movedToLost = notMovedSerials
        whenever(coreService.moveIdToLost(eq("123"), eq(1), any()))
            .thenReturn(movedToLost.map { SerialInventory.builder().serialNumber(it).build() })

        service.moveNotPlacedToLost(1)

        verify(coreService, times(1))
            .moveIdToLost("123", 1, ItrnSourceType.PLACEMENT_LOST_CHECK)
        verify(placementOrderContentService, times(1))
            .moveToLost(movedToLost, "123", 1)
    }

    @Test
    fun `addIdToPlacementOrder, parent id not exists so breakParentLink is false`() {
        val testId = "ID"

        whenever(placementOrderContentService.getOrCreatePlacementOrder(PlacementOrderType.PLACEMENT))
            .thenReturn(testPlacementPrepareOrder)
        whenever(idNestingService.getNestedIds(testId))
            .thenReturn(emptyList())
        whenever(placementOrderIdDao.getActiveOrderKeyByIdAndStatuses(testId, NOT_FULLY_PLACED_STATUSES))
            .thenReturn(null)
        whenever(coreService.isIdExists(testId)).thenReturn(true)
        whenever(coreService.getLotLocIdById(testId)).thenReturn(
            listOf(LotLocId.builder().id(testId).qtyAllocated(BigDecimal.ZERO).qtyPicked(BigDecimal.ZERO).build())
        )
        whenever(idNestingService.getParentIdOrNull(testId)).thenReturn(null)

        service.addIdToPlacementOrder(testId, null, false)

        verify(placementOrderContentService, times(1))
            .addNewIdToOrder(testPlacementPrepareOrder, testId, null, false)
    }

    @Test
    fun `addIdToPlacementOrder, parent id exists and added so breakParentLink is true`() {
        val testId = "ID"
        val testParentId = "PARENT_ID"

        whenever(placementOrderContentService.getOrCreatePlacementOrder(PlacementOrderType.PLACEMENT))
            .thenReturn(testPlacementPrepareOrder)
        whenever(idNestingService.getNestedIds(testId))
            .thenReturn(emptyList())
        whenever(placementOrderIdDao.getActiveOrderKeyAndTypeByIdAndStatuses(testId, NOT_FULLY_PLACED_STATUSES))
            .thenReturn(OrderKeyAndType(3, PlacementOrderType.PLACEMENT))
        whenever(coreService.isIdExists(testId)).thenReturn(true)
        whenever(coreService.getLotLocIdById(testId)).thenReturn(
            listOf(LotLocId.builder().id(testId).qtyAllocated(BigDecimal.ZERO).qtyPicked(BigDecimal.ZERO).build())
        )
        whenever(idNestingService.getParentIdOrNull(testId)).thenReturn(testParentId)

        service.addIdToPlacementOrder(testId, null, false)

        verify(placementOrderContentService, times(1))
            .moveIdFromAnotherPlacementOrder(testPlacementPrepareOrder, 3, testId, null, true)
    }

    @Test
    fun `Add id to placement order, throws exception when there is no nested ids AND no uits for id`() {
        whenever(coreService.fakeUitExists(listOf("123"))).thenReturn(true)

        Assertions.assertThrows(IdHasFakeUitException::class.java) {
            service.addIdToPlacementOrder("123", null, false)
        }
    }

    @Test
    fun `Add id to placement order throws exception when there is nested ids AND no uits for id`() {
        whenever(idNestingService.getNestedIds("123")).thenReturn(listOf("456"))
        whenever(coreService.fakeUitExists(listOf("123", "456"))).thenReturn(true)

        Assertions.assertThrows(IdHasFakeUitException::class.java) {
            service.addIdToPlacementOrder("123", null, false)
        }
    }

    @Test
    fun `Add id to placement order does not throw exception when there is uits for id`() {
        whenever(coreService.fakeUitExists(listOf("123"))).thenReturn(false)
        whenever(coreService.isIdExists("123")).thenReturn(true)

        Assertions.assertDoesNotThrow {
            service.addIdToPlacementOrder("123", null, false)
        }
    }

    private fun setUpReturnIdsByOrderKey(ids: List<PlacementOrderIdState>, orderKey: Int) {
        whenever(
            placementOrderContentService.getIdsWithStatusIn(
                orderKey,
                listOf(PlacementItemStatus.NOT_PLACED, PlacementItemStatus.PARTLY_PLACED)
            )
        ).thenReturn(ids)
    }

    companion object {
        private val testPlacementInProgressOrder: PlacementOrder
        private val testPlacementPrepareOrder: PlacementOrder
        private val testOptimizationInProgressOrder: PlacementOrder

        init {
            val now = LocalDateTime.now()
            testPlacementInProgressOrder = PlacementOrder(
                1, "test",
                PlacementOrderStatus.IN_PROGRESS, PlacementOrderType.PLACEMENT,
                now, null, "", now, "", now
            )
            testPlacementPrepareOrder = PlacementOrder(
                2, "test",
                PlacementOrderStatus.PREPARE, PlacementOrderType.PLACEMENT,
                now, null, "", now, "", now
            )
            testOptimizationInProgressOrder = PlacementOrder(
                3, "test",
                PlacementOrderStatus.IN_PROGRESS, PlacementOrderType.OPTIMIZATION,
                now, null, "", now, "", now
            )
        }
    }
}
