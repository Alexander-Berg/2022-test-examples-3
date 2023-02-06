package ru.yandex.common.util.currency.exchange;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;
import ru.yandex.common.util.currency.Bank;
import ru.yandex.common.util.currency.Currency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author valeriashanti
 * @date 20/08/2020
 */
public class CurrencyRatesXmlParserTest {

    private static final String TEST_XML_RESOURCE_PATH = "/currency/currency-rates.xml";

    @Test
    public void updateCurrencyRates() throws IOException, SAXException {
        CurrencyRatesXmlParser currencyRatesXmlParser = new CurrencyRatesXmlParser();
        try (FileInputStream inputStream = new FileInputStream(
                new File(getClass().getResource(TEST_XML_RESOURCE_PATH).getFile()))
        ) {
            currencyRatesXmlParser.parseXmlStream(inputStream);
        }

        assertEquals(7, currencyRatesXmlParser.getRates().size());
        assertTrue(currencyRatesXmlParser.getRates().containsKey(Bank.NBU));
        assertEquals(6, currencyRatesXmlParser.getRates().get(Bank.NBU).size());
        assertTrue(currencyRatesXmlParser.getRates().get(Bank.YNDX).containsKey(Currency.UE));
        assertTrue(currencyRatesXmlParser.getRates().get(Bank.NBRB).containsKey(Currency.BYN));
    }
}
