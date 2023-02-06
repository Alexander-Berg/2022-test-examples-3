package ru.yandex.market.mbi.msapi.handler.lines;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author aostrikov
 */
public class JsonLineParserTest {

    private JsonLineParser parser;

    @Before
    public void setUp() {
        parser = new JsonLineParser();
    }

    @Test
    public void shouldGetFieldNames() {
        ParsedLine line = parser.parseJson("{\"name\": \"Alex\", \"age\": 19, \"keyWithNullValue\":null}");
        assertArrayEquals(new String[] {"name", "age", "keyWithNullValue"}, line.fieldNames());
        assertEquals("Alex\t19\t", line.toTskv());
    }

    @Test
    public void shouldGetFieldValues() {
        ParsedLine line = parser.parseJson("{\"age\": 19, \"name\": \"Alex\", \"keyWithNullValue\":null}");

        assertArrayEquals(new String[] {"age", "name", "keyWithNullValue"}, line.fieldNames());
        assertEquals("19\tAlex\t", line.toTskv());
    }

    @Test
    public void shouldIgnoreEscapeSymbol() {
        ParsedLine line = parser.parseJson("{\"age\": 19, \"name\": \"Sergei\\tTelnov\"}");

        assertArrayEquals(new String[] {"age", "name"}, line.fieldNames());
        assertEquals("19\tSergei\\tTelnov", line.toTskv());
        assertEquals(2, line.toTskv().split("\t").length);
    }
}
