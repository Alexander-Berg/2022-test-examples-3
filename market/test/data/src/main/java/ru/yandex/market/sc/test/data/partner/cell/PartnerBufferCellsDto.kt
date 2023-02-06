package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.test.data.common.Wrapper

data class PartnerBufferCellsDto(
    val cells: List<PartnerBufferCellDto>?,
    val ordersToSortTotal: Int?,
    val ordersTotal: Int?,
) : Wrapper<List<PartnerBufferCellDto>?> {
    override fun unwrap(): List<PartnerBufferCellDto>? {
        return cells
    }
}
