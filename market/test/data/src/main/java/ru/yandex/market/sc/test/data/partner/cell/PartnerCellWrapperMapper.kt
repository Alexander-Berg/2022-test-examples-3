package ru.yandex.market.sc.test.data.partner.cell

import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.data.functional.Functional.orThrow
import ru.yandex.market.sc.core.utils.data.validation.Validation.validateAll

object PartnerCellWrapperMapper {
    fun map(dto: PartnerCellDtoWrapper): Exceptional<PartnerCellWrapper> = catch {
        validateAll {
            notNull(dto.cell, "cell")
        }

        requireNotNull(dto.cell)

        PartnerCellWrapper(
            cell = dto.cell.let(PartnerCellMapper::map).orThrow()
        )
    }
}