package ru.yandex.market.replenishment.autoorder.utils

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ActiveProfiles("unittest")
class DoubleStringParserTest {

    @Test
    fun testWrongStringParse() {
        val parser = DoubleStringParser()
        assertEquals(null, parser.parseDouble("tnwler"))
        assertEquals(null, parser.parseDouble("13tnwler"))
        assertEquals(null, parser.parseDouble("tnwler12"))
        assertEquals(null, parser.parseDouble("1tnwler1"))
        assertEquals(null, parser.parseDouble("1t1nwler1"))
        assertEquals(null, parser.parseDouble("1 tnwler"))
    }

    @Test
    fun testSimpleNumber() {
        val parser = DoubleStringParser()
        assertEquals(12345678.0, parser.parseDouble("12345678"))
    }

    @Test
    fun testRuFormatWithoutGrouping() {
        val parser = DoubleStringParser()
        assertEquals(123456789.54321, parser.parseDouble("123456789.54321"))
    }

    @Test
    fun testRuFormatWithGroupingBySpace() {
        val parser = DoubleStringParser()
        assertEquals(123456789.54321, parser.parseDouble("123 456 789.54321"))
    }

    @Test
    fun testRuFormatWithGroupingByComma() {
        val parser = DoubleStringParser()
        assertEquals(123456789.54321, parser.parseDouble("123,456,789.54321"))
    }

    @Test
    fun testUsFormatWithoutGrouping() {
        val parser = DoubleStringParser()
        assertEquals(123456789.54321, parser.parseDouble("123456789,54321"))
    }

    @Test
    fun testUsFormatWithGroupingBySpace() {
        val parser = DoubleStringParser()
        assertEquals(123456789.54321, parser.parseDouble("123 456 789,54321"))
    }

    @Test
    fun testUsFormatWithGroupingByComma() {
        val parser = DoubleStringParser()
        assertEquals(123456789.54321, parser.parseDouble("123.456.789,54321"))
    }

    @Test
    fun testFormatWithDecimalCommaAndGroupingByApostrophe() {
        val parser = DoubleStringParser()
        assertEquals(123456789.54321, parser.parseDouble("123'456'789,54321"))
    }

    @Test
    fun testFormatWithDecimalDotAndGroupingByApostrophe() {
        val parser = DoubleStringParser()
        assertEquals(123456789.54321, parser.parseDouble("123'456'789.54321"))
    }
}
