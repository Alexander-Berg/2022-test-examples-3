package ru.yandex.market.mbo.toolkit.templates;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author commince
 * @date 22.05.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class TemplateParserTest {

    @Test
    public void parseDoubleQuotes() throws Exception {
        String quotes = "{\"delimiter\":\" \",\"values\":[[(1),(\"\\\"\" + v7351771 + \"\\\"\"),null,(true)]]}";

        Map<String, Object> result = (Map<String, Object>) TemplateParser.SINGLETON.parseTemplate(quotes);
        assertEquals(" ", result.get("delimiter"));
        Object[] values = (Object[]) result.get("values");
        Object[] innerValues = (Object[]) values[0];

        assertEquals("1", innerValues[0]);
        assertEquals("\"\\\"\" + v7351771 + \"\\\"\"", innerValues[1]);
        assertNull(innerValues[2]);
        assertEquals("true", innerValues[3]);
        return;
    }

    @Test
    public void parseTemplate() throws Exception {
        String quotes = "{\"delimiter\":\" \",\"values\":[[(1),(\"Модель: \" + v7351771),null,(true)]]}";

        Map<String, Object> result = (Map<String, Object>) TemplateParser.SINGLETON.parseTemplate(quotes);
        assertEquals(" ", result.get("delimiter"));
        Object[] values = (Object[]) result.get("values");
        Object[] innerValues = (Object[]) values[0];

        assertEquals("1", innerValues[0]);
        assertEquals("\"Модель: \" + v7351771", innerValues[1]);
        assertNull(innerValues[2]);
        assertEquals("true", innerValues[3]);
        return;
    }

}
