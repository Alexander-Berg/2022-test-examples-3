package ru.yandex.calendar.logic.ics.iv5j.ical.component;

import java.io.StringReader;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.component.XComponent;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.meta.IcssMeta;
import ru.yandex.calendar.logic.ics.iv5j.ical.meta.IcssMetaTestSupport;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class ComponentsMetaTest extends IcssMetaTestSupport<IcsComponent, CalendarComponent> {

    @Override
    protected Class<CalendarComponent> dataClass() {
        return CalendarComponent.class;
    }

    @Override
    protected IcssMeta<IcsComponent, ?, CalendarComponent, ?, ?> meta() {
        return ComponentsMeta.M;
    }

    @Override
    protected Class<? extends CalendarComponent> xDataClass() {
        return XComponent.class;
    }

    @Override
    protected String packageSuffix() {
        return "component";
    }

    @Test
    public void test() {
        Assert.assertTrue(ComponentsMeta.M.newTheir(CalendarComponent.VEVENT) instanceof VEvent);
        Assert.assertTrue(ComponentsMeta.M.newTheir("DFGDG") instanceof XComponent);
    }

    @Test
    public void vtimezone1() throws Exception {
        // http://tools.ietf.org/html/rfc5545#page-69
        String ics =
                "BEGIN:VTIMEZONE\n" +
                "TZID:America/New_York\n" +
                "LAST-MODIFIED:20050809T050000Z\n" +
                "BEGIN:STANDARD\n" +
                "DTSTART:20071104T020000\n" +
                "TZOFFSETFROM:-0400\n" +
                "TZOFFSETTO:-0500\n" +
                "TZNAME:EST\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "DTSTART:20070311T020000\n" +
                "TZOFFSETFROM:-0500\n" +
                "TZOFFSETTO:-0400\n" +
                "TZNAME:EDT\n" +
                "END:DAYLIGHT\n" +
                "END:VTIMEZONE\n" +
                "";
        String ics2 = "BEGIN:VCALENDAR\n" + ics + "END:VCALENDAR\n";
        Calendar calendar = new CalendarBuilder().build(new StringReader(ics2));
        VTimeZone vtimezone = (VTimeZone) calendar.getComponents().get(0);
        IcsVTimeZone icsVtimezone = (IcsVTimeZone) ComponentsMeta.M.fromIcal4j(vtimezone);
        Assert.A.some("20050809T050000Z", icsVtimezone.getPropertyValue("LAST-MODIFIED"));
        ListF<IcsComponent> observances = icsVtimezone.getComponents("STANDARD");
        Assert.A.hasSize(1, observances);
        IcsComponent standard = observances.single();
        Assert.A.some("EST", standard.getPropertyValue("TZNAME"));

        Assert.A.hasSize(2, icsVtimezone.toComponent().getObservances());
    }

    @Test
    public void vtimezone2() throws Exception {
        String ics =
            "BEGIN:VCALENDAR\n" +
            "PRODID:-//tzurl.org//NONSGML Olson 2010b//EN\n" +
            "VERSION:2.0\n" +
            "BEGIN:VTIMEZONE\n" +
            "TZID:Europe/Moscow\n" +
            "TZURL:http://tzurl.org/zoneinfo/Europe/Moscow\n" +
            "X-LIC-LOCATION:Europe/Moscow\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+0300\n" +
            "TZOFFSETTO:+0400\n" +
            "TZNAME:MSD\n" +
            "DTSTART:19930328T020000\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\n" +
            "END:DAYLIGHT\n" +
            "BEGIN:STANDARD\n" +
            "TZOFFSETFROM:+0400\n" +
            "TZOFFSETTO:+0300\n" +
            "TZNAME:MSK\n" +
            "DTSTART:19961027T030000\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\n" +
            "END:STANDARD\n" +
            "BEGIN:STANDARD\n" +
            "TZOFFSETFROM:+023020\n" +
            "TZOFFSETTO:+0230\n" +
            "TZNAME:MMT\n" +
            "DTSTART:18800101T000000\n" +
            "RDATE:18800101T000000\n" +
            "END:STANDARD\n" +
            "BEGIN:STANDARD\n" +
            "TZOFFSETFROM:+0230\n" +
            "TZOFFSETTO:+023048\n" +
            "TZNAME:MMT\n" +
            "DTSTART:19160703T000000\n" +
            "RDATE:19160703T000000\n" +
            "END:STANDARD\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+023048\n" +
            "TZOFFSETTO:+033048\n" +
            "TZNAME:MST\n" +
            "DTSTART:19170701T230000\n" +
            "RDATE:19170701T230000\n" +
            "END:DAYLIGHT\n" +
            "BEGIN:STANDARD\n" +
            "TZOFFSETFROM:+033048\n" +
            "TZOFFSETTO:+023048\n" +
            "TZNAME:MMT\n" +
            "DTSTART:19171228T000000\n" +
            "RDATE:19171228T000000\n" +
            "END:STANDARD\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+023048\n" +
            "TZOFFSETTO:+043048\n" +
            "TZNAME:MDST\n" +
            "DTSTART:19180531T220000\n" +
            "RDATE:19180531T220000\n" +
            "END:DAYLIGHT\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+043048\n" +
            "TZOFFSETTO:+033048\n" +
            "TZNAME:MST\n" +
            "DTSTART:19180916T010000\n" +
            "RDATE:19180916T010000\n" +
            "END:DAYLIGHT\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+033048\n" +
            "TZOFFSETTO:+043048\n" +
            "TZNAME:MDST\n" +
            "DTSTART:19190531T230000\n" +
            "RDATE:19190531T230000\n" +
            "END:DAYLIGHT\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+043048\n" +
            "TZOFFSETTO:+0400\n" +
            "TZNAME:MSD\n" +
            "DTSTART:19190701T020000\n" +
            "RDATE:19190701T020000\n" +
            "END:DAYLIGHT\n" +
            "BEGIN:STANDARD\n" +
            "TZOFFSETFROM:+0400\n" +
            "TZOFFSETTO:+0300\n" +
            "TZNAME:MSK\n" +
            "DTSTART:19190816T000000\n" +
            "RDATE:19190816T000000\n" +
            "RDATE:19211001T000000\n" +
            "RDATE:19811001T000000\n" +
            "RDATE:19821001T000000\n" +
            "RDATE:19831001T000000\n" +
            "RDATE:19840930T030000\n" +
            "RDATE:19850929T030000\n" +
            "RDATE:19860928T030000\n" +
            "RDATE:19870927T030000\n" +
            "RDATE:19880925T030000\n" +
            "RDATE:19890924T030000\n" +
            "RDATE:19900930T030000\n" +
            "RDATE:19920926T230000\n" +
            "RDATE:19930926T030000\n" +
            "RDATE:19940925T030000\n" +
            "RDATE:19950924T030000\n" +
            "END:STANDARD\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+0300\n" +
            "TZOFFSETTO:+0400\n" +
            "TZNAME:MSD\n" +
            "DTSTART:19210214T230000\n" +
            "RDATE:19210214T230000\n" +
            "RDATE:19810401T000000\n" +
            "RDATE:19820401T000000\n" +
            "RDATE:19830401T000000\n" +
            "RDATE:19840401T000000\n" +
            "RDATE:19850331T020000\n" +
            "RDATE:19860330T020000\n" +
            "RDATE:19870329T020000\n" +
            "RDATE:19880327T020000\n" +
            "RDATE:19890326T020000\n" +
            "RDATE:19900325T020000\n" +
            "RDATE:19920328T230000\n" +
            "END:DAYLIGHT\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+0400\n" +
            "TZOFFSETTO:+0500\n" +
            "TZNAME:MSD\n" +
            "DTSTART:19210320T230000\n" +
            "RDATE:19210320T230000\n" +
            "END:DAYLIGHT\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+0500\n" +
            "TZOFFSETTO:+0400\n" +
            "TZNAME:MSD\n" +
            "DTSTART:19210901T000000\n" +
            "RDATE:19210901T000000\n" +
            "END:DAYLIGHT\n" +
            "BEGIN:STANDARD\n" +
            "TZOFFSETFROM:+0300\n" +
            "TZOFFSETTO:+0200\n" +
            "TZNAME:EET\n" +
            "DTSTART:19221001T000000\n" +
            "RDATE:19221001T000000\n" +
            "RDATE:19910929T030000\n" +
            "END:STANDARD\n" +
            "BEGIN:STANDARD\n" +
            "TZOFFSETFROM:+0200\n" +
            "TZOFFSETTO:+0300\n" +
            "TZNAME:MSK\n" +
            "DTSTART:19300621T000000\n" +
            "RDATE:19300621T000000\n" +
            "RDATE:19920119T020000\n" +
            "END:STANDARD\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+0300\n" +
            "TZOFFSETTO:+0300\n" +
            "TZNAME:EEST\n" +
            "DTSTART:19910331T020000\n" +
            "RDATE:19910331T020000\n" +
            "END:DAYLIGHT\n" +
            "END:VTIMEZONE\n" +
            "END:VCALENDAR\n" +
            "";
        Calendar calendar = new CalendarBuilder().build(new StringReader(ics));
        VTimeZone vtimezone = (VTimeZone) calendar.getComponents().get(0);
        IcsVTimeZone icsVtimezone = (IcsVTimeZone) ComponentsMeta.M.fromIcal4j(vtimezone);
        ListF<IcsComponent> standards = icsVtimezone.getComponents("STANDARD");
        Assert.A.hasSize(7, standards);
        ListF<IcsComponent> daylights = icsVtimezone.getComponents("DAYLIGHT");
        Assert.A.hasSize(10, daylights);
    }

    @Test
    public void vtimezone3() {
        IcsVTimeZone icsVtimezone = IcsTimeZones.icsVTimeZoneForIdFull("Europe/Moscow");
        ListF<IcsComponent> standards = icsVtimezone.getComponents("STANDARD");
        Assert.A.isTrue(standards.size() > 5);
        ListF<IcsComponent> daylights = icsVtimezone.getComponents("DAYLIGHT");
        Assert.A.isTrue(daylights.size() > 5);
    }

} //~
