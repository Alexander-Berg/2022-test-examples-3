package ru.yandex.market.sc.test.data.common.sortingcenter

import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.data.validation.Validation.validateAll
import ru.yandex.market.sc.test.data.constants.Global.DEFAULT
import ru.yandex.market.sc.test.data.constants.Global.DEFAULT_TOKEN

object SortingCenterMapper {
    fun map(dto: SortingCenterDto): Exceptional<SortingCenter> = catch {
        validateAll {
            notNull(dto.id, "id")
        }

        SortingCenter(
            id = dto.id!!,
            address = dto.address ?: DEFAULT,
            scName = dto.scName ?: DEFAULT,
            regionTagSuffix = dto.regionTagSuffix ?: DEFAULT,
            token = dto.token ?: DEFAULT_TOKEN,
            partnerName = dto.partnerName ?: DEFAULT,
            partnerId = dto.partnerId,
        )
    }
}