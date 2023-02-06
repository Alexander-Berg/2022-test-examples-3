package ru.yandex.calendar.logic.ics.iv5j.ical;

import org.junit.Test;

import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class IcsCalendarTest {

    @Test
    public void eventByRecurrenceIdComparator() {
        IcsVEvent e1 = new IcsVEvent();

        IcsVEvent e2 = new IcsVEvent();
        e2 = e2.withRecurrence(TestDateTimes.moscow(2010, 12, 16, 4, 35));

        Assert.A.isTrue(IcsCalendar.eventByRecurrenceIdComparator().compare(e1, e2)  < 0);
        Assert.A.isTrue(IcsCalendar.eventByRecurrenceIdComparator().compare(e2, e1)  > 0);
        Assert.A.isTrue(IcsCalendar.eventByRecurrenceIdComparator().compare(e1, e1) == 0);
        Assert.A.isTrue(IcsCalendar.eventByRecurrenceIdComparator().compare(e2, e2) == 0);

        e1 = e1.withRecurrence(TestDateTimes.moscow(2005, 10, 13, 4, 35));

        Assert.A.isTrue(IcsCalendar.eventByRecurrenceIdComparator().compare(e1, e2)  < 0);
        Assert.A.isTrue(IcsCalendar.eventByRecurrenceIdComparator().compare(e2, e1)  > 0);
        Assert.A.isTrue(IcsCalendar.eventByRecurrenceIdComparator().compare(e1, e1) == 0);
        Assert.A.isTrue(IcsCalendar.eventByRecurrenceIdComparator().compare(e2, e2) == 0);
    }

} //~
