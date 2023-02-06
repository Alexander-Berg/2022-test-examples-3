package ru.yandex.market.ff.util;

import java.time.LocalTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.model.util.TimePeriod;
import ru.yandex.market.ff.service.util.dateTime.TimePeriodUtil;

public class TimePeriodUtilTest {

    @Test
    void parseTimePeriodTest() {
        TimePeriod timePeriod = TimePeriodUtil.parseTimePeriod("07:00-11:00");
        Assertions.assertEquals(LocalTime.of(7, 0), timePeriod.getFrom());
        Assertions.assertEquals(LocalTime.of(11, 0), timePeriod.getTo());
    }
}
