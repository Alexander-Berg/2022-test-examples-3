package ru.yandex.market.mapi.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.enums.ReasonToBuyType
import ru.yandex.market.mapi.client.fapi.model.FapiOffer
import ru.yandex.market.mapi.client.fapi.model.FapiProduct
import ru.yandex.market.mapi.core.AbstractNonSpringTest
import ru.yandex.market.mapi.core.util.mockFlags
import ru.yandex.market.mapi.core.util.mockRearrs
import ru.yandex.market.mapi.dto.BadgeParams

class TokenBadgeBuilderTest: AbstractNonSpringTest() {

    @Test
    fun exclusiveTest() {
        val product = FapiProduct()
        product.isExclusive = true

        val result = TokenBadgeBuilder.build(product)
        assertEquals(result, listOf(BadgeParams.Exclusive))
    }

    @Test
    fun latestTest() {
        mockFlags("hypeGoodsOnKt")
        val product = FapiProduct()
        product.reasonsToBuy = listOf(FapiProduct.ReasonToBuy(id = ReasonToBuyType.HYPE_GOODS))

        val result = TokenBadgeBuilder.build(product)
        assertEquals(result, listOf(BadgeParams.Latest))
    }

    @Test
    fun resaleTest() {
        mockRearrs("market_resale_goods_exp=1")
        val offer = FapiOffer()
        offer.isResale = true

        val result = TokenBadgeBuilder.build(offer = offer)
        assertEquals(result, listOf(BadgeParams.Resale))
    }
}
