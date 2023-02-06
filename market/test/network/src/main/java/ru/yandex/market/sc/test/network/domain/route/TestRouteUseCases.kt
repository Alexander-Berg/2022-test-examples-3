package ru.yandex.market.sc.test.network.domain.route

import kotlinx.coroutines.runBlocking
import ru.yandex.market.sc.core.data.cell.CellForRouteBase
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.network.domain.NetworkLotUseCases
import ru.yandex.market.sc.core.network.domain.NetworkRouteUseCases
import ru.yandex.market.sc.test.network.domain.cell.TestCellUseCases
import ru.yandex.market.sc.test.network.domain.common.TestSortingCenterUtils
import ru.yandex.market.sc.test.network.repository.manual.ManualLotRepository
import ru.yandex.market.sc.test.network.repository.manual.ManualOrderRepository
import ru.yandex.market.sc.test.network.repository.partner.PartnerLotRepository
import javax.inject.Inject
import javax.inject.Singleton
import ru.yandex.market.sc.core.network.domain.NetworkCellUseCases as CellCoreUseCases

@Singleton
class TestRouteUseCases @Inject constructor(
    private val networkLotUseCases: NetworkLotUseCases,
    private val manualLotRepository: ManualLotRepository,
    private val partnerLotRepository: PartnerLotRepository,
    private val manualOrderRepository: ManualOrderRepository,
    private val cellCoreUseCases: CellCoreUseCases,
    private val networkRouteUseCases: NetworkRouteUseCases,
    private val testCellUseCases: TestCellUseCases,
    private val sortingCenterUtils: TestSortingCenterUtils,
) {
    fun cleanRoute(
        courierId: Long,
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
        scId: Long? = sortingCenterUtils.getCurrentScId(),
    ) = runBlocking {
        try {
            val route = networkRouteUseCases.getRouteForCourier(courierId.toString())
            route.cells.forEach { cell ->
                moveOrdersToBuffer(route.id, cell, scPartnerId, scId)
                shipOrDeleteLot(route.id, cell, scPartnerId)
            }
        } catch (ignore: Exception) {
        }
    }

    fun getRoute(
        routeId: Long,
    ) = runBlocking {
        networkRouteUseCases.getRoute(routeId)
    }

    private suspend fun moveOrdersToBuffer(
        routeId: Long,
        cell: CellForRouteBase,
        scPartnerId: Long,
        scId: Long?
    ) {
        val bufferCell = testCellUseCases.getBufferCell(scPartnerId = scPartnerId)
        cellCoreUseCases.getCellForRoute(cell.id, routeId).orders.keys
            .map { manualOrderRepository.getOrder(it.value, scId) }
            .forEach { order ->
                if (!order.multiPlace) {
                    manualOrderRepository.acceptAndSortOrder(
                        cellId = bufferCell.id,
                        externalOrderId = order.externalId.value,
                        ignoreTodayRouteOnKeep = true,
                    )
                } else order.places.forEach { place ->
                    manualOrderRepository.acceptAndSortOrder(
                        cellId = bufferCell.id,
                        externalOrderId = order.externalId.value,
                        externalPlaceId = place.externalId.value,
                        ignoreTodayRouteOnKeep = true,
                    )
                }

            }
    }

    private suspend fun shipOrDeleteLot(
        routeId: Long,
        cell: CellForRouteBase,
        scPartnerId: Long,
    ) {
        cellCoreUseCases.getCellForRoute(cell.id, routeId).lotStatuses.forEach { entity ->
            val (lotExternalId, lotStatus) = entity
            when (lotStatus) {
                Lot.Status.CREATED -> {
                    val lot = networkLotUseCases.getLot(lotExternalId)
                    partnerLotRepository.deleteLot(scPartnerId, lot.id)
                }
                Lot.Status.PROCESSING,
                Lot.Status.PACKED,
                -> {
                    val lot = networkLotUseCases.getLot(lotExternalId)
                    manualLotRepository.prepareToShipLot(lot.id)
                    networkRouteUseCases.shipRouteWithLot(
                        routeId,
                        cell.id,
                        lotShippedExternalId = lotExternalId
                    )
                }
                Lot.Status.READY -> networkRouteUseCases.shipRouteWithLot(
                    routeId,
                    cell.id,
                    lotExternalId
                )
                Lot.Status.SHIPPED,
                Lot.Status.UNKNOWN,
                -> Unit
            }
        }
    }
}
