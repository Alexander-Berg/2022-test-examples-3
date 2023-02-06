package ru.yandex.market.mapi.core.util.text

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.04.2022
 */
class MapiTextUtilsTest {
    @Test
    fun testLinkFormat() {
        assertEquals(null, MapiTextUtils.formatLink(null))
        assertEquals("yamarket://link", MapiTextUtils.formatLink("//link"))
        assertEquals("yamarket://link", MapiTextUtils.formatLink("/link"))
        assertEquals("https://link", MapiTextUtils.formatLink("https://link"))
    }

    @Test
    fun testPictureFormat() {
        assertEquals(null, MapiTextUtils.formatPicture(null))
        assertEquals("https://picture/orig", MapiTextUtils.formatPicture("//picture/orig"))
        assertEquals("https://picture/orig", MapiTextUtils.formatPicture("//picture/"))
        assertEquals("http://picture/orig", MapiTextUtils.formatPicture("http://picture/"))
        assertEquals("http://picture/org", MapiTextUtils.formatPicture("http://picture/org"))

        assertEquals(
            "https://avatars.mds.yandex.net/get-ns/group/img/orig",
            MapiTextUtils.formatPicture("ns", "group", "img")
        )
        assertEquals(null, MapiTextUtils.formatPicture("ns", "group", null))
        assertEquals(null, MapiTextUtils.formatPicture("ns", null, "img"))
        assertEquals(null, MapiTextUtils.formatPicture(null, "group", "img"))
    }

    @Test
    fun testColorFormat() {
        assertEquals(null, MapiTextUtils.formatColor(null))
        assertEquals("#123123", MapiTextUtils.formatColor("#123123"))
        assertEquals("#123123", MapiTextUtils.formatColor("123123"))
    }

    @Test
    fun testCurrency() {
        assertEquals("₽", MapiTextUtils.getCurrency("RUB"))
        assertEquals("KTP", MapiTextUtils.getCurrency("KTP"))
    }

    @Test
    fun testPluralize() {
        assertEquals("1 день", pluralizeDay(1))
        assertEquals("2 дня", pluralizeDay(2))
        assertEquals("3 дня", pluralizeDay(3))
        assertEquals("4 дня", pluralizeDay(4))
        assertEquals("5 дней", pluralizeDay(5))
        assertEquals("6 дней", pluralizeDay(6))
        assertEquals("7 дней", pluralizeDay(7))
        assertEquals("8 дней", pluralizeDay(8))
        assertEquals("9 дней", pluralizeDay(9))
        assertEquals("10 дней", pluralizeDay(10))
        assertEquals("11 дней", pluralizeDay(11))
        assertEquals("19 дней", pluralizeDay(19))
        assertEquals("20 дней", pluralizeDay(20))
        assertEquals("21 день", pluralizeDay(21))

        assertEquals("балл", MapiTextUtils.pluralizePlusPoint(1))
        assertEquals("балла", MapiTextUtils.pluralizePlusPoint(2))
        assertEquals("баллов", MapiTextUtils.pluralizePlusPoint(5))
    }

    private fun pluralizeDay(num: Long): String {
        return "$num " + MapiTextUtils.pluralize(num, "день", "дня", "дней")
    }

    @Test
    fun testCapitalize() {
        assertEquals("Some text", MapiTextUtils.capitalize("some text"))
        assertEquals("Some text", MapiTextUtils.capitalize("Some text"))
        assertEquals("!some text", MapiTextUtils.capitalize("!some text"))
        assertEquals("Какой-то текст", MapiTextUtils.capitalize("какой-то текст"))
    }

    @Test
    fun testDecapitalize() {
        assertEquals("some text", MapiTextUtils.decapitalize("some text"))
        assertEquals("some text", MapiTextUtils.decapitalize("Some text"))
        assertEquals("!some text", MapiTextUtils.decapitalize("!some text"))
        assertEquals("какой-то текст", MapiTextUtils.decapitalize("Какой-то текст"))
    }

    @Test
    fun testCutLeadingZeros() {
        assertEquals("9:00", MapiTextUtils.cutLeadingZeros("09:00"))
        assertEquals("9:00", MapiTextUtils.cutLeadingZeros("00000009:00"))
        assertEquals("19:00", MapiTextUtils.cutLeadingZeros("19:00"))
    }

    @Test
    fun testSplitUserInput() {
        assertEquals(listOf("1", "2", "3"), MapiTextUtils.splitUserInput("1,2,3"))
        assertEquals(listOf("1,2,3"), MapiTextUtils.splitUserInput("1,2,3", delimiter = ";"))
        assertEquals(listOf("1", "2", "3"), MapiTextUtils.splitUserInput("1;; 2 ; 3", delimiter = ";"))
    }
}
