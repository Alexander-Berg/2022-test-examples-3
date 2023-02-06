package ru.yandex.calendar.frontend.ews.exp;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;

import com.microsoft.schemas.exchange.services._2006.types.AttendeeType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemTypeType;
import com.microsoft.schemas.exchange.services._2006.types.DeletedOccurrenceInfoType;
import com.microsoft.schemas.exchange.services._2006.types.EmailAddressType;
import com.microsoft.schemas.exchange.services._2006.types.EndDateRecurrenceRangeType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfDeletedOccurrencesType;
import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.ReadableInstant;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function;
import ru.yandex.bolts.function.Function1B;
import ru.yandex.calendar.frontend.ews.EwsUtils;
import ru.yandex.calendar.frontend.ews.ExtendedCalendarItemProperties;
import ru.yandex.calendar.frontend.ews.WindowsTimeZones;
import ru.yandex.calendar.frontend.ews.hook.EwsFirewallTestConfiguration;
import ru.yandex.calendar.frontend.ews.hook.EwsNtfContextConfiguration;
import ru.yandex.calendar.frontend.ews.imp.EwsImporter;
import ru.yandex.calendar.frontend.ews.imp.ExchangeEventDataConverter;
import ru.yandex.calendar.frontend.ews.imp.TestCalItemFactory;
import ru.yandex.calendar.frontend.ews.proxy.EwsActionLogData;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxy;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventHelper;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Rdate;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.EventChangesInfo;
import ru.yandex.calendar.logic.event.EventChangesInfoForExchange;
import ru.yandex.calendar.logic.event.EventInfoDbLoader;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.MainEventInfo;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.repetition.RdateChangesInfo;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.event.repetition.RepetitionRoutines;
import ru.yandex.calendar.logic.event.repetition.RepetitionUtils;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.ResourceDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.participant.ParticipantData;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.ParticipantsData;
import ru.yandex.calendar.logic.sharing.participant.ResourceParticipantInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.base.Cf2;
import ru.yandex.calendar.util.dates.AuxDateTime;
import ru.yandex.calendar.util.dates.DateTimeManager;
import ru.yandex.calendar.util.dates.DayOfWeek;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.calendar.test.auto.db.util.TestManager.testExchangeConfRr21;
import static ru.yandex.calendar.test.auto.db.util.TestManager.testExchangeSmolnyEmail;
import static ru.yandex.calendar.test.auto.db.util.TestManager.testExchangeThreeLittlePigsEmail;

@ContextConfiguration(classes = {
        EwsNtfContextConfiguration.class,
        EwsFirewallTestConfiguration.class
})
public class EwsExportRoutinesTest extends AbstractConfTest {
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private EwsImporter ewsImporter;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EwsProxy ewsProxy;
    @Autowired
    private UserManager userManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventResourceDao eventResourceDao;
    @Autowired
    private GenericBeanDao genericBeanDao;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private ResourceRoutines resourceRoutines;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private DateTimeManager dateTimeManager;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private RepetitionRoutines repetitionRoutines;
    @Autowired
    private EventInfoDbLoader eventInfoDbLoader;

    @Test
    public void exportNewCreatedEvent() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        PassportLogin user = new PassportLogin("yandex-team-mm-10491");
        PassportUid uid = userManager.getUidByLoginForTest(user);
        Email email = new Email(user.getNormalizedValue() + "@yandex.ru");

        testManager.cleanUser(uid);

        Event event = testManager.createDefaultMeeting(uid, "sample event (organizer export)");
        testManager.addUserParticipantToEvent(event.getId(), user, Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        ewsProxyWrapper.cancelMeetings(
                testExchangeThreeLittlePigsEmail, event.getStartTs(), event.getEndTs());

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(
                event.getId(), ActionInfo.webTest(event.getStartTs().toInstant().minus(Duration.standardHours(1))));
        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();

        Option<CalendarItemType> item = ewsProxyWrapper.getEvent(exchangeId);
        Assert.A.some(item);
        ExtendedCalendarItemProperties extProperties =
                EwsUtils.convertExtendedProperties(item.get().getExtendedProperty());
        Assert.A.isTrue(extProperties.getWasCreatedFromYaTeamCalendar());
        Assert.A.equals(extProperties.getOrganizerEmail().get(), email);
    }

    @Test
    public void exportNewCreatedEventWithRepetitionDue() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        PassportLogin user = new PassportLogin("yandex-team-mm-10492");
        PassportUid uid = userManager.getUidByLoginForTest(user);

        testManager.cleanUser(uid);

        Event event = testManager.createDefaultMeeting(uid, "sample event (organizer export)");
        Instant dueTs = TestDateTimes.plusDays(event.getStartTs(), 2);
        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(event.getId(), dueTs);
        testManager.addUserParticipantToEvent(event.getId(), user, Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        ewsProxyWrapper.cancelMeetings(
                testExchangeThreeLittlePigsEmail, event.getStartTs(), dueTs);

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(
                event.getId(), ActionInfo.webTest(event.getStartTs().toInstant().minus(Duration.standardHours(1))));
        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();
        CalendarItemType createdItem = ewsProxyWrapper.getEvent(exchangeId).get();
        EventData eventData = ExchangeEventDataConverter.convert(
                createdItem, UidOrResourceId.resource(r.getId()),
                Option.empty(), resourceRoutines::selectResourceEmails);
        final Repetition actualRepetition = eventData.getRepetition();

        Assert.equals(dueTs, actualRepetition.getDueTs().get());
        Assert.equals(1, actualRepetition.getREach().get());
        Assert.equals(RegularRepetitionRule.DAILY, actualRepetition.getType());
    }

    @Test
    public void exportRepeatingWithExdateAndRecurrence() {
        Email exchangeEmail = TestManager.testExchangeUserEmail;

        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(2211, exchangeEmail.getLocalPart());
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(2212);

        DateTime start = MoscowTime.dateTime(2017, 12, 11, 18, 0);
        Duration timeShift = Duration.standardHours(2);

        DateTime pastRecurrenceStart = start.plusDays(1);
        DateTime exdateStart = start.plusDays(2);
        DateTime futureRecurrenceStart = start.plusDays(3);

        Event master = testManager.createDefaultEwsExportedEvent(organizer.getUid(), "Repeating", start);
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        testManager.createExdate(exdateStart.toInstant(), master.getId());

        Cf.list(pastRecurrenceStart, futureRecurrenceStart).forEach(ts -> {
            Event recurrence = testManager.createDefaultRecurrence(organizer.getUid(), master.getId(), ts, timeShift);

            testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);
            testManager.addUserParticipantToEvent(recurrence.getId(), attendee.getUid(), Decision.UNDECIDED, false);
        });

