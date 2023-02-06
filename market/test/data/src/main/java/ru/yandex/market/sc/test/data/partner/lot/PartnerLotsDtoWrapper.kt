package ru.yandex.market.sc.test.data.partner.lot

import ru.yandex.market.sc.test.data.common.Wrapper


data class PartnerLotsDtoWrapper(val lots: List<PartnerLotDto>?) : Wrapper<List<PartnerLotDto>?> {
    override fun unwrap(): List<PartnerLotDto>? {
        return lots
    }

    companion object {
        fun wrap(value: List<PartnerLotDto>?) = PartnerLotsDtoWrapper(lots = value)
    }
}
