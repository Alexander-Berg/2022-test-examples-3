package ru.yandex.market.mapi.utils

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.util.getAlternativeOffersWithShowPlaces
import ru.yandex.market.mapi.core.AbstractNonSpringTest
import ru.yandex.market.mapi.core.model.action.product.ProductPersonalDiscountPopupAction
import ru.yandex.market.mapi.core.model.enums.MapiColors
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.asResource
import ru.yandex.market.mapi.dto.TextPart
import ru.yandex.market.mapi.section.product.general.ProductOfferInfoSection
import kotlin.test.assertEquals

/* Тесты на форматтирование информации в OfferInfo */
class OfferInfoFeaturesTest : AbstractNonSpringTest() {

    private val response = JsonHelper.parse<ResolveProductOffersResponse>(
        "/utils/resolveProductOffersPromoFormatterTest.json".asResource()
    )

    @Test
    fun testPromocode() {
        val alternativeOffersWithShowPlaces = response.getAlternativeOffersWithShowPlaces()!!
        val actual1 = alternativeOffersWithShowPlaces[0].offer.promocodeText()
        val actual2 = alternativeOffersWithShowPlaces[1].offer.promocodeText()

        assertEquals(
            message = "Промокод 1ого альтернативного оффера",
            expected = listOf(
                TextPart.Text(
                    "Еще -10% по промокоду ",
                    false,
                    MapiColors.BLACK
                ),
                TextPart.Text(
                    "ZFAYCP6T",
                    true,
                    MapiColors.BLACK
                ),
                TextPart.Text(
                    " до 19 июля.",
                    false,
                    MapiColors.BLACK
                ),
                TextPart.Text(
                    " Копировать",
                    false,
                    MapiColors.BLUE_LINK
                )
            ),
            actual = actual1
        )

        assertEquals(
            message = "Промокод 2ого альтернативного оффера",
            expected = listOf(
                TextPart.Text(
                    "Еще -10% по промокоду ",
                    false,
                    MapiColors.BLACK
                ),
                TextPart.Text(
                    "UPBGPFMB",
                    true,
                    MapiColors.BLACK
                ),
                TextPart.Text(
                    " до 31 декабря.",
                    false,
                    MapiColors.BLACK
                ),
                TextPart.Text(
                    " Копировать",
                    false,
                    MapiColors.BLUE_LINK
                )
            ),
            actual = actual2
        )
    }

    @Test
    fun testPromoInfo() {
        val alternativeOffersWithShowPlaces = response.getAlternativeOffersWithShowPlaces()!!
        val cheapestAsGiftActual = alternativeOffersWithShowPlaces[0].offer.formatPromoIcon()

        assertEquals(
            message = "cheapest-as-gift иконка",
            expected = ProductOfferInfoSection.PromoIcon.FOUR_FOR_FIVE,
            actual = cheapestAsGiftActual
        )
    }

    @Test
    fun testPersonalDiscount() {
        val (offer, showPlace) = response.getAlternativeOffersWithShowPlaces()!![1]
        val promos = response.collections?.promo
            ?.filter { showPlace.promoIds?.contains(it.id) ?: false }
        val personalDiscountActual = offer.personalDiscount(promos)

        assertEquals(
            message = "direct-discount текст",
            expected = ProductOfferInfoSection.PersonalDiscount(
                title = "ваша цена",
                subtitle = "ниже до 31 декабря",
                actions = mapOf(
                    "onClick" to ProductPersonalDiscountPopupAction(
                        title = "Вам — персональная скидка. Приятных покупок!",
                        basePrice = ProductPersonalDiscountPopupAction.PriceText("Обычная цена", "945 ₽"),
                        basePriceWithDiscount = null,
                        personalDiscount = ProductPersonalDiscountPopupAction.PriceText("Ваша скидка 15%", "-142 ₽"),
                        totalPrice = ProductPersonalDiscountPopupAction.PriceText("Итого", "803 ₽"),
                        allProductsLink = null,
                    )
                ),
            ),
            actual = personalDiscountActual,
        )
    }

    @Test
    fun testTimer() {
        val alternativeOffersWithShowPlaces = response.getAlternativeOffersWithShowPlaces()!!
        val cheapestAsGiftActual = alternativeOffersWithShowPlaces[0].offer.formatProductTimer()
        val expectedStart = "2019-11-01T12:20:55Z"
        val expectedEnd = "2022-12-31T14:00:04Z"

        assertEquals(
            message = "blue-flash таймер",
            expected = ProductOfferInfoSection.TimerInterval(
                expectedStart, expectedEnd
            ),
            actual = cheapestAsGiftActual
        )
    }
}
