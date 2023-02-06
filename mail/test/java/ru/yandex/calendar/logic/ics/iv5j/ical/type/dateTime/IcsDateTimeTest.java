package ru.yandex.calendar.logic.ics.iv5j.ical.type.dateTime;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVTimeZone;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author Stepan Koltsov
 */
public class IcsDateTimeTest {

    @Test
    public void parseDateTime() {
        IcsDateTime time = IcsDateTime.parse("19980119T020000", Option.empty());
        IcsDateTime expected = IcsDateTime.localDateTime(new LocalDateTime(1998, 1, 19, 2, 0, 0));
        Assert.A.equals(expected, time);
    }

    @Test
    public void parseDateTimeZ() {
        IcsDateTime time = IcsDateTime.parse("19980119T020000Z", Option.empty());
        IcsDateTime expected = IcsDateTime.dateTime(new DateTime(1998, 1, 19, 2, 0, 0, 0, DateTimeZone.UTC));
        Assert.A.equals(expected, time);
    }

    @Test
    public void parseDate() {
        IcsDateTime time = IcsDateTime.parse("19980119", Option.empty());
        IcsDateTime expected = IcsDateTime.localDate(new LocalDate(1998, 1, 19));
        Assert.A.equals(expected, time);
    }

    @Test
    public void formatDateTimeValue() {
        IcsDateTime icsDateTime = IcsDateTime.dateTime(TestDateTimes.moscowDateTime(2010, 12, 18, 23, 33));
        Assert.A.equals("20101218T233300", icsDateTime.toPropertyValue());
    }

    @Test
    public void formatDateValue() {
        IcsDateTime icsDateTime = IcsDateTime.localDate(new LocalDate(2010, 12, 18));
        Assert.A.equals("20101218", icsDateTime.toPropertyValue());
    }

    @Test
    public void correspondToVTimeZone() {
        Function1V<DateTimeZone> test = (tz) -> {
            IcsDateTime icsDateTime = IcsDateTime.dateTime(new DateTime(2015, 3, 19, 23, 15, tz));
            IcsVTimeZone timezone = IcsTimeZones.icsVTimeZoneForIdFull(tz.getID());

            Assert.equals(timezone.getTzId(), icsDateTime.toPropertyParameters().single().getValue());
        };
        test.apply(DateTimeZone.forOffsetHoursMinutes(2, 37));
        test.apply(DateTimeZone.forOffsetHours(3));
        test.apply(MoscowTime.TZ);
    }

} //~
