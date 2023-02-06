package ru.yandex.market.notifier.util;

import java.util.Map;

import org.joda.time.Period;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimePeriodUtilsTest {

    private static final Map<Period, Period> PERIOD_TESTS = Map.of(
            Period.hours(1).withMinutes(58), Period.hours(2).withMinutes(0),
            Period.hours(1).withMinutes(44), Period.hours(1).withMinutes(50),
            Period.hours(2).withMinutes(0), Period.hours(2).withMinutes(0),
            Period.hours(2).withMinutes(30), Period.hours(2).withMinutes(30),
            Period.hours(3).withMinutes(31), Period.hours(3).withMinutes(40),
            Period.hours(3).withMinutes(49), Period.hours(3).withMinutes(50)
    );

    @Test
    public void testRoundUpPeriod() {
        PERIOD_TESTS.forEach((t, expected) -> Assertions.assertEquals(expected, TimePeriodUtils.roundUpPeriod(t)));
    }
}
