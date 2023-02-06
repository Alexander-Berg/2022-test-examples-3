package ru.yandex.direct.core.entity.bids.utils;

import java.math.BigDecimal;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.currencies.CurrencyRub;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(JUnitParamsRunner.class)
public class PriceUtilsTest {
    private Currency currency = CurrencyRub.getInstance();

    @Test
    @Parameters(method = "testCases")
    public void applyContextPriceCoef(BigDecimal searchPrice, int contextPriceCoef, BigDecimal expectedPrice) {
        BigDecimal actualPrice =
                PriceUtils.applyContextPriceCoef(searchPrice, contextPriceCoef, currency);
        assertThat("hello world", actualPrice, is(expectedPrice));
    }

    private Object[] testCases() {
        return new Object[][]{
                {BigDecimal.valueOf(123.45), 0, BigDecimal.valueOf(123.45)},
                {BigDecimal.valueOf(123.45), 100, BigDecimal.valueOf(123.45)},
                {BigDecimal.valueOf(123.45), 10, BigDecimal.valueOf(12.40)},
                {currency.getMinPrice(), 10, currency.getMinPrice()},
        };
    }
}
