package ru.yandex.market.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.hamcrest.Matcher;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.currency.CurrencyRate;
import ru.yandex.common.util.currency.RateSource;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * @author vbudnev
 */
public class CurrencyMatchers {

    public static Matcher<CurrencyRate> rateTsMatcher(String date) {
        return MbiMatchers.<CurrencyRate>newAllOfBuilder()
                .add(CurrencyRate::getCurrency, Currency.USD, "currency")
                .add(CurrencyRate::getRateSource, RateSource.CBRF_DAILY, "rateSource")
                .add(
                        CurrencyRate::getEventTime,
                        DateUtil.asDate(LocalDate.parse(date, DateTimeFormatter.ISO_DATE)),
                        "eventTime"
                )
                .build();
    }

    public static Matcher<CurrencyRate> rateMatcher(
            Currency currency,
            RateSource rateSource,
            Integer nominal,
            String date,
            double value
    ) {
        return rateMatcher(currency, rateSource, nominal, date, BigDecimal.valueOf(value));
    }

    public static Matcher<CurrencyRate> rateMatcher(
            Currency currency,
            RateSource rateSource,
            Integer nominal,
            String date,
            BigDecimal value
    ) {
        return MbiMatchers.<CurrencyRate>newAllOfBuilder()
                .add(CurrencyRate::getCurrency, currency, "currency")
                .add(CurrencyRate::getRateSource, rateSource, "rateSource")
                .add(CurrencyRate::getNominal, nominal, "nominal")
                .add(CurrencyRate::getValue, value, "value")
                .add(
                        CurrencyRate::getEventTime,
                        DateUtil.asDate(LocalDate.parse(date, DateTimeFormatter.ISO_DATE)),
                        "eventTime"
                )
                .build();
    }

}
