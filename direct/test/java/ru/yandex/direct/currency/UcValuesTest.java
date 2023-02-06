package ru.yandex.direct.currency;

import java.util.HashSet;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.currency.CurrencyCode.YND_FIXED;

@RunWith(Parameterized.class)
public class UcValuesTest {

    private CurrencyCode currencyCode;

    public UcValuesTest(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<CurrencyCode> data() {
        HashSet<CurrencyCode> allCurrencies = Sets.newHashSet(CurrencyCode.values());
        allCurrencies.remove(YND_FIXED);
        return allCurrencies;
    }

    @Test
    public void nonNullValueProvided() {
        assertThat(Currencies.getCurrency(currencyCode).getUcDefaultConversionValue(), notNullValue());
        assertThat(Currencies.getCurrency(currencyCode).getUcDefaultWeekBudget(), notNullValue());
        assertThat(Currencies.getCurrency(currencyCode).getEcomUcDefaultWeekBudget(), notNullValue());
    }
}
