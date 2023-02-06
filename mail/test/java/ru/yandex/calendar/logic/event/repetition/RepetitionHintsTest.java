package ru.yandex.calendar.logic.event.repetition;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class RepetitionHintsTest extends CalendarTestBase {

    @Test
    public void testDefaults() {
        RepetitionHints expected;
        RepetitionHints actual;

        expected = new RepetitionHints(
                RegularRepetitionRule.DAILY, 1, Cf.list(DateTimeConstants.TUESDAY),
                27, -1, 12, Option.<LocalDate>empty());
        actual = RepetitionHints.createDefaults(TestDateTimes.moscowDateTime(2011, 12, 27, 0, 0));
        Assert.equals(expected, actual);

        expected = new RepetitionHints(
                RegularRepetitionRule.DAILY, 1, Cf.list(DateTimeConstants.WEDNESDAY),
                14, 2, 12, Option.<LocalDate>empty());
        actual = RepetitionHints.createDefaults(TestDateTimes.moscowDateTime(2011, 12, 14, 0, 0));
        Assert.equals(expected, actual);
    }

    @Test
    public void testByRepetitionWeekly() {
        Repetition repetition = new Repetition();
        repetition.setType(RegularRepetitionRule.WEEKLY);
        repetition.setREach(2);
        repetition.setRWeeklyDays("mon,fri");
        repetition.setRMonthlyLastweekNull();
        repetition.setDueTsNull();

        RepetitionHints expected = new RepetitionHints(
                RegularRepetitionRule.WEEKLY, 2,
                Cf.list(DateTimeConstants.MONDAY, DateTimeConstants.FRIDAY),
                6, 1, 12, Option.<LocalDate>empty());
        RepetitionHints actual = RepetitionHints.createByRepetition(
                        TestDateTimes.moscowDateTime(2011, 12, 6, 0, 0), repetition);
        Assert.equals(expected, actual);
    }

    @Test
    public void testByRepetitionMonthlyRLastWeek() {
        Repetition repetition = new Repetition();
        repetition.setType(RegularRepetitionRule.MONTHLY_DAY_WEEKNO);
        repetition.setREach(3);
        repetition.setRMonthlyLastweek(true);
        repetition.setDueTsNull();

        // day doesn't matter for MONTHLY_DAY_WEEKNO, is got from the date
        RepetitionHints expected = new RepetitionHints(
                RegularRepetitionRule.MONTHLY_DAY_WEEKNO, 3,
                Cf.list(DateTimeConstants.THURSDAY),
                8, -1, 12, Option.<LocalDate>empty());
        RepetitionHints actual = RepetitionHints.createByRepetition(
                        TestDateTimes.moscowDateTime(2011, 12, 8, 18, 50), repetition);
        Assert.equals(expected, actual);
    }

    @Test
    public void testByRepetitionYearlyDue() {
        Repetition repetition = new Repetition();
        repetition.setType(RegularRepetitionRule.YEARLY);
        repetition.setREach(1);
        repetition.setRWeeklyDaysNull();
        repetition.setRMonthlyLastweekNull();
        repetition.setDueTs(TestDateTimes.moscow(2015, 12, 5, 12, 30));

        RepetitionHints expected = new RepetitionHints(
                RegularRepetitionRule.YEARLY, 1,
                Cf.<Integer>list(),
                15, 3, 12, Option.of(new LocalDate(2015, 12, 5)));
        RepetitionHints actual = RepetitionHints.createByRepetition(
                        TestDateTimes.moscowDateTime(2011, 12, 15, 12, 30), repetition);
        Assert.equals(expected, actual);
    }
}
