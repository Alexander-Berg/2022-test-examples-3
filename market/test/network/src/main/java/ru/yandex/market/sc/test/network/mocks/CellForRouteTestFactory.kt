package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.cell.CellForRoute
import ru.yandex.market.sc.core.data.cell.CellForRouteBase
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.data.order.OrderShipStatus
import ru.yandex.market.sc.core.data.place.PlaceInfo
import ru.yandex.market.sc.core.utils.data.ExternalId

object CellForRouteTestFactory {
    fun forCell(cell: Cell, prepared: Boolean = false): CellForRouteBuilder {
        return CellForRouteBuilder(cell, prepared)
    }

    class CellForRouteBuilder(val cell: Cell, val prepared: Boolean) {
        private val actions: MutableList<CellForRoute.Action> = mutableListOf()

        private val orders: MutableMap<ExternalId, OrderShipStatus> = mutableMapOf()
        private val shippedOrders: MutableMap<ExternalId, OrderShipStatus> = mutableMapOf()
        private val placesInfoByOrder: MutableMap<ExternalId, Map<ExternalId, PlaceInfo>> =
            mutableMapOf()

        private val lotStatuses: MutableMap<ExternalId, Lot.Status> = mutableMapOf()

        fun withActions(vararg actionsToAdd: CellForRoute.Action): CellForRouteBuilder {
            actions.addAll(actionsToAdd)
            return this
        }

        fun withLot(status: Lot.Status, externalId: ExternalId? = null): CellForRouteBuilder {
            val lotExternalId: ExternalId =
                externalId ?: IdManager.getId().let(IdManager::getExternalId)

            lotStatuses[lotExternalId] = status
            return this
        }

        fun withOrder(
            status: OrderShipStatus,
            shipped: Boolean = false,
            externalId: ExternalId? = null
        ): CellForRouteOrderBuilder {
            val orderExternalId = externalId ?: IdManager.getId().let(IdManager::getExternalId)

            orders[orderExternalId] = status

            if (shipped) {
                shippedOrders[orderExternalId] = status
            }

            return CellForRouteOrderBuilder(this, orderExternalId, placesInfoByOrder)
        }

        fun build(): CellForRoute {
            val ordersNotInCell = placesInfoByOrder.count {
                it.value.all { placeInfoEntry -> !placeInfoEntry.value.inCell }
            }

            return CellForRoute(
                id = cell.id,
                status = cell.status,
                type = cell.type,
                subType = cell.subType,
                number = cell.number,
                orders = orders,
                shippedOrders = shippedOrders,
                placesInfoByOrder = placesInfoByOrder,
                lotStatuses = lotStatuses,
                ordersNotInCell = ordersNotInCell,
                cellPrepared = prepared,
                actions = actions,
            )
        }
    }

    class CellForRouteOrderBuilder(
        private val cellForRouteBuilder: CellForRouteBuilder,
        val orderExternalId: ExternalId,
        private val placesInfoByOrder: MutableMap<ExternalId, Map<ExternalId, PlaceInfo>>
    ) {
        private val placeInfoMapBuilder = PlaceInfoTestFactory.forOrder(orderExternalId)

        fun withPlace(
            inCell: Boolean,
            placeExternalId: ExternalId? = null
        ): CellForRouteOrderBuilder {
            placeInfoMapBuilder.withPlace(inCell, placeExternalId)
            return this
        }

        fun build(): CellForRouteBuilder {
            placesInfoByOrder[orderExternalId] = placeInfoMapBuilder
                .buildPlaceInfoMap()
            return cellForRouteBuilder
        }
    }

    fun mapToCellForRoute(
        cell: Cell,
        orders: Map<ExternalId, OrderShipStatus> = mapOf(),
        shippedOrders: Map<ExternalId, OrderShipStatus> = mapOf(),
        placesInfoByOrder: Map<ExternalId, Map<ExternalId, PlaceInfo>> = mapOf(),
        lotStatuses: Map<ExternalId, Lot.Status> = mapOf(),
        ordersNotInCell: Int = 0,
        cellPrepared: Boolean = false,
        actions: List<CellForRoute.Action> = listOf(),
    ) = CellForRoute(
        id = cell.id,
        status = cell.status,
        type = cell.type,
        subType = cell.subType,
        number = cell.number,
        orders = orders,
        shippedOrders = shippedOrders,
        placesInfoByOrder = placesInfoByOrder,
        lotStatuses = lotStatuses,
        ordersNotInCell = ordersNotInCell,
        cellPrepared = cellPrepared,
        actions = actions,
    )

    fun mapToCellForRouteBase(
        cell: Cell,
        address: String? = null,
        lotCount: Int = 0,
        isEmpty: Boolean = false,
    ) = CellForRouteBase(
        id = cell.id,
        number = cell.number,
        address = address,
        lotCount = lotCount,
        empty = isEmpty,
    )
}
