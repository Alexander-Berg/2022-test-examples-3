package ru.yandex.calendar.logic.ics.iv5j.ical.type.recur;

import net.fortuna.ical4j.model.WeekDay;
import org.joda.time.LocalDate;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.type.IcsRecurRulePartByDay;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.type.IcsRecurRulePartCount;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.type.IcsRecurRulePartUntil;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class IcsRecurTest {

    @Test
    public void someBug() {
        String q = "FREQ=MONTHLY;INTERVAL=1";
        IcsRecur recur = IcsRecurParser.P.parseRecur(q);
        Assert.A.hasSize(0, recur.getMonthDayList());
    }

    @Test
    public void construct() {
        IcsRecur recur = new IcsRecur(Freq.MONTHLY)
                .withCount(25)
                .withoutPart(IcsRecurRulePartCount.COUNT)
                .withInterval(38)
                .withWeekStartDay(WeekDay.MO)
                .withUntilDate(new LocalDate(2011, 11, 18))
                .withUntilTs(TestDateTimes.moscow(2011, 11, 18, 0, 0))
                .withPart(new IcsRecurRulePartByDay(Cf.list(WeekDay.MO, WeekDay.TH)));

        Assert.none(recur.getCount());
        Assert.some(recur.getInterval());
        Assert.some(recur.getWeekStartDay());
        Assert.some(recur.getUntil());
        Assert.notEmpty(recur.getDayList());
        Assert.isEmpty(recur.getMinuteList());
        Assert.some(recur.getPartByType(IcsRecurRulePartByDay.class));
        Assert.some(recur.getPartByName(IcsRecurRulePartByDay.BYDAY));
        Assert.hasSize(5, recur.getParts());
    }

    @Test
    public void untilTsAndUntilDate() {
        IcsRecur recur = new IcsRecur(Freq.DAILY).withUntilDate(new LocalDate(2011, 11, 18));
        Assert.some("20111118", recur.getValueByType(IcsRecurRulePartUntil.class));

        recur = new IcsRecur(Freq.WEEKLY).withUntilTs(TestDateTimes.moscow(2011, 11, 18, 0, 0));
        Assert.some("20111117T200000Z", recur.getValueByType(IcsRecurRulePartUntil.class));
    }

} //~
