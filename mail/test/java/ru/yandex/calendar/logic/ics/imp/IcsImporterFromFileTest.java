package ru.yandex.calendar.logic.ics.imp;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.function.Function1B;
import ru.yandex.calendar.frontend.ews.imp.EwsImporterCreateEventTest;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Rdate;
import ru.yandex.calendar.logic.beans.generated.RdateFields;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventInstanceInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.LayerIdPredicate;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class IcsImporterFromFileTest extends IcsImporterFromFileTestBase {

    @Autowired
    protected GenericBeanDao genericBeanDao;
    @Autowired
    protected EventRoutines eventRoutines;
    @Autowired
    protected EventDao eventDao;
    @Autowired
    private TestManager testManager;


    private void checkRecurEventsExistence(long eId, SetF<Instant> recurSet) {
        long mainEventId = eventDao.findEventById(eId).getMainEventId();
        String sql = "SELECT recurrence_id FROM event WHERE main_event_id = ? AND recurrence_id IS NOT NULL";
        SetF<Instant> recurIds = jdbcTemplate.queryForList(sql, Instant.class, mainEventId).unique();
        Assert.A.equals(recurSet, recurIds);
    }

    @Test
    public void exdate() throws Exception {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10709");

        long eventId = importOneEventIcs("import/exdate.ics", user.getUid());

        ListF<Rdate> exdates = eventDao.findRdatesByEventId(eventId);
        Assert.A.forAll(exdates, Function1B.wrap(RdateFields.IS_RDATE.getF()).notF());

        ListF<Instant> expectedExdates = Cf.list(
                TestDateTimes.moscow(2008, 7, 4, 13, 0),
                TestDateTimes.moscow(2008, 8, 1, 13, 0),
                TestDateTimes.moscow(2008, 10, 10, 13, 0)
            );
        Assert.A.equals(expectedExdates, exdates.map(RdateFields.START_TS.getF()).sorted());
    }

    @Test
    public void exdateWithTz() throws Exception {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10708");

        long eventId = importOneEventIcs("import/exdate_tz.ics", user.getUid());

        Rdate exdate = eventDao.findRdatesByEventId(eventId).single();
        Assert.A.isFalse(exdate.getIsRdate());
        Assert.A.equals(TestDateTimes.moscow(2003, 4, 6, 19, 0), exdate.getStartTs());
    }

    @Test
    public void rdate() throws Exception {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10707");

        long eventId = importOneEventIcs("import/rdate.ics", user.getUid());

        Assert.A.isEmpty(eventDao.findExdateRdatesByEventId(eventId));

        ListF<Instant> rdates = eventDao.findRdateRdatesByEventId(eventId)
            .map(RdateFields.START_TS.getF())
            .sorted()
            ;

        ListF<Instant> expectedRdates = Cf.list(
                TestDateTimes.moscow(2008, 3, 4, 0, 0),
                TestDateTimes.moscow(2008, 5, 4, 0, 0),
                TestDateTimes.moscow(2008, 7, 4, 0, 0)
            );

        Assert.A.equals(
                expectedRdates,
                rdates
            );

    }

    @Test
    public void rdateWithPeriod() throws Exception {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10706");

        long eventId = importOneEventIcs("import/rdate_period.ics", user.getUid());

        ListF<Rdate> rdates = eventDao.findRdateRdatesByEventId(eventId)
            .sortedBy(RdateFields.START_TS.getF())
            ;

        Assert.A.hasSize(2, rdates);

        Assert.A.equals(TestDateTimes.moscow(2006, 1, 19, 0, 0), rdates.get(0).getStartTs());
        Assert.A.equals(TestDateTimes.moscow(2006, 1, 19, 4, 0), rdates.get(0).getEndTs().get());

        Assert.A.equals(TestDateTimes.moscow(2006, 1, 20, 0, 0), rdates.get(1).getStartTs());
        Assert.A.equals(TestDateTimes.moscow(2006, 1, 20, 4, 0), rdates.get(1).getEndTs().get());
    }

    @Test
    public void recurEvent() throws Exception {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10705");

        long eId = importOneEventIcs("import/recurrence_id.ics", user.getUid());
        SetF<Instant> recurSet = Cf.<Instant>hashSet();
        recurSet.add(TestDateTimes.moscow(2008, 8, 29, 13, 0));
        recurSet.add(TestDateTimes.moscow(2008, 7, 25, 13, 0));
        checkRecurEventsExistence(eId, recurSet);
    }

    @Test
    public void recurEventWithTz() throws Exception {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10701");

        long eId = importOneEventIcs("import/recurrence_id_tz.ics", user.getUid());
        DateTime dt = new DateTime(2008, 2, 22, 23, 15, 0, 0, DateTimeZone.forID("US/Eastern"));
        checkRecurEventsExistence(eId, Cf.set(dt.toInstant()));
    }

    @Test
    public void getSortedInst() throws Exception {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10702");

        importOneEventIcs("import/rdate.ics", user.getUid());
        Instant startMs = TestDateTimes.moscow(2008, 3, 1, 0, 0);
        Instant endMs = TestDateTimes.moscow(2008, 3, 30, 23, 0);
        ListF<EventInstanceInfo> infoSet = eventRoutines.getSortedInstancesIMayView(Option.of(user.getUid()),
                startMs, Option.of(endMs), LayerIdPredicate.allForUser(user.getUid(), false), ActionSource.WEB);
        Assert.assertHasSize(1, infoSet);
        EventInstanceInfo info = infoSet.first();
        Assert.A.equals(TestDateTimes.moscow(2008, 3, 4, 0, 0), info.getInterval().getStart());
        Assert.A.equals(TestDateTimes.moscow(2008, 3, 5, 0, 0), info.getInterval().getEnd());
    }

    @Test
    public void eduBoardFixed() throws Exception {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10703");

        importIcsByUid("import/eduboard_fixed.ics", user.getUid());
    }

    /**
     * @see EwsImporterCreateEventTest#properStartTsInAllDayEvent()
     */
    @Test
    public void properStartTs() throws Exception {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10704");
        String calendar =
                "BEGIN:VCALENDAR\n" +
                "METHOD:PUBLISH\n" +
                "PRODID:Microsoft Exchange Server 2007\n" +
                "VERSION:2.0\n" +
                "BEGIN:VTIMEZONE\n" +
                "TZID:(UTC+03:00) Moscow\\, St. Petersburg\\, Volgograd\n" +
                "BEGIN:STANDARD\n" +
                "DTSTART:16010101T030000\n" +
                "TZOFFSETFROM:+0400\n" +
                "TZOFFSETTO:+0300\n" +
                "RRULE:FREQ=YEARLY;INTERVAL=1;BYMONTH=10;BYDAY=-1SU\n" +
                "END:STANDARD\n" +
                "BEGIN:DAYLIGHT\n" +
                "DTSTART:16010101T020000\n" +
                "TZOFFSETFROM:+0300\n" +
                "TZOFFSETTO:+0400\n" +
                "RRULE:FREQ=YEARLY;INTERVAL=1;BYMONTH=3;BYDAY=-1SU\n" +
                "END:DAYLIGHT\n" +
                "END:VTIMEZONE\n" +
                "BEGIN:VEVENT\n" +
                "RRULE:FREQ=WEEKLY;WKST=MO;INTERVAL=1;UNTIL=20111213T210000Z;BYDAY=MO\n" +
                "SUMMARY;LANGUAGE=en-US:Dummy name\n" +
                "DTSTART;TZID=\"(UTC+03:00) Moscow, St. Petersburg, Volgograd\":20110718T160000\n" +
                "DTEND;TZID=\"(UTC+03:00) Moscow, St. Petersburg, Volgograd\":20110718T170000\n" +
                "UID:040000008200E00074C5B7101A82E00800000000B04355_Dummy\n" +
                "DTSTAMP:20110715T132548Z\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";
        IcsImportStats stats = importCalendar(user.getUid(), IcsCalendar.parseString(calendar));
        Event event = eventDao.findEventById(stats.getNewEventIds().single());
        Assert.equals(TestDateTimes.utc(2011, 7, 18, 12, 0), event.getStartTs());
    }
}
