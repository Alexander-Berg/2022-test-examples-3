package ru.yandex.market.pricingmgmt.util.promo.hack

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.yandex.market.pricingmgmt.util.promo.hack.HackPlanUtils.calcMinPlan
import ru.yandex.market.pricingmgmt.util.promo.hack.HackPlanUtils.formatPrice

internal class HackPlanUtilsTest {

    @ParameterizedTest
    @CsvSource(value = ["1 234,56 руб.:123456", "1 234,00 руб.:123400", "1,23 руб.:123"], delimiter = ':')
    fun formatPrice_ok(expected: String, price: Long) {
        assertEquals(expected, formatPrice(price))
    }

    @ParameterizedTest
    @CsvSource(value = ["3,5,2", "5,5,1", "0, 0, 300"])
    fun calcMinPlan_ok(expected: Long, minPlanInPrice: Long, price: Long) {
        assertEquals(expected, calcMinPlan(minPlanInPrice, price))
    }
}
