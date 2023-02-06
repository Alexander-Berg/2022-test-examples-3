package ru.yandex.market.sc.test.network.domain.lot

import kotlinx.coroutines.runBlocking
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.data.order.Order
import ru.yandex.market.sc.core.network.arch.ext.unwrap
import ru.yandex.market.sc.core.network.domain.NetworkLotUseCases
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.core.network.domain.NetworkSortableUseCases
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.domain.common.TestSortingCenterUtils
import ru.yandex.market.sc.test.network.repository.manual.ManualLotRepository
import ru.yandex.market.sc.test.network.repository.partner.PartnerLotRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestLotUseCases @Inject constructor(
    private val networkLotUseCases: NetworkLotUseCases,
    private val networkOrderUseCases: NetworkOrderUseCases,
    private val networkSortableUseCases: NetworkSortableUseCases,
    private val manualLotRepository: ManualLotRepository,
    private val partnerLotRepository: PartnerLotRepository,
    private val sortingCenterUtils: TestSortingCenterUtils,
) {
    fun createLots(
        cellId: Long,
        count: Int = 3,
        scId: Long = sortingCenterUtils.getCurrentScId(),
    ): List<Lot> = runBlocking {
        manualLotRepository.getLotsById(manualLotRepository.createLots(cellId, count, scId).toSet())
    }

    fun getLot(externalId: ExternalId): Lot = runBlocking {
        networkLotUseCases.getLot(externalId)
    }

    fun createOrphanLots(
        count: Int = 3,
        scId: Long = sortingCenterUtils.getCurrentScId(),
    ): List<ExternalId> = runBlocking {
        manualLotRepository.createOrphanLots(count, scId)
    }

    fun deleteAllLotsForOrder(
        order: Order,
        scId: Long = sortingCenterUtils.getCurrentScPartnerId()
    ) = runBlocking {
        order.places.forEach { place ->
            place.availableLots.forEach { lot ->
                runCatching { partnerLotRepository.deleteLot(scId, lot.id) }
            }
        }
    }

    fun deleteAllLotsForCell(
        cellId: Long,
        scId: Long = sortingCenterUtils.getCurrentScPartnerId()
    ) = runBlocking {
        partnerLotRepository.getLots(scId, listOf(cellId)).unwrap().forEach { lot ->
            runCatching {
                partnerLotRepository.deleteLot(scId, lot.id)
            }
        }
    }

    fun sortToOrphanLot(order: Order, lot: Lot, parentCell: Cell) = runBlocking {
        order.places.forEach { place ->
            networkOrderUseCases.acceptOrder(order.externalId, place.externalId)
            networkSortableUseCases.sort(
                order.externalId,
                lot.externalId,
                place.externalId,
                parentDestinationExternalId = ExternalId(parentCell.id)
            )
        }
        getLot(lot.externalId)
    }

    fun sortToLot(order: Order, lot: Lot) = runBlocking {
        order.places.forEach {
            networkSortableUseCases.sort(order.externalId, lot.externalId, it.externalId)
        }
    }

    fun sortToLot(externalId: ExternalId, lot: Lot) = runBlocking {
        val order = networkOrderUseCases.getOrder(externalId)
        sortToLot(order, lot)
    }

    fun prepareToShipLot(lotId: Long) = runBlocking {
        manualLotRepository.prepareToShipLot(lotId)
    }

    fun shipLot(
        externalLotId: ExternalId,
        scId: Long = sortingCenterUtils.getCurrentScId(),
    ) = runBlocking {
        manualLotRepository.shipLot(externalLotId.value, scId)
    }

    fun addStamp(lotExternalId: ExternalId, stampId: ExternalId) = runBlocking {
        networkLotUseCases.addStamp(lotExternalId = lotExternalId, stampId = stampId)
    }
}
