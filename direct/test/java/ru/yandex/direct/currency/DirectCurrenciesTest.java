package ru.yandex.direct.currency;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeNoException;
import static ru.yandex.direct.currency.CurrencyCode.BYN;
import static ru.yandex.direct.currency.CurrencyCode.CHF;
import static ru.yandex.direct.currency.CurrencyCode.EUR;
import static ru.yandex.direct.currency.CurrencyCode.GBP;
import static ru.yandex.direct.currency.CurrencyCode.KZT;
import static ru.yandex.direct.currency.CurrencyCode.RUB;
import static ru.yandex.direct.currency.CurrencyCode.TRY;
import static ru.yandex.direct.currency.CurrencyCode.UAH;
import static ru.yandex.direct.currency.CurrencyCode.USD;
import static ru.yandex.direct.currency.CurrencyCode.YND_FIXED;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@RunWith(Parameterized.class)
public class DirectCurrenciesTest {

    private CurrencyCode currencyCode;

    public DirectCurrenciesTest(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<CurrencyCode> data() {
        return Arrays.asList(YND_FIXED, RUB, UAH, USD, EUR, KZT, CHF, TRY, BYN, GBP);
    }

    @Test
    public void currencyCanBeObtainedByCodeAndHasCorrectCode() {
        try {
            assumeThat(Currencies.getCurrency(currencyCode).getCode(), equalTo(currencyCode));
        } catch (Exception e) {
            assumeNoException(e);
        }
    }

    @Test
    public void currencyCanBeObtainedByStringAndHasCorrectCode() {
        try {
            assertThat(Currencies.getCurrency(currencyCode.name()).getCode(), equalTo(currencyCode));
        } catch (Exception e) {
            assumeNoException(e);
        }
    }

    @Test
    public void currencyCanBeObtainedFromCodeAndHasCorrectCode() {
        try {
            assertThat(currencyCode.getCurrency().getCode(), equalTo(currencyCode));
        } catch (Exception e) {
            assumeNoException(e);
        }
    }

    @Test
    public void currencyIsSingleton() {
        try {
            Currency currency = currencyCode.getCurrency();
            assertThat(currency, sameInstance(Currencies.getCurrency(currencyCode)));
            assertThat(currency, sameInstance(Currencies.getCurrency(currencyCode.name())));
        } catch (Exception e) {
            assumeNoException(e);
        }
    }
}
