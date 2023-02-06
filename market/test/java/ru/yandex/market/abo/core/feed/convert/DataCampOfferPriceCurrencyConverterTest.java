package ru.yandex.market.abo.core.feed.convert;

import Market.DataCamp.DataCampOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.currency.exchange.CurrencyException;
import ru.yandex.common.util.currency.exchange.CurrencyRates;
import ru.yandex.market.abo.core.currency.CurrencyManager;
import ru.yandex.market.abo.util.DataCampCurrencyUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author valeriashanti
 * @date 10/08/2020
 */
class DataCampOfferPriceCurrencyConverterTest {

    private static final CurrencyRates CURRENCY_RATES = DataCampCurrencyUtil.createRates();

    private DataCampOfferPriceCurrencyConverter currencyConverter;

    @BeforeEach
    void setUp() {
        var currencyManager = mock(CurrencyManager.class);
        currencyConverter = new DataCampOfferPriceCurrencyConverter(currencyManager);
        when(currencyManager.getRates()).thenReturn(CURRENCY_RATES);
        when(currencyManager.convert(any(), any(), any(), any())).thenCallRealMethod();
    }

    @Test
    public void convertSimpleOffer() {
        var offer = createOffer("USD", "CBRF", "RUR", 2000000000L);
        var price = convertPrice(offer, Currency.RUR);
        assertEquals(14630.44, price);
    }

    @Test
    public void convertPriceTwoTimes() {
        var offer = createOffer("USD", "ECB", "EUR", 2000000000L);
        var price = convertPrice(offer, Currency.RUR);
        assertEquals(14584.84, price);
    }

    @Test
    public void convertPriceThatAlreadyInRUR() {
        var offer = createOffer("RUR", "CBRF", "RUR", 5000000000L);
        var price = convertPrice(offer, Currency.RUR);
        assertEquals(500.0, price);
    }

    @Test
    public void convertPriceToSameCurrency() {
        var offer = createOffer("EUR", "ECB", "RUR", 8530000000L);
        var price = convertPrice(offer, Currency.EUR);
        assertEquals(853.0, price);
    }

    @Test
    public void convertSameCurrencyDiffRateToRUR() {
        var offer = createOffer("USD", "ECB", "USD", 8530000000L);
        var price = convertPrice(offer, Currency.RUR);
        assertEquals(62398.83, price);
    }

    @Test
    public void convertPriceDiffRatesAndCurrencies() {
        var offer = createOffer("KZT", "CBRF", "EUR", 8530000000L);
        var price = convertPrice(offer, Currency.EUR);
        assertEquals(1.75, price);
    }

    @Test
    public void convertPriceDiffRatesAndCurrenciesToRUR() {
        var offer = createOffer("KZT", "CBRF", "EUR", 5000000000L);
        var price = convertPrice(offer, Currency.RUR);
        assertEquals(88.28, price);
    }

    @Test
    public void convertSameCurrDiffBankRate() {
        var offer = createOffer("RUR", "ECB", "RUR", 5000000000L);
        var price = convertPrice(offer, Currency.RUR);
        assertEquals(500.0, price);
    }

    @Test
    public void convertOfferWithNoRate() {
        var offer = createOffer("RUR", null, "RUR", 5000000000L);
        var price = convertPrice(offer, Currency.EUR);
        assertEquals(5.82, price);
    }

    @Test
    public void convertOfferWithRateEqualsOne() {
        var offer = DataCampCurrencyUtil.createDataCampOffer("EUR", "1", "RUR", 5000000000L);
        var price = convertPrice(offer, Currency.RUR);
        assertEquals(500.0, price);
    }

    @Test
    public void convertException() {
        var offer = createOffer("EUR", "NBU", "USD", 5000000000L);
        Exception exception = assertThrows(CurrencyException.class, () -> {
            currencyConverter.convertPriceIfNeeded(offer, Currency.RUR);
        });
        assertTrue(exception.getMessage().contains("Нет актуальных курсов валют для конвертации"));
    }

    private double convertPrice(DataCampOffer.Offer offer, Currency currency) {
        return DataCampOfferConverter.extractPrice(currencyConverter.convertPriceIfNeeded(offer, currency));
    }

    private static DataCampOffer.Offer createOffer(String id, String rate, String refId, long price) {
        return DataCampCurrencyUtil.createDataCampOffer(id, rate, refId, price);
    }
}
