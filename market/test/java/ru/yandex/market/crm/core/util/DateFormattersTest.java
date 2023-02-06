package ru.yandex.market.crm.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DateFormattersTest {

    @Test
    public void unixTimestampToDateYMD() {
        assertEquals("14.01.2011", DateFormatters.unixTimestampToDateYMD(1294999198));
        assertEquals("01.01.1970", DateFormatters.unixTimestampToDateYMD(0));
    }
}
