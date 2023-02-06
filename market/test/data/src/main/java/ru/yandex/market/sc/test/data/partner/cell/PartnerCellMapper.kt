package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.core.utils.data.PageDtoMapper
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.data.validation.Validation.validateAll
import ru.yandex.market.sc.core.utils.ext.parseOrDefault

object PartnerCellMapper : PageDtoMapper<PartnerCellDto, PartnerCell> {
    override fun map(dto: PartnerCellDto): Exceptional<PartnerCell> = catch {
        validateAll {
            notNull(dto.id, "id")
            notNull(dto.sortingCenterId, "sortingCenterId")
        }

        PartnerCell(
            id = dto.id!!,
            number = dto.number,
            status = parseOrDefault(dto.status, Cell.Status.UNKNOWN),
            type = parseOrDefault(dto.type, Cell.Type.UNKNOWN),
            subType = parseOrDefault(dto.subType, Cell.SubType.UNKNOWN),
            ordersCount = dto.ordersCount ?: 0,
            deleted = dto.deleted ?: false,
            canBeDeleted = dto.canBeDeleted ?: true,
            canBeUpdated = dto.canBeUpdated ?: true,
            sortingCenterId = dto.sortingCenterId!!,
            courierId = dto.courierId,
            warehouseYandexId = dto.warehouseYandexId?.let { ExternalId(it) },
        )
    }

    fun mapToCell(cell: PartnerCell) = Cell(
        id = cell.id,
        status = cell.status,
        placeCount = cell.ordersCount,
        type = cell.type,
        subType = cell.subType,
        number = cell.number ?: "без названия",
        cargoType = Cell.CargoType.UNKNOWN
    )
}
