package ru.yandex.market.sc.test.network.repository.manual

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.yandex.market.sc.core.utils.data.functional.Functional.orThrow
import ru.yandex.market.sc.test.data.manual.inbound.ManualInbound
import ru.yandex.market.sc.test.data.manual.inbound.ManualInboundMapper
import ru.yandex.market.sc.test.network.api.SortingCenterManualService
import ru.yandex.market.sc.test.network.data.manual.inbound.ManualInboundRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManualInboundRepository @Inject constructor(
    private val sortingCenterManualService: SortingCenterManualService,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun getInbound(externalId: String): ManualInbound = withContext(ioDispatcher) {
        ManualInboundMapper.map(sortingCenterManualService.getInbound(externalId)).orThrow()
    }

    suspend fun createDemoInbound(
        scId: Long,
        warehouseFromYandexId: String,
        externalId: String? = null,
        registryId: String? = null,
        nextLogisticPointId: String? = null,
        transportationId: String? = null,
        request: ManualInboundRequest? = null,
    ): String = withContext(ioDispatcher) {
        if (request != null) sortingCenterManualService.createDemoInbound(
            scId,
            warehouseFromYandexId,
            externalId,
            registryId,
            nextLogisticPointId,
            transportationId,
            request,
        ) else sortingCenterManualService.createDemoInbound(
            scId,
            warehouseFromYandexId,
            externalId,
            registryId,
            nextLogisticPointId,
            transportationId
        )
    }

    suspend fun fixInbound(externalId: String) = withContext(ioDispatcher) {
        sortingCenterManualService.fixInbound(externalId)
    }
}