package ru.yandex.market.core.abo._public.impl;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.core.AbstractParserTest;

/**
 * @author zoom
 */
public class ElementCountParserTest extends AbstractParserTest {

    @Test
    public void should() throws IOException, SAXException {
        try (InputStream in = getContentStream("OK-result.xml")) {
            ElementCountParser parser = new ElementCountParser("/parent/el");
            Integer count = parser.parseStream(in);
            assertEquals((Integer) 4, count);
        }
    }


    @Test
    public void shouldParserWithIntegerValueFilter() throws IOException, SAXException {
        try (InputStream in = getContentStream("OK-filter-integer-value.xml")) {
            ElementCountParser parser = new ElementCountParser("/parent/el", value -> value > 0);
            Integer count = parser.parseStream(in);
            assertEquals((Integer) 3, count);
        }
    }

}