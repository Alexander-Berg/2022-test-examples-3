package ru.yandex.calendar.logic.event.repetition;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author Daniel Brylev
 */
public class RepetitionToStringConverterTest extends CalendarTestBase {

    @Test
    public void daily() {
        DateTime start = MoscowTime.dateTime(2011, 11, 20, 23, 0);
        Interval i = new Interval(start, start.plusHours(1));

        Repetition r = createRepetition(RegularRepetitionRule.DAILY);
        Assert.equals("ежедневно", convertRu(i, r));
        Assert.equals("every day", convertEn(i, r));

        r.setREach(3);
        Assert.equals("каждый 3-й день", convertRu(i, r));
        Assert.equals("every 3rd day", convertEn(i, r));

        r.setDueTs(start.plusMonths(1).plusDays(1).toInstant());
        Assert.equals("каждый 3-й день до 20 декабря 2011 года", convertRu(i, r));
        Assert.equals("every 3rd day till 20th Dec 2011", convertEn(i, r));
    }

    @Test
    public void weekly() {
        DateTime start = MoscowTime.dateTime(2011, 11, 11, 0, 0);
        Interval i = new Interval(start, start.plusHours(1));

        Repetition r = createRepetition(RegularRepetitionRule.WEEKLY);
        r.setRWeeklyDays("SAT,SUN");
        Assert.equals("по субботам и воскресеньям", convertRu(i, r));
        Assert.equals("on Saturdays and Sundays", convertEn(i, r));

        r.setREach(2);
        Assert.equals("по субботам и воскресеньям каждой 2-й недели", convertRu(i, r));
        Assert.equals("every 2nd week on Saturdays and Sundays", convertEn(i, r));

        r.setREach(1);
        r.setRWeeklyDays("TUE,THU,SAT");
        Assert.equals("по вт, чт и сб", convertRu(i, r));
        Assert.equals("on Tue, Thu, Sat", convertEn(i, r));

        r.setRWeeklyDays("TUE");
        Assert.equals("по вторникам", convertRu(i, r));
        Assert.equals("on Tuesdays", convertEn(i, r));

        r.setRWeeklyDays("SUN,MON,TUE,WED,THU,FRI,SAT");
        r.setDueTs(start.plusMonths(1).plusDays(1).toInstant());
        Assert.equals("каждый день до 11 декабря 2011 года", convertRu(i, r));
        Assert.equals("every day till 11th Dec 2011", convertEn(i, r));
    }

    @Test
    public void monthly() {
        DateTime start = MoscowTime.dateTime(2012, 5, 6, 0, 0);
        Interval i = new Interval(start, start.plusHours(1));

        Repetition r = createRepetition(RegularRepetitionRule.MONTHLY_NUMBER);
        Assert.equals("6 числа каждого месяца", convertRu(i, r));
        Assert.equals("monthly on day 6", convertEn(i, r));

        r.setType(RegularRepetitionRule.MONTHLY_DAY_WEEKNO);
        Assert.equals("в 1-е воскресенье каждого месяца", convertRu(i, r));
        Assert.equals("monthly on 1st Sunday", convertEn(i, r));

        i = new Interval(start.plusWeeks(1), start.plusWeeks(1).plusHours(1));
        r.setREach(4);
        Assert.equals("во 2-е воскресенье каждого 4-го месяца", convertRu(i, r));
        Assert.equals("every 4th month on 2nd Sunday", convertEn(i, r));

        start = MoscowTime.dateTime(2012, 5, 14, 0, 0);
        i = new Interval(start, start.plusHours(1));
        r.setRMonthlyLastweek(true);
        Assert.equals("в последний понедельник каждого 4-го месяца", convertRu(i, r));
        Assert.equals("every 4th month on last Monday", convertEn(i, r));
    }

    @Test
    public void yearlyMonthlyDayWeekNo() {
        DateTime start = MoscowTime.dateTime(2017, 1, 19, 0, 0);
        Interval i = new Interval(start, start.plusHours(1));

        Repetition r = createRepetition(RegularRepetitionRule.MONTHLY_DAY_WEEKNO);
        r.setREach(12);

        Assert.equals("в 3-й четверг января", convertRu(i, r));
        Assert.equals("on 3rd Thursday of January", convertEn(i, r));

        i = new Interval(start.plusDays(10), start.plusDays(11));
        r.setRMonthlyLastweek(true);
        r.setDueTs(start.toInstant());

        Assert.equals("в последнее воскресенье января до 2017 года", convertRu(i, r));
        Assert.equals("on last Sunday of January till 2017", convertEn(i, r));
    }

    @Test
    public void yearly() {
        DateTime start = MoscowTime.dateTime(2010, 10, 10, 23, 0);
        Interval i = new Interval(start, start.plusHours(1));

        Repetition r = createRepetition(RegularRepetitionRule.YEARLY);
        Assert.equals("10 октября", convertRu(i, r));
        Assert.equals("on 10th Oct", convertEn(i, r));

        r.setREach(5);
        Assert.equals("10 октября каждый 5-й год", convertRu(i, r));
        Assert.equals("every 5th year on 10th Oct", convertEn(i, r));
    }

    private String convertRu(Interval i, Repetition r) {
        return convert(i, r, Language.RUSSIAN);
    }

    private String convertEn(Interval i, Repetition r) {
        return convert(i, r, Language.ENGLISH);
    }

    private String convert(Interval i, Repetition r, Language lang) {
        return RepetitionToStringConverter.convert(RepetitionInstanceInfo.create(i, Option.of(r)), lang);
    }

    private Repetition createRepetition(RegularRepetitionRule rule) {
        Repetition r = new Repetition();
        r.setType(rule);
        r.setREach(1);
        r.setRWeeklyDays(Option.<String>empty());
        r.setRMonthlyLastweek(false);
        r.setDueTs(Option.<Instant>empty());
        return r;
    }
}
