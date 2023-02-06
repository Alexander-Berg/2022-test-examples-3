package ru.yandex.calendar.logic.event.repetition;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author gutman
 * @author Stepan Koltsov
 */
public class RepetitionUtilsTest {

    @Test
    public void eachDay() {
        Repetition r = createDailyRepetition();
        DateTime start = TestDateTimes.moscowDateTime(2010, 8, 30, 15, 30);
        Instant lastStart = RepetitionUtils.instanceStart(RepetitionInstanceInfo.create(interval30Minutes(start), Option.of(r)), 10);
        Assert.A.equals(start.plusDays(9).toInstant(), lastStart);
    }

    @Test
    public void eachOtherDay() {
        Repetition r = createDailyRepetition();
        r.setREach(2);
        DateTime start = TestDateTimes.moscowDateTime(2010, 8, 30, 15, 30);
        Instant lastStart = RepetitionUtils.instanceStart(RepetitionInstanceInfo.create(interval30Minutes(start), Option.of(r)), 10);
        Assert.A.equals(start.plusDays(18).toInstant(), lastStart);
    }

    @Test
    public void weeklyDays() {
        Repetition r = new Repetition();
        r.setType(RegularRepetitionRule.WEEKLY);
        r.setREach(1);
        r.setRWeeklyDays("mon,wed,fri");
        DateTime monday = TestDateTimes.moscowDateTime(2010, 8, 30, 15, 30);
        Instant lastStart = RepetitionUtils.instanceStart(RepetitionInstanceInfo.create(interval30Minutes(monday), Option.of(r)), 10);
        Assert.A.equals(monday.plusWeeks(3).toInstant(), lastStart);
    }

    @Test
    public void weeklyDaysOutsideStart() {
        DateTime monday = TestDateTimes.moscowDateTime(2013, 4, 15, 15, 30);
        DateTime tuesday = monday.withDayOfWeek(2);
        DateTime wednesday = monday.withDayOfWeek(3);
        DateTime thursday = monday.withDayOfWeek(4);
        DateTime nextMonday = monday.plusWeeks(1);

        Repetition repetition = new Repetition();
        repetition.setType(RegularRepetitionRule.WEEKLY);
        repetition.setRWeeklyDays("mon,wed");
        repetition.setREach(1);

        Assert.equals(monday.toInstant(), firstInstanceStart(monday, repetition));
        Assert.equals(wednesday.toInstant(), firstInstanceStart(wednesday, repetition));

        Assert.equals(wednesday.toInstant(), firstInstanceStart(tuesday, repetition));
        Assert.equals(nextMonday.toInstant(), firstInstanceStart(thursday, repetition));
    }

    private static Instant firstInstanceStart(DateTime repetitionStart, Repetition repetition) {
        return RepetitionUtils.instanceStart(
                RepetitionInstanceInfo.create(interval30Minutes(repetitionStart), Option.of(repetition)), 1);
    }

    @Test
    public void getClosestInstanceInterval() {
        DateTime start = TestDateTimes.moscowDateTime(2013, 10, 3, 15, 30);
        DateTime end = start.plusHours(3);
        DateTime middle = new DateTime((start.getMillis() + end.getMillis()) / 2, start.getZone());

        InstantInterval interval = new InstantInterval(start, end);
        RepetitionInstanceInfo repetition = RepetitionInstanceInfo.noRepetition(interval);

        Assert.some(interval, RepetitionUtils.getClosestInstanceInterval(repetition, start.minusHours(1).toInstant()));
        Assert.some(interval, RepetitionUtils.getClosestInstanceInterval(repetition, middle.toInstant()));
        Assert.some(interval, RepetitionUtils.getClosestInstanceInterval(repetition, end.plusHours(1).toInstant()));
    }

    @Test
    public void monthlyNumber() {
        Repetition r = createMonthlyNumberRepetition();
        DateTime start = TestDateTimes.moscowDateTime(2010, 8, 30, 15, 30);
        Instant lastStart = RepetitionUtils.instanceStart(RepetitionInstanceInfo.create(interval30Minutes(start), Option.of(r)), 3);
        Assert.A.equals(start.plusMonths(2).toInstant(), lastStart);
    }

