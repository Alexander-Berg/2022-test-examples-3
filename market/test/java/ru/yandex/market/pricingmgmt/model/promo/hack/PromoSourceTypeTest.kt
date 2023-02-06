package ru.yandex.market.pricingmgmt.model.promo.hack

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.pricingmgmt.model.promo.PromoSourceType
import ru.yandex.market.pricingmgmt.model.promo.PromoSourceType.Companion.getByPromoId

internal class PromoSourceTypeTest{
    @Test
    fun getByPromoId_anaplan() {
        Assertions.assertEquals(PromoSourceType.ANAPLAN, getByPromoId("#123"))
    }

    @Test
    fun getByPromoId_ciface() {
        Assertions.assertEquals(PromoSourceType.CIFACE, getByPromoId("cf_123"))
    }

    @Test
    fun getByPromoId_empty() {
        Assertions.assertEquals(PromoSourceType.UNKNOWN, getByPromoId(""))
    }

    @Test
    fun getByPromoId_unknown() {
        Assertions.assertEquals(PromoSourceType.UNKNOWN, getByPromoId("unknown"))
    }
}
