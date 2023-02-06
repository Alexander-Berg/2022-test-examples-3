package ru.yandex.market.abo.shoppinger.checker

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PriceRangeCheckerTest {

    private val PRICE_IN_RANGE = "1024"
    private val PRICE_OUT_RANGE = "1500"
    private val HTML = ("<!DOCTYPE html><html xml:lang=\"ru\" lang=\"ru\">" +
        "<meta http-equiv=\"Content-Type\"" +
        " content=\"text/html; charset=UTF-8\"/><body><p>blah blah bla</p><p>1023</p></body></html>").toByteArray()

    @Test
    fun findPriceInRange() {
        assertTrue(PriceRangeChecker.findPrice(PRICE_IN_RANGE, HTML).result)
    }

    @Test
    fun findPriceOutRange() {
        assertFalse(PriceRangeChecker.findPrice(PRICE_OUT_RANGE, HTML).result)
    }

}
