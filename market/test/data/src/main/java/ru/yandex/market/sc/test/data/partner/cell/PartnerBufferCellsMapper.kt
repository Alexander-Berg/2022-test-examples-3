package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.data.functional.Functional.filterSuccessValues

object PartnerBufferCellsMapper {
    fun map(dto: PartnerBufferCellsDto): Exceptional<PartnerBufferCells> = catch {
        PartnerBufferCells(
            cells = dto.cells?.map(PartnerBufferCellMapper::map)?.filterSuccessValues() ?: listOf(),
            ordersToSortTotal = dto.ordersToSortTotal ?: 0,
            ordersTotal = dto.ordersTotal ?: 0,
        )
    }
}