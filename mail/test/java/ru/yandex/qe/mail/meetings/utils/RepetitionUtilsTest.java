package ru.yandex.qe.mail.meetings.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import org.junit.Test;

import ru.yandex.qe.mail.meetings.services.calendar.dto.Repetition;

import static org.junit.Assert.assertEquals;

/**
 * @author Sergey Galyamichev
 */
public class RepetitionUtilsTest {
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DAY_2019_11_01_T_09_30 = "2019-11-01T09:30:00";
    private static final String DAY_2019_11_26_T_07_00 = "2019-11-26T07:00:00";

    private static final ThreadLocal<DateFormat> dateConverterHolder =
            ThreadLocal.withInitial(() -> {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                return sdf;
            });

    private static Date toDate(String string) throws Exception {
        if (string == null) {
            return null;
        }
        return dateConverterHolder.get().parse(string);
    }

    @Test
    public void testTomorrow() throws Exception {
        Repetition repetition = buildRepetition(Repetition.Type.DAILY, 1);
        assertEquals("2019-11-02T09:30:00", RepetitionUtils.next(DAY_2019_11_01_T_09_30, repetition));
    }

    @Test
    public void testNextWeek() throws Exception {
        Repetition repetition = repeatWeeklyOn("fri", 1);
        assertEquals("2019-11-08T09:30:00", RepetitionUtils.next(DAY_2019_11_01_T_09_30, repetition));
    }

    @Test(expected = NoSuchElementException.class)
    public void testNextWeekWithDue() throws Exception {
        Repetition repetition = repeatWeeklyOn("fri", 1);
        repetition.setDueDate(toDate("2019-11-05"));
        assertEquals("2019-11-08T09:30:00", RepetitionUtils.next(DAY_2019_11_01_T_09_30, repetition));
    }

    @Test
    public void testDueIncluded() throws Exception {
        Repetition repetition = buildRepetition(Repetition.Type.DAILY, 1);
        repetition.setDueDate(toDate("2019-11-02"));
        assertEquals("2019-11-02T09:30:00", RepetitionUtils.next(DAY_2019_11_01_T_09_30, repetition));
    }

    @Test
    public void testNextTwoWeeks() throws Exception {
        Repetition repetition = repeatWeeklyOn("thu,fri", 2);
        assertEquals("2019-11-14T09:30:00", RepetitionUtils.next(DAY_2019_11_01_T_09_30, repetition));
    }

    @Test
    public void testWeekly() throws Exception {
        Repetition repetition = repeatWeeklyOn("mon,tue,wed,thu,fri", 1);
        assertEquals("2019-11-27T07:00:00", RepetitionUtils.next(DAY_2019_11_26_T_07_00, repetition));
        assertEquals("2019-12-02T07:00:00", RepetitionUtils.next("2019-11-29T07:00:00", repetition));
    }

    @Test
    public void testNextMonths() throws Exception {
        Repetition repetition = buildRepetition( Repetition.Type.MONTHLY_NUMBER, 3);
        assertEquals("2020-02-01T09:30:00", RepetitionUtils.next(DAY_2019_11_01_T_09_30, repetition));
    }

    @Test
    public void testNextMonthsDayWeek() throws Exception {
        Repetition repetition = buildRepetition( Repetition.Type.MONTHLY_DAY_WEEKNO, 1);
        assertEquals("2019-12-06T09:30:00", RepetitionUtils.next(DAY_2019_11_01_T_09_30, repetition));
    }

    @Test
    public void testNextMonthsDayWeekOverNY() throws Exception {
        Repetition repetition = buildRepetition( Repetition.Type.MONTHLY_DAY_WEEKNO, 3);
        assertEquals("2020-02-07T09:30:00", RepetitionUtils.next(DAY_2019_11_01_T_09_30, repetition));
    }


    @Test
    public void testNextYears() throws Exception {
        Repetition repetition = buildRepetition( Repetition.Type.YEARLY, 4);
        assertEquals("2023-11-01T09:30:00", RepetitionUtils.next(DAY_2019_11_01_T_09_30, repetition));
    }

    private static Repetition repeatWeeklyOn(String days, int each) {
        Repetition repetition = buildRepetition(Repetition.Type.WEEKLY, each);
        repetition.setWeeklyDays(days);
        return repetition;
    }

    private static Repetition buildRepetition(Repetition.Type type, int each) {
        Repetition repetition = new Repetition();
        repetition.setType(type);
        repetition.setEach(each);
        return repetition;
    }
}
