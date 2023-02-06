package ru.yandex.market.sc.test.network.domain.outbound

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.runBlocking
import ru.yandex.market.sc.core.data.outbound.OutboundType
import ru.yandex.market.sc.core.network.domain.NetworkOutboundUseCases
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.constants.Configuration
import ru.yandex.market.sc.test.network.domain.common.TestSortingCenterUtils
import ru.yandex.market.sc.test.network.repository.manual.ManualOutboundRepository
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestOutboundUseCases @Inject constructor(
    private val manualOutboundRepository: ManualOutboundRepository,
    private val sortingCenterUtils: TestSortingCenterUtils,
    private val networkOutboundUseCases: NetworkOutboundUseCases
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun createDemoOutbound(
        scId: Long = sortingCenterUtils.getCurrentScId(),
        toDate: Date = getDate(hourShift = 4),
        fromDate: Date = getDate(hourShift = -1),
        type: OutboundType = OutboundType.XDOC,
        externalId: ExternalId? = null,
        partnerToExternalId: ExternalId? = null,
        logisticPointToExternalId: ExternalId = Configuration.LOGISTIC_POINT_TO_EXTERNAL_ID,
    ): ExternalId = runBlocking {
        manualOutboundRepository.createDemoOutbound(
            scId,
            toDate,
            fromDate,
            type,
            logisticPointToExternalId.value,
            externalId?.value,
            partnerToExternalId?.value,
        ).let { ExternalId(it) }
    }

    fun putOutboundRegistry(
        scId: Long = sortingCenterUtils.getCurrentScId(),
        outboundExternalId: ExternalId,
        palletExternalIds: List<ExternalId>,
    ) = runBlocking {
        manualOutboundRepository.putOutboundRegistry(
            scId = scId,
            externalId = outboundExternalId.value,
            registryExternalId = "$outboundExternalId-registry",
            palletExternalIds = palletExternalIds,
        )
    }

    fun closeAllOutbounds() = runBlocking {
        runCatching {
            networkOutboundUseCases.getOutbounds(status = null).forEach {
                closeOutbound(it.externalId)
            }
        }
    }

    fun closeOutbound(externalId: ExternalId) = runBlocking {
        manualOutboundRepository.closeOutbound(externalId.value)
    }

    private fun getDate(shift: Int = 0, hourShift: Int = 0): Date = with(Calendar.getInstance()) {
        add(Calendar.DATE, shift)
        add(Calendar.HOUR, hourShift)
        time
    }
}