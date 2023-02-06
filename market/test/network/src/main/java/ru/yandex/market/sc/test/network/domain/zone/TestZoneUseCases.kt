package ru.yandex.market.sc.test.network.domain.zone

import kotlinx.coroutines.runBlocking
import ru.yandex.market.sc.test.data.partner.zone.PartnerZone
import ru.yandex.market.sc.test.data.partner.zone.PartnerZoneRequest
import ru.yandex.market.sc.test.network.domain.common.TestSortingCenterUtils
import ru.yandex.market.sc.test.network.repository.partner.PartnerZoneRepository
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class TestZoneUseCases @Inject constructor(
    private val partnerZoneRepository: PartnerZoneRepository,
    private val sortingCenterUtils: TestSortingCenterUtils,
) {
    fun createZone(
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
        name: String,
        processIds: List<Long> = listOf()
    ): PartnerZone = runBlocking {
        partnerZoneRepository.createZone(
            scPartnerId = scPartnerId,
            request = PartnerZoneRequest(
                name = name,
                processIds = processIds
            )
        )
    }

    fun deleteZone(
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
        id: Long,
    ): PartnerZone = runBlocking {
        partnerZoneRepository.deleteZone(
            scPartnerId = scPartnerId,
            id = id
        )
    }
}
