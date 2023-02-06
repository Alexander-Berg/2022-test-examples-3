package ru.yandex.market.sc.test.data.manual.inbound

import ru.yandex.market.sc.test.data.common.sortingcenter.SortingCenterDto

data class ManualInboundDto(
    val inboundExternalId: String?,
    val fromDate: String?,
    val toDate: String?,
    val status: String?,
    val sortingCenter: SortingCenterDto?,
    val warehouseFrom: String?,
    val transportationId: String?,
    val inboundType: String?,
    val carNumber: String?,
)
