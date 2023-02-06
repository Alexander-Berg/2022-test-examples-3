package ru.yandex.market.sc.test.data.common.inbound

data class InboundRegistryOrderDto(
    val orderExternalId: String?,
    val placeExternalId: String?,
    val status: String?,
    val palledId: String?,
)
