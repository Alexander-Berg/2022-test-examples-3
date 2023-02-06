package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.test.data.common.Wrapper

data class PartnerCellDtoWrapper(val cell: PartnerCellDto?) : Wrapper<PartnerCellDto?> {
    override fun unwrap(): PartnerCellDto? {
        return cell
    }

}
