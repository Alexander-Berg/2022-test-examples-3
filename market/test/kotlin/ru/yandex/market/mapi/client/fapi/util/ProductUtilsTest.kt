package ru.yandex.market.mapi.client.fapi.util

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveProductOffersResponse
import ru.yandex.market.mapi.core.util.JsonHelper
import ru.yandex.market.mapi.core.util.asResource
import kotlin.test.assertEquals

class ProductUtilsTest {

    private val response = JsonHelper.parse<ResolveProductOffersResponse>(
        "/product/resolveProductOffersForProductUtils.json".asResource()
    )

    @Test
    fun testDefaultOffer() {
        val defaultOffer = response.getDefaultOffer()
        assertEquals(
            message = "Ожидался дефолтный оффер с другим id",
            expected = "S8mNlH7G7DFc7BcHaAdBQA",
            actual = defaultOffer?.id
        )
    }

    @Test
    fun testExpressOffer() {
        val defaultOffer = response.getExpressOffer()
        assertEquals(
            message = "Ожидался express-оффер с другим id",
            expected = "swdGuvqBtKvlBzXVsl7dJg",
            actual = defaultOffer?.id
        )
    }

    @Test
    fun testAlternativeOffers() {
        val alternativeOffers = response.getAlternativeOffersWithShowPlaces()
        val alternativeOfferIds = alternativeOffers?.map { it.offer.id }
        assertEquals(
            message = "Оффера, их количество или их порядок не совпадает",
            expected = arrayListOf(
                "WIDSfli-PnBLJDYuMbueLA",
                "swdGuvqBtKvlBzXVsl7dJg",
                "S8mNlH7G7DFc7BcHaAdBQA"
            ),
            actual = alternativeOfferIds
        )
    }
}
