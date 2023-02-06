package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.core.data.cell.Cell

data class PartnerCellTypes(
    val cellTypes: List<Cell.Type>,
    val midMilesCourierAvailable: List<Cell.Type>,
    val warehouseAvailable: List<Cell.Type>,
)
