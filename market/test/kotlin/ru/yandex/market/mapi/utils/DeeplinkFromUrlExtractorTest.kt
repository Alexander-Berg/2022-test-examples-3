package ru.yandex.market.mapi.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DeeplinkFromUrlExtractorTest {

    @Test
    fun fullUrlTest() {
        val result = DeeplinkFromUrlExtractor.extract("https://fenek.market.yandex.ru/264672/goLink?puid18=no&puid14=tier_1&puid12=no&puid11=192&hash=88f23261493119d6&puid13=no&puid16=no&sj=Au8eU9jzzLhIxxF0KlXV6G100FjCl4qCgYi_E9FvriNiOOoa3ymHV-9v55IGjg%3D%3D&rand=bhimmvt&rqs=akCaPorQPk1qUGZi60g9xwbmDSJ9mYHY&p5=leqdb&puid19=no&pr=kpeuacn&p1=ciylq&puid15=no&p2=gsbi&pf=yamarket%3A%2F%2Fbrowser%3Fhybrid-mode%3D1%26url%3Dhttps%253A%252F%252Fm.market.yandex.ru%252Fspecial%252Fpodborkastil_pr%253Ffrom%253Dadfox_app_market_podborkastil_pr%2526banner%253Dpodborkastil_pr_1020x228%2526placement%253D%2526fmt%253D1020x228")
        assertEquals(result, "yamarket://browser?hybrid-mode=1&url=https%3A%2F%2Fm.market.yandex.ru%2Fspecial%2Fpodborkastil_pr%3Ffrom%3Dadfox_app_market_podborkastil_pr%26banner%3Dpodborkastil_pr_1020x228%26placement%3D%26fmt%3D1020x228")
    }

    @Test
    fun emptyUrlTest() {
        val result = DeeplinkFromUrlExtractor.extract("")
        assertEquals(result, null)
    }

    @Test
    fun badUrlTest() {
        val result = DeeplinkFromUrlExtractor.extract("fajhfkchsailfbdls")
        assertEquals(result, null)
    }

    @Test
    fun emptyQueryTest() {
        val result = DeeplinkFromUrlExtractor.extract("https://fenek.market.yandex.ru/264672/goLink?")
        assertEquals(result, null)
    }

    @Test
    fun queryWithoutPfParamTest() {
        val result = DeeplinkFromUrlExtractor.extract("https://fenek.market.yandex.ru/264672/goLink?puid18=no&puid14=tier_1&puid12=no")
        assertEquals(result, null)
    }

    @Test
    fun emptyPfParamTest() {
        val result = DeeplinkFromUrlExtractor.extract("https://fenek.market.yandex.ru/264672/goLink?puid18=no&pf=&puid14=tier_1&puid12=no")
        assertEquals(result, null)
    }
}
