package ru.yandex.market.api.partner.controllers.quality.impl;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.api.partner.AbstractParserTest;
import ru.yandex.market.api.partner.controllers.quality.MarketPaymentCheckShopResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author zoom
 */
public class MarketPaymentCheckShopResultParserTest extends AbstractParserTest {

    @Test
    void shouldParseWellWhenOkResponse() throws IOException, SAXException {
        try (InputStream in = getContentStream("OK-result.xml")) {
            MarketPaymentCheckShopResult response = new MarketPaymentCheckShopResultParser().parseStream(in);
            assertTrue(response.isOk());
        }
    }

    @Test
    void shouldReturnErrorWhenInvalidShopStatus() throws IOException, SAXException {
        try (InputStream in = getContentStream("ERROR-invalid-status.xml")) {
            MarketPaymentCheckShopResult response = new MarketPaymentCheckShopResultParser().parseStream(in);
            assertTrue(response.isError());
            assertEquals(MarketPaymentCheckShopResult.INVALID_STATUS, response);
        }
    }

    @Test
    void shouldReturnErrorWhenAccessDenied() throws IOException, SAXException {
        try (InputStream in = getContentStream("ERROR-forbidden.xml")) {
            MarketPaymentCheckShopResult response = new MarketPaymentCheckShopResultParser().parseStream(in);
            assertTrue(response.isError());
            assertEquals(MarketPaymentCheckShopResult.FORBIDDEN, response);
        }
    }

    @Test
    void shouldReturnErrorWhenUnsupportedXmlResult() throws IOException, SAXException {
        try (InputStream in = getContentStream("UNSUPPORTED.xml")) {
            MarketPaymentCheckShopResult response = new MarketPaymentCheckShopResultParser().parseStream(in);
            assertTrue(response.isError());
            assertEquals(MarketPaymentCheckShopResult.UNSUPPORTED, response);
        }
    }

    @Test
    void shouldReturnShopInfoRequiredResult() throws IOException, SAXException {
        try (InputStream in = getContentStream("UNI_SHOP.xml")) {
            MarketPaymentCheckShopResult response = MarketPaymentCheckShopResultParser.parseIt(in);
            assertTrue(response.isError());
            assertEquals(MarketPaymentCheckShopResult.INFO_REQUIRED, response);
        }
    }
}