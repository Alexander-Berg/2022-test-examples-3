package ru.yandex.calendar.logic.event.repetition;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.IcsRecur;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.IcsRecurRulePart;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.type.IcsRecurRulePartByDay;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.type.IcsRecurRulePartFreq;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.type.IcsRecurRulePartInterval;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.type.IcsRecurRulePartUntil;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class RegularRepetitionRuleTest extends CalendarTestBase {

    // https://jira.yandex-team.ru/browse/CAL-3228
    @Test
    public void monthlyWeekNoRule() {
        IcsRecur recur = new IcsRecur(Cf.<IcsRecurRulePart>list(
            new IcsRecurRulePartFreq("MONTHLY"),
            new IcsRecurRulePartUntil("20120821T200000Z"),
            new IcsRecurRulePartInterval("1"),
            new IcsRecurRulePartByDay("4WE")
        ));
        Assert.A.equals(RegularRepetitionRule.MONTHLY_DAY_WEEKNO, RegularRepetitionRule.find(recur));
    }

}
