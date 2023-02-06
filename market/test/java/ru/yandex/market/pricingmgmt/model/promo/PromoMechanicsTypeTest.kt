package ru.yandex.market.pricingmgmt.model.promo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class PromoMechanicsTypeTest {
    private fun unique(function: (PromoMechanicsType) -> Any?) {
        assertFalse(
            PromoMechanicsType
                .values()
                .filter { function.invoke(it) != null }
                .groupBy { function.invoke(it) }
                .filter { it.value.size > 1 }
                .any()
        )
    }

    @Test
    fun uniqueCodes() {
        unique { promoMechanicsType -> promoMechanicsType.code }
    }

    @Test
    fun uniqueClientValues() {
        unique { promoMechanicsType -> promoMechanicsType.clientValue }
    }

    @Test
    fun onlyUnknownWithoutClientValue(){
        val noClientValue =  PromoMechanicsType
            .values()
            .filter { it.clientValue == null }
            .toList()

        assertEquals(1, noClientValue.size)
        assertEquals(PromoMechanicsType.UNKNOWN, noClientValue[0])
    }
}
