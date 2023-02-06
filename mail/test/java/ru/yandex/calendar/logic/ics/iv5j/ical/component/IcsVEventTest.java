package ru.yandex.calendar.logic.ics.iv5j.ical.component;

import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Test;

import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

/**
 * @author Stepan Koltsov
 */
public class IcsVEventTest {

    @Test
    public void toIcal4j() {
        Assert.A.sizeIs(0, new IcsVEvent().toComponent().getProperties());
    }

    @Test
    public void getStartEndFullDay() {
        // http://tools.ietf.org/html/rfc5545#section-3.6.1
        String ics =
                "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "UID:20070423T123432Z-541111@example.com\n" +
                "DTSTAMP:20070423T123432Z\n" +
                "DTSTART;VALUE=DATE:20070628\n" +
                "DTEND;VALUE=DATE:20070709\n" +
                "SUMMARY:Festival International de Jazz de Montreal\n" +
                "TRANSP:TRANSPARENT\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR\n";
        IcsVTimeZones tzs = IcsVTimeZones.fallback(MoscowTime.TZ);
        IcsVEvent event = IcsCalendar.parseString(ics).getEvents().single();
        Instant expectedStart = new LocalDate(2007, 6, 28).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Instant expectedEnd = new LocalDate(2007, 7, 9).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Assert.A.equals(expectedStart, event.getStart(tzs));
        Assert.A.equals(expectedEnd, event.getEnd(tzs));
    }

    @Test
    public void getStartEndFullDayUnspecifiedEnd() {
        // http://tools.ietf.org/html/rfc5545#section-3.6.1
        String ics =
                "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "UID:19970901T130000Z-123403@example.com\n" +
                "DTSTAMP:19970901T130000Z\n" +
                "DTSTART;VALUE=DATE:19971102\n" +
                "SUMMARY:Our Blissful Anniversary\n" +
                "TRANSP:TRANSPARENT\n" +
                "CLASS:CONFIDENTIAL\n" +
                "CATEGORIES:ANNIVERSARY,PERSONAL,SPECIAL OCCASION\n" +
                "RRULE:FREQ=YEARLY\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";
        IcsVTimeZones tzs = IcsVTimeZones.fallback(MoscowTime.TZ);
        IcsVEvent event = IcsCalendar.parseString(ics).getEvents().single();
        Instant expectedStart = new LocalDate(1997, 11, 2).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Instant expectedEnd = new LocalDate(1997, 11, 3).toDateTimeAtStartOfDay(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        Assert.A.equals(expectedStart, event.getStart(tzs));
        Assert.A.equals(expectedEnd, event.getEnd(tzs));
    }

} //~
