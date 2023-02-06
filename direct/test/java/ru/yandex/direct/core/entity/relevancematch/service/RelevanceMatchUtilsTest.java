package ru.yandex.direct.core.entity.relevancematch.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.currency.currencies.CurrencyRub;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class RelevanceMatchUtilsTest {
    @Test
    public void calculatePrice_PhrasePricesLowerThanMin() {
        List<BigDecimal> prices = Collections.singletonList(BigDecimal.valueOf(0.1));
        CurrencyRub currency = CurrencyRub.getInstance();
        BigDecimal actualPrice = RelevanceMatchUtils.calculatePrice(prices, currency);
        assertThat(actualPrice, equalTo(currency.getMinPrice()));
    }

    @Test
    public void calculatePrice_PhrasePricesBiggerThanMax() {
        List<BigDecimal> prices = Collections.singletonList(BigDecimal.valueOf(25001));
        CurrencyRub currency = CurrencyRub.getInstance();
        BigDecimal actualPrice = RelevanceMatchUtils.calculatePrice(prices, currency);
        assertThat(actualPrice, equalTo(currency.getMaxPrice().setScale(1)));
    }

    @Test
    public void calculatePrice_calcQuantile() {
        List<BigDecimal> prices =
                Arrays.asList(BigDecimal.valueOf(12.1), BigDecimal.valueOf(12.2), BigDecimal.valueOf(12.3));
        CurrencyRub currency = CurrencyRub.getInstance();
        BigDecimal actualPrice = RelevanceMatchUtils.calculatePrice(prices, currency);
        assertThat(actualPrice, equalTo(BigDecimal.valueOf(12.2)));
    }
}
