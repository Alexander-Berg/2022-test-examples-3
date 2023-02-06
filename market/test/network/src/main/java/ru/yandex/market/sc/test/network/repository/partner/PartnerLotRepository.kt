package ru.yandex.market.sc.test.network.repository.partner

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.yandex.market.sc.core.network.arch.data.Page
import ru.yandex.market.sc.core.network.data.page.PageMapper
import ru.yandex.market.sc.core.utils.data.functional.Functional.orThrow
import ru.yandex.market.sc.test.data.partner.lot.*
import ru.yandex.market.sc.test.network.api.SortingCenterPartnerService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartnerLotRepository @Inject constructor(
    private val sortingCenterPartnerService: SortingCenterPartnerService,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val pageLotMapper = PageMapper(PartnerLotMapper)

    suspend fun getLots(scPartnerId: Long, cellIds: List<Long>?): Page<PartnerLot> =
        withContext(ioDispatcher) {
            pageLotMapper.map(sortingCenterPartnerService.getLots(scPartnerId, cellIds)).orThrow()
        }

    suspend fun createLot(scPartnerId: Long, request: PartnerLotRequest): PartnerLotsWrapper =
        withContext(ioDispatcher) {
            PartnerLotsDtoWrapperMapper.map(
                sortingCenterPartnerService.createLot(
                    scPartnerId,
                    request
                )
            ).orThrow()
        }

    suspend fun getLot(scPartnerId: Long, lotId: Long): PartnerLotsWrapper =
        withContext(ioDispatcher) {
            PartnerLotsDtoWrapperMapper.map(sortingCenterPartnerService.getLot(scPartnerId, lotId))
                .orThrow()
        }

    suspend fun deleteLot(scPartnerId: Long, lotId: Long): PartnerLotsWrapper =
        withContext(ioDispatcher) {
            PartnerLotsDtoWrapperMapper.map(
                sortingCenterPartnerService.deleteLot(
                    scPartnerId,
                    lotId
                )
            ).orThrow()
        }

    suspend fun shipLots(scPartnerId: Long, routeId: Long) = withContext(ioDispatcher) {
        sortingCenterPartnerService.shipLots(scPartnerId, routeId)
    }
}
