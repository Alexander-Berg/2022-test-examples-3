package ru.yandex.market.crm.operatorwindow.util;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CurrencyFormattersTest {

    @Test
    @Disabled("что тестирует? почему зависит от часового пояся?")
    public void checkDateTime() {
        String result = ViewDateFormatters.dateTime(new Date(1501476481000L));
        Assertions.assertEquals("31.07.2017 07:48:01", result);
    }

    @Test
    public void checkPrice() {
        String result = OperatorCurrencyFormatters.price(new BigDecimal(12345.6789));
        Assertions.assertEquals("12 346", result);
        result = OperatorCurrencyFormatters.price(new BigDecimal(12345.0));
        Assertions.assertEquals("12 345", result);
        result = OperatorCurrencyFormatters.price(new BigDecimal(12345.5));
        Assertions.assertEquals("12 346", result);
        result = OperatorCurrencyFormatters.price(new BigDecimal(12345.495));
        Assertions.assertEquals("12 346", result);
        result = OperatorCurrencyFormatters.price(new BigDecimal(12345.494));
        Assertions.assertEquals("12 346", result);
    }

}
