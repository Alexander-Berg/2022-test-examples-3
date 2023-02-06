package ru.yandex.market.sc.test.data.manual.inbound

import ru.yandex.market.sc.core.data.inbound.Inbound
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.data.common.sortingcenter.SortingCenter

data class ManualInbound(
    val externalId: ExternalId,
    val fromDate: String,
    val toDate: String,
    val status: Inbound.Status,
    val sortingCenter: SortingCenter,
    val warehouseFrom: String,
    val transportationId: ExternalId,
    val inboundType: Inbound.Type,
    val carNumber: String? = null,
)