        testManager.updateIsEwser(organizer);
        ewsProxyWrapper.cancelMasterAndSingleMeetings(exchangeEmail, start.toInstant(), start.plusHours(1).toInstant());

        MainEventInfo main = eventInfoDbLoader.getMainEventInfoById(
                Option.empty(), master.getMainEventId(), ActionSource.EXCHANGE);

        ewsExportRoutines.exportToExchangeIfNeeded(main, ActionInfo.webTest(exdateStart));

        Function1B<ReadableInstant> occurrenceExists = ts -> ewsProxyWrapper.getOccurrencesByTimeInterval(
                exchangeEmail, new InstantInterval(ts, ts), main.getMainEvent().getExternalId()).isNotEmpty();

        Assert.isTrue(occurrenceExists.apply(start));

        Assert.isTrue(occurrenceExists.apply(pastRecurrenceStart));
        Assert.isFalse(occurrenceExists.apply(pastRecurrenceStart.plus(timeShift)));

        Assert.isFalse(occurrenceExists.apply(exdateStart));

        Assert.isFalse(occurrenceExists.apply(futureRecurrenceStart));
        Assert.isTrue(occurrenceExists.apply(futureRecurrenceStart.plus(timeShift)));
    }


    private enum TestActor {
        ORGANIZER,
        ATTENDEE,
        RESOURCE,
    }

    @Test
    public void exportEventWithUserOrganizerAndThenRemoveItInExchangeByAttendee() {
        exportEventWithUserOrganizerAndThenRemoveItInExchange(TestActor.ATTENDEE);
    }

    @Test
    public void exportEventWithUserOrganizerAndThenRemoveItInExchangeByOrganizer() {
        exportEventWithUserOrganizerAndThenRemoveItInExchange(TestActor.ORGANIZER);
    }

    @Ignore  // CAL-8212
    @Test
    public void exportEventWithUserOrganizerAndThenRemoveItInExchangeByResource() {
        exportEventWithUserOrganizerAndThenRemoveItInExchange(TestActor.RESOURCE);
    }

    private void exportEventWithUserOrganizerAndThenRemoveItInExchange(TestActor actor) {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        Email resourceEmail = ResourceRoutines.getResourceEmail(r);
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10493");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10494");

        Event event = testManager.createDefaultMeeting(organizer.getUid(),
                "exportEventWithUserOrganizerAndThenRemoveItInExchange");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);
        testManager.addResourceParticipantToEvent(event.getId(), r);
        final Option<ParticipantInfo> resourceParticipantO =
            eventInvitationManager.getParticipantByEventIdAndEmail(event.getId(), resourceEmail);
        Assert.assertTrue(resourceParticipantO.isPresent());

        ewsProxyWrapper.cancelMeetings(
                testExchangeThreeLittlePigsEmail, event.getStartTs(), event.getEndTs());

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(
                event.getId(), ActionInfo.webTest(event.getStartTs().minus(Duration.standardHours(1))));
        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();

        if (actor == TestActor.RESOURCE) {
            ewsImporter.removeEvent(
                    UidOrResourceId.resource(r.getId()), exchangeId, ActionInfo.exchangeTest());
            Assert.assertNone(genericBeanDao.findBeanById(EventHelper.INSTANCE, event.getId()));
        } else {
            PassportUid uid = actor == TestActor.ATTENDEE ? attendee.getUid() : organizer.getUid();
            ewsImporter.removeEvent(UidOrResourceId.user(uid), exchangeId, ActionInfo.exchangeTest());
            Option<Event> updatedEvent = genericBeanDao.findBeanById(EventHelper.INSTANCE, event.getId());
            Assert.assertSome(updatedEvent);
            Assert.some(Decision.NO, eventRoutines.findEventUser(uid, event.getId()).map(EventUser::getDecision));
        }
    }

    @Ignore  // CAL-8038
    @Test
    public void exportEventWithUserOrganizerAndThenUpdateItInExchange() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        Email resourceEmail = ResourceRoutines.getResourceEmail(r);
        TestUserInfo user = testManager.prepareRandomYaTeamUser(2);
        PassportUid uid = user.getUid();

        Event event = testManager.createDefaultMeeting(uid, "sample event (export with user org., then update)");
        testManager.addUserParticipantToEvent(event.getId(), user.getLogin(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);
        final Option<ParticipantInfo> resourceParticipantO =
            eventInvitationManager.getParticipantByEventIdAndEmail(event.getId(), resourceEmail);
        Assert.assertTrue(resourceParticipantO.isPresent());

        ewsProxyWrapper.cancelMeetings(
                testExchangeThreeLittlePigsEmail, event.getStartTs(), event.getEndTs());

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(
                event.getId(), ActionInfo.webTest(event.getStartTs().minus(Duration.standardHours(1))));
        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();
        CalendarItemType calItem = ewsProxyWrapper.getEvent(exchangeId).get();
        calItem.setSubject("New event name");
        ewsImporter.createOrUpdateEventForTest(UidOrResourceId.resource(r.getId()), calItem, ActionInfo.exchangeTest(), false);
        Event event1 = eventRoutines.findEventByExchangeId(exchangeId).get();
        Assert.A.equals(calItem.getSubject(), event1.getName());
        // check that main event_layer is not removed
        Option<Event> eventO =
                eventRoutines.findMasterEventBySubjectIdAndExternalId(UidOrResourceId.user(uid), calItem.getUID());
        Assert.assertTrue(eventO.isPresent());
    }

    @Test
    public void ignoreErrorForPastBusyResources() {
        createEventWithBusyResource(Duration.standardMinutes(-100));
    }

    @Test
    public void errorForFutureBusyResources() {
        try {
            createEventWithBusyResource(Duration.standardMinutes(10));
            Assert.fail();
        } catch (Exception ok) {}
    }

    protected void createEventWithBusyResource(Duration nowToEventStart) {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        PassportLogin user = new PassportLogin("yandex-team-mm-10494");
        PassportUid uid = userManager.getUidByLoginForTest(user);

        testManager.cleanUser(uid);

        Event event = testManager.createDefaultMeeting(uid, "sample event (organizer export)");
        testManager.addUserParticipantToEvent(event.getId(), user, Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        Instant now = event.getStartTs().minus(nowToEventStart);

        ewsProxyWrapper.cancelMeetings(
                testExchangeThreeLittlePigsEmail, event.getStartTs(), event.getEndTs());

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest(now));

        Event event2 = testManager.createDefaultMeeting(uid, "sample event 2");
        testManager.addUserParticipantToEvent(event2.getId(), uid, Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event2.getId(), r);

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event2.getId(), ActionInfo.webTest(now));
    }

    @Test
    public void tryToExportEventTwice() {
        PassportLogin user = new PassportLogin("yandex-team-mm-10495");
        Resource r = testManager.cleanAndCreateSmolny();
        PassportUid uid = userManager.getUidByLoginForTest(user);

        Event event = testManager.createDefaultEvent(uid, "name");
        testManager.addUserParticipantToEvent(event.getId(), uid, Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        ewsProxyWrapper.cancelMeetings(
                testExchangeSmolnyEmail, event.getStartTs(), event.getEndTs());

        Instant now = event.getStartTs().minus(10000);
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest(now));
        String exchangeId1 = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest(now));
        String exchangeId2 = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();
        Assert.A.equals(exchangeId1, exchangeId2);
        Assert.assertTrue(ewsProxyWrapper.getEvent(exchangeId1).isPresent());
    }

    @Test
    public void tryToExportEventAlreadyExistedInExchange() throws DatatypeConfigurationException {
        PassportLogin user = new PassportLogin("yandex-team-mm-10496");
        Resource r = testManager.cleanAndCreateSmolny();
        PassportUid uid = userManager.getUidByLoginForTest(user);

        Event event = testManager.createDefaultEvent(uid, "name");
        testManager.addUserParticipantToEvent(event.getId(), uid, Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        ewsProxyWrapper.cancelMeetings(
                testExchangeSmolnyEmail, event.getStartTs(), event.getEndTs());

        CalendarItemType calItem = TestCalItemFactory.createDefaultCalendarItemForExport(
                event.getStartTs().toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE), event.getName());
        calItem.setUID(eventDao.findExternalIdByEventId(event.getId()));
        String exchangeId1 = ewsProxyWrapper.createEvent(resourceRoutines.getExchangeEmail(r), calItem, EwsActionLogData.test());

        Instant now = event.getStartTs().minus(10000);
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest(now));
        String exchangeId2 = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();

        Assert.A.equals(exchangeId1, exchangeId2);
        Assert.assertTrue(ewsProxyWrapper.getEvent(exchangeId1).isPresent());
    }

    // https://jira.yandex-team.ru/browse/CAL-3270
    @Test
    public void startAndEndDateInRepeatedEventMoscow() {
        startAndEndDateInRepeatedEvent(TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalTime(0, 0));
        startAndEndDateInRepeatedEvent(TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalTime(2, 0));
        startAndEndDateInRepeatedEvent(TimeUtils.EUROPE_MOSCOW_TIME_ZONE, new LocalTime(23, 0));
    }

    @Test
    public void startAndEndDateInRepeatedEventUtc() {
        startAndEndDateInRepeatedEvent(DateTimeZone.UTC, new LocalTime(0, 0));
        startAndEndDateInRepeatedEvent(DateTimeZone.UTC, new LocalTime(2, 0));
        startAndEndDateInRepeatedEvent(DateTimeZone.UTC, new LocalTime(23, 0));
    }

    @Test
    public void startAndEndDateInRepeatedEventMinsk() {
        startAndEndDateInRepeatedEvent(DateTimeZone.forID("Europe/Minsk"), new LocalTime(0, 0));
        startAndEndDateInRepeatedEvent(DateTimeZone.forID("Europe/Minsk"), new LocalTime(2, 0));
        startAndEndDateInRepeatedEvent(DateTimeZone.forID("Europe/Minsk"), new LocalTime(23, 0));
    }

    private void startAndEndDateInRepeatedEvent(DateTimeZone tz, LocalTime startTime) {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10497");

        Instant startTs = new LocalDate(2011, 6, 1).toDateTime(startTime, tz).toInstant();
        Event event = testManager.createDefaultEvent(
                user.getUid(), "exportNewCreatedEventWithRepetition0200MsdDue",
                startTs, startTs.plus(Duration.standardMinutes(30)));
        long repetitionId = testManager.createWeeklyRepetition(DayOfWeek.WEDNESDAY);
        Instant exclusiveDueTs = TestDateTimes.plusDays(event.getStartTs(), 14 +1);
        eventDao.updateRepetitionDueTs(repetitionId, exclusiveDueTs);
        testManager.linkRepetitionToEvent(event.getId(), repetitionId);

        testManager.addUserParticipantToEvent(event.getId(), user.getLogin(), Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        ewsProxyWrapper.cancelMeetings(
                testExchangeThreeLittlePigsEmail,
                startTs, exclusiveDueTs);

        testManager.updateEventTimezone(event.getId(), tz);

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(
                event.getId(), ActionInfo.webTest(event.getStartTs().minus(Duration.standardHours(1))));
        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();
        CalendarItemType createdItem = ewsProxyWrapper.getEvent(exchangeId).get();

        Assert.some(tz, WindowsTimeZones.getZoneByWinName(createdItem.getTimeZone()));
        Assert.A.equals(startTs, EwsUtils.xmlGregorianCalendarInstantToInstant(createdItem.getStart()));
        Assert.A.equals(new LocalDate(2011, 6, 1),
                EwsUtils.xmlGregorianCalendarLocalDateToLocalDate(createdItem.getRecurrence().getEndDateRecurrence().getStartDate()));
        Assert.A.equals(new LocalDate(2011, 6, 15),
                EwsUtils.xmlGregorianCalendarLocalDateToLocalDate(createdItem.getRecurrence().getEndDateRecurrence().getEndDate()));
    }

    // https://jira.yandex-team.ru/browse/CAL-3256
    @Test
    public void exportRepeatingMeetingWithRecurrenceId() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10498");

        DateTime eventStart = TestDateTimes.moscowDateTime(2011, 6, 3,  10, 0);
        Tuple2<ListF<Event>, Repetition> t = testManager.createMeetingWithRepetitionDueAndRecurrenceIdO(
                user.getUid(), "exportRecurrenceId", eventStart,
                RegularRepetitionRule.WEEKLY, 3, Cf.list(Either.right(r)),
                Option.of(Tuple2.tuple(new LocalDate(2011, 6, 10), Duration.standardHours(-2))));

        ewsProxyWrapper.cancelMeetings(
                testExchangeThreeLittlePigsEmail,
                eventStart.toInstant(), eventStart.plusDays(14).plusHours(1).toInstant());

        String externalId = mainEventDao.findExternalIdByMainEventId(t.get1().get(0).getMainEventId());
        long masterEventId = t.get1().get(0).getId();
        long recurrenceEventId = t.get1().get(1).getId();
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(
                masterEventId, ActionInfo.webTest(eventStart.toInstant().minus(Duration.standardHours(1))));
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(
                recurrenceEventId, ActionInfo.webTest(eventStart.toInstant().minus(Duration.standardHours(1))));

        Assert.A.equals(
            Cf.list(CalendarItemTypeType.RECURRING_MASTER, CalendarItemTypeType.EXCEPTION),
            Cf.list(masterEventId, recurrenceEventId).map(new Function<Long, CalendarItemTypeType>() {
                public CalendarItemTypeType apply(Long a) {
                    String exchangeId = eventResourceDao.findExchangeIds(Cf.list(a)).single();
                    CalendarItemType createdItem = ewsProxyWrapper.getEvent(exchangeId).get();
                    return createdItem.getCalendarItemType();
                }
            })
        );

        DateTime jun10 = TestDateTimes.moscowDateTime(2011, 6, 10,  0, 0);
        Assert.A.hasSize(1, ewsProxyWrapper.getOccurrencesByTimeInterval(
                testExchangeThreeLittlePigsEmail,
                new InstantInterval(jun10.toInstant(), jun10.plusDays(1).toInstant()),
                externalId));
    }

    @Test
    public void exportFixedTimezone() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10499");

        DateTime eventStart = TestDateTimes.moscowDateTime(2017, 5, 19,  18, 0);
        Event event = testManager.createDefaultEvent(user.getUid(), "Fixed timezone", eventStart);

        testManager.addResourceParticipantToEvent(event.getId(), r);
        testManager.updateEventTimezone(event.getId(), DateTimeZone.forOffsetHoursMinutes(3, 15));

        ewsProxyWrapper.cancelMeetings(
                testExchangeThreeLittlePigsEmail,
                eventStart.toInstant(), eventStart.plusDays(1).toInstant());

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(
                event.getId(), ActionInfo.webTest(eventStart.toInstant().minus(Duration.standardHours(1))));

        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();
        Assert.equals(event.getStartTs(), EwsUtils.xmlGregorianCalendarInstantToInstant(
                ewsProxyWrapper.getEvent(exchangeId).get().getStart()));
    }

    @Test
    public void exportBirthday() {
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();
        TestUserInfo user = testManager.prepareRandomYaTeamUser(6456456);

        DateTime eventStart = TestDateTimes.moscowDateTime(1973, 6, 26, 0, 0);
        DateTime nowDaysStart = eventStart.withYear(2018);

        Event eventData = new Event();
        eventData.setStartTs(eventStart.toInstant());
        eventData.setEndTs(eventStart.plusDays(1).toInstant());
        eventData.setIsAllDay(true);

        Repetition repetitionData = new Repetition();
        repetitionData.setType(RegularRepetitionRule.YEARLY);
        repetitionData.setREach(1);

        Event event = testManager.createDefaultEvent(user.getUid(), "May's birthday", eventData);
        testManager.linkRepetitionToEvent(event.getId(), eventDao.saveRepetition(repetitionData));
        testManager.addResourceParticipantToEvent(event.getId(), resource);

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                testExchangeThreeLittlePigsEmail,
                eventStart.minusDays(1).toInstant(), eventStart.plusDays(2).toInstant());

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest());

        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();

        CalendarItemType master = ewsProxyWrapper.getEvent(exchangeId).get();
        CalendarItemType occurrence = ewsProxy.getEvents(Cf.list(EwsUtils.createOccurrenceItemIdType(
                exchangeId, nowDaysStart.getYear() - eventStart.getYear() + 1)), false).single();

        Function<CalendarItemType, InstantInterval> convertInterval = item -> EventRoutines.getInstantInterval(
                ExchangeEventDataConverter.convert(
                        item, UidOrResourceId.resource(resource.getId()),
                        Option.empty(), resourceRoutines::selectResourceEmails).getEvent());

        Assert.equals(new InstantInterval(nowDaysStart, Duration.standardDays(1)), convertInterval.apply(occurrence));
        Assert.equals(new InstantInterval(eventStart, Duration.standardDays(1)), convertInterval.apply(master));
    }

    @Test
    public void eachSeventhDayRepetition() {
        eachNthDayRepetition(new LocalTime(0, 0), 7);
        eachNthDayRepetition(new LocalTime(1, 0), 7);
        eachNthDayRepetition(new LocalTime(12, 0), 7);
        eachNthDayRepetition(new LocalTime(23, 0), 7);
    }

    @Test
    public void eachThirdDayRepetition() {
        eachNthDayRepetition(new LocalTime(0, 0), 3);
//        eachNthDayRepetition(new LocalTime(1, 0), 3); // times out
//        eachNthDayRepetition(new LocalTime(12, 0), 3);
        eachNthDayRepetition(new LocalTime(23, 0), 3);
    }

    @Test
    public void eachDayRepetition() {
        eachNthDayRepetition(new LocalTime(0, 0), 1);
//        eachNthDayRepetition(new LocalTime(1, 0), 1); // times out
//        eachNthDayRepetition(new LocalTime(12, 0), 1);
        eachNthDayRepetition(new LocalTime(23, 0), 1);
    }

    // https://jira.yandex-team.ru/browse/CAL-4219
    private void eachNthDayRepetition(LocalTime time, int repeatIntervalDays) {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10499");

        DateTimeZone tz = TimeUtils.EUROPE_MOSCOW_TIME_ZONE;

        DateTime start = new LocalDate(2011, 10, 17).toDateTime(time, tz);
        DateTime due = start.plusDays(repeatIntervalDays * 2 + 5).withTimeAtStartOfDay();

        EventData eventData = testManager.createDefaultEventData(user.getUid(), "eachSeventhDayRepetition");
        eventData.getEvent().setStartTs(start.toInstant());
        eventData.getEvent().setEndTs(start.plusHours(1).toInstant());
        Repetition repetition = new Repetition();
        repetition.setType(RegularRepetitionRule.DAILY);
        repetition.setREach(repeatIntervalDays);
        repetition.setDueTs(due.toInstant());
        eventData.setRepetition(repetition);
        ParticipantData userParticipantData =
                new ParticipantData(user.getEmail(), user.getLogin().getRawValue(), Decision.YES, true, true, false);
        ParticipantData resourceParticipantData =
                new ParticipantData(resourceRoutines.getExchangeEmail(r), "resource", Decision.YES, true, false, false);
        eventData.setInvData(ParticipantsData.merge(
                userParticipantData, Cf.list(resourceParticipantData)));

        ewsProxyWrapper.cancelMeetings(
                testExchangeThreeLittlePigsEmail, start.toInstant(), due.toInstant());

        CreateInfo event = eventRoutines.createUserOrFeedEvent(UidOrResourceId.user(user.getUid()), EventType.USER,
                eventRoutines.createMainEvent(user.getUid(), eventData, ActionInfo.webTest(start.toInstant())),
                eventData, NotificationsData.useLayerDefaultIfCreate(),
                InvitationProcessingMode.SAVE_ONLY, ActionInfo.webTest(start.toInstant()));

        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getEvent().getId())).single();
        CalendarItemType createdItem = ewsProxyWrapper.getEvent(exchangeId).get();
        EventData inExchange = ExchangeEventDataConverter.convert(
                createdItem, UidOrResourceId.resource(r.getId()),
                Option.empty(), resourceRoutines::selectResourceEmails);
        final Repetition actualRepetition = inExchange.getRepetition();

        Assert.equals(due, actualRepetition.getDueTs().get().toDateTime(tz).withHourOfDay(0).withMinuteOfHour(0));
        Assert.equals(repeatIntervalDays, actualRepetition.getREach().get());
        Assert.equals(RegularRepetitionRule.DAILY, actualRepetition.getType());

        Assert.A.equals(start.toInstant(),
                EwsUtils.xmlGregorianCalendarInstantToInstant(ewsProxyWrapper.getOccurrenceByIndex(exchangeId, 1).get().getStart()));
        Assert.A.equals(start.plusDays(repeatIntervalDays).toInstant(),
                EwsUtils.xmlGregorianCalendarInstantToInstant(ewsProxyWrapper.getOccurrenceByIndex(exchangeId, 2).get().getStart()));
        Assert.A.equals(start.plusDays(repeatIntervalDays * 2 ).toInstant(),
                EwsUtils.xmlGregorianCalendarInstantToInstant(ewsProxyWrapper.getOccurrenceByIndex(exchangeId, 3).get().getStart()));
    }

    @Test
    public void correctRecurrenceStart() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-14444");

        Event event = testManager.createDefaultEvent(user.getUid(), "removeSingleInstanceAndFuture");
        Instant dueTs = TestDateTimes.plusDays(event.getStartTs(), 3);
        long repetitionId = testManager.createDailyRepetitionWithDueTsAndLinkToEvent(event.getId(), dueTs);
        testManager.addUserParticipantToEvent(event.getId(), user.getLogin(), Decision.UNDECIDED, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);

        InstantInterval interval = new InstantInterval(event.getStartTs().minus(Duration.standardDays(1)), dueTs);
        ewsProxyWrapper.cancelMeetings(
                testExchangeThreeLittlePigsEmail, interval.getStart(), interval.getEnd());

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest());
        ResourceParticipantInfo resourceParticipant =
            resourceDao.findResourceParticipants(Cf.list(event.getId())).single();
        Assert.A.isTrue(resourceParticipant.getExchangeId().isPresent());

        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();
        CalendarItemType exchangeEvent = ewsProxyWrapper.getEvent(exchangeId).get();

        Instant expectedRecurrenceStart = new DateTime(
                event.getStartTs(),
                dateTimeManager.getTimeZoneForUid(user.getUid())).withTimeAtStartOfDay().toInstant();

        LocalDate exchangeStartDate = EwsUtils.xmlGregorianCalendarLocalDateToLocalDate(exchangeEvent.getRecurrence().getEndDateRecurrence().getStartDate());
        DateTimeZone exchangeTz = EwsUtils.getOrDefaultZone(exchangeEvent);

        Assert.A.equals(expectedRecurrenceStart, exchangeStartDate.toDateTimeAtStartOfDay(exchangeTz).toInstant());
    }

    private String createMeetingInExchange(Resource resource, String subject, Instant start, Instant end)
            throws Exception
    {
        CalendarItemType calItem = TestCalItemFactory.createDefaultCalendarItemForExport(
                start.toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE), subject);
        calItem.setEnd(EwsUtils.instantToXMLGregorianCalendar(end, TimeUtils.EUROPE_MOSCOW_TIME_ZONE));

        return ewsProxyWrapper.createEvent(resourceRoutines.getExchangeEmail(resource), calItem, EwsActionLogData.test());
    }

    @Test
    public void conflictingItem() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(9999);
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();
        Email resourceEmail = resourceRoutines.getExchangeEmail(resource);

        Instant conflictingStart = MoscowTime.instant(2014, 7, 9, 21, 0);
        Instant conflictingDue = conflictingStart.plus(Duration.standardDays(4));

        ewsProxyWrapper.cancelMeetings(testExchangeThreeLittlePigsEmail, conflictingStart, conflictingDue);

        Event conflictingMaster = testManager.createDefaultEvent(user.getUid(), "Conflicting event", conflictingStart);
        addUserAndResourceToEvent(conflictingMaster, user, resource);

        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(conflictingMaster.getId(), conflictingDue);

        Event conflictingRecurrence = testManager.createDefaultRecurrence(
                user.getUid(), conflictingMaster.getId(), conflictingStart.plus(Duration.standardDays(1)));
        addUserAndResourceToEvent(conflictingRecurrence, user, resource);

        Event conflictingSingle = testManager.createDefaultEvent(
                user.getUid(), "Conflicting single", conflictingRecurrence.getStartTs().plus(Duration.standardHours(9)));
        addUserAndResourceToEvent(conflictingSingle, user, resource);

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(conflictingMaster.getId(), ActionInfo.webTest());
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(conflictingRecurrence.getId(), ActionInfo.webTest());
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(conflictingSingle.getId(), ActionInfo.webTest());


        Event meeting = testManager.createDefaultEvent(user.getUid(), "Occurrence overlap", conflictingMaster.getStartTs());
        addUserAndResourceToEvent(meeting, user, resource);
        ConflictingItem conflict = exportMeetingGetConflict(resourceEmail, meeting, ActionInfo.webTest(conflictingDue));

        Assert.equals(CalendarItemTypeType.OCCURRENCE, conflict.getType());
        Assert.none(conflict.getRecurrenceId());
        Assert.equals(conflictingMaster.getStartTs(), conflict.getStart());
        Assert.equals(conflictingMaster.getEndTs(), conflict.getEnd());
        Assert.equals(conflictingMaster.getName(), conflict.getName());

        meeting = testManager.createDefaultEvent(user.getUid(), "Recurrence overlap", conflictingRecurrence.getStartTs());
        addUserAndResourceToEvent(meeting, user, resource);
        conflict = exportMeetingGetConflict(resourceEmail, meeting, ActionInfo.webTest(conflictingDue));

        Assert.equals(CalendarItemTypeType.EXCEPTION, conflict.getType());
        Assert.some(conflictingRecurrence.getStartTs(), conflict.getRecurrenceId());
        Assert.equals(conflictingRecurrence.getStartTs(), conflict.getStart());
        Assert.equals(conflictingRecurrence.getEndTs(), conflict.getEnd());
        Assert.equals(conflictingRecurrence.getName(), conflict.getName());

        meeting = testManager.createDefaultEvent(user.getUid(), "Single overlap", conflictingSingle.getStartTs());
        addUserAndResourceToEvent(meeting, user, resource);
        conflict = exportMeetingGetConflict(resourceEmail, meeting, ActionInfo.webTest(conflictingDue));

        Assert.equals(CalendarItemTypeType.SINGLE, conflict.getType());
        Assert.none(conflict.getRecurrenceId());
        Assert.equals(conflictingSingle.getStartTs(), conflict.getStart());
        Assert.equals(conflictingSingle.getEndTs(), conflict.getEnd());
        Assert.equals(conflictingSingle.getName(), conflict.getName());
    }

    public ConflictingItem exportMeetingGetConflict(Email email, Event meeting, ActionInfo actionInfo) {
        RepetitionInstanceInfo repetitionInfo = repetitionRoutines.getRepetitionInstanceInfoByEvent(meeting);
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(meeting.getId(), actionInfo);
        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(meeting.getId())).single();
        return ewsExportRoutines.getItemWithActualConflicts(email, exchangeId, repetitionInfo, meeting.getStartTs())
                .get().getConflictingItems().single();
    }

    public void addUserAndResourceToEvent(Event event, TestUserInfo user, Resource resource) {
        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), resource);
    }

    @Test
    public void weeklyRecurrenceStart() {
        PassportUid u = testManager.prepareUser("yandex-team-mm-14461").getUid();
        Resource r = testManager.cleanAndCreateThreeLittlePigs();

        SetF<DayOfWeek> set = Cf.set(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY);
        exportAndCheckRecurrenceStart(u, r, new LocalTime(12, 0), DayOfWeek.TUESDAY, set);
        exportAndCheckRecurrenceStart(u, r, new LocalTime(12, 0), DayOfWeek.THURSDAY, set);
        exportAndCheckRecurrenceStart(u, r, new LocalTime(12, 0), DayOfWeek.SATURDAY, set);

        set = Cf.set(DayOfWeek.values());
        exportAndCheckRecurrenceStart(u, r, new LocalTime(12, 0), DayOfWeek.SUNDAY, set);
        exportAndCheckRecurrenceStart(u, r, new LocalTime(12, 0), DayOfWeek.MONDAY, set);

        set = Cf.set(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY);
        exportAndCheckRecurrenceStart(u, r, new LocalTime(0, 15), DayOfWeek.WEDNESDAY, set);
        exportAndCheckRecurrenceStart(u, r, new LocalTime(23, 45), DayOfWeek.WEDNESDAY, set);
        exportAndCheckRecurrenceStart(u, r, new LocalTime(23, 45), DayOfWeek.THURSDAY, set);
        exportAndCheckRecurrenceStart(u, r, new LocalTime(0, 15), DayOfWeek.THURSDAY, set);
    }

    private void exportAndCheckRecurrenceStart(
            PassportUid creator, Resource resource,
            LocalTime startTime, DayOfWeek startDay, SetF<DayOfWeek> repeatDays)
    {
        LocalDate date = new LocalDate(2012, 8, 1);

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                testExchangeThreeLittlePigsEmail,
                date.withDayOfWeek(1).toDateTimeAtStartOfDay(MoscowTime.TZ).toInstant(),
                date.withDayOfWeek(7).plusDays(1).toDateTimeAtStartOfDay(MoscowTime.TZ).toInstant());

        Instant start = date.withDayOfWeek(startDay.getJoda()).toDateTime(startTime, MoscowTime.TZ).toInstant();
        Instant end = start.plus(Duration.standardHours(1));

        Event event = testManager.createDefaultEvent(creator, "Weekly repeating meeting", start, end);
        testManager.addUserParticipantToEvent(event.getId(), creator, Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), resource);

        Repetition repetition = new Repetition();
        repetition.setREach(1);
        repetition.setDueTs(start.plus(Duration.standardDays(22)));
        repetition.setType(RegularRepetitionRule.WEEKLY);
        repetition.setRWeeklyDays(repeatDays.map(DayOfWeek.getDbValueF()).mkString(","));
        testManager.linkRepetitionToEvent(event.getId(), genericBeanDao.insertBeanGetGeneratedKey(repetition));

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest());

        ResourceParticipantInfo participant = resourceDao.findResourceLayersWithEvent(event.getId()).single();
        String exchangeId = participant.getExchangeId().get();

        Option<CalendarItemType> calItemO = ewsProxyWrapper.getEvent(exchangeId);
        Assert.some(calItemO);
        Assert.notNull(calItemO.get().getRecurrence());
        Assert.notNull(calItemO.get().getRecurrence().getEndDateRecurrence());

        EndDateRecurrenceRangeType recurrence = calItemO.get().getRecurrence().getEndDateRecurrence();
        LocalDate repetitionStart = EwsUtils.xmlGregorianCalendarLocalDateToLocalDate(recurrence.getStartDate());

        Assert.notNull(calItemO.get().getFirstOccurrence());
        Assert.some(start, EwsUtils.toInstantO(calItemO.get().getFirstOccurrence().getStart()));
        Assert.some(start, EwsUtils.toInstantO(calItemO.get().getFirstOccurrence().getOriginalStart()));

        Assert.equals(repetitionStart, start.toDateTime(MoscowTime.TZ).toLocalDate());
    }

    // CAL-6152
    @Test
    public void expandToDay() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(14451);
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();
        Email resourceEmail = testExchangeThreeLittlePigsEmail;

        Event event = testManager.createDefaultEvent(user.getUid(), "expandToDay");
        testManager.addResourceParticipantToEvent(event.getId(), resource);

        DateTimeZone eventTz = eventRoutines.getEventTimeZone(event.getId());
        InstantInterval eventInterval = EventRoutines.getInstantInterval(event);

        ewsProxyWrapper.cancelMasterAndSingleMeetings(resourceEmail, eventInterval
                .withStart(eventInterval.getStart().minus(Duration.standardDays(2)))
                .withEnd(eventInterval.getEnd().plus(Duration.standardDays(2))));

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest());

        InstantInterval dayInterval = AuxDateTime.expandToDaysInterval(eventInterval, eventTz);
        EventChangesInfo changes = EventChangesInfo.EMPTY;

        changes.getEventChanges().setStartTs(dayInterval.getStart());
        changes.getEventChanges().setEndTs(dayInterval.getEnd());
        changes.getEventChanges().setIsAllDay(true);

        ewsExportRoutines.exportToExchangeIfNeededOnUpdate(
                event.getId(), changes, Option.empty(), ActionInfo.webTest());

        String exchangeId = eventResourceDao.findExchangeIds(Cf.list(event.getId())).single();
        CalendarItemType item = ewsProxyWrapper.getEvent(exchangeId).get();

        Assert.equals(dayInterval.getStart(), EwsUtils.xmlGregorianCalendarInstantToInstant(item.getStart()));
        Assert.equals(dayInterval.getEnd(), EwsUtils.xmlGregorianCalendarInstantToInstant(item.getEnd()));
        Assert.isTrue(item.isIsAllDayEvent());
    }

    @Test
    public void addResourcesWithTask() {
        val initial = List.of(testExchangeThreeLittlePigsEmail);
        val added = List.of(testExchangeSmolnyEmail);
        val removed = Collections.<Email>emptyList();
        assertThat(testExportResource(initial, added, removed)).containsExactlyInAnyOrder(testExchangeThreeLittlePigsEmail, testExchangeSmolnyEmail);
    }

    @Test
    public void removeResourcesWithTask() {
        val initial = List.of(testExchangeThreeLittlePigsEmail, testExchangeSmolnyEmail);
        val added = Collections.<Email>emptyList();
        val removed = List.of(testExchangeSmolnyEmail);
        assertThat(testExportResource(initial, added, removed)).containsExactlyInAnyOrder(testExchangeThreeLittlePigsEmail);
    }

    @Test
    public void removeAndAddResourcesWithTask() {
        val initial = List.of(testExchangeThreeLittlePigsEmail, testExchangeSmolnyEmail);
        val added = List.of(testExchangeConfRr21);
        val removed = List.of(testExchangeSmolnyEmail);
        assertThat(testExportResource(initial, added, removed)).containsExactlyInAnyOrder(testExchangeThreeLittlePigsEmail, testExchangeConfRr21);
    }

    private Set<Email> testExportResource(List<Email> initialMails, List<Email> addedMails, List<Email> removedMails) {
        val user = testManager.prepareRandomYaTeamUser(14451);
        val allResources = Map.of(
                testExchangeSmolnyEmail, testManager.cleanAndCreateSmolny(),
                testExchangeThreeLittlePigsEmail, testManager.cleanAndCreateThreeLittlePigs(),
                testExchangeConfRr21, testManager.cleanAndCreateConfRr21());

        val event = testManager.createDefaultEvent(user.getUid(), "expandToDay");
        testManager.addUserParticipantToEvent(event.getId(), user, Decision.YES, true);
        initialMails.forEach(email ->
                testManager.addResourceParticipantToEvent(event.getId(), allResources.get(email)));

        val eventInterval = EventRoutines.getInstantInterval(event);

        ewsProxyWrapper.cancelMasterAndSingleMeetings(testExchangeThreeLittlePigsEmail, eventInterval
                .withStart(eventInterval.getStart().minus(Duration.standardDays(2)))
                .withEnd(eventInterval.getEnd().plus(Duration.standardDays(2))));
        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest());

        val changes = new EventChangesInfoForExchange(new Event(), new Repetition(), true, Cf.list());
        val newResources = constructResourceInfos(allResources, addedMails, event.getId());
        val removedResources = constructResourceInfos(allResources, removedMails, event.getId());
        val notChangedMails = StreamEx.of(initialMails)
                .remove(removedMails::contains)
                .toImmutableList();
        val notChangedResources = constructResourceInfos(allResources, notChangedMails, event.getId());

        addedMails.forEach(email ->
                testManager.addResourceParticipantToEvent(event.getId(), allResources.get(email)));
        removedMails.forEach(email ->
                eventResourceDao.deleteEventResourceByResourceId(Cf.list(allResources.get(email).getId())));

        val resourcesShort = new ResourceParticipantChangesInfo(newResources, removedResources, notChangedResources);
        ewsExportRoutines.exportToExchangeIfNeededOnUpdate(changes, Optional.empty(), event.getId(), resourcesShort, ActionInfo.webTest());

        val exchangeIds = eventResourceDao.findExchangeIds(Cf.list(event.getId()));
        final var items = ewsProxyWrapper.getEvents(exchangeIds);

        for (CalendarItemType item : items) {
            assertThat(EwsUtils.xmlGregorianCalendarInstantToInstant(item.getStart())).isEqualTo(eventInterval.getStart());
            assertThat(EwsUtils.xmlGregorianCalendarInstantToInstant(item.getEnd())).isEqualTo(eventInterval.getEnd());
        }

        return StreamEx.of(items)
                .map(CalendarItemType::getResources)
                .nonNull()
                .flatMap(r -> r.getAttendee().stream())
                .map(AttendeeType::getMailbox)
                .map(EmailAddressType::getEmailAddress)
                .map(Email::new)
                .toImmutableSet();
    }

    private ResourceParticipantBriefInfo constructResourceInfo(Resource resource, Email resourceEmail, long eventId) {
        val exchangeEmail = resourceRoutines.getExchangeEmail(resource).getEmail();
        return new ResourceParticipantBriefInfo(
                exchangeEmail, resourceEmail.getEmail(), resource.getId(), eventId, true, Optional.empty(), true, true);
    }

    private List<ResourceParticipantBriefInfo> constructResourceInfos(Map<Email, Resource> resources, List<Email> resourceEmails, long eventId) {
        return StreamEx.of(resourceEmails)
                .map(email -> constructResourceInfo(resources.get(email), email, eventId))
                .toImmutableList();
    }

    // CAL-6288
    @Test
    public void onceOccurredWeeklyMeeting() {
        PassportUid creator = testManager.prepareUser("yandex-team-mm-14462").getUid();
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        LocalDate monday = new LocalDate(2013, 7, 22);
        LocalDate tuesday = monday.plusDays(1);
        LocalTime startTime = new LocalTime(16, 0);

        Instant mondayStart = monday.toDateTime(startTime, MoscowTime.TZ).toInstant();
        Instant tuesdayStart = tuesday.toDateTime(startTime, MoscowTime.TZ).toInstant();
        Instant end = mondayStart.plus(Duration.standardHours(1));

        ewsProxyWrapper.cancelMasterAndSingleMeetings(testExchangeThreeLittlePigsEmail, mondayStart, end);

        Event event = testManager.createDefaultEvent(creator, "Once occurred weekly meeting", mondayStart, end);
        testManager.addUserParticipantToEvent(event.getId(), creator, Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), resource);

        Repetition repetition = new Repetition();
        repetition.setREach(1);
        repetition.setDueTs(tuesdayStart);
        repetition.setType(RegularRepetitionRule.WEEKLY);
        repetition.setRWeeklyDays("mon,tue");
        testManager.linkRepetitionToEvent(event.getId(), genericBeanDao.insertBeanGetGeneratedKey(repetition));

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), ActionInfo.webTest());

        Assert.some(ewsProxyWrapper.getOccurrenceByIndex(getExchangeId(event.getId()), 1));
        Assert.none(ewsProxyWrapper.getOccurrenceByIndex(getExchangeId(event.getId()), 2));
    }

    @Test
    public void exportExdates() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14471").getUid();
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        DateTime start = MoscowTime.dateTime(2012, 10, 18, 9, 0);
        DateTime end = start.plusHours(1);

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                testExchangeThreeLittlePigsEmail, start.toInstant(), start.plusDays(7).toInstant());

        long eventId = testManager.createDefaultEvent(uid, "changeExdates", start.toInstant(), end.toInstant()).getId();
        testManager.addUserParticipantToEvent(eventId, uid, Decision.YES, true);
        testManager.addResourceParticipantToEvent(eventId, resource);
        testManager.createDailyRepetitionWithDueTsAndLinkToEvent(eventId, start.plusDays(7).toInstant());

        ListF<Instant> expected = Cf.list(start.plusDays(3), start.plusDays(6)).map(ReadableInstant::toInstant);
        repetitionRoutines.createRdates(expected.map(instantToExdateF(eventId)), ActionInfo.webTest());
        CalendarItemType exportedItem = exportExdates(eventId, expected);
        Assert.equals(expected.unique(), getDeletedOccurrencesStarts(exportedItem).unique());

        ListF<Instant> added = Cf.list(start.plusDays(4), start.plusDays(5)).map(ReadableInstant::toInstant);
        expected = expected.plus(added);
        repetitionRoutines.createRdates(added.map(instantToExdateF(eventId)), ActionInfo.webTest());
        Assert.equals(expected.unique(), getDeletedOccurrencesStarts(exportExdates(eventId, added)).unique());

        exportedItem = exportExdates(eventId, Cf.list());
        Assert.equals(expected.unique(), getDeletedOccurrencesStarts(exportedItem).unique());

        exportedItem = exportExdates(eventId, expected.plus(added));
        Assert.sizeIs(expected.size(), getDeletedOccurrencesStarts(exportedItem));
    }

    @Test
    public void exportExdatesOfEventWithDaylightSavingTimezone() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14473").getUid();
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        DateTimeZone tz = DateTimeZone.forID("Europe/Kiev");
        DateTime start = new DateTime(2013, 1, 1, 10, 0, tz);
        DateTime end = start.plusHours(1);

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                testExchangeThreeLittlePigsEmail, start.toInstant(), end.toInstant());

        long eventId = testManager.createDefaultEvent(uid, "exportExdates", start.toInstant(), end.toInstant()).getId();

        testManager.addUserParticipantToEvent(eventId, uid, Decision.YES, true);
        testManager.addResourceParticipantToEvent(eventId, resource);

        testManager.updateEventTimezone(eventId, tz);
        testManager.linkRepetitionToEvent(eventId, testManager.createWeeklyRepetition(DayOfWeek.TUESDAY));

        ListF<Instant> expected = Cf.list(start.plusWeeks(25), start.plusWeeks(50)).map(ReadableInstant::toInstant);
        repetitionRoutines.createRdates(expected.map(instantToExdateF(eventId)), ActionInfo.webTest());

        CalendarItemType exportedItem = exportExdates(eventId, expected);
        Assert.equals(expected.unique(), getDeletedOccurrencesStarts(exportedItem).unique());
    }

    private CalendarItemType exportExdates(long eventId, ListF<Instant> newExdates) {
        ListF<Rdate> exdates = newExdates.map(RepetitionUtils::consExdate);

        EventChangesInfo.EventChangesInfoFactory factory = new EventChangesInfo.EventChangesInfoFactory();
        factory.setRdateChangesInfo(new RdateChangesInfo(exdates, Cf.set()));

        ewsExportRoutines.exportToExchangeIfNeededOnUpdate(
                eventId, factory.create(), Option.empty(), ActionInfo.webTest());

        String exchangeId = resourceDao.findResourceLayersWithEvent(eventId).single().getExchangeId().get();
        return ewsProxyWrapper.getEvent(exchangeId).get();
    }

    private String getExchangeId(long eventId) {
        return eventResourceDao.findExchangeIds(Cf.list(eventId)).single();
    }

    private static ListF<Instant> getDeletedOccurrencesStarts(CalendarItemType item) {
        NonEmptyArrayOfDeletedOccurrencesType array = item.getDeletedOccurrences();
        return array != null && !array.getDeletedOccurrence().isEmpty()
                ? Cf.x(array.getDeletedOccurrence()).map(deletedOccurrenceToInstantF())
                : Cf.list();
    }

    private static Function<Instant, Rdate> instantToExdateF(final long eventId) {
        return Cf2.f2(RepetitionUtils::consExdateEventId).bind1(eventId);
    }

    private static Function<DeletedOccurrenceInfoType, Instant> deletedOccurrenceToInstantF() {
        return o -> EwsUtils.xmlGregorianCalendarInstantToInstant(o.getStart());
    }
}
