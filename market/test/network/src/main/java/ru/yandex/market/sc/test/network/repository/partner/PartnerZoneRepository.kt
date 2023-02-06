package ru.yandex.market.sc.test.network.repository.partner

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.yandex.market.sc.core.utils.data.functional.Functional.orThrow
import ru.yandex.market.sc.test.data.partner.zone.PartnerZone
import ru.yandex.market.sc.test.data.partner.zone.PartnerZoneMapper
import ru.yandex.market.sc.test.data.partner.zone.PartnerZoneRequest
import ru.yandex.market.sc.test.network.api.SortingCenterPartnerService
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PartnerZoneRepository @Inject constructor(
    private val sortingCenterPartnerService: SortingCenterPartnerService,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun createZone(scPartnerId: Long, request: PartnerZoneRequest): PartnerZone =
        withContext(ioDispatcher) {
            PartnerZoneMapper.map(sortingCenterPartnerService.createZone(scPartnerId, request))
                .orThrow()
        }

    suspend fun getZone(scPartnerId: Long, id: Long): PartnerZone = withContext(ioDispatcher) {
        PartnerZoneMapper.map(sortingCenterPartnerService.getZone(scPartnerId, id)).orThrow()
    }

    suspend fun deleteZone(scPartnerId: Long, id: Long): PartnerZone = withContext(ioDispatcher) {
        PartnerZoneMapper.map(sortingCenterPartnerService.deleteZone(scPartnerId, id)).orThrow()
    }
}
