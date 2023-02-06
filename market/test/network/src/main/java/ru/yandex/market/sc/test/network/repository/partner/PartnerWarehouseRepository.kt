package ru.yandex.market.sc.test.network.repository.partner

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.yandex.market.sc.core.utils.data.functional.Functional.orThrow
import ru.yandex.market.sc.test.data.partner.warehouse.PartnerWarehouses
import ru.yandex.market.sc.test.data.partner.warehouse.PartnerWarehousesMapper
import ru.yandex.market.sc.test.network.api.SortingCenterPartnerService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartnerWarehouseRepository @Inject constructor(
    private val sortingCenterPartnerService: SortingCenterPartnerService,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun getWarehouses(scPartnerId: Long): PartnerWarehouses = withContext(ioDispatcher) {
        PartnerWarehousesMapper.map(sortingCenterPartnerService.getWarehouses(scPartnerId))
            .orThrow()
    }
}
