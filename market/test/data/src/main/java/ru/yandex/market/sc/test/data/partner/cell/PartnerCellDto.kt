package ru.yandex.market.sc.test.data.partner.cell

data class PartnerCellDto(
    val id: Long?,
    val number: String?,
    val status: String?,
    val type: String?,
    val subType: String?,
    val ordersCount: Int?,
    val deleted: Boolean?,
    val canBeDeleted: Boolean?,
    val canBeUpdated: Boolean?,
    val sortingCenterId: Long?,
    val courierId: Long?,
    val warehouseYandexId: String?,
)