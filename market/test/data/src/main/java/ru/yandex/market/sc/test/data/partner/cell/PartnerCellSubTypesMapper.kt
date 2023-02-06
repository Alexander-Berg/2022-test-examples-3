package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.ext.parseOrDefault

object PartnerCellSubTypesMapper {
    fun map(dto: PartnerCellSubTypesDto): Exceptional<PartnerCellSubTypes> = catch {
        PartnerCellSubTypes(
            subTypes = dto.subTypes.orEmpty()
                .mapKeys { parseOrDefault(it.key, Cell.Type.UNKNOWN) }
                .mapValues {
                    it.value.map { subType ->
                        parseOrDefault(
                            subType,
                            Cell.SubType.UNKNOWN
                        )
                    }
                }
        )
    }
}
