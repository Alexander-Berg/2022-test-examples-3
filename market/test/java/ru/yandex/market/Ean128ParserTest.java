package ru.yandex.market;

import org.junit.jupiter.api.Test;

import ru.yandex.market.identifier.gs1.Ean128Parser;
import ru.yandex.market.identifier.rules.CisParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Ean128ParserTest {

    private final CisParser cisParser = new CisParser();

    @Test
    void extractGtin() {
        var barcode = "0112345678000001";
        var gtin = Ean128Parser.parse(barcode).getGtin();
        assertNotNull(gtin);
        assertEquals("12345678000001", gtin);
    }
}
