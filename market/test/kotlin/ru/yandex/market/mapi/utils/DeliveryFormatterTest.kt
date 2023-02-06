package ru.yandex.market.mapi.utils

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.client.fapi.util.getAlternativeOffersWithShowPlaces
import ru.yandex.market.mapi.core.AbstractNonSpringTest
import ru.yandex.market.mapi.core.model.enums.MapiColors
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.asResource
import ru.yandex.market.mapi.dto.DeliveryOption
import ru.yandex.market.mapi.dto.TextPart
import kotlin.test.assertEquals

class DeliveryFormatterTest : AbstractNonSpringTest() {

    private val response = JsonHelper.parse<ResolveProductOffersResponse>(
        "/utils/resolveProductOffersDeliveryFormatterTest1.json".asResource()
    )

    @Test
    fun testDelivery() {
        val alternativeOffersWithShowPlaces = response.getAlternativeOffersWithShowPlaces()!!
        val firstActual = formatOfferDelivery(alternativeOffersWithShowPlaces[0].offer)
        val secondActual = formatOfferDelivery(alternativeOffersWithShowPlaces[1].offer)
        val thirdActual = formatOfferDelivery(alternativeOffersWithShowPlaces[2].offer)

        assertEquals(
            message = "Сформированная доставка отличается от ожидаемой в первом оффере",
            expected = DeliveryOption(
                text = listOf(
                    TextPart.Text(
                        text = "Курьером ",
                        color = MapiColors.COAL_BLACK,
                    ),
                    TextPart.Text(
                        text = "в четверг, 17 марта ",
                        color = MapiColors.COAL_BLACK,
                    ),
                    TextPart.Text(
                        text = "— ",
                        color = MapiColors.COAL_BLACK,
                    ),
                    TextPart.Text(
                        text = "0 ₽",
                        bold = true,
                        color = MapiColors.COAL_BLACK,
                    )
                )
            ),
            actual = firstActual.options[0],
        )

        assertEquals(
            message = "Сформированная доставка отличается от ожидаемой в первом оффере",
            expected = DeliveryOption(
                text = listOf(
                    TextPart.Text(
                        text = "Самовывозом ",
                        color = MapiColors.COAL_BLACK,
                    ),
                    TextPart.Text(
                        text = "17 — 19 марта ",
                        color = MapiColors.COAL_BLACK,
                    ),
                    TextPart.Text(
                        text = "— ",
                        color = MapiColors.COAL_BLACK,
                    ),
                    TextPart.Text(
                        text = "бесплатно",
                        bold = true,
                        color = MapiColors.GRASS_GREEN,
                    )
                )
            ),
            actual = firstActual.options[1],
        )

        assertEquals(
            message = "Сформированная доставка отличается от ожидаемой во втором оффере",
            expected = DeliveryOption(
                text = listOf(
                    TextPart.Text(
                        text = "Курьером ",
                        color = MapiColors.COAL_BLACK,
                    ),
                    TextPart.Text(
                        text = "завтра, 16 марта ",
                        color = MapiColors.COAL_BLACK,
                    ),
                    TextPart.Text(
                        text = "— ",
                        color = MapiColors.COAL_BLACK,
                    ),
                    TextPart.Text(
                        text = "0 ₽",
                        bold = true,
                        color = MapiColors.COAL_BLACK,
                    )
                )
            ),
            actual = secondActual.options[0],
        )

        assertEquals(
            message = "Сформированная доставка отличается от ожидаемой в третьем оффере",
            expected = DeliveryOption(
                text = listOf(
                    TextPart.Text(
                        text = "Самовывозом ",
                        color = MapiColors.COAL_BLACK,
                    ),
                    TextPart.Text(
                        text = "— ",
                        color = MapiColors.COAL_BLACK,
                    ),
                    TextPart.Text(
                        text = "бесплатно",
                        bold = true,
                        color = MapiColors.GRASS_GREEN,
                    )
                )
            ),
            actual = thirdActual.options[1],
        )
    }
}
