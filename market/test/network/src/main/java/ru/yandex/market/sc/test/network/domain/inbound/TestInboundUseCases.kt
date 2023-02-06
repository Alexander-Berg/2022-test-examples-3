package ru.yandex.market.sc.test.network.domain.inbound

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.runBlocking
import ru.yandex.market.sc.core.data.inbound.Inbound
import ru.yandex.market.sc.core.data.sortable.SortableType
import ru.yandex.market.sc.core.network.domain.NetworkInboundUseCases
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.data.manual.inbound.ManualInbound
import ru.yandex.market.sc.test.network.constants.Configuration
import ru.yandex.market.sc.test.network.data.manual.inbound.ManualInboundRequest
import ru.yandex.market.sc.test.network.domain.common.TestSortingCenterUtils
import ru.yandex.market.sc.test.network.repository.manual.ManualInboundRepository
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestInboundUseCases @Inject constructor(
    private val networkInboundUseCases: NetworkInboundUseCases,
    private val manualInboundRepository: ManualInboundRepository,
    private val sortingCenterUtils: TestSortingCenterUtils,
) {
    fun getInbound(externalId: ExternalId): ManualInbound = runBlocking {
        manualInboundRepository.getInbound(externalId.value)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createInbound(
        externalId: ExternalId? = null,
        registryId: ExternalId? = null,
        externalRequestId: ExternalId? = null,
        type: Inbound.Type = Inbound.Type.DEFAULT,
        scId: Long = sortingCenterUtils.getCurrentScId(),
        warehouseFromYandexId: ExternalId = Configuration.WAREHOUSE_SHOP_YANDEX_ID,
        nextLogisticPointId: ExternalId? = Configuration.LOGISTIC_POINT_TO_EXTERNAL_ID,
        transportationId: String? = null,
        placesIdsByOrderId: Map<String, List<String>>? = null,
        orderIdToPalletId: Map<String, String>? = null,
        palletIdToStampId: Map<String, String>? = null,
        fromDate: Date? = null,
        toDate: Date? = null
    ): ManualInbound = runBlocking {
        val request =
            ManualInboundRequest(
                inboundType = type,
                externalRequestId = externalRequestId?.value,
                placesIdsByOrderId = placesIdsByOrderId,
                orderIdToPalletId = orderIdToPalletId,
                palletIdToStampId = palletIdToStampId,
                fromDate = fromDate?.toLocalDateTime()?.toString(),
                toDate = toDate?.toLocalDateTime()?.toString()
            )
        val inboundExternalId = manualInboundRepository.createDemoInbound(
            scId,
            warehouseFromYandexId.value,
            externalId?.value,
            registryId?.value,
            nextLogisticPointId?.value,
            transportationId,
            request
        )
        manualInboundRepository.getInbound(inboundExternalId)
    }

    fun createPallet(inboundExternalId: ExternalId, count: Int = 3): List<ExternalId> {
        return MutableList(count) { ExternalId("$XDOC_PREFIX${inboundExternalId.value}-$it") }
    }

    fun createBox(inboundExternalId: ExternalId, count: Int = 3): List<ExternalId> {
        return MutableList(count) { ExternalId("$XDOC_PREFIX${inboundExternalId.value}-$it") }
    }

    fun fixInbound(externalId: ExternalId) = runBlocking {
        manualInboundRepository.fixInbound(externalId.value)
    }

    fun acceptAndLinkToInbound(
        externalId: ExternalId,
        palletId: ExternalId,
        type: Inbound.Type = Inbound.Type.XDOC_TRANSIT,
        sortableType: SortableType = SortableType.XDOC_PALLET
    ) = runBlocking {
        networkInboundUseCases.acceptInbound(externalId, type)
        networkInboundUseCases.linkToInbound(externalId, palletId, sortableType)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun Date.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()) =
        LocalDateTime.from(toInstant().atZone(zoneId))

    companion object {
        const val XDOC_PREFIX = "XDOC-"
    }
}
