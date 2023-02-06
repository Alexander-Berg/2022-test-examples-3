package ru.yandex.market.sc.test.data.partner.lot

import ru.yandex.market.sc.test.data.common.Wrapper

data class PartnerLotsWrapper(private val lots: List<PartnerLot>) : Wrapper<List<PartnerLot>> {
    override fun unwrap(): List<PartnerLot> {
        return lots
    }

    companion object {
        fun wrap(value: List<PartnerLot>) = PartnerLotsWrapper(lots = value)
    }
}
