package ru.yandex.market.sc.test.network.data.manual.outbound

import ru.yandex.market.sc.core.data.outbound.OutboundType
import ru.yandex.market.sc.test.data.manual.courier.ManualOutboundCourier

data class ManualOutboundRequest(
    val courier: ManualOutboundCourier? = null,
    val outboundType: OutboundType? = null,
    val numberOfOrders: Long? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
)