    @Test
    public void monthly31() {
        Repetition r = createMonthlyNumberRepetition();
        DateTime start = TestDateTimes.moscowDateTime(2010, 8, 31, 15, 30);
        Instant lastStart = RepetitionUtils.instanceStart(RepetitionInstanceInfo.create(interval30Minutes(start), Option.of(r)), 3);
        Assert.A.equals(start.withMonthOfYear(12).toInstant(), lastStart);
    }

    @Test
    public void monthlyDayWeekno() {
        Repetition r = createMonthlyDayWeeknoRepetition();
        r.setRMonthlyLastweek(true);
        DateTime start = TestDateTimes.moscowDateTime(2010, 8, 30, 15, 30);
        Instant lastStart = RepetitionUtils.instanceStart(RepetitionInstanceInfo.create(interval30Minutes(start), Option.of(r)), 3);
        Assert.A.equals(
                start.plusMonths(2).withDayOfWeek(start.getDayOfWeek()).toInstant(), lastStart);
    }


    // https://jira.yandex-team.ru/browse/CAL-3269
    @Test
    public void monthlyDayWeeknoUtc() {
        monthlyDayWeekno(new LocalTime(1, 0), DateTimeZone.UTC);
        monthlyDayWeekno(new LocalTime(5, 0), DateTimeZone.UTC);
        monthlyDayWeekno(new LocalTime(15, 0), DateTimeZone.UTC);
        monthlyDayWeekno(new LocalTime(20, 0), DateTimeZone.UTC);
        monthlyDayWeekno(new LocalTime(23, 0), DateTimeZone.UTC);
    }

