package ru.yandex.antifraud;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.util.Currency;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.antifraud.currency.CurrencyMap;
import ru.yandex.antifraud.currency.CurrencyRateMap;
import ru.yandex.test.util.TestBase;

public class CurrencyTest extends TestBase {
    public CurrencyTest() {
        super(false, 0L);
    }
    @Test
    public void test() throws Exception {
        final CurrencyRateMap rateMap;
        try (final BufferedReader reader =
                     Files.newBufferedReader(resource("currencies_rate.json.txt/0_upload_file"))) {
            rateMap = new CurrencyRateMap(reader);
        }

        final Currency usdCurrency = CurrencyMap.INSTANCE.getItemByName("USD");
        Assert.assertNotNull(usdCurrency);

        final Currency rubCurrency = CurrencyMap.INSTANCE.getItemByName("RUB");
        Assert.assertNotNull(rubCurrency);

        final Currency shekelCurrency = CurrencyMap.INSTANCE.getItemByName("ILS");
        Assert.assertNotNull(shekelCurrency);

        Double rate = rateMap.rateToRub(usdCurrency);

        Assert.assertNotNull(rate);
        Assert.assertEquals(rate, 72.8491, 0.001);

        rate = rateMap.rate(usdCurrency, rubCurrency);
        Assert.assertEquals(rate, 72.8491, 0.001);

        rate = rateMap.rate(rubCurrency, usdCurrency);
        Assert.assertEquals(rate, 1. / 72.8491, 0.001);

        rate = rateMap.rate(shekelCurrency, rubCurrency);
        Assert.assertEquals(22.708572319201995, rate, 0.001);

        rate = rateMap.rate(rubCurrency, shekelCurrency);
        Assert.assertEquals(1. / 22.708572319201995, rate, 0.001);
    }
}
