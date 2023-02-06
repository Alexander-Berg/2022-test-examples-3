package ru.yandex.market.mbo.excel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author yuramalinov
 * @created 07.06.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class StreamXlsxParserTest {
    @Test
    public void testColIndex() {
        assertEquals(-1, StreamXlsxParser.parseColIndex(""));
        assertEquals(0, StreamXlsxParser.parseColIndex("A"));
        assertEquals(0, StreamXlsxParser.parseColIndex("A1"));
        assertEquals(1, StreamXlsxParser.parseColIndex("B1"));
        assertEquals(27, StreamXlsxParser.parseColIndex("AB1"));
        assertEquals((2 * 26 + 2) * 26 + 2 - 1, StreamXlsxParser.parseColIndex("BBB1"));
    }
}
