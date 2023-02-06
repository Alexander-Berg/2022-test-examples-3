package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.order.OrderResortInfo
import ru.yandex.market.sc.core.data.order.ResortReasonCode
import ru.yandex.market.sc.core.utils.data.DateWithoutTime
import ru.yandex.market.sc.core.utils.data.ExternalId

object OrderResortInfoTestFactory {
    fun get(
        availableCells: List<Cell> = listOf(),
        deliveryServiceName: String? = null,
        forPlace: Boolean = false,
        multiPlace: Boolean = false,
        possibleOutgoingRouteDate: String? = null,
        resortReasonCode: ResortReasonCode? = null,
    ): OrderResortInfo {
        val orderExternalId = IdManager.getId().let(IdManager::getExternalId)

        val placeExternalId = if (forPlace) {
            IdManager.getIndexedExternalId(orderExternalId)
        } else {
            null
        }

        return OrderResortInfo(
            availableCells = availableCells,
            deliveryServiceName = deliveryServiceName,
            multiPlace = multiPlace,
            needResort = resortReasonCode != null,
            orderExternalId = orderExternalId,
            placeExternalId = placeExternalId,
            possibleOutgoingRouteDate = possibleOutgoingRouteDate?.let { DateWithoutTime(it) },
            resortReasonCode = resortReasonCode,
        )
    }

    fun get(
        availableCells: List<Cell> = listOf(),
        deliveryServiceName: String? = null,
        multiPlace: Boolean = false,
        orderExternalId: ExternalId,
        placeExternalId: ExternalId? = null,
        possibleOutgoingRouteDate: String? = null,
        resortReasonCode: ResortReasonCode? = null,
    ): OrderResortInfo {
        return OrderResortInfo(
            availableCells = availableCells,
            deliveryServiceName = deliveryServiceName,
            multiPlace = multiPlace,
            needResort = resortReasonCode != null,
            orderExternalId = orderExternalId,
            placeExternalId = placeExternalId,
            possibleOutgoingRouteDate = possibleOutgoingRouteDate?.let { DateWithoutTime(it) },
            resortReasonCode = resortReasonCode,
        )
    }
}
