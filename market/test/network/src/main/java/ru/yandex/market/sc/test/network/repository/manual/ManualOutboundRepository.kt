package ru.yandex.market.sc.test.network.repository.manual

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.yandex.market.sc.core.data.outbound.OutboundType
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.api.SortingCenterManualService
import ru.yandex.market.sc.test.network.data.manual.outbound.ManualOutboundRequest
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManualOutboundRepository @Inject constructor(
    private val sortingCenterManualService: SortingCenterManualService,
    private val ioDispatcher: CoroutineDispatcher,
) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createDemoOutbound(
        scId: Long,
        toDate: Date,
        fromDate: Date,
        type: OutboundType,
        logisticPointToExternalId: String,
        externalId: String? = null,
        partnerToExternalId: String? = null,
    ): String =
        withContext(ioDispatcher) {
            val request = ManualOutboundRequest(
                outboundType = type,
                fromDate = fromDate.toLocalDateTime().toString(),
                toDate = toDate.toLocalDateTime().toString()
            )
            sortingCenterManualService.createDemoOutbound(
                scId,
                externalId,
                partnerToExternalId,
                logisticPointToExternalId,
                request
            )
        }

    suspend fun putOutboundRegistry(
        scId: Long,
        externalId: String,
        registryExternalId: String,
        palletExternalIds: List<ExternalId>,
    ) = withContext(ioDispatcher) {
        sortingCenterManualService.putOutboundRegistry(
            externalId,
            scId,
            registryExternalId,
            palletExternalIds = palletExternalIds.map { it.value },
        )
    }

    suspend fun closeOutbound(externalId: String) {
        sortingCenterManualService.closeOutbound(externalId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun Date.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()) =
        LocalDateTime.from(toInstant().atZone(zoneId))
}
