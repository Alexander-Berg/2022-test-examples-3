package ru.yandex.market.crm.campaign.util;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.core.util.CrmCurrencyFormatters;
import ru.yandex.market.crm.core.util.DateFormatters;

public class FormattersTest {
    @Test
    public void checkMoney() {
        String result = CrmCurrencyFormatters.money(12345.6789);
        Assert.assertEquals("12 346", result);
        result = CrmCurrencyFormatters.money(12345.0);
        Assert.assertEquals("12 345", result);
        result = CrmCurrencyFormatters.money(12345.7);
        Assert.assertEquals("12 346", result);
        result = CrmCurrencyFormatters.money(12345.5);
        Assert.assertEquals("12 346", result);
        result = CrmCurrencyFormatters.money(12345.499);
        Assert.assertEquals("12 345", result);
    }

    @Test
    public void checkDate() {
        String result = DateFormatters.asIsoDate(new Date(1501476481000L));
        Assert.assertEquals("2017-07-31", result);
    }
}
