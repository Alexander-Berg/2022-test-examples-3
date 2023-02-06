package ru.yandex.common.util.currency;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static ru.yandex.common.util.currency.Currency.UE;

public class CurrencyTest {

    @Test
    public void testJdkCurrencyForAll() {
        for (Currency currency : Currency.values()) {
            if( currency == UE){
                // принимаем что для этой нашей валюты не будет соответствующей реально валюты
                continue;
            }
            assertNotNull("jdkCurrency is null for: " + currency, currency.getJdkCurrency());
        }
    }

}
