package ru.yandex.calendar.logic.event.web;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventFields;
import ru.yandex.calendar.logic.beans.generated.EventHelper;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.beans.generated.RepetitionHelper;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.domain.PassportAuthDomains;
import ru.yandex.calendar.logic.domain.PassportAuthDomainsHolder;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.ModificationInfo;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationData;
import ru.yandex.calendar.logic.event.model.EventInvitationsData;
import ru.yandex.calendar.logic.event.model.EventUserData;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.layer.LayerDao;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.notification.SendingSmsDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.ResourceType;
import ru.yandex.calendar.logic.sending.param.EventLocation;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.sharing.participant.EventParticipants;
import ru.yandex.calendar.logic.sharing.participant.Participants;
import ru.yandex.calendar.logic.sharing.perm.EventActionClass;
import ru.yandex.calendar.logic.user.Group;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.test.auto.db.util.TestLayerCollLastUpdateChecker;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractEwsExportedLoginsTest;
import ru.yandex.calendar.util.dates.DateTimeManager;
import ru.yandex.calendar.util.dates.DayOfWeek;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public class EventWebManagerUpdateEventTest extends AbstractEwsExportedLoginsTest {

    @Autowired
    private EventDao eventDao;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private TestLayerCollLastUpdateChecker testLayerCollLastUpdateChecker;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private MailSenderMock mailSenderMock;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private SendingSmsDao sendingSmsDao;
    @Autowired
    private LayerDao layerDao;
    @Autowired
    private DateTimeManager dateTimeManager;
    @Autowired
    private PassportAuthDomainsHolder passportAuthDomainsHolder;

    public EventWebManagerUpdateEventTest(EwsUsage ewsUsage) {
        super(ewsUsage);
    }

    @Before
    public void mockEwsProxyWrapper(){
        setMockEwsProxyWrapper();
    }

    @Test
    public void singleEvent1() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-11102").getUserInfo();
        PassportUid uid = user.getUid();

        ActionInfo actionInfo = ActionInfo.webTest();

        long layerId =
                testManager.createDefaultLayerForUser(uid, actionInfo.getNow().minus(Duration.standardDays(1)));

        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(uid, "singleEvent1");

        EventData eventData = new EventData();
        eventData.setInstanceStartTs(event.getStartTs());
        eventData.getEvent().setId(event.getId());
        eventData.getEvent().setStartTs(TestDateTimes.addDaysMoscow(event.getStartTs(), 1));
        eventData.getEvent().setEndTs(TestDateTimes.addDaysMoscow(event.getEndTs(), 1));
        eventData.getEvent().setName("New name 1");
        eventData.getEvent().setDescription("New description 1");
        eventData.getEvent().setLocation("New location 1");
        eventData.getRepetition().setREach(1);
        eventData.getRepetition().setType(RegularRepetitionRule.DAILY);
        eventData.setTimeZone(MoscowTime.TZ);

        eventWebManager.update(user, eventData, true, actionInfo);

        Event updatedEvent = eventDao.findEventById(event.getId());
        Event eventChanges = EventHelper.INSTANCE.findChanges(updatedEvent, eventData.getEvent());
        Assert.A.isTrue(eventChanges.isEmpty());
        Repetition r = eventDao.findRepetitionById(updatedEvent.getRepetitionId().get());
        Repetition repetitionChanges = RepetitionHelper.INSTANCE.findChanges(r, eventData.getRepetition());
        Assert.A.isTrue(repetitionChanges.isEmpty());

        testLayerCollLastUpdateChecker.assertUpdated(layerId, actionInfo.getNow());
    }

    @Test
    public void singleEvent2() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-11103").getUserInfo();
        PassportUid uid = user.getUid();

        ActionInfo actionInfo = ActionInfo.webTest();

        long layerId =
                testManager.createDefaultLayerForUser(uid, actionInfo.getNow().minus(Duration.standardDays(1)));

        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(uid, "singleEvent2");

        EventData eventData = new EventData();
        eventData.setInstanceStartTs(event.getStartTs());
        eventData.getEvent().setId(event.getId());
        eventData.getEvent().setStartTs(TestDateTimes.addDaysMoscow(event.getStartTs(), 1));
        eventData.getEvent().setEndTs(TestDateTimes.addDaysMoscow(event.getEndTs(), 1));
        eventData.getEvent().setName("New name 2");
        eventData.getEvent().setDescription("New description 2");
        eventData.getEvent().setLocation("New location 2");
        eventData.setTimeZone(MoscowTime.TZ);

        eventWebManager.update(user, eventData, true, actionInfo);

        Event updatedEvent = eventDao.findEventById(event.getId());
        Event eventChanges = EventHelper.INSTANCE.findChanges(updatedEvent, eventData.getEvent());
        Assert.A.isTrue(eventChanges.isEmpty());
        Assert.assertNull(updatedEvent.getRepetitionId().getOrNull());

        testLayerCollLastUpdateChecker.assertUpdated(layerId, actionInfo.getNow());
    }

    @Test
    public void singleRecurInstance1() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-11103").getUserInfo();
        PassportUid uid = user.getUid();

        ActionInfo actionInfo = ActionInfo.webTest();

        long layerId =
                testManager.createDefaultLayerForUser(uid, actionInfo.getNow().minus(Duration.standardDays(1)));

        Event recurrenceEvent = testManager.createEventWithRepetitionAndRecurrence(uid).get2();

        EventData eventData = new EventData();
        eventData.setInstanceStartTs(recurrenceEvent.getStartTs());
        eventData.getEvent().setId(recurrenceEvent.getId());
        eventData.getEvent().setStartTs(TestDateTimes.plusHours(recurrenceEvent.getStartTs(), 1));
        eventData.getEvent().setEndTs(TestDateTimes.plusHours(recurrenceEvent.getEndTs(), 1));
        eventData.getEvent().setName("New name 3");
        eventData.getEvent().setDescription("New description 3");
        eventData.getEvent().setLocation("New location 3");
        eventData.setTimeZone(MoscowTime.TZ);

        eventWebManager.update(user, eventData, false, actionInfo);

        Event updatedEvent = eventDao.findEventById(recurrenceEvent.getId());
        Event eventChanges = EventHelper.INSTANCE.findChanges(updatedEvent, eventData.getEvent());
        Assert.A.isTrue(eventChanges.isEmpty());
        Assert.assertNull(updatedEvent.getRepetitionId().getOrNull());

        testLayerCollLastUpdateChecker.assertUpdated(layerId, actionInfo.getNow());
    }

    @Test
    public void eventWithRepetition() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-11106").getUserInfo();
        PassportUid uid = user.getUid();

        ActionInfo actionInfo = ActionInfo.webTest();

        long layerId =
                testManager.createDefaultLayerForUser(uid, actionInfo.getNow().minus(Duration.standardDays(1)));

        Event event = testManager.createEventWithDailyRepetition(uid);

        EventData eventData = new EventData();
        eventData.setInstanceStartTs(event.getStartTs());
        eventData.getEvent().setId(event.getId());
        eventData.getEvent().setName("New name");
        eventData.getRepetition().setREach(1);
        eventData.getRepetition().setType(RegularRepetitionRule.DAILY);
        eventData.setTimeZone(MoscowTime.TZ);

        Long recurrenceEventId = eventWebManager.update(user, eventData, false, actionInfo).get();

        Event recurrenceEvent = eventDao.findEventById(recurrenceEventId);
        Assert.A.equals(eventData.getEvent().getName(), recurrenceEvent.getName());
        Assert.A.equals(event.getStartTs(), recurrenceEvent.getRecurrenceId().get());
        Assert.assertNull(recurrenceEvent.getRepetitionId().getOrNull());

        testLayerCollLastUpdateChecker.assertUpdated(layerId, actionInfo.getNow());
    }

    @Test
    public void eventWithRepetitionRemoveRepetition() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-11106").getUserInfo();
        PassportUid uid = user.getUid();

        ActionInfo actionInfo = ActionInfo.webTest();

        long layerId =
                testManager.createDefaultLayerForUser(uid, actionInfo.getNow().minus(Duration.standardDays(1)));

        Event event = testManager.createEventWithDailyRepetition(uid);

        EventData eventData = new EventData();
        eventData.setInstanceStartTs(event.getStartTs());
        eventData.getEvent().setId(event.getId());
        eventData.getEvent().setName(event.getName() + "2");

        Long recurrenceEventId = eventWebManager.update(user, eventData, false, actionInfo).get();

        Event updatedEvent = eventDao.findEventById(recurrenceEventId);
        Assert.assertNull(updatedEvent.getRepetitionId().getOrNull());

        testLayerCollLastUpdateChecker.assertUpdated(layerId, actionInfo.getNow());
    }

    @Test
    public void eventWithRepetitionRdateAndRecurInst1() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-11107").getUserInfo();
        PassportUid uid = user.getUid();

        ActionInfo actionInfo = ActionInfo.webTest();

        long layerId =
                testManager.createDefaultLayerForUser(uid, actionInfo.getNow().minus(Duration.standardDays(1)));

        int rdateCountBeforeTest = eventDao.findRdateCount();
        int eventCountBeforeTest = eventDao.findEventCount(EventFields.RECURRENCE_ID.column().isNotNull());

        Event event = testManager.createEventWithRepetitionAndRdateAndRecurrence(uid).get1();

        EventData eventData = new EventData();
        eventData.setInstanceStartTs(event.getStartTs());
        eventData.getEvent().setId(event.getId());
        eventData.getEvent().setStartTs(TestDateTimes.addDaysMoscow(event.getStartTs(), 1));
        eventData.getEvent().setEndTs(TestDateTimes.addDaysMoscow(event.getEndTs(), 1));
        eventData.getRepetition().setREach(1);
        eventData.getRepetition().setType(RegularRepetitionRule.DAILY);
        eventData.setTimeZone(MoscowTime.TZ);

        eventWebManager.update(user, eventData, true, actionInfo);

        Event updatedEvent = eventDao.findEventById(event.getId());
        Event eventChanges = EventHelper.INSTANCE.findChanges(updatedEvent, eventData.getEvent());
        Assert.A.isTrue(eventChanges.isEmpty());
        Repetition r = eventDao.findRepetitionById(updatedEvent.getRepetitionId().get());
        Repetition repetitionChanges = RepetitionHelper.INSTANCE.findChanges(r, eventData.getRepetition());
        Assert.A.isTrue(repetitionChanges.isEmpty());

        Assert.assertEquals(rdateCountBeforeTest, eventDao.findRdateCount());
        Assert.assertEquals(eventCountBeforeTest, eventDao.findEventCount(EventFields.RECURRENCE_ID.column().isNotNull()));

        testLayerCollLastUpdateChecker.assertUpdated(layerId, actionInfo.getNow());
    }

    @Test
    public void eventWithRepetitionRdateAndRecurInst2() {
        UserInfo user = testManager.prepareUser("yandex-team-mm-11108").getUserInfo();
        PassportUid uid = user.getUid();

        ActionInfo actionInfo = ActionInfo.webTest();

        long layerId =
                testManager.createDefaultLayerForUser(uid, actionInfo.getNow().minus(Duration.standardDays(1)));

        Event event = testManager.createEventWithRepetitionAndRdateAndRecurrence(uid).get1();

        int rdateCountBeforeTest = eventDao.findRdateCount();
        int eventCountBeforeTest = eventDao.findEventCount(EventFields.RECURRENCE_ID.column().isNotNull());

        EventData eventData = new EventData();
        eventData.setInstanceStartTs(TestDateTimes.addDaysMoscow(event.getStartTs(), 2));
        eventData.getEvent().setId(event.getId());
        eventData.getEvent().setStartTs(TestDateTimes.addDaysMoscow(event.getStartTs(), 2));
        eventData.getEvent().setEndTs(TestDateTimes.addDaysMoscow(event.getEndTs(), 2));
        eventData.getEvent().setName("New name");
        eventData.getRepetition().setREach(1);
        eventData.getRepetition().setType(RegularRepetitionRule.DAILY);
        eventData.setTimeZone(MoscowTime.TZ);

        eventWebManager.update(user, eventData, true, actionInfo);

        // check new event
        Event newEvent = eventDao.findEventById(event.getId());
        Assert.equals(eventData.getEvent().getName(), newEvent.getName());
        Repetition r = eventDao.findRepetitionById(newEvent.getRepetitionId().get());
        Repetition repetitionChanges = RepetitionHelper.INSTANCE.findChanges(r, eventData.getRepetition());
        Assert.isTrue(repetitionChanges.isEmpty());

        Assert.assertEquals(rdateCountBeforeTest, eventDao.findRdateCount());
        Assert.assertEquals(
                eventCountBeforeTest, eventDao.findEventCount(EventFields.RECURRENCE_ID.column().isNotNull()));

        testLayerCollLastUpdateChecker.assertUpdated(layerId, actionInfo.getNow());
    }

    // CAL-7531
    @Test
    public void stumpWithNoInstancesAndFuture() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(1100);

        Event master = testManager.createDefaultEventWithDailyRepetition(organizer.getUid(), "Stump");
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        DateTime start = master.getStartTs().toDateTime(MoscowTime.TZ);

        testManager.createExdate(start.toInstant(), master.getId());
        testManager.createExdate(start.plusDays(1).toInstant(), master.getId());

        EventData eventData = new EventData();
        eventData.getEvent().setId(master.getId());
        eventData.setTimeZone(dateTimeManager.getTimeZoneForUid(organizer.getUid()));
        eventData.setInstanceStartTs(start.plusDays(2).toInstant());

        eventData.getEvent().setName("New name");
        eventData.getEvent().setStartTs(start.plusDays(2).toInstant());
        eventData.getEvent().setEndTs(start.plusDays(2).plusHours(1).toInstant());

        Assert.none(eventWebManager.update(organizer.getUserInfo(), eventData, true, ActionInfo.webTest()));

        Event updated = eventDao.findEventById(master.getId());
        Assert.equals(eventData.getEvent().getName(), updated.getName());
        Assert.equals(eventData.getEvent().getStartTs(), updated.getStartTs());
        Assert.equals(eventData.getEvent().getEndTs(), updated.getEndTs());
    }

    @Test
    public void recurrenceInstanceFirstAndFuture() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(1107);
        Event master = testManager.createDefaultEventWithDailyRepetition(user.getUid(), "recurrenceFirstAndFuture");
        Event recurrence = testManager.createDefaultRecurrence(user.getUid(), master.getId(), master.getStartTs());

        EventData eventData = new EventData();
        eventData.getEvent().setId(recurrence.getId());
        eventData.getEvent().setName("New name");

        eventData.getEvent().setStartTs(recurrence.getStartTs().plus(360000));
        eventData.getEvent().setEndTs(recurrence.getEndTs().plus(360000));
        eventData.setTimeZone(MoscowTime.TZ);

        Option<Long> newEventId = eventWebManager.update(user.getUserInfo(), eventData, true, ActionInfo.webTest());

        Assert.none(newEventId);
        Assert.isEmpty(eventDao.findEventsByIdsSafe(Cf.list(recurrence.getId())));

        master = eventDao.findEventById(master.getId());
        Assert.equals(0, EventHelper.INSTANCE.findChanges(master, eventData.getEvent().<Event>withoutId()).cardinality());
    }

    // CAL-6013
    @Test
    public void createRecurrenceFromAttendee() {
        UserInfo organizer = testManager.prepareUser("yandex-team-mm-11110").getUserInfo();
        UserInfo attendee = testManager.prepareUser("yandex-team-mm-11111").getUserInfo();

        Event event = testManager.createDefaultEventWithDailyRepetition(organizer.getUid(), "Repeating");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        event = event.copy();
        event.setPermParticipants(EventActionClass.EDIT);
        event.setParticipantsInvite(true);
        eventDao.updateEvent(event);

        EventData eventData = new EventData();
        eventData.getEvent().setStartTs(event.getStartTs().plus(Duration.standardDays(5)));
        eventData.getEvent().setEndTs(event.getEndTs().plus(Duration.standardDays(5)));

        eventData.getEvent().setId(event.getId());
        eventData.setInstanceStartTs(eventData.getEvent().getStartTs());

        long createdEventId = eventWebManager.update(attendee, eventData, false, ActionInfo.webTest()).get();
        Event createdEvent = eventDao.findEventById(createdEventId);

        Assert.equals(eventData.getInstanceStartTs(), createdEvent.getRecurrenceId());
        Assert.equals(eventData.getEvent().getStartTs(), createdEvent.getStartTs());
        Assert.equals(eventData.getEvent().getEndTs(), createdEvent.getEndTs());
        Assert.equals(organizer.getUid(), createdEvent.getCreatorUid());
    }

    @Test
    public void createTailFromAttendee() {
        UserInfo organizer = testManager.prepareUser("yandex-team-mm-11110").getUserInfo();
        UserInfo attendee = testManager.prepareUser("yandex-team-mm-11111").getUserInfo();

        Event event = testManager.createDefaultEventWithDailyRepetition(organizer.getUid(), "Repeating");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        event = event.copy();
        event.setPermParticipants(EventActionClass.EDIT);
        event.setParticipantsInvite(true);
        eventDao.updateEvent(event);

        EventData data = new EventData();

        data.getEvent().setStartTs(event.getStartTs().plus(Duration.standardHours(50)));
        data.getEvent().setEndTs(event.getEndTs().plus(Duration.standardHours(50)));

        data.getEvent().setId(event.getId());
        data.setInstanceStartTs(event.getStartTs().plus(Duration.standardDays(2)));

        data.setRepetition(TestManager.createDailyRepetitionTemplate());

        long createdEventId = eventWebManager.update(
                attendee, data, true, ActionInfo.webTest(event.getStartTs())).get();

        Assert.equals(organizer.getUid(), eventDao.findEventById(createdEventId).getCreatorUid());
    }

    @Test
    public void changeMeetingOrganizer() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(884);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(488);
        TestUserInfo newOrganizer = testManager.prepareRandomYaTeamUser(848);

        TestUserInfo superUser = testManager.prepareRandomYaTeamSuperUser(844);

        Event master = testManager.createDefaultEvent(organizer.getUid(), "Repeating");
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.YES, false);

        Event doneRecurrence = testManager.createDefaultRecurrence(organizer.getUid(),
                master.getId(), master.getStartTs().plus(Duration.standardDays(1)));

        testManager.addUserParticipantToEvent(doneRecurrence.getId(), newOrganizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(doneRecurrence.getId(), attendee.getUid(), Decision.YES, false);

        Event organizersRecurrence = testManager.createDefaultRecurrence(organizer.getUid(),
                master.getId(), master.getStartTs().plus(Duration.standardDays(2)));

        testManager.addUserParticipantToEvent(organizersRecurrence.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(organizersRecurrence.getId(), newOrganizer.getUid(), Decision.YES, false);
        testManager.addUserParticipantToEvent(organizersRecurrence.getId(), attendee.getUid(), Decision.YES, false);

        Event attendeeRecurrence = testManager.createDefaultRecurrence(organizer.getUid(),
                master.getId(), master.getStartTs().plus(Duration.standardDays(3)));

        testManager.addUserParticipantToEvent(attendeeRecurrence.getId(), attendee.getUid(), Decision.YES, true);

        EventData eventData = new EventData();
        eventData.setEvent(master);
        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());

        eventData.setInvData(new EventInvitationsData(
                Option.of(newOrganizer.getEmail()),
                Cf.list(attendee.getEmail()).map(EventInvitationData.consWithNoNameF())));

        eventWebManager.update(superUser.getUserInfo(), eventData, true, ActionInfo.webTest(master.getStartTs()));

        Function1V<Event> check = event -> {
            Participants participants = eventDbManager.getEventWithRelationsByEvent(event).getParticipants();

            Assert.isTrue(participants.isMeeting());
            Assert.isFalse(participants.isParticipantWithInconsistent(organizer.getUid()));

            Assert.equals(newOrganizer.getUid(), participants.getOrganizer().getUid().get());
            Assert.equals(attendee.getUid(), participants.getAllAttendeesButNotOrganizer().single().getId().getUid());
        };

        check.apply(master);
        check.apply(doneRecurrence);
        check.apply(organizersRecurrence);
        check.apply(attendeeRecurrence);
    }

    @Test
    public void updateMasterWithPrivateTokenMakesAttendee() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(886);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(688);
        TestUserInfo guest = testManager.prepareRandomYaTeamUser(868);

        Event master = testManager.createDefaultEventWithDailyRepetition(organizer.getUid(), "Event");
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.YES, false);

        Instant recurrenceId = testManager.getRecurrenceIdInFuture(master, Instant.now());
        Event recurrence = testManager.createDefaultRecurrence(organizer.getUid(), master.getId(), recurrenceId);
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(recurrence.getId(), attendee.getUid(), Decision.YES, false);

        EventData data = new EventData();
        data.setEvent(master);
        data.setTimeZone(dateTimeManager.getTimeZoneForUid(organizer.getUid()));
        data.setInvData(new EventInvitationsData(Cf.list(organizer.getEmail(), attendee.getEmail())));

        Option<String> token = findEventUser(master, attendee).get().getPrivateToken();

        eventWebManager.update(
                guest.getUserInfo(), data, NotificationsData.notChanged(),
                token, Option.empty(), true, Option.empty(), ActionInfo.webTest());

        Assert.some(true, findEventUser(master, guest).map(EventUser.getIsAttendeeF()));
        Assert.some(true, findEventUser(recurrence, guest).map(EventUser.getIsAttendeeF()));
    }

    @Test
    public void updateRecurrenceWithPrivateTokenMakesAttendee() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(886);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(688);
        TestUserInfo guest = testManager.prepareRandomYaTeamUser(868);

        Event master = testManager.createDefaultEventWithDailyRepetition(organizer.getUid(), "Event");
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.YES, false);

        Instant recurrenceId = master.getStartTs().plus(Duration.standardDays(3));
        Event recurrence = testManager.createDefaultRecurrence(organizer.getUid(), master.getId(), recurrenceId);
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(recurrence.getId(), attendee.getUid(), Decision.YES, false);

        EventData data = new EventData();
        data.setEvent(recurrence);
        data.setInvData(new EventInvitationsData(Cf.list(organizer.getEmail(), attendee.getEmail())));

        Option<String> token = findEventUser(recurrence, attendee).get().getPrivateToken();

        eventWebManager.update(guest.getUserInfo(), data, NotificationsData.notChanged(),
                token, Option.empty(), false, Option.empty(), ActionInfo.webTest());

        Assert.none(findEventUser(master, guest));
        Assert.some(true, findEventUser(recurrence, guest).map(EventUser.getIsAttendeeF()));
    }

    @Test
    public void createTailInOtherLayer() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(1111);

        Event event = testManager.createEventWithDailyRepetition(user.getUid());
        testManager.createEventUser(user.getUid(), event.getId(), Decision.YES, Option.empty());

        long newLayerId = layerRoutines.createUserLayer(user.getUid());

        EventData data = new EventData();
        data.setEvent(event.copy());

        data.getEvent().setStartTs(event.getStartTs().plus(Duration.standardHours(28)));
        data.getEvent().setEndTs(event.getEndTs().plus(Duration.standardHours(28)));
        data.setInstanceStartTs(event.getStartTs().plus(Duration.standardDays(1)));

        data.setLayerId(newLayerId);

        Option<Long> newEventId = eventWebManager.update(
                user.getUserInfo(), data, true, ActionInfo.webTest(event.getStartTs()));
        Assert.some(newEventId);

        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(event.getId(), user.getDefaultLayerId()));
        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(newEventId.get(), newLayerId));

        Assert.none(eventLayerDao.findEventLayerByEventIdAndLayerId(event.getId(), newLayerId));
        Assert.none(eventLayerDao.findEventLayerByEventIdAndLayerId(newEventId.get(), user.getDefaultLayerId()));
    }

    @Test
    public void offcutDue() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(1111);

        DateTime start = MoscowTime.dateTime(2017, 6, 8, 13, 0);

        Event master = testManager.createDefaultEvent(user.getUid(), "offcut", start.toInstant());
        testManager.linkRepetitionToEvent(master.getId(), testManager.createWeeklyRepetition(DayOfWeek.THURSDAY));

        testManager.createDefaultRecurrence(user.getUid(), master.getId(), start.plusWeeks(1));

        EventData data = new EventData();
        data.setEvent(master.copy());

        data.getEvent().setStartTs(start.plusWeeks(2).plusHours(3).toInstant());
        data.getEvent().setEndTs(start.plusWeeks(2).plusHours(4).toInstant());
        data.setInstanceStartTs(start.plusWeeks(2).toInstant());

        ModificationInfo info = eventWebManager.update(
                user.getUserInfo(), data, Option.empty(), true, ActionInfo.webTest(start));

        Assert.some(start.toLocalDate().plusWeeks(1),
                info.getUpdatedEvent().flatMapO(er -> er.getRepetitionInfo().getUntilDate()));
    }

    @Test
    public void undueKeepsRecurrences() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(1111);

        DateTime start = MoscowTime.dateTime(2018, 3, 20, 20, 50);

        Event master = testManager.createDefaultEvent(user.getUid(), "offcut", start.toInstant());
        testManager.linkRepetitionToEvent(master.getId(),
                testManager.createDailyRepetitionWithDueTs(start.plusDays(20).toInstant()));

        Event recurrence = testManager.createDefaultRecurrence(user.getUid(), master.getId(), start.plusWeeks(1));

        EventData data = new EventData();
        data.setEvent(master.copy());
        data.setRepetition(TestManager.createDailyRepetitionTemplate());

        eventWebManager.update(user.getUserInfo(), data, Option.empty(), true, ActionInfo.webTest(start));

        Assert.notEmpty(eventDao.findEventsByIdsSafe(Cf.list(recurrence.getId())));
    }

    @Test
    public void noCuttingForPublic() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(1111);
        DateTime start = MoscowTime.dateTime(2017, 6, 8, 13, 0);

        Event master = testManager.createDefaultEvent(user.getUid(), "noCutInPublic", start.toInstant());
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        EventData data = new EventData();
        data.setEvent(master.copy());

        data.getEvent().setStartTs(start.plusWeeks(2).plusHours(3).toInstant());
        data.getEvent().setEndTs(start.plusWeeks(2).plusHours(4).toInstant());

        data.setInstanceStartTs(start.plusWeeks(2).toInstant());
        data.setTimeZone(dateTimeManager.getTimeZoneForUid(user.getUid()));

        PassportAuthDomains domains = passportAuthDomainsHolder.getDomains();
        passportAuthDomainsHolder.setDomainsForTest(PassportAuthDomains.PUBLIC);
        try {
            ModificationInfo info = eventWebManager.update(
                user.getUserInfo(), data, Option.empty(), true, ActionInfo.webTest(start));

            Assert.none(info.getNewEvent());

        } finally {
            passportAuthDomainsHolder.setDomainsForTest(domains);
        }
    }

    @Test
    public void updatePastOccurrenceTimeAndFutureForPublic() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-10931");
        Event master = testManager.createDefaultEventWithDailyRepetition(creator.getUid(), "dailyRepeatingEvent");

        Instant masterStart = master.getStartTs();
        Duration masterDuration = new Duration(master.getStartTs(), master.getEndTs());

        Instant instanceStart = masterStart.plus(Duration.standardDays(1));

        Duration offset = Duration.standardMinutes(30);
        Duration newDuration = masterDuration.plus(Duration.standardHours(1));

        EventData data = new EventData();
        data.setInstanceStartTs(instanceStart);

        data.getEvent().setId(master.getId());
        data.getEvent().setStartTs(instanceStart.plus(offset));
        data.getEvent().setEndTs(instanceStart.plus(offset).plus(newDuration));

        data.setTimeZone(dateTimeManager.getTimeZoneForUid(creator.getUid()));

        PassportAuthDomains domains = passportAuthDomainsHolder.getDomains();
        passportAuthDomainsHolder.setDomainsForTest(PassportAuthDomains.PUBLIC);
        try {
            ModificationInfo info = eventWebManager.update(
                    creator.getUserInfo(), data, Option.empty(), true,
                    ActionInfo.webTest(masterStart.plus(Duration.standardDays(5))));

            Assert.equals(masterStart.plus(offset), info.getUpdatedEvent().get().getEvent().getStartTs());
            Assert.equals(masterStart.plus(offset).plus(newDuration), info.getUpdatedEvent().get().getEvent().getEndTs());
        } finally {
            passportAuthDomainsHolder.setDomainsForTest(domains);
        }
    }

    // CAL-6756
    @Test
    public void superUserInvitesHimself() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(1115);
        TestUserInfo superUser = testManager.prepareYandexUser(TestManager.createDbrylev(), Group.SUPER_USER);

        Event event = testManager.createDefaultEvent(organizer.getUid(), "Meeting");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        EventData data = new EventData();
        data.setEvent(event);
        data.setLayerId(superUser.getDefaultLayerId());
        data.setInvData(organizer.getEmail(), superUser.getEmail());

        eventWebManager.update(superUser.getUserInfo(), data, false, ActionInfo.webTest());
        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(event.getId(), superUser.getDefaultLayerId()));

        EventUser eventUser = findEventUser(event, superUser).get();
        Assert.equals(true, eventUser.getIsAttendee());
        Assert.equals(Decision.YES, eventUser.getDecision());
    }

    @Test
    public void smsOnUpdateSingleMeeting() {
        UserInfo organizer = testManager.prepareUser("yandex-team-mm-10848").getUserInfo();
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10849");

        Event event = testManager.createDefaultEvent(organizer.getUid(), "smsOnUpdateSingleMeeting");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.MAYBE, false);
        testManager.saveEventNotifications(attendee.getUid(), event.getId(), Notification.sms(Duration.ZERO));

        EventData eventData = new EventData();
        eventData.getEvent().setId(event.getId());
        eventData.getEvent().setStartTs(TestDateTimes.plusHours(event.getStartTs(), 1));
        eventData.getEvent().setEndTs(TestDateTimes.plusHours(event.getEndTs(), 1));
        eventData.setInvData(attendee.getEmail());
        eventData.setTimeZone(MoscowTime.TZ);

        sendingSmsDao.deleteByUid(attendee.getUid());
        eventWebManager.update(organizer, eventData, false, ActionInfo.webTest(event.getStartTs()));
        Assert.hasSize(1, sendingSmsDao.findByUid(attendee.getUid()));

        sendingSmsDao.deleteByUid(attendee.getUid());
        eventWebManager.update(organizer, eventData, false, ActionInfo.webTest(event.getStartTs()));
        Assert.isEmpty(sendingSmsDao.findByUid(attendee.getUid()));
    }

    @Test
    public void smsOnUpdateOccurrence() {
        UserInfo organizer = testManager.prepareUser("yandex-team-mm-10848").getUserInfo();
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10849");

        Event master = testManager.createDefaultEvent(organizer.getUid(), "moveSmsOnUpdateOccurrence");
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);

        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.MAYBE, false);
        testManager.saveEventNotifications(attendee.getUid(), master.getId(), Notification.sms(Duration.ZERO));

        EventData eventData = new EventData();
        eventData.setInstanceStartTs(master.getStartTs());
        eventData.getEvent().setId(master.getId());
        eventData.getEvent().setStartTs(TestDateTimes.plusHours(master.getStartTs(), 1));
        eventData.getEvent().setEndTs(TestDateTimes.plusHours(master.getEndTs(), 1));
        eventData.setInvData(attendee.getEmail());
        eventData.setTimeZone(MoscowTime.TZ);

        sendingSmsDao.deleteByUid(attendee.getUid());

        Option<Long> recurrenceEventId = eventWebManager.update(
                organizer, eventData, false, ActionInfo.webTest(master.getStartTs()));
        Assert.hasSize(1, sendingSmsDao.findByUid(attendee.getUid()));

        Event recurrence = eventDao.findEventById(recurrenceEventId.get()).copy();
        eventData.setInstanceStartTs(Option.<Instant>empty());
        eventData.getEvent().setId(recurrence.getId());

        sendingSmsDao.deleteByUid(attendee.getUid());
        eventWebManager.update(organizer, eventData, false, ActionInfo.webTest(master.getStartTs()));
        Assert.isEmpty(sendingSmsDao.findByUid(attendee.getUid()));

        eventData.getEvent().setStartTs(TestDateTimes.plusHours(recurrence.getStartTs(), 1));
        eventData.getEvent().setEndTs(TestDateTimes.plusHours(recurrence.getEndTs(), 1));

        sendingSmsDao.deleteByUid(attendee.getUid());
        eventWebManager.update(organizer, eventData, false, ActionInfo.webTest(master.getStartTs()));
        Assert.hasSize(1, sendingSmsDao.findByUid(attendee.getUid()));
    }

    @Test
    public void smsOnRepetitionRuleChange() {
        UserInfo organizer = testManager.prepareUser("yandex-team-mm-10848").getUserInfo();
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10849");

        Event event = testManager.createDefaultEvent(organizer.getUid(), "smsOnRepetitionRuleChange");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.MAYBE, false);
        testManager.saveEventNotifications(attendee.getUid(), event.getId(), Notification.sms(Duration.ZERO));

        EventData eventData = new EventData();
        eventData.getEvent().setId(event.getId());
        eventData.getRepetition().setREach(3);
        eventData.getRepetition().setType(RegularRepetitionRule.DAILY);
        eventData.setInvData(attendee.getEmail());
        eventData.setTimeZone(MoscowTime.TZ);

        Instant now = TestDateTimes.plusDays(event.getStartTs(), 3);

        sendingSmsDao.deleteByUid(attendee.getUid());
        Assert.none(eventWebManager.update(organizer, eventData, true, ActionInfo.webTest(event.getStartTs())));
        Assert.isEmpty(sendingSmsDao.findByUid(attendee.getUid()));

        eventData.getRepetition().setREach(5);
        sendingSmsDao.deleteByUid(attendee.getUid());
        Assert.none(eventWebManager.update(organizer, eventData, true, ActionInfo.webTest(now)));
        Assert.hasSize(1, sendingSmsDao.findByUid(attendee.getUid()));
    }

    // CAL-6896
    @Test
    public void attachDetachedEventUsingUpdate() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(1234);
        TestUserInfo user = testManager.prepareRandomYaTeamUser(4321);

        Event event = testManager.createDefaultEvent(creator.getUid(), "attachedDetachedEvent");
        testManager.addUserParticipantToEvent(event.getId(), creator.getUid(), Decision.YES, true);

        testManager.openEventAndLayer(event.getId(), creator.getDefaultLayerId());

        EventData data = new EventData();
        data.getEvent().setId(event.getId());

        Assert.none(findEventLayer(event, user));
        Assert.none(findEventUser(event, user));

        eventWebManager.update(user.getUserInfo(), data, false, ActionInfo.webTest());
        Option<Long> layerId = findEventLayer(event, user).map(EventLayer.getLayerIdF());
        Assert.some(findEventUser(event, user));
        Assert.some(layerId);

        eventWebManager.detachEvent(user.getUserInfo(), event.getId(), layerId, ActionInfo.webTest());
        Assert.some(Decision.NO, findEventUser(event, user).map(EventUser.getDecisionF()));
        Assert.none(findEventLayer(event, user));

        eventWebManager.update(user.getUserInfo(), data, false, ActionInfo.webTest());
        Assert.some(Decision.YES, findEventUser(event, user).map(EventUser.getDecisionF()));
        Assert.some(findEventLayer(event, user).map(EventLayer.getLayerIdF()));
    }

    // CAL-7558
    @Test
    public void onetimeInviteeAttachesWholeMeeting() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(1245);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(5421);
        TestUserInfo invitee = testManager.prepareRandomYaTeamUser(3333);

        Event master = testManager.createDefaultEvent(organizer.getUid(), "onetimeInviteeAttach");
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.YES, false);

        eventDao.updateEventPermAll(master.getId(), EventActionClass.VIEW);

        Event recurrence = testManager.createDefaultRecurrence(organizer.getUid(), master.getId(), master.getStartTs());
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(recurrence.getId(), attendee.getUid(), Decision.YES, false);
        testManager.addUserParticipantToEvent(recurrence.getId(), invitee.getUid(), Decision.YES, false);

        String privateToken = findEventUser(recurrence, invitee).get().getPrivateToken().get();
        EventData eventData = new EventData();
        eventData.getEvent().setId(master.getId());
        eventData.setInvData(Option.empty(), attendee.getEmail());

        eventWebManager.update(
                invitee.getUserInfo(), eventData, NotificationsData.notChanged(),
                Option.of(privateToken), Option.empty(), true, Option.empty(), ActionInfo.webTest());

        Assert.isTrue(findEventUser(recurrence, invitee).get().getIsAttendee());
        Assert.isFalse(findEventUser(master, invitee).get().getIsAttendee());
        Assert.isTrue(findEventUser(master, invitee).get().getIsSubscriber());
    }

    // CAL-6895
    @Test
    public void changeAlreadyChangedOrganizerAndParticipants() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(1333);
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(1334);
        TestUserInfo finalOrganizer = testManager.prepareRandomYaTeamUser(1335);

        Event event = testManager.createDefaultEvent(creator.getUid(),  "MMM");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addExternalUserParticipantToEvent(event.getId(), new Email("1@ya.ru"), Decision.UNDECIDED, false);
        testManager.addExternalUserParticipantToEvent(event.getId(), new Email("2@ya.ru"), Decision.UNDECIDED, false);

        EventData data = new EventData();
        data.getEvent().setId(event.getId());
        data.setInvData(Option.of(finalOrganizer.getEmail()), new Email("2@ya.ru"), new Email("3@ya.ru"));

        eventWebManager.update(organizer.getUserInfo(), data, false, ActionInfo.webTest());
    }

    // CAL-7296
    @Test
    @WantsEws
    public void mailToAllSingle() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(1776);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(1777);
        TestUserInfo unwanted = testManager.prepareRandomYaTeamUser(1778);
        TestUserInfo newbie = testManager.prepareRandomYaTeamUser(1779);

        setIsEwserIfNeeded(organizer);

        Event event = testManager.createDefaultEventWithDailyRepetition(organizer.getUid(), "633");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), unwanted.getUid(), Decision.YES, false);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        EventData data = new EventData();
        data.getEvent().setId(event.getId());
        data.setInvData(newbie.getEmail(), attendee.getEmail());

        setIsExportedWithEwsIfNeeded(event);

        MailSenderMock senderMock = mailSenderMock;
        senderMock.clear();

        eventWebManager.update(
                organizer.getUserInfo(), data, NotificationsData.notChanged(),
                Option.empty(), Option.empty(),
                false, Option.of(true), ActionInfo.webTest());

        if (isEwser(organizer)) {
            Assert.hasSize(1, senderMock.getEventMessageParameters());
        } else {
            Assert.hasSize(4, senderMock.getEventMessageParameters());
            Assert.some(MailType.EVENT_UPDATE, senderMock.findEventMailType(event.getId(), organizer.getEmail()));
            Assert.some(MailType.EVENT_UPDATE, senderMock.findEventMailType(event.getId(), attendee.getEmail()));
            Assert.some(MailType.EVENT_INVITATION, senderMock.findEventMailType(event.getId(), newbie.getEmail()));
            Assert.some(MailType.EVENT_CANCEL, senderMock.findEventMailType(event.getId(), unwanted.getEmail()));
        }
    }

    // CAL-7296
    @Test
    @WantsEws
    public void mailToInvitedAndRemovedRepeating() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(1776);
        TestUserInfo masterAttendee = testManager.prepareRandomYaTeamUser(1777);
        TestUserInfo unwanted = testManager.prepareRandomYaTeamUser(1778);
        TestUserInfo newbie = testManager.prepareRandomYaTeamUser(1779);

        setIsEwserIfNeeded(organizer);

        Event master = testManager.createDefaultEventWithDailyRepetition(organizer.getUid(), "633");
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(master.getId(), unwanted.getUid(), Decision.YES, false);
        testManager.addUserParticipantToEvent(master.getId(), masterAttendee.getUid(), Decision.YES, false);

        Event recurrence = testManager.createDefaultRecurrence(
                organizer.getUid(), master.getId(), master.getStartTs().plus(Duration.standardDays(2)));
        testManager.addUserParticipantToEvent(recurrence.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(recurrence.getId(), unwanted.getUid(), Decision.YES, false);

        EventData data = new EventData();
        data.getEvent().setId(master.getId());
        data.setInvData(newbie.getEmail(), masterAttendee.getEmail());

        data.getRepetition().setREach(1);
        data.getRepetition().setType(RegularRepetitionRule.DAILY);
        data.setInstanceStartTs(master.getStartTs().plus(Duration.standardDays(1)));

        setIsExportedWithEwsIfNeeded(master);

        MailSenderMock senderMock = mailSenderMock;
        senderMock.clear();

        ModificationInfo info = eventWebManager.update(
                organizer.getUserInfo(), data,
                NotificationsData.notChanged(),
                Option.empty(), Option.empty(),
                true, Option.of(false), ActionInfo.webTest(data.getInstanceStartTs().get()));

        Assert.none(info.getNewEvent());

        EventParticipants masterPs = eventDbManager.getParticipantsByEventIds(Cf.list(master.getId())).single();
        EventParticipants recurrencePs = eventDbManager.getParticipantsByEventIds(Cf.list(recurrence.getId())).single();

        Assert.equals(
                Cf.set(organizer.getParticipantId(), masterAttendee.getParticipantId(), newbie.getParticipantId()),
                masterPs.getParticipants().getParticipantIds().unique());
        Assert.equals(
                Cf.set(organizer.getParticipantId(), newbie.getParticipantId()),
                recurrencePs.getParticipants().getParticipantIds().unique());

        if (isEwser(organizer)) {
            Assert.hasSize(1, senderMock.getEventMessageParameters());
        } else {
            Assert.hasSize(3, senderMock.getEventMessageParameters());
            Assert.some(MailType.EVENT_UPDATE, senderMock.findEventMailType(master.getId(), organizer.getEmail()));
            Assert.none(senderMock.findEventMailType(master.getId(), masterAttendee.getEmail()));
            Assert.some(MailType.EVENT_INVITATION, senderMock.findEventMailType(master.getId(), newbie.getEmail()));
            Assert.some(MailType.EVENT_CANCEL, senderMock.findEventMailType(master.getId(), unwanted.getEmail()));

            Assert.none(senderMock.findEventMailType(recurrence.getId(), organizer.getEmail()));
            Assert.none(senderMock.findEventMailType(recurrence.getId(), masterAttendee.getEmail()));
            Assert.none(senderMock.findEventMailType(recurrence.getId(), unwanted.getEmail()));
            Assert.none(senderMock.findEventMailType(recurrence.getId(), newbie.getEmail()));
        }
    }

    @Test
    public void unchangedRecurrence() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(1776);
        ListF<Notification> notifications = Cf.list(Notification.email(Duration.ZERO));

        Event master = testManager.createEventWithDailyRepetition(creator.getUid());
        testManager.createEventUser(creator.getUid(), master.getId(), Decision.YES, Option.empty());
        testManager.saveEventNotifications(creator.getUid(), master.getId(), notifications.toArray(Notification.class));

        EventData data = new EventData();
        data.setEvent(master.copy());

        data.setInstanceStartTs(master.getStartTs().plus(Duration.standardDays(3)));
        data.getEvent().setStartTs(master.getStartTs().plus(Duration.standardDays(3)));
        data.getEvent().setEndTs(master.getEndTs().plus(Duration.standardDays(3)));
        data.setEventUserData(new EventUserData(new EventUser(), notifications));

        data.setRepetition(TestManager.createDailyRepetitionTemplate());

        Assert.none(eventWebManager.update(creator.getUserInfo(), data, false, ActionInfo.webTest()));
    }

    @Test
    public void attachSingleOccurrence() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(1776);
        TestUserInfo attacher = testManager.prepareRandomYaTeamUser(1777);

        Event master = testManager.createEventWithDailyRepetition(creator.getUid());
        testManager.openEventAndLayer(master.getId(), creator.getDefaultLayerId());

        Function<Duration, EventData> createData = offset -> {
            EventData data = new EventData();
            data.setEvent(master.copy());

            data.setInstanceStartTs(master.getStartTs().plus(offset));
            data.getEvent().setStartTs(master.getStartTs().plus(offset));
            data.getEvent().setEndTs(master.getEndTs().plus(offset));

            return data;
        };

        EventData data = createData.apply(Duration.standardDays(3));

        Option<Long> recurrenceId = eventWebManager.update(attacher.getUserInfo(), data, false, ActionInfo.webTest());
        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(recurrenceId.get(), attacher.getDefaultLayerId()));

        data = createData.apply(Duration.standardDays(5));
        data.setLayerId(attacher.getDefaultLayerId());

        recurrenceId = eventWebManager.update(attacher.getUserInfo(), data, false, ActionInfo.webTest());
        Assert.some(eventLayerDao.findEventLayerByEventIdAndLayerId(recurrenceId.get(), attacher.getDefaultLayerId()));

        Assert.none(eventLayerDao.findEventLayerByEventIdAndLayerId(master.getId(), attacher.getDefaultLayerId()));
    }

    @Test
    public void updateEventWithResourceAndLocation() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(1778);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(1779);

        Resource resource = testManager.cleanAndCreateResourceWithNoExchSync(
                "resource", "Resource", ResourceType.ROOM);

        Event event = testManager.createDefaultEvent(organizer.getUid(), "eventWithResourceAndLocation");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);
        testManager.addResourceParticipantToEvent(event.getId(), resource);

        EventData data = new EventData();
        data.getEvent().setId(event.getId());

        data.getEvent().setLocation("Location");
        data.setInvData(ResourceRoutines.getResourceEmail(resource), organizer.getEmail());

        mailSenderMock.clear();

        ModificationInfo updated = passportAuthDomainsHolder.withDomainsForTest("public",
                () -> eventWebManager.update(organizer.getUserInfo(), data,
                        Option.empty(), false, ActionInfo.webTest()));

        Assert.some(data.getEvent().getLocation(), updated.getUpdatedEvent().map(e -> e.getEvent().getLocation()));

        EventLocation location = mailSenderMock.getEventMessageParameters().single().getEventMessageInfo().getLocation();
        Assert.equals("Resource, Location", location.asTextForMailSubject(Language.RUSSIAN));
    }

    public Option<EventUser> findEventUser(Event event, TestUserInfo user) {
        return eventUserDao.findEventUserByEventIdAndUid(event.getId(), user.getUid());
    }

    public Option<EventLayer> findEventLayer(Event event, TestUserInfo user) {
        return eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(event.getId(), user.getUid());
    }
}
