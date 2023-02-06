package ru.yandex.market.sc.test.network.domain.warehouse

import kotlinx.coroutines.runBlocking
import ru.yandex.market.sc.test.data.partner.warehouse.PartnerWarehouses
import ru.yandex.market.sc.test.network.domain.common.TestSortingCenterUtils
import ru.yandex.market.sc.test.network.repository.partner.PartnerWarehouseRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestWarehouseUseCases @Inject constructor(
    private val partnerWarehouseRepository: PartnerWarehouseRepository,
    private val sortingCenterUtils: TestSortingCenterUtils,
) {
    fun getWarehouses(scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId()): PartnerWarehouses =
        runBlocking {
            partnerWarehouseRepository.getWarehouses(scPartnerId)
        }
}