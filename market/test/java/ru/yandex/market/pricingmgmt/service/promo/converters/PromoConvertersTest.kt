package ru.yandex.market.pricingmgmt.service.promo.converters

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest

internal class PromoConvertersTest : AbstractFunctionalTest() {

    @Autowired
    lateinit var promoConverters: PromoConverters

    @Test
    fun getCategoryRestrictionExtendedConverter() {
        assertNotNull(promoConverters.categoryRestrictionPromoConverter)
    }

    @Test
    fun getMskuRestrictionExtendedConverter() {
        assertNotNull(promoConverters.mskuRestrictionPromoConverter)
    }

    @Test
    fun getPartnerRestrictionExtendedConverter() {
        assertNotNull(promoConverters.partnerRestrictionPromoConverter)
    }

    @Test
    fun getVendorRestrictionExtendedConverter() {
        assertNotNull(promoConverters.vendorRestrictionPromoConverter)
    }
}
