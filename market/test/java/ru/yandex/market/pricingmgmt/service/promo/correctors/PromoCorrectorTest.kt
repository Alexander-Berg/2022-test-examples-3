package ru.yandex.market.pricingmgmt.service.promo.correctors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.yandex.market.pricingmgmt.model.promo.AssortmentLoadMethod
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CheapestAsGift
import ru.yandex.market.pricingmgmt.model.promo.mechanics.Promocode

internal class PromoCorrectorTest {
    private val promoCorrector = PromoCorrector()

    @Test
    fun correct_nothing() {
        val actual = Promo()
        actual.mechanicsType = PromoMechanicsType.PROMO_CODE
        actual.promocode = Promocode()

        val expected = Promo()
        expected.mechanicsType = PromoMechanicsType.PROMO_CODE
        expected.promocode = Promocode()

        promoCorrector.correct(actual)

        assertEquals(expected, actual)
    }

    @Test
    fun correct_skip() {
        val actual = Promo()
        actual.mechanicsType = null
        actual.promocode = Promocode()
        actual.cheapestAsGift = CheapestAsGift()

        val expected = Promo()
        expected.mechanicsType = null
        expected.promocode = Promocode()
        expected.cheapestAsGift = CheapestAsGift()

        promoCorrector.correct(actual)

        assertEquals(expected, actual)
    }

    @Test
    fun correct_remove_promocode() {
        val actual = Promo()
        actual.mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT
        actual.promocode = Promocode()
        actual.cheapestAsGift = CheapestAsGift()

        val expected = Promo()
        expected.mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT
        expected.promocode = null
        expected.cheapestAsGift = CheapestAsGift()

        promoCorrector.correct(actual)

        assertEquals(expected, actual)
    }

    @Test
    fun correct_remove_cheapestAsGift() {
        val actual = Promo()
        actual.mechanicsType = PromoMechanicsType.PROMO_CODE
        actual.promocode = Promocode()
        actual.cheapestAsGift = CheapestAsGift()

        val expected = Promo()
        expected.mechanicsType = PromoMechanicsType.PROMO_CODE
        expected.promocode = Promocode()
        expected.cheapestAsGift = null

        promoCorrector.correct(actual)

        assertEquals(expected, actual)
    }

    @Test
    fun correct_remove_both() {
        val actual = Promo()
        actual.mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT
        actual.promocode = Promocode()
        actual.cheapestAsGift = CheapestAsGift()

        val expected = Promo()
        expected.mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT
        expected.promocode = null
        expected.cheapestAsGift = null

        promoCorrector.correct(actual)

        assertEquals(expected, actual)
    }

    @Test
    fun correct_loyalty_promocode() {
        val actual = Promo()
        actual.mechanicsType = PromoMechanicsType.PROMO_CODE
        actual.assortmentLoadMethod = AssortmentLoadMethod.LOYALTY
        actual.promocode = Promocode()
        actual.cheapestAsGift = CheapestAsGift()

        val expected = Promo()
        expected.mechanicsType = PromoMechanicsType.PROMO_CODE
        expected.assortmentLoadMethod = AssortmentLoadMethod.LOYALTY
        expected.promocode = null
        expected.cheapestAsGift = null

        promoCorrector.correct(actual)

        assertEquals(expected, actual)
    }
}
