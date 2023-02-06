package ru.yandex.calendar.logic.ics.iv5j.ical.type;

import org.joda.time.Period;
import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class IcsDurationValueTest {

    private void testPeriod(String input, Period expected) {
        Period p = IcsDurationValue.parse(input).getPeriod();
        Assert.A.equals(expected, p);
    }

    @Test
    public void parse() {
        testPeriod("PT2S",  Period.seconds(2));
        testPeriod("PT10M", Period.minutes(10));
        testPeriod("PT1H",  Period.hours(1));

        testPeriod("P3D",   Period.days(3));
        testPeriod("P7W",   Period.weeks(7));

        testPeriod("PT1H22M",  new Period(1, 22, 0, 0));

        testPeriod("P15DT5H0M20S", new Period(0, 0, 0, 15, 5, 0, 20, 0));
    }

} //~
