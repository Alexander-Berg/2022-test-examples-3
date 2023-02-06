package ru.yandex.calendar.logic.ics.imp;

import net.fortuna.ical4j.model.component.VTimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.event.repetition.RepetitionRoutines;
import ru.yandex.calendar.logic.event.repetition.RepetitionUtils;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.TimeZoneRegistry2;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.ComponentsMeta;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVTimeZone;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsParameter;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsTzId;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsDtEnd;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsDtStart;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.dateTime.IcsDateTime;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author ssytnik
 */
public class IcsImporterNonMeetingCreateTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private IcsImporter icsImporter;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private RepetitionRoutines repetitionRoutines;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private MainEventDao mainEventDao;


    @Test
    public void customVTimezoneTZID() {
        customVTimezoneTZID("super-custom-tzid");
    }

    @Test
    public void customVTimezoneTZIDMoscowFromOutlook() {
        // we support this wide-spread special case
        customVTimezoneTZID("(UTC+03:00) Волгоград, Москва, Санкт-Петербург");
    }

    private void customVTimezoneTZID(String customTzId) {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-12200");

        ListF<IcsParameter> icsTzIdParamList = Cf.<IcsParameter>list(
            new ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsTzId(customTzId)
        );

        IcsVEvent icsVEvent = new IcsVEvent();
        icsVEvent = icsVEvent.withDtStampNow();
        icsVEvent = icsVEvent.withUid(CalendarUtils.generateExternalId());
        icsVEvent = icsVEvent.withSummary("customVTimezoneTZID");
        icsVEvent = icsVEvent.withDtStart(new IcsDtStart("20110211T101000", icsTzIdParamList)); // 13:10 11.02.2011
        icsVEvent = icsVEvent.withDtEnd(new IcsDtEnd("20110211T104000Z", icsTzIdParamList)); // 13:40 11.02.2011
        icsVEvent = icsVEvent.withSequenece(0);

        String timezone =
            "BEGIN:VTIMEZONE\n" +
            "TZID:" + customTzId + "\n" + // <--
            "X-LIC-LOCATION:Europe/Moscow\n" +
            "BEGIN:STANDARD\n" +
            "TZOFFSETFROM:+0400\n" +
            "TZOFFSETTO:+0300\n" +
            "TZNAME:MSK\n" +
            "DTSTART:19961027T030000\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\n" +
            "END:STANDARD\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+0300\n" +
            "TZOFFSETTO:+0400\n" +
            "TZNAME:MSD\n" +
            "DTSTART:19930328T020000\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\n" +
            "END:DAYLIGHT\n" +
            "END:VTIMEZONE";

        IcsVTimeZone icsVTimeZone = (IcsVTimeZone) ComponentsMeta.M.metaByName("VTIMEZONE").get().parseString(timezone);

        IcsCalendar c = new IcsCalendar(Cf.list(icsVEvent, icsVTimeZone));

        IcsImportStats stats = icsImporter.importIcsStuff(
                user.getUid(), c, IcsImportMode.importFile(LayerReference.defaultLayer()));

        Event event = eventDao.findEventById(stats.getNewEventIds().single());
        Assert.equals(MoscowTime.instant(2011, 2, 11, 10, 10, 0), event.getStartTs());
        Assert.equals(MoscowTime.instant(2011, 2, 11, 13, 40, 0), event.getEndTs());
    }

    @Test
    public void missingTimeZoneDefinition() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-12200");
        DateTimeZone userTz = DateTimeZone.forID("Asia/Yekaterinburg");
        settingsRoutines.updateTimezone(user.getUid(), userTz.getID());

        ListF<IcsParameter> icsTzIdParamList = Cf.list(new IcsTzId("Україна"));

        IcsVEvent icsVEvent = new IcsVEvent();
        icsVEvent = icsVEvent.withDtStampNow();
        icsVEvent = icsVEvent.withUid(CalendarUtils.generateExternalId());
        icsVEvent = icsVEvent.withSummary("noDateTimeZone");
        icsVEvent = icsVEvent.withDtStart(new IcsDtStart("20110211T101000", icsTzIdParamList));
        icsVEvent = icsVEvent.withDtEnd(new IcsDtEnd("20110211T104000", icsTzIdParamList));
        icsVEvent = icsVEvent.withSequenece(0);

        IcsImportStats stats = icsImporter.importIcsStuff(
                user.getUid(), icsVEvent.makeCalendar(), IcsImportMode.importFile(LayerReference.defaultLayer()));

        Event event = eventDao.findEventById(stats.getNewEventIds().single());
        Assert.equals(new DateTime(2011, 2, 11, 10, 10, 0, userTz).toInstant(), event.getStartTs());
    }

    @Test
    public void suitableTzForRepeatingOnly() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-12200");
        VTimeZone vtz = TimeZoneRegistry2.outlookTimeZones.getVTimeZone("Europe/Kiev").get();

        LocalDateTime start = new LocalDateTime(2015, 2, 11, 10, 10, 0);
        LocalDateTime end = start.plusHours(1);

        vtz.getTimeZoneId().setValue("Україна");

        IcsVEvent icsVEvent = new IcsVEvent();
        icsVEvent = icsVEvent.withDtStampNow();
        icsVEvent = icsVEvent.withUid(CalendarUtils.generateExternalId());
        icsVEvent = icsVEvent.withSummary("Single");
        icsVEvent = icsVEvent.withDtStart(IcsDateTime.dateTime(start, vtz.getTimeZoneId().getValue()));
        icsVEvent = icsVEvent.withDtEnd(IcsDateTime.dateTime(end, vtz.getTimeZoneId().getValue()));
        icsVEvent = icsVEvent.withSequenece(0);

        Function<IcsVEvent, IcsImportStats> importF = vevent -> icsImporter.importIcsStuff(
                user.getUid(), vevent.makeCalendar().addComponent(IcsVTimeZone.fromIcal4j(vtz)),
                IcsImportMode.importFile(LayerReference.defaultLayer()));

        Function<Event, String> findTzIdF = event -> mainEventDao.findTimezoneIdsByMainEventIds(
                Cf.list(event.getMainEventId())).single().get2();

        Event event = eventDao.findEventById(importF.apply(icsVEvent).getNewEventIds().single());
        DateTimeZone expectedTz = DateTimeZone.forOffsetHours(2);

        Assert.equals(start.toDateTime(expectedTz).toInstant(), event.getStartTs());
        Assert.equals(expectedTz.getID(), findTzIdF.apply(event));

        icsVEvent = icsVEvent.withUid(CalendarUtils.generateExternalId());
        icsVEvent = icsVEvent.withRecurrence(event.getStartTs());

        event = eventDao.findEventById(importF.apply(icsVEvent).getNewEventIds().single());
        DateTimeZone tz = DateTimeZone.forID(findTzIdF.apply(event));

        Assert.equals(3 * DateTimeConstants.MILLIS_PER_HOUR,
                tz.getOffset(event.getStartTs().plus(Duration.standardDays(180))));
    }

    /**
     * @url https://jira.yandex-team.ru/browse/CAL-2808
     */
    @Test
    public void bugDen() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-12211");

        String ics =
            "BEGIN:VCALENDAR\n" +
            "VERSION:2.0\n" +
            "PRODID:-//PYVOBJECT//NONSGML Version 1//EN\n" +
            "BEGIN:VEVENT\n" +
            "UID:20110211T145607Z-32101@invite01e.tools.yandex.net\n" +
            "DTSTART:20110216T170000\n" + // <!-- timezone not specified
            "DTEND:20110216T183000\n" +
            "CATEGORIES:4.Квартал\n" +
            "DTSTAMP:20101207T112006\n" + // <-- incorrect value here, must be UTC
            "LOCATION:4.Квартал\n" +
            "SUMMARY:Встреча по инфраструктуре\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n" +
            "";

        IcsCalendar calendar = IcsCalendar.parseString(ics);

        IcsImportStats stats = icsImporter.importIcsStuff(user.getUid(), calendar, IcsImportMode.caldavPutToDefaultLayerForTest());
        Assert.A.hasSize(1, stats.getNewEventIds());

        Event event = eventDao.findEventById(stats.getNewEventIds().single());
        Assert.A.equals(TestDateTimes.moscow(2011, 2, 16, 17, 0), event.getStartTs());
    }

    // CAL-7141
    @Test
    public void dueTsBeforeStart() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-12222");
        String ics =
                "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "UID:DB47228B-FD29-478A-8F67-15B827167F0F\n" +
                "SUMMARY:AAXIS: Invoice\n" +
                "DTSTART:20141117T180000Z\n" +
                "DTEND:20141117T194500Z\n" +
                "RRULE:FREQ=MONTHLY;UNTIL=20141115T161500Z;INTERVAL=1\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR\n";

        IcsCalendar calendar = IcsCalendar.parseString(ics);

        IcsImportStats stats = icsImporter.importIcsStuff(
                user.getUid(), calendar, IcsImportMode.caldavPutToDefaultLayerForTest());
        Assert.hasSize(1, stats.getNewEventIds());

        RepetitionInstanceInfo repetitionInfo =  repetitionRoutines.getRepetitionInstanceInfoByEventId(
                stats.getNewEventIds().single());
        Assert.hasSize(1, RepetitionUtils.getIntervals(repetitionInfo,
                MoscowTime.instant(2000, 1, 1, 0, 0),
                Option.of(MoscowTime.instant(2020, 1, 1, 0, 0)), false, -1));
    }

}
