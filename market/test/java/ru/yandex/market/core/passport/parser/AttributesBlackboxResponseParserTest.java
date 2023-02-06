package ru.yandex.market.core.passport.parser;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.passport.paser.AttributesBlackboxResponseParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AttributesBlackboxResponseParserTest {
    @Test
    void testParsing() throws IOException {
        AttributesBlackboxResponseParser parser = new AttributesBlackboxResponseParser();

        Map<Integer, Long> result = parser.parse(getClass().getResourceAsStream("userInfo.attributes.response.xml"));

        assertNotNull(result);
        assertEquals(1, result.size());
        Long actualAttribute = result.getOrDefault(1003, 0L);
        assertEquals(1L, actualAttribute.longValue());
    }
}
