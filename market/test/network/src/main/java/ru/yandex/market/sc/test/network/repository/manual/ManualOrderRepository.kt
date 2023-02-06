package ru.yandex.market.sc.test.network.repository.manual

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.yandex.market.sc.core.data.order.Order
import ru.yandex.market.sc.core.data.order.OrderMapper
import ru.yandex.market.sc.core.utils.data.functional.Functional.orThrow
import ru.yandex.market.sc.test.data.manual.courier.ManualCourier
import ru.yandex.market.sc.test.network.api.SortingCenterManualService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManualOrderRepository @Inject constructor(
    private val sortingCenterManualService: SortingCenterManualService,
    private val orderMapper: OrderMapper,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun getOrder(externalId: String, scId: Long?): Order = withContext(ioDispatcher) {
        orderMapper.map(sortingCenterManualService.getOrder(externalId, scId)).orThrow()
    }

    suspend fun acceptOrder(
        externalOrderId: String,
        externalPlaceId: String? = null,
    ) = withContext(ioDispatcher) {
        sortingCenterManualService.acceptOrder(externalOrderId, externalPlaceId)
    }

    suspend fun acceptAndSortOrder(
        cellId: Long? = null,
        courierId: Int? = null,
        externalOrderId: String,
        externalPlaceId: String? = null,
        ignoreTodayRouteOnKeep: Boolean? = null,
        scId: Long? = null,
    ) = withContext(ioDispatcher) {
        sortingCenterManualService.acceptAndSortOrder(
            cellId,
            courierId,
            externalOrderId,
            externalPlaceId,
            ignoreTodayRouteOnKeep,
            scId,
        )
    }

    suspend fun cancelOrder(externalOrderId: String) = withContext(ioDispatcher) {
        sortingCenterManualService.cancelOrder(externalOrderId)
    }

    suspend fun createDemo(
        courierId: Long? = null,
        courierRequest: ManualCourier? = null,
        deliveryServiceYandexId: String? = null,
        placesCnt: Int? = null,
        scId: Long,
        shipmentDate: String? = null,
        textExternalId: Boolean? = null,
        warehouseYandexId: String? = null,
    ): String = withContext(ioDispatcher) {
        if (courierRequest != null) {
            sortingCenterManualService.createDemoOrder(
                deliveryServiceYandexId,
                placesCnt,
                scId,
                shipmentDate,
                textExternalId,
                warehouseYandexId = warehouseYandexId,
                courierRequest = courierRequest
            )
        } else {
            sortingCenterManualService.createDemoOrder(
                deliveryServiceYandexId,
                placesCnt,
                scId,
                shipmentDate,
                textExternalId,
                warehouseYandexId = warehouseYandexId,
                courierId = courierId
            )
        }
    }

    suspend fun keepOrder(
        cellId: Int? = null,
        externalOrderId: String,
        externalPlaceId: String? = null,
        ignoreTodayRoute: Boolean = false,
    ) = withContext(ioDispatcher) {
        sortingCenterManualService.keepOrder(
            cellId,
            externalOrderId,
            externalPlaceId,
            ignoreTodayRoute
        )
    }

    suspend fun markOrderAsDamaged(externalOrderId: String) = withContext(ioDispatcher) {
        sortingCenterManualService.markOrderAsDamaged(externalOrderId)
    }

    suspend fun prepareToShipOrder(
        cellId: Int,
        routeId: Long,
        externalOrderId: String,
        externalPlaceId: String? = null,
    ) = withContext(ioDispatcher) {
        sortingCenterManualService.prepareToShipOrder(
            cellId,
            externalOrderId,
            externalPlaceId,
            routeId
        )
    }

    suspend fun returnOrder(externalOrderId: String) = withContext(ioDispatcher) {
        sortingCenterManualService.returnOrder(externalOrderId)
    }

    suspend fun revertMarkOrderAsDamaged(externalOrderId: String) = withContext(ioDispatcher) {
        sortingCenterManualService.revertMarkOrderAsDamaged(externalOrderId)
    }

    suspend fun revertReturnOrder(externalOrderId: String) = withContext(ioDispatcher) {
        sortingCenterManualService.revertReturnOrder(externalOrderId)
    }

    suspend fun shipOrder(
        courierId: Long,
        externalOrderId: String,
        externalPlaceId: String? = null,
        ignoreTodayRoute: Boolean = false,
        warehouseId: Long? = null,
    ) = withContext(ioDispatcher) {
        sortingCenterManualService.shipOrder(
            courierId,
            externalOrderId,
            externalPlaceId,
            ignoreTodayRoute,
            warehouseId
        )
    }

    suspend fun sortOrder(
        cellId: Int,
        externalOrderId: String,
        externalPlaceId: String? = null,
    ) = withContext(ioDispatcher) {
        sortingCenterManualService.sortOrder(cellId, externalOrderId, externalPlaceId)
    }

    suspend fun updateCourier(
        externalOrderId: String,
        courierId: Long?,
    ) = withContext(ioDispatcher) {
        sortingCenterManualService.updateCourier(courierId, externalOrderId)
    }

    suspend fun updateDeliveryDate(
        deliveryDate: String,
        externalOrderId: String,
    ) = withContext(ioDispatcher) {
        sortingCenterManualService.updateDeliveryDate(deliveryDate, externalOrderId)
    }

    suspend fun updateShipmentDate(
        shipmentDate: String,
        externalOrderId: String,
        force: Boolean = false,
    ) = withContext(ioDispatcher) {
        sortingCenterManualService.updateShipmentDate(shipmentDate, externalOrderId, force)
    }
}
