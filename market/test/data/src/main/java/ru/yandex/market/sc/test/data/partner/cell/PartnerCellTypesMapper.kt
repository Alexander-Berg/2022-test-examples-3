package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.ext.parseOrDefault

object PartnerCellTypesMapper {
    fun map(dto: PartnerCellTypesDto): Exceptional<PartnerCellTypes> = catch {
        PartnerCellTypes(
            cellTypes = dto.cellTypes?.map { parseOrDefault(it, Cell.Type.UNKNOWN) } ?: listOf(),
            midMilesCourierAvailable = dto.midMilesCourierAvailable?.map {
                parseOrDefault(
                    it,
                    Cell.Type.UNKNOWN
                )
            }
                ?: listOf(),
            warehouseAvailable = dto.warehouseAvailable?.map {
                parseOrDefault(
                    it,
                    Cell.Type.UNKNOWN
                )
            } ?: listOf(),
        )
    }
}
