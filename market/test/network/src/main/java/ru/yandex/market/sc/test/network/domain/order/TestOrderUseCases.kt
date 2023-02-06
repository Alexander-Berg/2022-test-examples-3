package ru.yandex.market.sc.test.network.domain.order

import kotlinx.coroutines.runBlocking
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.order.Order
import ru.yandex.market.sc.core.data.place.Place
import ru.yandex.market.sc.core.utils.data.DateMapper
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.data.manual.courier.ManualCourier
import ru.yandex.market.sc.test.data.partner.cell.PartnerCellMapper
import ru.yandex.market.sc.test.network.constants.Configuration
import ru.yandex.market.sc.test.network.domain.cell.TestCellUseCases
import ru.yandex.market.sc.test.network.domain.common.TestSortingCenterUtils
import ru.yandex.market.sc.test.network.repository.internal.InternalOrderRepository
import ru.yandex.market.sc.test.network.repository.manual.ManualOrderRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestOrderUseCases @Inject constructor(
    private val manualOrderRepository: ManualOrderRepository,
    private val internalOrderRepository: InternalOrderRepository,
    private val testCellUseCases: TestCellUseCases,
    private val sortingCenterUtils: TestSortingCenterUtils
) {
    fun createOrderToCourier(
        placeCount: Int = 1,
        textExternalId: Boolean = true,
        courierId: Long? = Configuration.COURIER_ID,
    ): Order = runBlocking {
        val externalId = createDemoOrder(textExternalId, placeCount, courierId)
        getOrder(externalId)
    }

    fun createOrderToKeep(placeCount: Int = 1, textExternalId: Boolean = true): Order =
        runBlocking {
            val externalId = createDemoOrder(textExternalId, placeCount, shipmentDate = futureDay)
            getOrder(externalId)
        }

    fun createOrderToReturn(
        placeCount: Int = 1,
        textExternalId: Boolean = true,
        warehouseYandexId: ExternalId? = null,
    ): Order = runBlocking {
        val externalId =
            createDemoOrder(textExternalId, placeCount, warehouseYandexId = warehouseYandexId)
        getOrder(externalId) {
            cancelOrder(order = it)
            acceptOrder(order = it)
        }
    }

    fun createOrderToDrop(placeCount: Int = 1, textExternalId: Boolean = true): Order =
        runBlocking {
            val externalId = createDemoOrder(textExternalId, placeCount, courierId = null)
            getOrder(externalId)
        }

    fun createReturnOrderToBufferCell(
        courierId: Long = Configuration.COURIER_ID,
        placeCount: Int = 1,
        textExternalId: Boolean = true
    ): Order = runBlocking {
        val externalId = createDemoOrder(textExternalId, placeCount, courierId)
        getOrder(externalId) {
            acceptAndSortOrder(order = it)
            shipOrder(order = it, courierId = courierId)
        }
    }

    fun createReturnOrderToReturnCell(
        courierId: Long = Configuration.COURIER_ID,
        placeCount: Int = 1,
        textExternalId: Boolean = true,
        deliveryServiceYandexId: String? = null,
        skipCourierOnCreation: Boolean = false
    ): Order = runBlocking {
        val externalId = createDemoOrder(
            textExternalId = textExternalId,
            placeCount = placeCount,
            deliveryServiceYandexId = deliveryServiceYandexId,
            courierId = if (skipCourierOnCreation) null else courierId
        )
        getOrder(externalId) {
            acceptAndSortOrder(order = it)
            shipOrder(order = it, courierId)
            returnOrder(order = it)
        }
    }

    fun createReturnOrderToReturnLot(
        courierId: Long = Configuration.COURIER_ID,
        placeCount: Int = 1,
        textExternalId: Boolean = true
    ): Order = runBlocking {
        acceptAndSortOrder(
            order = createReturnOrderToReturnCell(
                courierId,
                placeCount,
                textExternalId
            )
        )
    }

    fun createDamagedOrder(placeCount: Int = 1, textExternalId: Boolean = true): Order =
        runBlocking {
            val externalId = createDemoOrder(textExternalId, placeCount)
            getOrder(externalId) {
                cancelOrder(order = it)
                acceptOrder(order = it)
                markOrderAsDamaged(order = it)
            }
        }

    fun createOrderFromCourierToKeep(
        placeCount: Int = 1,
        textExternalId: Boolean = true,
        courierId: Long? = Configuration.COURIER_ID,
    ): Order = runBlocking {
        val externalId = createDemoOrder(textExternalId, placeCount, courierId)
        getOrder(externalId) {
            acceptAndSortOrder(order = it)
            updateShipmentDate(order = it, yesterday)
        }
    }

    fun createOrderForInitialAcceptance(
        placeCount: Int = 1,
        textExternalId: Boolean = false,
        isCancelled: Boolean = false
    ): Order = runBlocking {
        val externalId = createDemoOrder(
            textExternalId,
            placeCount,
        )
        getOrder(externalId) {
            if(isCancelled) cancelOrder(order = it)
        }
    }

    fun createOrderFromDropToKeep(placeCount: Int = 1, textExternalId: Boolean = true): Order =
        runBlocking {
            val externalId = createDemoOrder(textExternalId, placeCount, courierId = null)
            getOrder(externalId) {
                acceptAndSortOrder(order = it)
                updateShipmentDate(order = it, yesterday)
            }
        }

    fun createOrderFromDropToReturn(placeCount: Int = 1, textExternalId: Boolean = true): Order =
        runBlocking {
            val externalId = createDemoOrder(textExternalId, placeCount, courierId = null)
            getOrder(externalId) {
                acceptAndSortOrder(order = it)
                cancelOrder(order = it)
            }
        }

    fun createOrderFromDropToCourier(placeCount: Int = 1, textExternalId: Boolean = true): Order =
        runBlocking {
            val externalId = createDemoOrder(textExternalId, placeCount, courierId = null)
            getOrder(externalId) {
                acceptAndSortOrder(order = it)
                updateCourier(order = it)
            }
        }

    fun createOrderFromBufferToDrop(placeCount: Int = 1, textExternalId: Boolean = true): Order =
        runBlocking {
            val externalId = createDemoOrder(
                textExternalId,
                placeCount,
                courierId = null,
                shipmentDate = futureDay
            )
            val bufferCell = testCellUseCases.getBufferCell().let(PartnerCellMapper::mapToCell)
            getOrder(externalId) {
                acceptAndSortOrder(order = it, cell = bufferCell)
                updateShipmentDate(order = it, shipmentDate = today)
            }
        }

    fun createOrderFromBufferToCourier(placeCount: Int = 1, textExternalId: Boolean = true): Order =
        runBlocking {
            val externalId = createDemoOrder(textExternalId, placeCount, shipmentDate = futureDay)
            val bufferCell = testCellUseCases.getBufferCell().let(PartnerCellMapper::mapToCell)
            getOrder(externalId) {
                acceptAndSortOrder(order = it, cell = bufferCell)
                updateShipmentDate(order = it, shipmentDate = today)
            }
        }

    fun createOrderFromBufferToReturn(placeCount: Int = 1, textExternalId: Boolean = true): Order =
        runBlocking {
            val externalId = createDemoOrder(textExternalId, placeCount, shipmentDate = futureDay)
            val bufferCell = testCellUseCases.getBufferCell().let(PartnerCellMapper::mapToCell)
            getOrder(externalId) {
                acceptAndSortOrder(order = it, cell = bufferCell)
                cancelOrder(order = it)
            }
        }

    fun createClientReturnOrder(
        courierId: Long = Configuration.COURIER_ID,
        sortingCenterId: Long = sortingCenterUtils.getCurrentScId(),
        token: String = sortingCenterUtils.getCurrentScToken(),
    ): Order = runBlocking {
        val externalId = createDemoOrder(textExternalId = false, placeCount = 1)
        val clientReturnExternalId = ExternalId("$CLIENT_RETURN_PREFIX$externalId")
        internalOrderRepository.clientReturn(
            barcode = clientReturnExternalId.value,
            returnDate = today,
            courierId = courierId,
            sortingCenterId = sortingCenterId,
            token = token,
        )
        getOrder(clientReturnExternalId)
    }

    fun createOrderToLot(
        placeCount: Int = 1,
        textExternalId: Boolean = true,
        warehouseYandexId: ExternalId = Configuration.LOT_WAREHOUSE_YANDEX_ID,
        deliveryServiceYandexId: String = Configuration.DELIVERY_SERVICE_MIDDLE_MILE_ID,
    ): Order = runBlocking {
        val externalId = createDemoOrder(
            placeCount = placeCount,
            textExternalId = textExternalId,
            courierId = null,
            shipmentDate = null,
            warehouseYandexId = warehouseYandexId,
            deliveryServiceYandexId = deliveryServiceYandexId
        )
        getOrder(externalId)
    }

    fun acceptAndSortOrder(externalId: ExternalId) = runBlocking {
        getOrder(externalId) {
            acceptAndSortOrder(order = it)
        }
    }

    fun getOrder(externalId: ExternalId, scId: Long = sortingCenterUtils.getCurrentScId()) =
        runBlocking {
            manualOrderRepository.getOrder(externalId.value, scId)
        }

    fun acceptOrder(externalId: ExternalId) = runBlocking {
        manualOrderRepository.acceptOrder(externalId.value)
    }

    fun acceptAndSortOrder(
        order: Order,
        cell: Cell = order.places.first().availableCells.first(),
        placeExternalId: ExternalId? = null,
        ignoreTodayRouteOnKeep: Boolean = false,
        scId: Long = sortingCenterUtils.getCurrentScId(),
    ): Order = runBlocking {

        if (order.multiPlace) {
            val placeExternalIds = toPlaceExternalIds(order, placeExternalId)
            placeExternalIds.map { order.acceptAndSortOrder(it, cell.id, ignoreTodayRouteOnKeep) }
        } else manualOrderRepository.acceptAndSortOrder(
            externalOrderId = order.externalId.value,
            externalPlaceId = order.places.first().externalId.value,
            cellId = cell.id,
            ignoreTodayRouteOnKeep = ignoreTodayRouteOnKeep,
            scId = scId,
        )
        getOrder(externalId = order.externalId)
    }

    fun acceptAndSortOnePlace(
        order: Order,
        placeExternalId: ExternalId? = null,
        cell: Cell? = order.places.first().availableCells.firstOrNull(),
        ignoreTodayRouteOnKeep: Boolean = false,
    ): Order = runBlocking {
        manualOrderRepository.acceptAndSortOrder(
            externalOrderId = order.externalId.value,
            externalPlaceId = placeExternalId?.value,
            cellId = cell?.id,
            ignoreTodayRouteOnKeep = ignoreTodayRouteOnKeep,
        )
        getOrder(externalId = order.externalId)
    }

    private suspend fun createDemoOrder(
        textExternalId: Boolean,
        placeCount: Int,
        courierId: Long? = Configuration.COURIER_ID,
        shipmentDate: String? = DateMapper.getDate(),
        warehouseYandexId: ExternalId? = null,
        courierRequest: ManualCourier? = null,
        deliveryServiceYandexId: String? = null,
    ): ExternalId = manualOrderRepository.createDemo(
        scId = sortingCenterUtils.getCurrentScId(),
        courierId = courierId,
        textExternalId = textExternalId,
        placesCnt = placeCount,
        shipmentDate = shipmentDate,
        courierRequest = courierRequest,
        warehouseYandexId = warehouseYandexId?.value,
        deliveryServiceYandexId = deliveryServiceYandexId,
    ).let { ExternalId(it) }

    private suspend fun Order.acceptAndSortOrder(
        placeExternalId: ExternalId,
        cellId: Long,
        ignoreTodayRouteOnKeep: Boolean,
    ) {
        manualOrderRepository.acceptAndSortOrder(
            cellId = cellId,
            externalOrderId = externalId.value,
            externalPlaceId = placeExternalId.value,
            ignoreTodayRouteOnKeep = ignoreTodayRouteOnKeep,
        )
    }

    private suspend inline fun getOrder(
        externalId: ExternalId,
        scId: Long = sortingCenterUtils.getCurrentScId(),
        action: (Order) -> Unit = {},
    ): Order {
        return manualOrderRepository.getOrder(externalId.value, scId)
            .apply(action)
            .let { manualOrderRepository.getOrder(externalId.value, scId) }
    }

    private fun toPlaceExternalIds(
        order: Order,
        placeExternalId: ExternalId?,
    ): List<ExternalId> {
        return placeExternalId?.let(::listOf) ?: order.places.map(Place::externalId)
    }

    private suspend fun shipOrder(order: Order, courierId: Long) = order.apply {
        manualOrderRepository.shipOrder(courierId = courierId, externalOrderId = externalId.value)
    }

    private suspend fun acceptOrder(order: Order, placeExternalId: ExternalId? = null) {
        if (order.multiPlace) {
            val placeExternalIds = toPlaceExternalIds(order, placeExternalId)
            placeExternalIds.map { order.acceptPlace(it) }
        } else manualOrderRepository.acceptOrder(
            externalOrderId = order.externalId.value,
            externalPlaceId = order.places.first().externalId.value
        )
    }

    private suspend fun Order.acceptPlace(placeExternalId: ExternalId?) {
        manualOrderRepository.acceptOrder(
            externalOrderId = externalId.value,
            externalPlaceId = placeExternalId?.value
        )
    }

    private suspend fun updateShipmentDate(order: Order, shipmentDate: String) = order.apply {
        manualOrderRepository.updateShipmentDate(
            externalOrderId = externalId.value,
            force = true,
            shipmentDate = shipmentDate
        )
    }

    private suspend fun updateCourier(order: Order, courierId: Long? = Configuration.COURIER_ID) =
        order.apply {
            manualOrderRepository.updateCourier(
                courierId = courierId,
                externalOrderId = externalId.value
            )
        }

    private suspend fun cancelOrder(order: Order) = order.apply {
        manualOrderRepository.cancelOrder(externalId.value)
    }

    private suspend fun returnOrder(order: Order) = order.apply {
        manualOrderRepository.returnOrder(externalId.value)
    }

    private suspend fun markOrderAsDamaged(order: Order) = order.apply {
        manualOrderRepository.markOrderAsDamaged(externalId.value)
    }

    companion object {
        private const val CLIENT_RETURN_PREFIX = "VOZVRAT_SF_PVZ_"

        private val yesterday
            get() = DateMapper.getDate(-1)

        private val today
            get() = DateMapper.getDate(0)

        private val futureDay
            get() = DateMapper.getDate(2)

    }
}
