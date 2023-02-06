package ru.yandex.calendar.util.dates;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author Stepan Koltsov
 */
public class AuxDateTimeTest {

    @Test
    public void toDateTimeIgnoreGapRegular() {
        DateTime r = AuxDateTime.toDateTimeIgnoreGap(new LocalDateTime(2010, 3, 14, 18, 15, 22), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        Assert.A.equals(new DateTime(2010, 3, 14, 18, 15, 22, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE), r);
    }

    @Test
    public void toDateTimeIgnoreGapDaylightSavingGap() {
        DateTime r = AuxDateTime.toDateTimeIgnoreGap(new LocalDateTime(2010, 3, 28, 2, 22, 33), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        Assert.A.equals(new DateTime(2010, 3, 28, 3, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE), r);
    }

    @Test
    public void getWeekOfMonth() {
        Assert.A.equals(1, AuxDateTime.getWeekOfMonth(TestDateTimes.moscowDateTime(2011, 1, 3, 0, 0)));
        Assert.A.equals(1, AuxDateTime.getWeekOfMonth(TestDateTimes.moscowDateTime(2011, 1, 6, 0, 0)));
        Assert.A.equals(1, AuxDateTime.getWeekOfMonth(TestDateTimes.moscowDateTime(2011, 1, 7, 0, 0)));
        Assert.A.equals(2, AuxDateTime.getWeekOfMonth(TestDateTimes.moscowDateTime(2011, 1, 8, 0, 0)));

        Assert.A.equals(1, AuxDateTime.getWeekOfMonth(new LocalDate(2011, 1, 3)));
        Assert.A.equals(1, AuxDateTime.getWeekOfMonth(new LocalDate(2011, 1, 6)));
        Assert.A.equals(1, AuxDateTime.getWeekOfMonth(new LocalDate(2011, 1, 7)));
        Assert.A.equals(2, AuxDateTime.getWeekOfMonth(new LocalDate(2011, 1, 8)));
    }

} //~
