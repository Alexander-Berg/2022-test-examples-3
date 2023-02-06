package ru.yandex.market.replenishment.autoorder.utils

import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.market.replenishment.autoorder.utils.SskuParser.joinSsku
import ru.yandex.market.replenishment.autoorder.utils.SskuParser.parseRsId
import ru.yandex.market.replenishment.autoorder.utils.SskuParser.parseShopSku

@RunWith(SpringRunner::class)
@ActiveProfiles("unittest")
open class SskuParserTest {
    @get:Rule
    val thrown = ExpectedException.none()

    companion object {
        private const val VALID_RS_ID = "000111"
        private const val VALID_SHOP_SKU_ONE = "222"
        private const val VALID_SSKU_ONE = "$VALID_RS_ID.$VALID_SHOP_SKU_ONE"
        private const val VALID_SHOP_SKU_TWO = "$VALID_SHOP_SKU_ONE.333"
        private const val VALID_SSKU_TWO = "$VALID_RS_ID.$VALID_SHOP_SKU_TWO"
        private const val ERROR_MESSAGE = "Неверный формат SSKU"
    }

    @Test
    fun testRsIdFromSskuWithOneDot() {
        assertEquals(VALID_RS_ID, parseRsId(VALID_SSKU_ONE))
    }

    @Test
    fun testShopSkuFromSskuWithOneDot() {
        assertEquals(VALID_SHOP_SKU_ONE, parseShopSku(VALID_SSKU_ONE))
    }

    @Test
    fun testRsIdFromSskuWithTwoDots() {
        assertEquals(VALID_RS_ID, parseRsId(VALID_SSKU_TWO))
    }

    @Test
    fun testShopSkuFromSskuWithTwoDots() {
        assertEquals(VALID_SHOP_SKU_TWO, parseShopSku(VALID_SSKU_TWO))
    }

    @Test
    fun testRsIdFromEmptySsku() {
        assertNull(parseRsId(""))
        assertNull(parseRsId(null))
    }

    @Test
    fun testShopSkuFromEmptySsku() {
        assertNull(parseShopSku(""))
        assertNull(parseShopSku(null))
    }

    @Test
    fun testRsIdFromBlankSsku() {
        assertNull(parseRsId(" "))
    }

    @Test
    fun testShopSkuFromBlankSsku() {
        assertNull(parseRsId(" "))
        assertNull(parseShopSku(" "))
    }

    @Test
    fun testRsIdFromSskuWithoutDots() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(ERROR_MESSAGE)
        parseRsId(VALID_SHOP_SKU_ONE)
    }

    @Test
    fun testShopSkuFromSskuWithoutDots() {
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(ERROR_MESSAGE)
        parseShopSku(VALID_SHOP_SKU_ONE)
    }

    @Test
    fun testJoinSsku() {
        assertEquals(joinSsku("000111", "4242"), "000111.4242")
    }
}
