package ru.yandex.market.common.balance.test;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.balance.xmlrpc.model.BalanceDataStructure;

public class BalanceDataStructureTest {

    @Test
    void testDateParsing() throws ParseException {
        LocalDateTime localDateTime = LocalDateTime.of(
                2020, Month.APRIL, 10, 0, 0, 0
        );
        Date expected = Date.from(localDateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant());
        compareDates("Fri Apr 10 00:00:00 MSK 2020", expected);
    }

    private void compareDates(String input, Date expected) throws ParseException {
        Date actual = BalanceDataStructure.DEFAULT_DATE_FORMAT.get().parse(input);
        Assertions.assertEquals(expected, actual);
    }
}
