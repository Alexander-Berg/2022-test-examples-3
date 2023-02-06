package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.cell.CellTypeDefinable
import ru.yandex.market.sc.core.utils.data.ExternalId

data class PartnerCell(
    val id: Long,
    val number: String?,
    val status: Cell.Status,
    override val type: Cell.Type,
    override val subType: Cell.SubType,
    val ordersCount: Int,
    val deleted: Boolean,
    val canBeDeleted: Boolean,
    val canBeUpdated: Boolean,
    val sortingCenterId: Long,
    val courierId: Long? = null,
    val warehouseYandexId: ExternalId? = null,
) : CellTypeDefinable
