package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.data.validation.Validation.validateAll
import ru.yandex.market.sc.core.utils.ext.parseOrDefault

object PartnerBufferCellMapper {
    fun map(dto: PartnerBufferCellDto): Exceptional<PartnerBufferCell> = catch {
        validateAll {
            notNull(dto.id, "id")
            notNull(dto.sortingCenterId, "sortingCenterId")
        }

        PartnerBufferCell(
            id = dto.id!!,
            number = dto.number,
            ordersToSortCount = dto.ordersToSortCount ?: 0,
            ordersTotalCount = dto.ordersTotalCount ?: 0,
            sortingCenterId = dto.sortingCenterId!!,
            deleted = dto.deleted ?: false,
            status = parseOrDefault(dto.status, Cell.Status.UNKNOWN),
            type = parseOrDefault(dto.type, Cell.Type.UNKNOWN),
            warehouseYandexId = dto.warehouseYandexId,
        )
    }
}
