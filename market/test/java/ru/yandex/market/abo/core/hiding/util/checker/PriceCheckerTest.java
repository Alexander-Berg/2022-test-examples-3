package ru.yandex.market.abo.core.hiding.util.checker;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.abo.api.entity.offer.hidden.details.HidingDetails;
import ru.yandex.market.abo.core.feed.convert.DataCampOfferPriceCurrencyConverter;
import ru.yandex.market.abo.core.hiding.util.model.FreshOfferWrapper;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.util.DataCampCurrencyUtil;
import ru.yandex.market.common.report.indexer.model.OfferDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 28.06.17.
 */
class PriceCheckerTest {
    private static final double MARKET_PRICE = 100.723;

    @InjectMocks
    private PriceChecker priceChecker;
    @Mock
    private OfferDetails feed;
    @Mock
    private Offer offer;
    private FreshOfferWrapper freshOffer;
    @Mock
    private DataCampOfferPriceCurrencyConverter currencyConverter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(offer.getPrice()).thenReturn(new BigDecimal(MARKET_PRICE));
        freshOffer = new FreshOfferWrapper(feed);
    }

    @Test
    void diff() {
        when(feed.getPrice()).thenReturn(MARKET_PRICE + 100);
        HidingDetails details = priceChecker.diff(offer, freshOffer, null);
        assertNotNull(details);
        assertEquals(details.getPriceComparison().getMarketParam().getPrice().doubleValue(),
                offer.getPrice().doubleValue());
        assertEquals(details.getPriceComparison().getShopParam().getPrice().doubleValue(), freshOffer.getPrice());
    }

    @Test
    void no_diff_same_price() {
        when(feed.getPrice()).thenReturn(MARKET_PRICE);
        assertNull(priceChecker.diff(offer, freshOffer, null));
    }

    @Test
    void no_diff_lower_price() {
        when(feed.getPrice()).thenReturn(MARKET_PRICE - 5);
        assertNull(priceChecker.diff(offer, freshOffer, null));
    }

    @Test
    void differentCurrencies() {
        when(feed.getPrice()).thenReturn(MARKET_PRICE + 100);
        when(feed.getShopCurrency()).thenReturn(Currency.BYN);
        when(offer.getShopCurrency()).thenReturn(Currency.RUR);
        assertNull(priceChecker.diff(offer, freshOffer, null));
    }

    @Test
    void dataCampOfferCheck() {
        when(feed.getPrice()).thenReturn(MARKET_PRICE + 50);
        when(offer.getPriceCurrency()).thenReturn(Currency.RUR);
        when(currencyConverter.convertPriceIfNeeded(any(), any())).thenReturn(BigDecimal.valueOf(1507230000));

        var dataCampOffer = DataCampCurrencyUtil.createDataCampOffer("EUR", "ECB", "RUR", 20000000);
        var freshOfferDataCamp = new FreshOfferWrapper(dataCampOffer);

        ArgumentCaptor<Currency> captor = ArgumentCaptor.forClass(Currency.class);
        HidingDetails diff = priceChecker.diff(offer, freshOfferDataCamp, null);

        assertNotNull(diff);
        assertEquals(diff.getPriceComparison().getShopParam().getPrice().doubleValue(),
                Math.round(freshOffer.getPrice()));
        verify(currencyConverter, times(1)).convertPriceIfNeeded(any(), captor.capture());
        assertEquals(Currency.RUR, captor.getValue());
    }
}
