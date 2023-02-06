package ru.yandex.calendar.logic.ics.iv5j.ical;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVTimeZone;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class IcsTimeZonesTest {

    @Test
    public void americaNewYork() {
        Assert.some(DateTimeZone.forID("America/New_York"), IcsTimeZones.forId("America/New_York"));
    }

    @Test
    public void russianStandardTime() {
        Assert.some(DateTimeZone.forID("Europe/Moscow"), IcsTimeZones.forId("Russian Standard Time"));
    }

    @Test
    public void findByLocalizedNames() {
        // English GMT / UTC
        Assert.some(DateTimeZone.forID("Europe/Moscow"), IcsTimeZones.forId("(GMT+03:00) Moscow, St. Petersburg, Volgograd"));
        Assert.some(DateTimeZone.forID("Europe/Moscow"), IcsTimeZones.forId("(UTC+03:00) Moscow, St. Petersburg, Volgograd"));
        // Russian GMT / UTC
        Assert.some(DateTimeZone.forID("Europe/Moscow"), IcsTimeZones.forId("(GMT+03:00) Волгоград, Москва, Санкт-Петербург"));
        Assert.some(DateTimeZone.forID("Europe/Moscow"), IcsTimeZones.forId("(UTC+03:00) Волгоград, Москва, Санкт-Петербург"));
        // DST offset
        Assert.some(DateTimeZone.forID("Europe/Moscow"), IcsTimeZones.forId("(GMT+04:00) Волгоград, Москва, Санкт-Петербург"));
        Assert.some(DateTimeZone.forID("Europe/Moscow"), IcsTimeZones.forId("(UTC+04:00) Волгоград, Москва, Санкт-Петербург"));
        // http://calendars.office.microsoft.com/pubcalstorage/wxfyt2xz1903440/Календарь_-_Victor-EugL.ics
        Assert.some(DateTimeZone.forID("Europe/Moscow"), IcsTimeZones.forId("Москва, Санкт-Петербург, Волгоград"));
        // No offset
        Assert.some(DateTimeZone.forID("GMT"), IcsTimeZones.forId("(GMT) Монровия, Рейкьявик"));
        Assert.some(DateTimeZone.forID("GMT"), IcsTimeZones.forId("(UTC) Монровия, Рейкьявик"));
        // Some others
        Assert.some(DateTimeZone.forID("Asia/Bangkok"), IcsTimeZones.forId("(UTC+07:00) Бангкок, Джакарта, Ханой"));
    }

    @Test
    public void utcPlus() {
        Assert.equals(3 * 3600 * 1000, IcsTimeZones.forId("GMT+0300").get().getOffset(0));
        Assert.equals(-3 * 3600 * 1000, IcsTimeZones.forId("GMT-0300").get().getOffset(0));
        Assert.equals(7 * 3600 * 500, IcsTimeZones.forId("GMT+0330").get().getOffset(0));

        Assert.equals(3 * 3600 * 1000, IcsTimeZones.forId("UTC+3:00").get().getOffset(0));
        Assert.equals(7 * 3600 * 500, IcsTimeZones.forId("UTC+3:30").get().getOffset(0));
        Assert.equals(3 * 3600 * 1000, IcsTimeZones.forId("+3:00").get().getOffset(0));
        Assert.equals(7 * 3600 * 500, IcsTimeZones.forId("+3:30").get().getOffset(0));

        Assert.equals(3 * 3600 * 1000, IcsTimeZones.forId("Etc/GMT-3").get().getOffset(0));
    }

    @Test
    public void outlookAndRawTimeZones() {
        IcsVTimeZone outlook = IcsTimeZones.icsVTimeZoneForIdForOutlook("Europe/Moscow");
        Assert.isTrue(outlook.getComponents().size() <= 2);
        IcsVTimeZone raw = IcsTimeZones.icsVTimeZoneForIdFull("Europe/Moscow");
        Assert.isTrue(raw.getComponents().size() > 5);
    }

} //~
