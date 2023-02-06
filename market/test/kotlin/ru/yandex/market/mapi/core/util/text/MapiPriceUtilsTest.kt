package ru.yandex.market.mapi.core.util.text

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

/**
 * @author Ilya Kislitsyn / ilyakis@ / 30.06.2022
 */
class MapiPriceUtilsTest {
    @Test
    fun testPriceFormatWithCurrency() {
        assertPrice("4.42 ₽", 4.423, "RUR")
        assertPrice("4 123.42 ₽", 4123.423, "RUR")
        assertPrice("4 120.42 ₽", 4120.423, "RUR")
        assertPrice("4 120 ₽", 4120.0, "RUR")
        assertPrice("1 239 874.42 ₽", 1239874.423, "RUR")
        assertPrice("1 239 874 ₽", 1239874.0, "RUR")

        assertPriceRub("1 239 874.42 ₽", 1239874.423)
        assertPriceRub("1 239 874 ₽", 1239874.0)

        assertPrice("4 123.42 б.p.", 4123.423, "BYN")
        assertPrice("4 120.42 б.p.", 4120.423, "BYN")
        assertPrice("4 120.00 б.p.", 4120.0, "BYN")
        assertPrice("1 239 874.00 б.p.", 1239874.0, "BYN")
        assertPrice("1 239 874.42 б.p.", 1239874.423, "BYN")

        assertPrice("4.42 NONE", 4.423, "NONE")
    }

    @Test
    fun testFormatPlusPoints() {
        assertEquals("12 345 баллов Плюса", BigDecimal.valueOf(12345).formatAsPlusPoints())
        assertEquals("123 балла Плюса", BigDecimal.valueOf(123).formatAsPlusPoints())
        assertEquals("123.45 балла Плюса", BigDecimal.valueOf(123.45).formatAsPlusPoints())

        assertEquals("121 балл Плюса", BigDecimal.valueOf(121).formatAsPlusPoints())
        assertEquals("121.45 балл Плюса", BigDecimal.valueOf(121.451).formatAsPlusPoints())

        assertEquals("123 балла Плюса", 123.formatAsPlusPoints())
        assertEquals("121 балл Плюса", 121.formatAsPlusPoints())
    }

    @Test
    fun testFormatSimple() {
        assertEquals("123", BigDecimal.valueOf(123).formatAsPriceString())
        assertEquals("123.45", BigDecimal.valueOf(123.45).formatAsPriceString())
    }

    private fun assertPrice(expected: String, amount: Double, currency: String) {
        assertEquals(expected, MapiPriceUtils.formatPrice(BigDecimal.valueOf(amount), currency))
    }

    private fun assertPriceRub(expected: String, amount: Double) {
        assertEquals(expected, MapiPriceUtils.formatPriceRub(BigDecimal.valueOf(amount)))
    }
}
