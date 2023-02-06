package ru.yandex.market;

import org.junit.jupiter.api.Test;

import ru.yandex.market.identifier.rules.CisParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CisParserTest {
    private final CisParser cisParser = new CisParser();

    @Test
    void extractIdentityFromCis() {
        var value = "\u001d0104650194496408215'4iRsB_JcDQ-\u001d91EE07\u001d" +
                "929CmJoU45/CPoiJh7p7ajYJWdze5wgGIQJsxmw9fqurc=";
        var shortValue = cisParser.extractIdentityFromCis(value);
        assertTrue(shortValue.isPresent());
        assertEquals("0104650194496408215'4iRsB_JcDQ-", shortValue.orElseThrow());
    }
}