    // https://jira.yandex-team.ru/browse/CAL-3269
    @Test
    public void monthlyDayWeeknoMoscow() {
        monthlyDayWeekno(new LocalTime(1, 0), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        monthlyDayWeekno(new LocalTime(5, 0), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        monthlyDayWeekno(new LocalTime(15, 0), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        monthlyDayWeekno(new LocalTime(20, 0), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        monthlyDayWeekno(new LocalTime(23, 0), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
    }

    @Test
    public void monthlyDayWeeknoNo5thWeekno() {
        DateTime start = MoscowTime.dateTime(2012, 9, 29, 18, 37); // 5th saturday

        Repetition r = createMonthlyDayWeeknoRepetition();
        RepetitionInstanceInfo rii = RepetitionInstanceInfo.create(interval30Minutes(start), Option.of(r));

        ListF<DateTime> expectedStarts = Cf.list(
                MoscowTime.dateTime(2012, 9, 29, 18, 37),
                MoscowTime.dateTime(2012, 10, 27, 18, 37),
                MoscowTime.dateTime(2012, 11, 24, 18, 37));
        ListF<DateTime> actualStarts = getRepetitionStartsSince(rii, start, 3)
                .map(TimeUtils.instant.dateTimeWithZoneF().bind2(MoscowTime.TZ));

        Assert.equals(expectedStarts, actualStarts);
    }

    // https://jira.yandex-team.ru/browse/CAL-3269
    @Test
    public void monthlyNumberUtc() {
        monthlyNumber(new LocalTime(1, 0), DateTimeZone.UTC);
        monthlyNumber(new LocalTime(5, 0), DateTimeZone.UTC);
        monthlyNumber(new LocalTime(15, 0), DateTimeZone.UTC);
        monthlyNumber(new LocalTime(20, 0), DateTimeZone.UTC);
        monthlyNumber(new LocalTime(23, 0), DateTimeZone.UTC);
    }

    // https://jira.yandex-team.ru/browse/CAL-3269
    @Test
    public void monthlyNumberMoscow() {
        monthlyNumber(new LocalTime(1, 0), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        monthlyNumber(new LocalTime(5, 0), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        monthlyNumber(new LocalTime(15, 0), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        monthlyNumber(new LocalTime(20, 0), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        monthlyNumber(new LocalTime(23, 0), TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
    }

    private void monthlyDayWeekno(LocalTime localTime, DateTimeZone tz) {
        Repetition r = createMonthlyDayWeeknoRepetition();

        DateTime start = new LocalDate(2011, 6, 1).toDateTime(localTime, tz);
        RepetitionInstanceInfo rii = RepetitionInstanceInfo.create(interval30Minutes(start), Option.of(r));

        ListF<Instant> actualStarts = getRepetitionStartsSince(rii, start, 3);

        Assert.A.equals(Cf.list( // 1st Wed-s
            new LocalDate(2011, 6, 1).toDateTime(localTime, tz).toInstant(),
            new LocalDate(2011, 7, 6).toDateTime(localTime, tz).toInstant(),
            new LocalDate(2011, 8, 3).toDateTime(localTime, tz).toInstant()
        ), actualStarts);
    }

    private void monthlyNumber(LocalTime localTime, DateTimeZone tz) {
        Repetition r = createMonthlyNumberRepetition();

        DateTime start = new LocalDate(2011, 6, 1).toDateTime(localTime, tz);
        RepetitionInstanceInfo rii = RepetitionInstanceInfo.create(interval30Minutes(start), Option.of(r));

        ListF<Instant> actualStarts = getRepetitionStartsSince(rii, start, 3);

        Assert.A.equals(Cf.list( // 1st days of month
            new LocalDate(2011, 6, 1).toDateTime(localTime, tz).toInstant(),
            new LocalDate(2011, 7, 1).toDateTime(localTime, tz).toInstant(),
            new LocalDate(2011, 8, 1).toDateTime(localTime, tz).toInstant()
        ), actualStarts);
    }

    private ListF<Instant> getRepetitionStartsSince(RepetitionInstanceInfo rii, DateTime start, int limit) {
        return RepetitionUtils
                .getIntervals(rii, start.toInstant(), Option.<Instant>empty(), false, limit)
                .map(InstantInterval::getStart);
    }

    @Test
    public void yearlyFeb29() {
        Repetition r = new Repetition();
        r.setType(RegularRepetitionRule.YEARLY);
        r.setREach(1);
        r.setRMonthlyLastweek(true);
        DateTime start = new DateTime(2012, 2, 29, 15, 30, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        Instant lastStart = RepetitionUtils.instanceStart(RepetitionInstanceInfo.create(interval30Minutes(start), Option.of(r)), 2);
        Assert.A.equals(lastStart, start.plusYears(4).toInstant());
    }

    @Test
    public void getIntervals() {
        Repetition repetition = createDailyRepetition();
        DateTime start = TestDateTimes.moscowDateTime(2010, 12, 6, 14, 15);
        RepetitionInstanceInfo repetitionInstanceInfo = RepetitionInstanceInfo.create(interval30Minutes(start), Option.of(repetition));
        ListF<InstantInterval> intervals = RepetitionUtils.getIntervals(repetitionInstanceInfo, start.plusHours(27).toInstant(), Option.<Instant>empty(), true, 10);
        Assert.A.hasSize(10, intervals);
        Assert.A.equals(start.plusDays(2).toInstant(), intervals.first().getStart());
    }

    @Test
    public void isValidStart() {
        Repetition repetition = createDailyRepetition();
        DateTime start = TestDateTimes.moscowDateTime(2010, 12, 6, 14, 15);
        RepetitionInstanceInfo repetitionInstanceInfo = RepetitionInstanceInfo.create(interval30Minutes(start), Option.of(repetition));

        for (int i = 0; i < 10; ++i) {
            Assert.A.isTrue(RepetitionUtils.isValidStart(repetitionInstanceInfo, start.plusDays(i).toInstant()));
            Assert.A.isFalse(RepetitionUtils.isValidStart(repetitionInstanceInfo, start.plusHours(1).toInstant()));
            Assert.A.isFalse(RepetitionUtils.isValidStart(repetitionInstanceInfo, start.plusMinutes(30).toInstant()));
            Assert.A.isFalse(RepetitionUtils.isValidStart(repetitionInstanceInfo, start.minusMinutes(30).toInstant()));
        }
    }

    @Test
    public void unitsBetweenDefinedForAllEnums() {
        for (RegularRepetitionRule rule : RegularRepetitionRule.values()) {
            if (rule == RegularRepetitionRule.NONE) continue;

            RepetitionUtils.unitsBetween(TestDateTimes.moscowDateTime(2010, 12, 20, 12, 33), TestDateTimes.moscowDateTime(2010, 12, 30, 12, 33), rule);
        }
    }

    @Test
    public void getIntervalsWithRegularRepetition() {
        for (int i = 0; i < 1000; ++i) {
            DateTimeZone tz = TimeUtils.EUROPE_MOSCOW_TIME_ZONE;
            RepetitionInstanceInfo rii = createRandomRepetition(tz);

            long periodMillis = 24L * 60 * 60 * 1000 * RepetitionUtils.getApproxMinLengthDays(rii.getRepetition().get());
            Instant start = rii.getEventInterval().getStart().toInstant().plus(periodMillis * (5 - Random2.R.nextInt(10)));
            Instant end = start.plus(periodMillis * 100);

            ListF<InstantInterval> intervals = RepetitionUtils.getIntervals(rii, start, Option.of(end), true, Integer.MAX_VALUE);

            Assert.A.notEmpty(intervals);

            for (InstantInterval interval : intervals) {
                Assert.A.isTrue(interval.overlaps(new InstantInterval(start, end)));
                Assert.A.equals(rii.getEventInterval().getStart().toDateTime(tz).toLocalTime(), interval.getStart().toDateTime(tz).toLocalTime());
                Assert.A.equals(rii.getEventInterval().getEnd().toDateTime(tz).toLocalTime(), interval.getEnd().toDateTime(tz).toLocalTime());
            }
        }
    }

    @Test
    public void getIntervalsWithRegularRepetitionWithExdates() {
        for (int i = 0; i < 1000; ++i) {
            DateTimeZone tz = TimeUtils.EUROPE_MOSCOW_TIME_ZONE;
            RepetitionInstanceInfo rii = createRandomRepetition(tz);

            long periodMillis = 24L * 60 * 60 * 1000 * RepetitionUtils.getApproxMinLengthDays(rii.getRepetition().get());
            Instant start = rii.getEventInterval().getStart().toInstant().plus(periodMillis * (5 - Random2.R.nextInt(10)));
            Instant end = start.plus(periodMillis * 100);

            ListF<InstantInterval> intervals = RepetitionUtils.getIntervals(rii, start, Option.of(end), true, Integer.MAX_VALUE);

            Assert.A.notEmpty(intervals);

            ListF<Instant> exs = Random2.R.randomElements(intervals, intervals.length() / 3).map(InstantInterval::getStart);
            rii = rii.withExdates(exs.map(RepetitionUtils::consExdate));
            ListF<InstantInterval> minusExdates = RepetitionUtils.getIntervals(rii, start, Option.of(end), true, Integer.MAX_VALUE);

            Assert.A.equals(
                    intervals.map(InstantInterval::getStart).unique().minus(exs),
                    minusExdates.map(InstantInterval::getStart).unique());
        }
    }

    // http://ml.yandex-team.ru/thread17124063/#message17124063
    // http://calendar.yandex.ru/admin/helper_admin.xml?cmd=e+35860638
    @Test
    public void due() {
        // 15:00 UTC, 18:00 GMT+3
        DateTime eventStart = new DateTime(2010, 10, 11, 18, 0, 0, 0, DateTimeZone.forID("Europe/Kiev"));
        Interval eventInterval = new Interval(eventStart, Period.hours(1));

        Repetition repetition = createMonthlyNumberRepetition();
        // 16:00 UTC, 18:00 GMT+2
        repetition.setDueTs(TestDateTimes.utc(2011, 3, 11, 16, 0));
        RepetitionInstanceInfo rii = RepetitionInstanceInfo.create(eventInterval, Option.of(repetition));

        Instant iStartMs = TestDateTimes.utc(2011, 3, 11, 10, 0);
        Assert.assertEmpty(RepetitionUtils.getIntervals(rii, iStartMs, Option.<Instant>empty(), false, 1));
    }

    @Test
    public void exclusiveDue() {
        DateTime start = MoscowTime.dateTime(2012, 11, 15, 16, 0);

        InstantInterval interval = new InstantInterval(start, start.plusHours(1));
        Instant due = start.plusDays(2).toInstant();

        RepetitionInstanceInfo repInfo = RepetitionInstanceInfo.create(
                interval, MoscowTime.TZ, Option.of(createDailyRepetition(due)));

        Assert.isEmpty(RepetitionUtils.getIntervals(repInfo, due, Option.<Instant>empty(), true, 1));
        Assert.hasSize(2, RepetitionUtils.getIntervals(repInfo, start.toInstant(), Option.of(due), true, 0));
    }

    @Test
    public void zeroLengthSingleEvent() {
        Instant start = MoscowTime.instant(2012, 10, 15, 17, 25);
        InstantInterval interval = new InstantInterval(start, start);

        RepetitionInstanceInfo repetitionInfo = RepetitionInstanceInfo.noRepetition(interval);

        Assert.notEmpty(RepetitionUtils.getInstanceIntervalStartingAt(repetitionInfo, start));
        Assert.isTrue(RepetitionUtils.getFirstInstanceInterval(repetitionInfo).get().isEmpty());

        Assert.notEmpty(RepetitionUtils.getIntervals(repetitionInfo, start.minus(13), Option.<Instant>empty(), true, 1));
        Assert.notEmpty(RepetitionUtils.getIntervals(repetitionInfo, start, Option.of(start), true, 1));
        Assert.notEmpty(RepetitionUtils.getIntervals(repetitionInfo, start, Option.of(start.plus(13)), true, 1));
        Assert.isEmpty(RepetitionUtils.getIntervals(repetitionInfo, start.minus(13), Option.of(start), true, 1));
    }

    @Test
    public void zeroLengthInstanceRepeatingEvent() {
        Instant start = MoscowTime.instant(2012, 10, 15, 17, 25);
        InstantInterval interval = new InstantInterval(start, start);

        RepetitionInstanceInfo repetitionInfo =
                RepetitionInstanceInfo.create(interval, MoscowTime.TZ, Option.of(createDailyRepetition()));

        ListF<InstantInterval> is = RepetitionUtils.getIntervals(repetitionInfo, start, Option.<Instant>empty(), true, 4);
        Assert.equals(start.plus(Duration.standardDays(3)), is.get(3).getStart());
    }

    @Test
    public void emptyInterval() {
        Instant start = MoscowTime.instant(2012, 11, 26, 19, 30);
        Instant end = start.plus(Duration.standardHours(2));
        Instant mid = start.plus(Duration.standardHours(1));

        InstantInterval interval = new InstantInterval(start, end);

        RepetitionInstanceInfo repetitionInfo = RepetitionInstanceInfo.noRepetition(interval);

        Assert.notEmpty(RepetitionUtils.getIntervals(repetitionInfo, start, Option.of(start), true, 1));
        Assert.notEmpty(RepetitionUtils.getIntervals(repetitionInfo, start, Option.of(start), false, 1));

        Assert.notEmpty(RepetitionUtils.getIntervals(repetitionInfo, mid, Option.of(mid), true, 1));
        Assert.isEmpty(RepetitionUtils.getIntervals(repetitionInfo, mid, Option.of(mid), false, 1));

        Assert.isEmpty(RepetitionUtils.getIntervals(repetitionInfo, end, Option.of(end), true, 1));
        Assert.isEmpty(RepetitionUtils.getIntervals(repetitionInfo, end, Option.of(end), false, 1));
    }

    // CAL-7298
    @Test
    public void dueSameAsStart() {
        DateTime start = MoscowTime.dateTime(2015, 6, 10, 16, 0);

        InstantInterval interval = new InstantInterval(start, start.plusHours(1));
        Repetition repetition = createDailyRepetition(start);

        RepetitionInstanceInfo repetitionInfo = new RepetitionInstanceInfo(
                interval, MoscowTime.TZ, Option.of(repetition), Cf.list(), Cf.list(), Cf.list());

        ListF<InstantInterval> is = RepetitionUtils.getIntervals(repetitionInfo,
                MoscowTime.instant(2015, 6, 10, 0, 0),
                Option.of(MoscowTime.instant(2015, 6, 11, 0, 0)), true, 1);

        Assert.hasSize(1, is);
        Assert.equals(interval.getStart(), is.single().getStart());
        Assert.equals(interval.getEnd(), is.single().getEnd());
    }

    // CAL-7586
    @Test
    public void dueOverlap() {
        DateTime start = MoscowTime.dateTime(2015, 11, 24, 16, 0);

        InstantInterval interval = new InstantInterval(start, start.plusHours(4));
        Repetition repetition = createDailyRepetition(start.plusHours(1));

        RepetitionInstanceInfo repetitionInfo = new RepetitionInstanceInfo(
                interval, MoscowTime.TZ, Option.of(repetition), Cf.list(), Cf.list(), Cf.list());

        ListF<InstantInterval> is = RepetitionUtils.getIntervals(repetitionInfo,
                start.plusHours(2).toInstant(),
                Option.of(start.plusHours(3).toInstant()), true, 1);

        Assert.hasSize(1, is);
    }

    @Test
    public void allDayInstanceStartsInLocalTimeGap() {
        DateTimeZone tz = DateTimeZone.forID("Asia/Beirut");
        LocalDateTime localStart = new LocalDateTime(2014, 3, 28, 0, 0);

        InstantInterval interval = new InstantInterval(localStart.toDateTime(tz), localStart.plusDays(1).toDateTime(tz));
        Instant start = interval.getStart();
        RepetitionInstanceInfo repetition = RepetitionInstanceInfo.create(interval, tz, createDailyRepetition());

        Assert.isTrue(tz.isLocalDateTimeGap(localStart.plusDays(2)));

        ListF<InstantInterval> expected = Cf.list(
                interval(localStart.plusDays(0), localStart.plusDays(1), tz),
                interval(localStart.plusDays(1), localStart.plusDays(2).plusHours(1), tz),
                interval(localStart.plusDays(2).plusHours(1), localStart.plusDays(3), tz),
                interval(localStart.plusDays(3), localStart.plusDays(4), tz));

        Assert.equals(expected, RepetitionUtils.getIntervals(repetition, start, Option.<Instant>empty(), true, 4));
    }

    private static InstantInterval interval(LocalDateTime start, LocalDateTime end, DateTimeZone tz) {
        return new InstantInterval(start.toDateTime(tz), end.toDateTime(tz));
    }

    private static Repetition createDailyRepetition() {
        return createDailyRepetition(Option.<ReadableInstant>empty());
    }

    private static Repetition createDailyRepetition(ReadableInstant due) {
        return createDailyRepetition(Option.of(due));
    }

    private static Repetition createDailyRepetition(Option<ReadableInstant> due) {
        Repetition r = new Repetition();
        r.setType(RegularRepetitionRule.DAILY);
        r.setDueTs(due.map(ReadableInstant::toInstant));
        r.setREach(1);
        return r;
    }

    private static Repetition createMonthlyNumberRepetition() {
        Repetition r = new Repetition();
        r.setType(RegularRepetitionRule.MONTHLY_NUMBER);
        r.setREach(1);
        return r;
    }

    private static Repetition createMonthlyDayWeeknoRepetition() {
        Repetition r = new Repetition();
        r.setType(RegularRepetitionRule.MONTHLY_DAY_WEEKNO);
        r.setREach(1);
        r.setRMonthlyLastweek(false);
        return r;
    }

    private static Interval interval30Minutes(DateTime start) {
        return new Interval(start, Minutes.minutes(30));
    }

    private RepetitionInstanceInfo createRandomRepetition(DateTimeZone tz) {
        int rEach = 1 + Random2.R.nextInt(10);
        ListF<RegularRepetitionRule> rules = RegularRepetitionRule.R.valuesList()
                .filter(Cf.Object.equalsF(RegularRepetitionRule.NONE).notF());

        RegularRepetitionRule rule = Random2.R.randomElement(rules);

        Repetition repetition = new Repetition();
        repetition.setREach(rEach);
        repetition.setType(rule);

        if (rule == RegularRepetitionRule.WEEKLY) {
            repetition.setRWeeklyDays(randomWeekDays().mkString(","));
        }
        InstantInterval eventInterval = new InstantInterval(new DateTime(2010, 9, 20, 18, 30, 12, 0, tz).toInstant(), new DateTime(2010, 9, 20, 19, 30, 12, 0, tz).toInstant());

        return RepetitionInstanceInfo.create(eventInterval, tz, Option.of(repetition));
    }

    private ListF<String> randomWeekDays() {
         return Random2.R.randomElements(Cf.list("mon", "tue", "wed", "thu", "fri", "sat", "sun"), 1 + Random2.R.nextInt(6));
    }

} //~
