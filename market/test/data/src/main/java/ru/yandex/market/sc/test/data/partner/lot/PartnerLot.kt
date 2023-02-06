package ru.yandex.market.sc.test.data.partner.lot

import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.data.sortable.SortableType

data class PartnerLot(
    val id: Long,
    val name: String?,
    val cellName: String,
    val category: Cell.SubType,
    val status: Lot.Status,
    val createdAt: String? = null,
    val warehouse: String? = null,
    val type: SortableType,
)
