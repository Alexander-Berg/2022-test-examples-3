package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.core.data.cell.Cell

data class PartnerCellSubTypes(
    val subTypes: Map<Cell.Type, List<Cell.SubType>>,
)
