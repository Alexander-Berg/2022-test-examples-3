package ru.yandex.market.sc.test.data.common.inbound

import ru.yandex.market.sc.core.utils.data.ExternalId

data class InboundRegistryOrder(
    val orderExternalId: ExternalId,
    val placeExternalId: ExternalId,
    val status: Status,
    val palledId: ExternalId,
) {
    enum class Status {
        CREATED,
        ACCEPTED,
        FIXED,
        UNKNOWN,
    }
}
