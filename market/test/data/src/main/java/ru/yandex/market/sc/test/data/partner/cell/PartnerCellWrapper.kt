package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.test.data.common.Wrapper

data class PartnerCellWrapper(val cell: PartnerCell) : Wrapper<PartnerCell> {
    override fun unwrap(): PartnerCell {
        return cell
    }

    fun asCell(): Cell {
        return this.unwrap().let(PartnerCellMapper::mapToCell)
    }

    companion object {
        fun wrap(value: PartnerCell) = PartnerCellWrapper(cell = value)
    }
}

