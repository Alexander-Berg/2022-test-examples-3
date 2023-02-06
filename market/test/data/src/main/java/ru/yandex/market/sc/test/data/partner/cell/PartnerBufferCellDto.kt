package ru.yandex.market.sc.test.data.partner.cell

data class PartnerBufferCellDto(
    val id: Long?,
    val number: String?,
    val ordersToSortCount: Int?,
    val ordersTotalCount: Int?,
    val sortingCenterId: Long?,
    val deleted: Boolean?,
    val status: String?,
    val type: String?,
    val warehouseYandexId: String?,
)
