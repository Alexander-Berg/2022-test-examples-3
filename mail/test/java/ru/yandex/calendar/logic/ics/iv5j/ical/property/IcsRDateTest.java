package ru.yandex.calendar.logic.ics.iv5j.ical.property;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsValue;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.IcsRDateInterval;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author Stepan Koltsov
 */
public class IcsRDateTest {

    @Test
    public void parsePeriods() {
        IcsRDate icsRDate = new IcsRDate("19960403T020000Z/19960403T040000Z,19960404T010000Z/PT3H", Cf.list(new IcsValue("PERIOD")));
        ListF<IcsRDateInterval> icsInstants = icsRDate.getIntervals();
        Assert.A.hasSize(2, icsInstants);
        Assert.A.equals(new DateTime(1996, 4, 3, 2, 0, 0, 0, DateTimeZone.UTC).toInstant(),
                icsInstants.first().getStart(IcsVTimeZones.fallback(MoscowTime.TZ)));
    }

} //~
