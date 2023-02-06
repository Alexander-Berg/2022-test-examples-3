package ru.yandex.market.api.search;

import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;

import static org.junit.Assert.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class SearchTypeTest extends UnitTestBase {

    @Test
    public void shouldProcessIsbnRequests() {
        assertEquals("isbn:\"123456\"", SearchType.ISBN.getLiteral("123456"));

        // MARKETAPI-3394: При поиске по ISBN не отдается модель, если в нём дефисы
        assertEquals("isbn:\"123456\"", SearchType.ISBN.getLiteral("123-456"));
        assertEquals("isbn:\"123456\"", SearchType.ISBN.getLiteral("123 456"));
    }

    @Test
    public void shouldExtractOriginalIsbnRequest() {
        assertEquals("123", SearchType.ISBN.getText("isbn:\"123\""));
    }

    @Test
    public void shouldProcessBarcodeRequests() {
        assertEquals("barcode:\"123456\"", SearchType.BARCODE.getLiteral("123456"));
        assertEquals("barcode:\"123456\"", SearchType.BARCODE.getLiteral("123-456"));
        assertEquals("barcode:\"123456\"", SearchType.BARCODE.getLiteral("123 456"));
    }

    @Test
    public void shouldExtractOriginalBarcode() {
        assertEquals("123", SearchType.BARCODE.getText("barcode:\"123\""));
    }
}
