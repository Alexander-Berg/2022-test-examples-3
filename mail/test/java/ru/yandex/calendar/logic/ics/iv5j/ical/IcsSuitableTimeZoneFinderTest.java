package ru.yandex.calendar.logic.ics.iv5j.ical;

import java.io.StringReader;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VTimeZone;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class IcsSuitableTimeZoneFinderTest extends CalendarTestBase {

    @Test
    public void fixedOffsetTz() {
        VTimeZone moscow = TimeZoneRegistry2.fullTimeZones.getVTimeZone("Europe/Moscow").get();
        DateTimeZone moscowTz = IcsSuitableTimeZoneFinder.findSuitableTz(moscow);

        Assert.isTrue(moscowTz.isFixed());
        Assert.equals(moscowTz.getOffset(0), MoscowTime.TZ.getOffset(Instant.now()));
    }

    @Test
    public void crazyTransitionTz() throws Exception {
        String ics = "" +
                "BEGIN:VTIMEZONE\n" +
                "TZID:Тагил!\n" +
                "BEGIN:DAYLIGHT\n" +
                "TZOFFSETFROM: -1800\n" +
                "TZOFFSETTO:+0500\n" +
                "DTSTART:19700329T000000\n" +
                "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\n" +
                "END:DAYLIGHT\n" +
                "BEGIN:STANDARD\n" +
                "TZOFFSETFROM:+0500\n" +
                "TZOFFSETTO:-0500\n" +
                "DTSTART:19701025T000000\n" +
                "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\n" +
                "END:STANDARD\n" +
                "END:VTIMEZONE\n";

        String icsX = "BEGIN:VCALENDAR\n" + ics + "END:VCALENDAR\n";
        Calendar calendar = new CalendarBuilder().build(new StringReader(icsX));
        VTimeZone icsTz = (VTimeZone) calendar.getComponent(Component.VTIMEZONE);

        DateTimeZone tz = IcsSuitableTimeZoneFinder.findSuitableTz(icsTz, new LocalDateTime(2015, 6, 1, 12, 0));
        Assert.isTrue(tz.isFixed());
        Assert.equals(tz.getOffset(0), 5 * 3600 * 1000);

        tz = IcsSuitableTimeZoneFinder.findSuitableTz(icsTz, new LocalDateTime(2015, 12, 1, 12, 0));
        Assert.isTrue(tz.isFixed());
        Assert.equals(tz.getOffset(0), -5 * 3600 * 1000);
    }

    @Test
    public void transitionStartDateTime() {
        VTimeZone kiev = TimeZoneRegistry2.outlookTimeZones.getVTimeZone("Europe/Kiev").get();

        IcsSuitableTimeZoneFinder.getObservanceStartDateTime(kiev, new LocalDateTime(2015, 3, 29, 3, 0));

        Function<LocalDateTime, LocalDateTime> startF =
                dt -> IcsSuitableTimeZoneFinder.getObservanceStartDateTime(kiev, dt).get();

        Assert.equals(new LocalDateTime(2015, 3, 29, 3, 0), startF.apply(new LocalDateTime(2015, 3, 29, 3, 0)));
        Assert.equals(new LocalDateTime(2015, 3, 29, 3, 0), startF.apply(new LocalDateTime(2015, 6, 1, 0, 0)));
        Assert.equals(new LocalDateTime(2015, 10, 25, 4, 0), startF.apply(new LocalDateTime(2015, 10, 25, 4, 0)));
        Assert.equals(new LocalDateTime(2015, 10, 25, 4, 0), startF.apply(new LocalDateTime(2015, 12, 1, 0, 0)));
    }

    @Test
    public void winterDaylight() {
        VTimeZone hobart = TimeZoneRegistry2.outlookTimeZones.getVTimeZone("Australia/Hobart").get();
        DateTimeZone hobartTz = IcsSuitableTimeZoneFinder.findSuitableTz(hobart);
        Assert.lt(
                hobartTz.getOffset(new LocalDate(2015, 6, 1).toDateTimeAtStartOfDay(hobartTz)),
                hobartTz.getOffset(new LocalDate(2015, 12, 1).toDateTimeAtStartOfDay(hobartTz)));
    }

    @Test
    public void distanceToDaysOfTransition() {
        VTimeZone santiago = TimeZoneRegistry2.outlookTimeZones.getVTimeZone("America/Santiago").get();
        VTimeZone asuncion = TimeZoneRegistry2.outlookTimeZones.getVTimeZone("America/Asuncion").get();

        Assert.equals(
                IcsSuitableTimeZoneFinder.getDstTzKeyIfDst(santiago).get().getOffsets(),
                IcsSuitableTimeZoneFinder.getDstTzKeyIfDst(asuncion).get().getOffsets());

        Assert.equals(santiago.getTimeZoneId().getValue(), IcsSuitableTimeZoneFinder.findSuitableTz(santiago).getID());
        Assert.equals(asuncion.getTimeZoneId().getValue(), IcsSuitableTimeZoneFinder.findSuitableTz(asuncion).getID());
    }

    @Test
    public void foundByFull() {
        for (String id : Cf.x(DateTimeZone.getAvailableIDs()).filterNot(i -> i.startsWith("tz20"))) {
            IcsSuitableTimeZoneFinder.findSuitableTz(
                    TimeZoneRegistry2.fullTimeZones.getVTimeZone(id).getOrThrow("No " + id));
        }
    }
}
