package ru.yandex.calendar.frontend.ews;

import org.joda.time.LocalDate;

import org.joda.time.DateTime;

import com.microsoft.schemas.exchange.services._2006.types.DayOfWeekIndexType;
import com.microsoft.schemas.exchange.services._2006.types.MonthNamesType;
import org.joda.time.DateTimeConstants;
import org.junit.Test;

import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.calendar.util.dates.AuxDateTime;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class EwsUtilsTest extends CalendarTestBase {

    @Test
    public void getDayOfWeekIndexTypeByWeekOfMonth() {
        Assert.A.equals(DayOfWeekIndexType.FOURTH, EwsUtils.getDayOfWeekIndexTypeByWeekOfMonth(4));
    }

    // https://jira.yandex-team.ru/browse/CAL-3262
    @Test
    public void getDayOfWeekIndexTypeByWeekOfMonthFromDateTime() {
        DateTime dateTime = TestDateTimes.moscowDateTime(2011, 8, 4, 17, 0); // 1st Thursday
        int oneBasedWeekOfMonth = AuxDateTime.getWeekOfMonth(dateTime);
        Assert.A.equals(DayOfWeekIndexType.FIRST, EwsUtils.getDayOfWeekIndexTypeByWeekOfMonth(oneBasedWeekOfMonth));
    }

    // https://jira.yandex-team.ru/browse/CAL-3262
    @Test
    public void getDayOfWeekIndexTypeByWeekOfMonthFromLocalDate() {
        LocalDate localDate = new LocalDate(2011, 8, 4); // 1st Thursday
        int oneBasedWeekOfMonth = AuxDateTime.getWeekOfMonth(localDate);
        Assert.A.equals(DayOfWeekIndexType.FIRST, EwsUtils.getDayOfWeekIndexTypeByWeekOfMonth(oneBasedWeekOfMonth));
    }


    @Test
    public void getMonthNamesTypeByJodaMonthOfYear() {
        Assert.A.equals(MonthNamesType.JUNE, EwsUtils.getMonthNamesTypeByJodaMonthOfYear(DateTimeConstants.JUNE));
    }

}
