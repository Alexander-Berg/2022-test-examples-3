package ru.yandex.market.sc.test.network.repository.manual

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.data.lot.LotMapper
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.core.utils.data.functional.Functional.orThrow
import ru.yandex.market.sc.test.data.partner.lot.PartnerLot
import ru.yandex.market.sc.test.data.partner.lot.PartnerLotMapper
import ru.yandex.market.sc.test.data.partner.lot.PartnerLotRequest
import ru.yandex.market.sc.test.network.api.SortingCenterManualService
import ru.yandex.market.sc.test.network.data.partner.lot.PartnerOrphanLotRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManualLotRepository @Inject constructor(
    private val sortingCenterManualService: SortingCenterManualService,
    private val lotMapper: LotMapper,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun prepareToShipLot(lotId: Long) = withContext(ioDispatcher) {
        sortingCenterManualService.prepareToShipLot(lotId)
    }

    suspend fun shipLot(externalLotId: String, scId: Long) = withContext(ioDispatcher) {
        sortingCenterManualService.shipLot(externalLotId, scId)
    }

    suspend fun getLotsById(lotIds: Set<Long>): List<Lot> = withContext(ioDispatcher) {
        lotIds.map { async { lotMapper.map(sortingCenterManualService.getLotById(it)).orThrow() } }.awaitAll()

    }

    suspend fun getLotByExternalId(lotExternalId: String, scId: Long): PartnerLot = withContext(ioDispatcher) {
        PartnerLotMapper.map(sortingCenterManualService.getLotByExternalId(lotExternalId, scId)).orThrow()
    }

    suspend fun createLots(cellId: Long, count: Int, scId: Long): List<Long> = withContext(ioDispatcher) {
        sortingCenterManualService.createLots(scId, PartnerLotRequest(cellId, count))
    }

    suspend fun createOrphanLots(count: Int, scId: Long): List<ExternalId> = withContext(ioDispatcher) {
        sortingCenterManualService.createOrphanLots(scId, PartnerOrphanLotRequest(count)).map { ExternalId(it) }
    }
}
