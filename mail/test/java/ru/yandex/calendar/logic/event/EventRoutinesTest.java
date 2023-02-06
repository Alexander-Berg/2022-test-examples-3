package ru.yandex.calendar.logic.event;


import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Try;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function1B;
import ru.yandex.bolts.function.Function2;
import ru.yandex.calendar.frontend.ews.ExchangeData;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.frontend.web.cmd.run.CommandRunException;
import ru.yandex.calendar.frontend.web.cmd.run.PermissionDeniedUserException;
import ru.yandex.calendar.frontend.web.cmd.run.Situation;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.avail.AvailabilityOverlap;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationUpdateData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.event.web.EventWebManager;
import ru.yandex.calendar.logic.event.web.EventWebUpdater;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.layer.LayerDao;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.participant.Participants;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.dates.DayOfWeek;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.lang.Validate;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class EventRoutinesTest extends AbstractConfTest {
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventWebUpdater eventWebUpdater;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private UserManager userManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private LayerDao layerDao;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventWebManager eventWebManager;

    @Test
    public void findEventsByExtId() {
        // just syntax check
        eventRoutines.findEventsAndEventLayersByExtId(12, "sdfsfsdf@dfgdgf");
    }

    private static PassportUid UID = new PassportUid(9252);
    private static PassportUid UID2= new PassportUid(9253);

    @Test
    public void createEventWithUnavailableResourceEndingInThePast() {
        createEventWithUnavailableResource(
                TestDateTimes.moscow(2012, 1, 30, 19, 0),
                TestDateTimes.moscow(2012, 1, 30, 20, 0),
                TestDateTimes.moscow(2012, 1, 30, 21, 0),
                ActionSource.WEB);
    }

    @Test
    public void createEventWithUnavailableResourceStartingInTheFuture() {
        try {
            createEventWithUnavailableResource(
                    TestDateTimes.moscow(2012, 1, 30, 19, 0),
                    TestDateTimes.moscow(2012, 1, 30, 20, 0),
                    TestDateTimes.moscow(2012, 1, 30, 18, 0),
                    ActionSource.WEB);
            Assert.A.fail("exception expected: resource is unavailable");
        } catch (CommandRunException e) {
            Assert.A.equals(Situation.BUSY_OVERLAP, e.getSituation().get());
        }
    }

    @Test
    public void createEventWithUnavailableResourceGoingOnNow() {
        try {
            createEventWithUnavailableResource(
                    TestDateTimes.moscow(2012, 1, 30, 19, 0),
                    TestDateTimes.moscow(2012, 1, 30, 20, 0),
                    TestDateTimes.moscow(2012, 1, 30, 19, 30),
                    ActionSource.WEB);
            Assert.A.fail("exception expected: resource is unavailable");
        } catch (CommandRunException e) {
            Assert.A.equals(Situation.BUSY_OVERLAP, e.getSituation().get());
        }
    }

    @Test
    public void createEventWithUnavailableResourceStartingInTheFutureFromExchange() {
        createEventWithUnavailableResource(
                TestDateTimes.moscow(2012, 1, 30, 19, 0),
                TestDateTimes.moscow(2012, 1, 30, 20, 0),
                TestDateTimes.moscow(2012, 1, 30, 18, 0),
                ActionSource.EXCHANGE);
    }

    public void createEventWithUnavailableResource(
            Instant eventStart, Instant eventEnd, Instant now, ActionSource actionSource)
    {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        Email email = ResourceRoutines.getResourceEmail(r);

        PassportUid uid = testManager.prepareRandomYaTeamUser(1232).getUid();

        testManager.cleanUser(uid);

        EventData first = testManager.createDefaultEventData(uid, "one");
        first.getEvent().setStartTs(eventStart);
        first.getEvent().setEndTs(eventEnd);
        first.setInvData(testManager.createParticipantsData(userManager.getEmailByUid(uid).get(), email));

        EventData conflicting = testManager.createDefaultEventData(uid, "two");
        conflicting.getEvent().setStartTs(eventStart);
        conflicting.getEvent().setEndTs(eventEnd);
        conflicting.setInvData(testManager.createParticipantsData(userManager.getEmailByUid(uid).get(), email));
        conflicting.setExchangeData(Option.of(new ExchangeData(UidOrResourceId.user(uid), "asdasd")));

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                TestManager.testExchangeThreeLittlePigsEmail, eventStart, eventEnd);

        createEvent(uid, first, ActionInfo.webTest(now));
        createEvent(uid, conflicting, new ActionInfo(actionSource, "?", now));
    }

    @Test
    public void createEventAfterEndOfTime() {
        assertFailureOnCreateWithWrongYear(20017);
    }

    @Test
    public void createEventBeforeStartOfTime() {
        assertFailureOnCreateWithWrongYear(17);
    }

    @Test
    public void moveEventAfterEndOfTime() {
        assertFailureOnMoveToWrongYear(20017);
    }

    @Test
    public void moveEventBeforeStartOfTime() {
        assertFailureOnMoveToWrongYear(17);
    }

    private void assertFailureOnCreateWithWrongYear(int year) {
        PassportUid uid = testManager.prepareRandomYaTeamUser(1238).getUid();
        EventData data = testManager.createDefaultEventData(
                uid, "impossible", TestDateTimes.moscow(year, 1, 1, 19, 0));
        Assert.failure(Try.tryCatchException(() -> createEvent(uid, data, ActionInfo.webTest(Instant.now()))));
    }

    private void assertFailureOnMoveToWrongYear(int year) {
        UserInfo user = testManager.prepareRandomYaTeamUser(1238).getUserInfo();
        PassportUid uid = user.getUid();

        EventData data = testManager.createDefaultEventData(uid, "default");

        long newEventId = createEvent(uid, data, ActionInfo.webTest(Instant.now())).getEventId();

        data.getEvent().setId(newEventId);
        data.getEvent().setStartTs(TestDateTimes.moscow(year, 1, 1, 19, 0));
        data.getEvent().setEndTs(TestDateTimes.moscow(year, 1, 1, 20, 0));

        Assert.failure(Try.tryCatchException(() -> eventWebManager.update(user, data, false, ActionInfo.webTest())));
    }

    @Test
    public void moveEventToATimeWhereRemovedResourceIsBusy() {
        Resource r = testManager.cleanAndCreateThreeLittlePigs();
        Email resourceEmail = ResourceRoutines.getResourceEmail(r);

        UserInfo user = testManager.prepareRandomYaTeamUser(1237).getUserInfo();
        PassportUid uid = user.getUid();

        EventData event = testManager.createDefaultEventData(uid, "one");
        event.setInvData(testManager.createParticipantsData(userManager.getEmailByUid(uid).get(), resourceEmail));

        EventData conflictingInFuture = testManager.createDefaultEventData(uid, "two");
        conflictingInFuture.getEvent().setStartTs(event.getEvent().getEndTs());
        conflictingInFuture.getEvent().setEndTs(event.getEvent().getEndTs().plus(Duration.standardHours(1)));
        conflictingInFuture.setInvData(testManager.createParticipantsData(userManager.getEmailByUid(uid).get(), resourceEmail));

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                TestManager.testExchangeThreeLittlePigsEmail, event.getEvent().getStartTs(), conflictingInFuture.getEvent().getEndTs());

        CreateInfo i = createEvent(uid, event, ActionInfo.webTest());
        createEvent(uid, conflictingInFuture, ActionInfo.webTest());

        event.getEvent().setStartTs(conflictingInFuture.getEvent().getStartTs());
        event.getEvent().setEndTs(conflictingInFuture.getEvent().getEndTs());
        event.setInvData(new EventInvitationUpdateData(Cf.<Email>list(), Cf.list(resourceEmail)));
        event.getEvent().setId(i.getEvent().getId());

        eventWebManager.update(user, event, true, ActionInfo.webTest());
    }

    @Test
    public void updateEventWithUnavailableResource() {
        Resource r = testManager.cleanAndCreateSmolny();
        Email email = ResourceRoutines.getResourceEmail(r);

        UserInfo user = testManager.prepareRandomYaTeamUser(936).getUserInfo();
        PassportUid uid = user.getUid();

        EventData withRoom = testManager.createDefaultEventData(uid, "room");
        withRoom.setInvData(testManager.createParticipantsData(userManager.getEmailByUid(uid).get(), email));

        EventData withoutRoom = testManager.createDefaultEventData(uid, "no-room");

        ewsProxyWrapper.cancelMasterAndSingleMeetings(
                TestManager.testExchangeSmolnyEmail, withRoom.getEvent().getStartTs(), withRoom.getEvent().getEndTs());

        createEvent(uid, withRoom, ActionInfo.webTest());
        CreateInfo pleinAir = createEvent(uid, withoutRoom, ActionInfo.webTest());

        withRoom.getEvent().setId(pleinAir.getEvent().getId());

        Instant now = withRoom.getEvent().getStartTs().minus(Duration.standardHours(1));
        try {
            eventWebUpdater.update(user, withRoom, NotificationsData.notChanged(), true, ActionInfo.webTest(now));
            Assert.A.fail("exception expected: resource is unavailable");
        } catch (CommandRunException e) {
            Assert.A.equals(Situation.BUSY_OVERLAP, e.getSituation().get());
        }
    }

    @Test
    public void removeLastAttendeeFromMeeting() {
        Email organizerEmail = userManager.getEmailByUid(UID).get();
        Email attendeeEmail = userManager.getEmailByUid(UID2).get();

        EventData meeting = testManager.createDefaultEventData(UID, "meeting");
        meeting.setInvData(testManager.createParticipantsData(organizerEmail, attendeeEmail));

        CreateInfo meetingInfo = createEvent(UID, meeting, ActionInfo.webTest());

        long eventId = meetingInfo.getEvent().getId();
        Assert.A.isTrue(eventInvitationManager.getParticipantsByEventId(eventId).isMeeting());

        meeting.setInvData(new EventInvitationUpdateData(Cf.<Email>list(), Cf.list(attendeeEmail)));
        meeting.getEvent().setId(eventId);
        eventWebUpdater.update(userManager.getUserInfo(UID), meeting,
                NotificationsData.notChanged(), true, ActionInfo.webTest());

        Participants participants = eventInvitationManager.getParticipantsByEventId(eventId);
        Assert.A.isTrue(participants.isNotMeetingStrict());
    }

    @Test
    public void notMeetingBecomesMeeting() {
        Email attendeeEmail = userManager.getEmailByUid(UID2).get();

        EventData notMeeting = testManager.createDefaultEventData(UID, "not meeting");
        CreateInfo notMeetingInfo = createEvent(UID, notMeeting, ActionInfo.webTest());

        long eventId = notMeetingInfo.getEvent().getId();

        Assert.A.isTrue(eventInvitationManager.getParticipantsByEventId(eventId).isNotMeetingOrIsInconsistent());

        notMeeting.setInvData(new EventInvitationUpdateData(Cf.list(attendeeEmail), Cf.<Email>list()));
        notMeeting.getEvent().setId(eventId);
        eventWebUpdater.update(userManager.getUserInfo(UID), notMeeting,
                NotificationsData.notChanged(), true, ActionInfo.webTest());
        Assert.A.isTrue(eventInvitationManager.getParticipantsByEventId(eventId).isMeeting());
    }

    @Test
    public void createEventOnSomeoneElseLayer() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(911);
        TestUserInfo victim = testManager.prepareRandomYaTeamUser(912);

        EventData event = testManager.createDefaultEventData(victim.getUid(), "event");
        Validate.V.equals(layerRoutines.getDefaultLayerId(victim.getUid()).get(), event.getLayerId().get());

        assertThatExceptionOfType(PermissionDeniedUserException.class)
                .isThrownBy(() -> createEvent(creator.getUid(), event, ActionInfo.webTest()));
    }

    @Test
    public void createEventOnAnothersSharedLayer() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(913);
        TestUserInfo victim = testManager.prepareRandomYaTeamUser(914);

        EventData event = testManager.createDefaultEventData(victim.getUid(), "event");
        Validate.V.equals(layerRoutines.getDefaultLayerId(victim.getUid()).get(), event.getLayerId().get());

        layerRoutines.startNewSharing(creator.getUid(), event.getLayerId().get(), LayerActionClass.CREATE);

        createEvent(creator.getUid(), event, ActionInfo.webTest());
    }

    @Test
    public void eventsAtTheSameTime() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-19001");
        TestUserInfo meetingOrganizer = testManager.prepareUser("yandex-team-mm-19002");

        Event event = testManager.createDefaultEvent(user.getUid(), "eventsAtTheSameTime1");
        Event meeting = testManager.createDefaultEvent(meetingOrganizer.getUid(), "eventsAtTheSameTime2");

        event.setStartTs(TestDateTimes.moscow(2011, 4, 4, 14, 0));
        event.setEndTs(TestDateTimes.moscow(2011, 4, 4, 15, 0));
        eventDao.updateEvent(event);

        meeting.setStartTs(TestDateTimes.moscow(2011, 4, 4, 14, 0));
        meeting.setEndTs(TestDateTimes.moscow(2011, 4, 4, 15, 0));
        eventDao.updateEvent(meeting);

        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);

        testManager.addUserParticipantToEvent(meeting.getId(), meetingOrganizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(meeting.getId(), user.getUid(), Decision.UNDECIDED, false);

        testManager.updateEventTimeIndents(event, meeting);

        ListF<EventInstanceInfo> eventInfos = eventRoutines.getSortedInstancesIMayView(Option.of(user.getUid()),
                TestDateTimes.moscow(2011, 4, 1, 0, 0), Option.of(TestDateTimes.moscow(2011, 4, 30, 23, 59)),
                LayerIdPredicate.allForUser(user.getUid(), true), ActionSource.WEB);

        Assert.A.hasSize(2, eventInfos);
    }

    @Test
    public void findResourceEventIntersectingGivenIntervalWithExdate() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-19011");
        Resource r = testManager.cleanAndCreateThreeLittlePigs();

        Event event = testManager.createDefaultEvent(user.getUid(), "findResourceEventIntersectingGivenIntervalWithExdate");
        testManager.addUserParticipantToEvent(event.getId(), user, Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);
        testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        Instant secondDayOfRepetition = event.getStartTs().toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).plusDays(1).toInstant();

        testManager.createExdate(secondDayOfRepetition, event.getId());
        testManager.updateEventTimeIndents(event);

        Interval interval = new Interval(secondDayOfRepetition.toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE),
                secondDayOfRepetition.toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).plusHours(1));

        RepetitionInstanceInfo repetitionInfo = RepetitionInstanceInfo.create(interval, Option.empty());

        Option<Tuple2<AvailabilityOverlap, Resource>> intersection = eventRoutines.findFirstResourceEventIntersectingGivenInterval(
                Option.empty(), Cf.list(r.getId()), Option.empty(),
                repetitionInfo, true, ActionInfo.webTest(event.getStartTs()));

        Assert.A.none(intersection);
    }

    @Test
    public void findResourceEventIntersectingGivenIntervalInFutureForRepeatingEvent() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-19021");
        Resource r = testManager.cleanAndCreateThreeLittlePigs();

        Event event = testManager.createDefaultEvent(
                user.getUid(), "findResourceEventIntersectingGivenIntervalInFutureForRepeatingEvent");
        testManager.addUserParticipantToEvent(event.getId(), user, Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), r);
        testManager.createDailyRepetitionAndLinkToEvent(event.getId());

        testManager.updateEventTimeIndents(event);

        DateTime intervalStart = event.getStartTs().toDateTime(TimeUtils.EUROPE_MOSCOW_TIME_ZONE).plusDays(3);
        Interval interval = new Interval(intervalStart, intervalStart.plusHours(1));

        RepetitionInstanceInfo repetitionInfo = RepetitionInstanceInfo.create(interval, Option.empty());

        Option<Tuple2<AvailabilityOverlap, Resource>> first =
                eventRoutines.findFirstResourceEventIntersectingGivenInterval(
                        Option.empty(), Cf.list(r.getId()), Option.empty(),
                        repetitionInfo, true, ActionInfo.webTest(event.getStartTs()));

        Option<Tuple2<AvailabilityOverlap, Resource>> shouldBeSomeAsFirst =
                eventRoutines.findFirstResourceEventIntersectingGivenInterval(
                        Option.empty(), Cf.list(r.getId()), Option.empty(),
                        repetitionInfo, false, ActionInfo.webTest(event.getEndTs()));

        Assert.A.some(first);
        Assert.A.some(shouldBeSomeAsFirst);
    }

    @Test
    public void normalizeExternalId() {
        Assert.A.equals("00MJE5OAyandexru", new ExternalId("00MJE5OA@yandex.ru").getNormalized());
        Assert.A.equals("inviteffe00398d4784468be6e68dad3a36d23", new ExternalId("invite:ffe00398-d478-4468-be6e-68dad3a36d23").getNormalized());
        Assert.A.equals("00B8169DBC948A63C325779E004BF991LotusNotesGenerated", new ExternalId("00B8169DBC948A63C325779E004BF991-Lotus_Notes_Generated").getNormalized());
        Assert.A.equals("DFAC511DFBA244FDA67EA94B9783FAD0", new ExternalId("{DFAC511D-FBA2-44FD-A67E-A94B9783FAD0}").getNormalized());
    }

    @Test
    public void occurrenceOverlaps() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(123);

        DateTime start = MoscowTime.dateTime(2018, 2, 16, 11, 0);
        Event master = testManager.createDefaultEvent(user.getUid(), "Repeating", start, start.plusHours(2));

        testManager.linkRepetitionToEvent(master.getId(),
                testManager.createWeeklyRepetition(DayOfWeek.byJodaDayOfWeek(start.getDayOfWeek())));


        Function2<Integer, Integer, Event> shiftOccurrence = (occurrenceWeek, startShiftHours) ->
                testManager.createDefaultRecurrence(user.getUid(), master.getId(),
                        start.plusWeeks(occurrenceWeek), Duration.standardHours(startShiftHours));

        Function1B<Event> overlaps = event -> {
            try {
                eventRoutines.ensureExchangeCompatibleIfNeeded(
                        ActorId.user(user.getUid()), eventDbManager.getEventWithRelationsByEvent(event));
                return false;

            } catch (CommandRunException e) {
                if (e.getSituation().isSome(Situation.EWS_OCCURRENCES_OVERLAP)) {
                    return true;
                }
                throw e;
            }
        };

        shiftOccurrence.apply(1, 0);
        Assert.isTrue(overlaps.apply(shiftOccurrence.apply(2, -7 * 24 + 1)));   // overlaps

        shiftOccurrence.apply(4, 0);
        Assert.isTrue(overlaps.apply(shiftOccurrence.apply(3, 7 * 24 - 1)));    // overlaps

        Assert.isTrue(overlaps.apply(shiftOccurrence.apply(6, -7 * 24 + 11)));  // same day
        Assert.isFalse(overlaps.apply(shiftOccurrence.apply(7, -10 * 24)));

        Assert.isTrue(overlaps.apply(shiftOccurrence.apply(9, 7 * 24 - 11)));   // same day
        Assert.isFalse(overlaps.apply(shiftOccurrence.apply(8, 10 * 24)));

        Assert.isTrue(overlaps.apply(shiftOccurrence.apply(11, -10 * 24)));     // crosses
        Assert.isTrue(overlaps.apply(shiftOccurrence.apply(12, 10 * 24)));      // crosses

        Assert.isFalse(overlaps.apply(shiftOccurrence.apply(14, 7 * 24 - 12))); // ends in date

        shiftOccurrence.apply(16, -11);
        Assert.isTrue(overlaps.apply(shiftOccurrence.apply(15, 7 * 24 - 12)));  // end overlaps

        shiftOccurrence.apply(17, 10 * 24);
        Assert.isTrue(overlaps.apply(shiftOccurrence.apply(18, 0)));            // moved forward then returned
    }

    private CreateInfo createEvent(PassportUid creatorUid, EventData eventData, ActionInfo actionInfo) {

        return eventRoutines.createUserOrFeedEvent(UidOrResourceId.user(creatorUid), EventType.USER,
                eventRoutines.createMainEvent(creatorUid, eventData, actionInfo),
                eventData, NotificationsData.createEmpty(),
                InvitationProcessingMode.SAVE_ATTACH, actionInfo);
    }

} //~
