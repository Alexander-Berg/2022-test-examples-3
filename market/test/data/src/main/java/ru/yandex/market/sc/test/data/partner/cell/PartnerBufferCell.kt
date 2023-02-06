package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.core.data.cell.Cell

data class PartnerBufferCell(
    val id: Long,
    val number: String?,
    val ordersToSortCount: Int,
    val ordersTotalCount: Int,
    val sortingCenterId: Long,
    val deleted: Boolean,
    val status: Cell.Status,
    val type: Cell.Type,
    val warehouseYandexId: String? = null,
)
