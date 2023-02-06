package ru.yandex.market.sc.test.network.data.manual.inbound

import ru.yandex.market.sc.core.data.inbound.Inbound
import ru.yandex.market.sc.test.data.manual.courier.ManualInboundCourier

data class ManualInboundRequest(
    val courier: ManualInboundCourier? = null,
    val inboundType: Inbound.Type? = null,
    val externalRequestId: String? = null,
    val numberOfOrders: Long? = null,
    val placesIdsByOrderId: Map<String, List<String>>? = null,
    val palletIdToStampId: Map<String, String>? = null,
    val orderIdToPalletId: Map<String, String>? = null,
    val regularOrdersIds: List<String>? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
)
