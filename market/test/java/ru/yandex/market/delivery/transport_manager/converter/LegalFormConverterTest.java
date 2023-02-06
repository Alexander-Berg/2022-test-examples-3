package ru.yandex.market.delivery.transport_manager.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LegalFormConverterTest {
    private final LegalFormConverter legalFormConverter = new LegalFormConverter();

    @Test
    void mapToCyrillicLegalFormTest_cyrillic() {
        assertEquals("ЗАО", legalFormConverter.mapToCyrillicLegalForm("ЗАО"));
    }

    @Test
    void mapToCyrillicLegalFormTest_latin() {
        assertEquals("ИП", legalFormConverter.mapToCyrillicLegalForm("IP"));
    }

    @Test
    void mapToCyrillicLegalFormTest_invalid() {
        assertNull(legalFormConverter.mapToCyrillicLegalForm("qwe"));
        assertNull(legalFormConverter.mapToCyrillicLegalForm("абц"));
    }
}
