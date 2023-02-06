package ru.yandex.direct.currency;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;


@RunWith(Parameterized.class)
public class CurrencyTranslationHolderTest {
    private CurrencyCode currencyCode;

    public CurrencyTranslationHolderTest(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }


    @Parameterized.Parameters(name = "{0}")
    public static Iterable<CurrencyCode> data() {
        return Arrays.asList(CurrencyCode.values());
    }

    @Test
    //проверяем, что для всех кодов валют найдется перевод
    public void getTranslation_EveryCurrencyCodeHasTranslation() {
        CurrencyTranslationHolder.ofCurrency(currencyCode);
    }

}
