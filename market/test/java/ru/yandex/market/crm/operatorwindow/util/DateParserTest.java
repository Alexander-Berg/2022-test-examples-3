package ru.yandex.market.crm.operatorwindow.util;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.utils.date.Dates;

public class DateParserTest {

    @Test
    public void parseDateTime() {
        LocalDateTime dt = DateParsers.parseDateTime("2019-05-15 04:58:46",
                Dates.YYYY_MM_DD_HH_MM_SS_FORMAT);
        Assertions.assertEquals(2019, dt.getYear());
        Assertions.assertEquals(5, dt.getMonthValue());
        Assertions.assertEquals(15, dt.getDayOfMonth());
        Assertions.assertEquals(4, dt.getHour());
        Assertions.assertEquals(58, dt.getMinute());
        Assertions.assertEquals(46, dt.getSecond());
    }
}
