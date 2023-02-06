package ru.yandex.calendar.logic.ics.iv5j.ical.property;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsTzId;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class IcsDateTimePropertyBaseTest {

    // http://tools.ietf.org/html/rfc5545#section-3.3.4
    @Test
    public void localDate() {
        IcsDtStart dtStart = new IcsDtStart("19970714");
        Assert.assertTrue(dtStart.isDate());
        Assert.assertFalse(dtStart.isWithZone());
        Assert.A.equals(new LocalDate(1997, 7, 14), dtStart.getLocalDate());
    }

    // http://tools.ietf.org/html/rfc5545#section-3.3.5
    @Test
    public void localDateTime() {
        IcsDtStart dtStart = new IcsDtStart("19980318T231213");
        Assert.assertFalse(dtStart.isDate());
        Assert.assertFalse(dtStart.isWithZone());
        Assert.A.equals(new LocalDateTime(1998, 3, 18, 23, 12, 13), dtStart.getLocalDateTime());
    }

    // http://tools.ietf.org/html/rfc5545#section-3.3.5
    @Test
    public void dateTimeUtc() {
        IcsDtStart dtStart = new IcsDtStart("19980419T071405Z");
        Assert.assertFalse(dtStart.isDate());
        Assert.assertTrue(dtStart.isWithZone());
        Assert.A.equals(new DateTime(1998, 4, 19, 7, 14, 5, 0, DateTimeZone.UTC),
                dtStart.getDateTime(IcsVTimeZones.fallback(DateTimeZone.UTC)));
    }

    // http://tools.ietf.org/html/rfc5545#section-3.3.5
    @Test
    public void dateTimeWithNamedZone() {
        IcsDtStart dtStart = new IcsDtStart("19980619T020304", Cf.list(new IcsTzId("America/New_York")));
        Assert.assertFalse(dtStart.isDate());
        Assert.assertTrue(dtStart.isWithZone());
        Assert.A.equals(new DateTime(1998, 6, 19, 2, 3, 4, 0, DateTimeZone.forID("America/New_York")),
                dtStart.getDateTime(IcsVTimeZones.fallback(DateTimeZone.UTC)));
    }

} //~
