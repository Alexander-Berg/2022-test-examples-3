package ru.yandex.market.sc.test.data.partner.lot

import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.data.sortable.SortableType.UNKNOWN
import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.PageDtoMapper
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.data.validation.Validation.validateAll
import ru.yandex.market.sc.core.utils.ext.parseOrDefault

object PartnerLotMapper : PageDtoMapper<PartnerLotDto, PartnerLot> {
    override fun map(dto: PartnerLotDto): Exceptional<PartnerLot> = catch {
        validateAll {
            notNull(dto.id, "id")
        }

        PartnerLot(
            id = dto.id!!,
            name = dto.name,
            cellName = dto.cellName!!,
            category = parseOrDefault(dto.category, Cell.SubType.UNKNOWN),
            type = parseOrDefault(dto.type, UNKNOWN),
            status = parseOrDefault(dto.status, Lot.Status.UNKNOWN),
            createdAt = dto.createdAt,
            warehouse = dto.warehouse,
        )
    }
}
