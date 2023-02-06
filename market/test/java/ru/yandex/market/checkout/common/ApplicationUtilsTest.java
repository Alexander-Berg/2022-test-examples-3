package ru.yandex.market.checkout.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApplicationUtilsTest {

    @Test
    public void currencyRatesUrlIsChangedCorrectly() {
        try {
            System.setProperty("ext.data.dir", "/any-dir");
            String url = "/market-checkouter/currency-rates.xml";
            String bundle = "checkouter";

            String result = ApplicationUtils.getCurrencyRatesUrl(url, bundle);

            assertEquals("/checkouter/currency_rates/currency-rates.xml", result);
        } finally {
            System.clearProperty("ext.data.dir");
        }
    }

    @Test
    public void currencyRatesUrlIsNotChangedWhenDirPropertyNotSet() {
        String url = "/market-checkouter/currency-rates.xml";
        String bundle = "checkouter";

        String result = ApplicationUtils.getCurrencyRatesUrl(url, bundle);

        assertEquals(url, result);
    }

    @Test
    public void currencyRatesUrlIsNotChangedWhenIsRtc() {
        try {
            System.setProperty("ext.data.dir", "/var/lib/yandex");
            String url = "/market-checkouter/currency-rates.xml";
            String bundle = "checkouter";

            String result = ApplicationUtils.getCurrencyRatesUrl(url, bundle);

            assertEquals(url, result);
        } finally {
            System.clearProperty("ext.data.dir");
        }
    }
}
