package ru.yandex.market.sc.test.data.partner.lot

import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.data.functional.Functional.filterSuccessValues

object PartnerLotsDtoWrapperMapper {
    fun map(dto: PartnerLotsDtoWrapper): Exceptional<PartnerLotsWrapper> = catch {
        PartnerLotsWrapper(
            lots = dto.lots
                .orEmpty()
                .map(PartnerLotMapper::map)
                .filterSuccessValues()
        )
    }
}