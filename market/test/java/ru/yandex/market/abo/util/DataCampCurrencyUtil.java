package ru.yandex.market.abo.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferPrice;
import NMarketIndexer.Common.Common;
import ru.yandex.common.util.currency.Bank;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.currency.CurrencyRate;
import ru.yandex.common.util.currency.RateSource;
import ru.yandex.common.util.currency.exchange.CurrencyRates;

/**
 * @author valeriashanti
 * @date 13/08/2020
 */
public class DataCampCurrencyUtil {

    public static DataCampOffer.Offer createDataCampOffer(String id, String rate, String refId, long price) {
        var priceBuilder = Common.PriceExpression.newBuilder().setId(id).setPrice(price);
        if (refId != null) {
            priceBuilder.setRefId(refId);
        }
        if (rate != null) {
            priceBuilder.setRate(rate);
        }
        var priceBundleBuilder = DataCampOfferPrice.PriceBundle.newBuilder().setBinaryPrice(priceBuilder);
        return DataCampOffer.Offer.newBuilder()
                .setPrice(DataCampOfferPrice.OfferPrice.newBuilder().setBasic(priceBundleBuilder)).build();
    }

    public static CurrencyRates createRates() {
        var rateUSD = createRate(Currency.USD, RateSource.CBRF_DAILY, 73.1522);
        var rateEURtoRUR = createRate(Currency.EUR, RateSource.CBRF_DAILY, 85.9246);
        var rateKZTtoRUR = createRate(Currency.KZT, RateSource.CBRF_DAILY, 0.176563);
        var rateRUR = createRate(Currency.RUR, RateSource.CBRF_DAILY, 1.0);

        var rateUSDtoEUR = createRate(Currency.USD, RateSource.ECB_DAILY, 0.8487);
        var rateRURtoEUR = createRate(Currency.RUR, RateSource.ECB_DAILY, 0.011638);
        var rateKZTtoEUR = createRate(Currency.KZT, RateSource.CBRF_DAILY, 0.002023);
        var rateEUR = createRate(Currency.EUR, RateSource.ECB_DAILY, 1.0);

        var bankRUR = Map.of(Currency.RUR, rateRUR, Currency.USD, rateUSD,
                Currency.EUR, rateEURtoRUR, Currency.KZT, rateKZTtoRUR);
        var bankECB = Map.of(Currency.EUR, rateEUR, Currency.USD, rateUSDtoEUR, Currency.RUR, rateRURtoEUR,
                Currency.KZT, rateKZTtoEUR);
        var banks = Map.of(Bank.CBRF, bankRUR, Bank.ECB, bankECB);

        return new CurrencyRates(banks);
    }

    public static CurrencyRate createRate(Currency currency, RateSource source, Double value) {
        return new CurrencyRate(currency, source, 1, BigDecimal.valueOf(value), new Date());
    }
}
