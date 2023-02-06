package ru.yandex.common.util.currency;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CurrencyRateTest {

    @Test
    public void testStandardSerialization() {
        final Currency currency = Currency.RUR;
        final RateSource rateSource = RateSource.CBRF_MONTHLY;
        final int nominal = 10;
        final BigDecimal value = BigDecimal.TEN;
        final Date eventTime = new Date();

        final CurrencyRate origin = new CurrencyRate(currency, rateSource, nominal, value, eventTime);
        final CurrencyRate clone = SerializationUtils.clone(origin);

        assertThat(clone, notNullValue());
        assertThat(clone.getCurrency(), equalTo(currency));
        assertThat(clone.getRateSource(), equalTo(rateSource));
        assertThat(clone.getNominal(), equalTo(nominal));
        assertThat(clone.getValue(), equalTo(value));
        assertThat(clone.getEventTime(), equalTo(eventTime));
    }

}
