package ru.yandex.market.sc.test.network.data.partner.cell

import ru.yandex.market.sc.core.data.cell.Cell

data class PartnerCellRequest(
    val number: String,
    val status: Cell.Status,
    val type: Cell.Type,
    val subType: Cell.SubType = Cell.SubType.DEFAULT,
    val courierId: Long? = null,
    val warehouseYandexId: String? = null,
)
