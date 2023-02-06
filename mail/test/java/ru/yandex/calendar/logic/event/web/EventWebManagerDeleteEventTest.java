package ru.yandex.calendar.logic.event.web;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple4;
import ru.yandex.calendar.frontend.web.cmd.run.PermissionDeniedUserException;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventHelper;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Rdate;
import ru.yandex.calendar.logic.beans.generated.RdateHelper;
import ru.yandex.calendar.logic.beans.generated.Repetition;
import ru.yandex.calendar.logic.beans.generated.RepetitionHelper;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.ModificationInfo;
import ru.yandex.calendar.logic.event.archive.DeletedEventDao;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.SendingSmsDao;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestStatusChecker;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.db.q.SqlCondition;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

public class EventWebManagerDeleteEventTest extends AbstractConfTest {
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private GenericBeanDao genericBeanDao;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private DeletedEventDao deletedEventDao;
    @Autowired
    private TestManager testManager;
    @Autowired
    private TestStatusChecker testStatusChecker;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private SendingSmsDao sendingSmsDao;

    @Test
    public void deleteSingleEvent() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10831").getUserInfo();
        PassportUid uid = user.getUid();

        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(uid, "deleteSingleEvent");
        ListF<Long> eIds = eventWebManager.deleteUserEvent(
                user, event.getId(), Option.of(event.getStartTs()),
                false, ActionInfo.webTest()
        ).getEventIds();
        Assert.A.equals(Cf.set(event.getId()), eIds.unique(), "Incorrect ids of changed events");
        int count = jdbcTemplate.queryForInt("SELECT COUNT(id) FROM event WHERE id = ?", event.getId());
        Assert.A.equals(0, count, "Event wasn't deleted correctly");
    }

    // Delete rdate instance of event
    @Test
    public void deleteEventWithRdate() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10841").getUserInfo();
        PassportUid uid = user.getUid();

        Tuple2<Event, Rdate> event = testManager.createEventWithRdate(uid);
        Event e = genericBeanDao.loadBeanById(EventHelper.INSTANCE, event._1.getId());
        ListF<Long> eIds = eventWebManager.deleteUserEvent(
                user, event._1.getId(), Option.of(event._2.getStartTs()),
                false, ActionInfo.webTest()
        ).getEventIds();
        Assert.A.equals(Cf.set(event._1.getId()), eIds.unique(), "Incorrect ids of changed events");
        e = genericBeanDao.loadBeanById(EventHelper.INSTANCE, event._1.getId());
        Assert.A.equals(1, e.getSequence(), "incorrect sequence value");
        Rdate rdate = genericBeanDao.findBean(RdateHelper.INSTANCE, SqlCondition.condition("event_id = ? AND is_rdate = ?", 1, true)).getOrNull();
        Assert.assertNull(rdate);
        rdate = genericBeanDao.findBean(RdateHelper.INSTANCE, SqlCondition.condition("event_id = ? AND is_rdate = ?", 1, false)).getOrNull();
        Assert.assertNull(rdate);
    }

    // Delete repetition instance of event
    @Test
    public void deleteEventWithRepetition() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10851").getUserInfo();
        PassportUid uid = user.getUid();

        Event event = testManager.createEventWithDailyRepetition(uid);
        Instant startTs = new DateTime(2009, 4, 28, 10, 0, 0, 0, TimeUtils.EUROPE_MOSCOW_TIME_ZONE).toInstant();
        ListF<Long> eIds = eventWebManager.deleteUserEvent(
                user, event.getId(), Option.of(startTs),
                false, ActionInfo.webTest()
        ).getEventIds();
        Assert.A.equals(Cf.set(event.getId()), eIds.unique(), "Incorrect ids of changed events");
        final SqlCondition condition = SqlCondition.condition("event_id = ? AND is_rdate = ?", event.getId(), false);
        Rdate rdate = genericBeanDao.findBean(RdateHelper.INSTANCE, condition).getOrNull();
        Assert.assertNotNull(rdate);
        Assert.A.equals(startTs, rdate.getStartTs());
    }

    @Test
    public void deleteEventWithRepetition2() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10861").getUserInfo();
        PassportUid uid = user.getUid();

        int beforeTest = eventDao.findEventCount();

        // Delete main event and future
        Event event = testManager.createEventWithDailyRepetition(uid);
        Instant startTs = event.getStartTs();
        eventWebManager.deleteUserEvent(
                user, event.getId(), Option.of(startTs),
                true, ActionInfo.webTest()
        );

        Assert.assertEquals(beforeTest, eventDao.findEventCount());
    }

    // Delete recurrence instance of event
    @Test
    public void deleteRecurEventAndFuture() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10871").getUserInfo();
        PassportUid uid = user.getUid();

        Tuple2<Event, Event> events = testManager.createEventWithRepetitionAndRecurrence(uid);
        ListF<Long> eIds = eventWebManager.deleteUserEvent(
                user, events._2.getId(), Option.of(events._2.getStartTs()),
                true, ActionInfo.webTest()
        ).getEventIds();
        Assert.hasSize(2, eIds.unique(), "Incorrect ids of changed events");

        Assert.none(genericBeanDao.findBean(EventHelper.INSTANCE, SqlCondition.condition("id = ?", events._1.getId())));
        Assert.none(genericBeanDao.findBean(EventHelper.INSTANCE, SqlCondition.condition("id = ?", events._2.getId())));

        Rdate rdate = genericBeanDao.findBean(RdateHelper.INSTANCE, SqlCondition.condition("event_id = ? AND is_rdate = ? AND start_ts = ?", events._1.getId(), false, TestManager.recurrenceId)).getOrNull();
        Assert.isNull(rdate);
    }

    // Delete recurrence instance of event
    @Test
    public void deleteRecurEvent() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10881").getUserInfo();
        PassportUid uid = user.getUid();
        Tuple4<Event, Long, Rdate, Event> t = testManager.createEventWithRepetitionAndRdateAndRecurrence(uid);
        Event masterEvent = t._1;
        long repetitionId = t._2;
        Event recurrenceEvent = t._4;

        ListF<Long> eIds = eventWebManager.deleteUserEvent(
                user, recurrenceEvent.getId(), Option.of(recurrenceEvent.getStartTs()),
                false, ActionInfo.webTest()
        ).getEventIds();
        Assert.A.equals(Cf.set(recurrenceEvent.getId()), eIds.unique(), "Incorrect ids of changed events");
        Event e = genericBeanDao.findBean(EventHelper.INSTANCE, SqlCondition.condition("id = ?", recurrenceEvent.getId())).getOrNull();
        Assert.assertNull(e);
        e = genericBeanDao.findBean(EventHelper.INSTANCE, SqlCondition.condition("id = ?", masterEvent.getId())).getOrNull();
        Assert.assertNotNull(e);
        Assert.A.equals(1, e.getSequence(), "incorrect sequence value");
        Repetition r = genericBeanDao.findBean(RepetitionHelper.INSTANCE, SqlCondition.condition("id = ?", repetitionId)).getOrNull();
        Assert.assertNotNull(r);
        Assert.assertNull(r.getDueTs().getOrNull());
        final SqlCondition condition = SqlCondition.condition("event_id = ? AND is_rdate = ?", masterEvent.getId(), true);
        Rdate rdate = genericBeanDao.findBean(RdateHelper.INSTANCE, condition).getOrNull();
        Assert.assertNotNull(rdate);
    }

    // Delete main instance of repeated event with recur event and future
    @Test
    public void deleteMainInstAndFuture() throws Exception {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10891").getUserInfo();
        PassportUid uid = user.getUid();
        Tuple4<Event, Long, Rdate, Event> t = testManager.createEventWithRepetitionAndRdateAndRecurrence(uid);

        long mainEventId = t._1.getId();
        long repetitionId = t._2;
        long recurrenceEventId = t._4.getId();

        ListF<Long> eIds = eventWebManager.deleteUserEvent(
                user, mainEventId, Option.of(t._1.getStartTs()),
                true, ActionInfo.webTest()
        ).getEventIds();
        Assert.A.equals(Cf.set(mainEventId, recurrenceEventId), eIds.unique(), "Incorrect ids of changed events");

        Assert.A.hasSize(0, eventDao.findEvents(SqlCondition.condition("id = ?", recurrenceEventId)));
        Assert.A.hasSize(0, eventDao.findEvents(SqlCondition.condition("id = ?", mainEventId)));

        Repetition r = genericBeanDao.findBean(RepetitionHelper.INSTANCE, SqlCondition.condition("id = ?", repetitionId)).getOrNull();
        Assert.assertNull(r);
        Rdate rdate = genericBeanDao.findBean(RdateHelper.INSTANCE, SqlCondition.condition("event_id = ? AND is_rdate = ?", 1, true)).getOrNull();
        Assert.assertNull(rdate);
    }

    @Test
    public void deleteDeletesMainEventAndAddsDeletedEvent() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10801");

        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(user.getUid(), "deleteDeletesMainEvent");

        ModificationInfo stats = eventWebManager.deleteEvent(
                user.getUserInfo(), event.getId(), Option.<Instant>empty(), true, ActionInfo.webTest());
        Assert.A.hasSize(1, stats.getEventIds());

        Assert.A.hasSize(1, deletedEventDao.findDeletedEventById(event.getId()));

        try {
            mainEventDao.findMainEventById(event.getMainEventId());
            Assert.fail();
        } catch (EmptyResultDataAccessException e) {
            // expected
        }
    }

    @Test
    public void attendeeCanNotDeleteEvent() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-10811");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10812");

        Event event = testManager.createDefaultEvent(creator.getUid(), "attendeeCanNotDeleteEvent");

        testManager.addUserParticipantToEvent(event.getId(), creator.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        EventUser oldAttendeeEventUser = eventUserDao.findEventUserByEventIdAndUid(event.getId(), attendee.getUid()).get();

        try {
            eventWebManager.deleteEvent(
                    attendee.getUserInfo(), event.getId(), Option.<Instant>empty(), false, ActionInfo.webTest());
            Assert.A.fail("attendee deleted event");
        } catch (PermissionDeniedUserException e) {
        }

        testStatusChecker.checkUserSequenceAndDtStampArePreserved(attendee.getUid(), event.getId(), oldAttendeeEventUser);
    }

    @Test
    public void nonattendeeCanNotDeleteEvent() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-10821");
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10822");

        Event event = testManager.createDefaultEvent(creator.getUid(), "nonattendeeCanNotDeleteEvent");

        testManager.addUserParticipantToEvent(event.getId(), creator.getUid(), Decision.YES, true);

        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.NO, false);
        eventUserDao.updateEventUserIsAttendeeByEventIdAndUserId(event.getId(), user.getUid(), false, ActionInfo.webTest());

        try {
            eventWebManager.deleteEvent(
                    user.getUserInfo(), event.getId(), Option.<Instant>empty(), false, ActionInfo.webTest());
            Assert.A.fail("non attendee deleted event");
        } catch (PermissionDeniedUserException e) {
        }
    }

    @Test
    public void deleteRecurrenceById() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10838");

        Instant start = MoscowTime.instant(2012, 7, 31, 13, 49);
        Instant recurrenceId = start.plus(Duration.standardDays(2));

        long masterEventId = testManager.createDefaultEvent(user.getUid(), "deleteRecurrenceById", start).getId();
        testManager.addUserParticipantToEvent(masterEventId, user.getUid(), Decision.YES, true);
        testManager.createDailyRepetitionAndLinkToEvent(masterEventId);

        long recurrenceEventId = testManager.createDefaultRecurrence(user.getUid(), masterEventId, recurrenceId).getId();
        testManager.addUserParticipantToEvent(recurrenceEventId, user.getUid(), Decision.YES, true);

        Option<Instant> instanceStart = Option.empty();
        eventWebManager.deleteEvent(user.getUserInfo(), recurrenceEventId, instanceStart, false, ActionInfo.webTest());

        Assert.notEmpty(eventDao.findEventsByIdsSafe(Cf.list(masterEventId)));
        Assert.isEmpty(eventDao.findEventsByIdsSafe(Cf.list(recurrenceEventId)));
    }

    @Test
    public void deleteFirstInstanceIsRecurrenceAndFuture() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-10839");

        Instant start = MoscowTime.instant(2012, 7, 31, 13, 49);

        long masterEventId = testManager.createDefaultEvent(user.getUid(), "deleteFirstRecurrence", start).getId();
        testManager.addUserParticipantToEvent(masterEventId, user.getUid(), Decision.YES, false);
        testManager.createDailyRepetitionAndLinkToEvent(masterEventId);

        long recurrenceEventId = testManager.createDefaultRecurrence(user.getUid(), masterEventId, start).getId();
        testManager.addUserParticipantToEvent(recurrenceEventId, user.getUid(), Decision.YES, false);

        Option<Instant> instanceStart = Option.empty();
        eventWebManager.deleteEvent(user.getUserInfo(), recurrenceEventId, instanceStart, true, ActionInfo.webTest());

        Assert.isEmpty(eventDao.findEventsByIdsSafe(Cf.list(masterEventId)));
        Assert.isEmpty(eventDao.findEventsByIdsSafe(Cf.list(recurrenceEventId)));
    }

    @Test
    public void singleEventDeletionSms() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10840");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10841");

        Event event = testManager.createDefaultEvent(organizer.getUid(), "singleEventDeletionSms");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        testManager.saveEventNotifications(attendee.getUid(), event.getId(), Notification.sms(Duration.ZERO));
        sendingSmsDao.deleteByUid(attendee.getUid());

        eventWebManager.deleteEvent(organizer.getUserInfo(), event.getId(),
                Option.<Instant>empty(), false, ActionInfo.webTest(event.getStartTs().minus(88888)));
        Assert.hasSize(1, sendingSmsDao.findByUid(attendee.getUid()));
    }

    @Test
    public void repeatingEventDeletionSms() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10842");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10843");

        Event master = testManager.createDefaultEvent(organizer.getUid(), "repeatingEventDeletionSms");
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());
        testManager.addUserParticipantToEvent(master.getId(), organizer.getUid(), Decision.YES, true);

        testManager.addUserParticipantToEvent(master.getId(), attendee.getUid(), Decision.MAYBE, false);
        testManager.saveEventNotifications(attendee.getUid(), master.getId(), Notification.sms(Duration.ZERO));

        Instant instanceTailStart = master.getStartTs().plus(Duration.standardDays(4));
        Instant recurrenceSingleStart = master.getStartTs().plus(Duration.standardDays(2));
        Instant instanceSingleStart = master.getStartTs().plus(Duration.standardDays(1));

        Event recurrenceSingle = testManager.createDefaultRecurrence(
                organizer.getUid(), master.getId(), recurrenceSingleStart);
        testManager.addUserParticipantToEvent(recurrenceSingle.getId(), organizer.getUid(), Decision.YES, true);

        testManager.addUserParticipantToEvent(recurrenceSingle.getId(), attendee.getUid(), Decision.UNDECIDED, false);
        testManager.saveEventNotifications(attendee.getUid(), recurrenceSingle.getId(), Notification.sms(Duration.ZERO));

        sendingSmsDao.deleteByUid(attendee.getUid());
        eventWebManager.deleteEvent(organizer.getUserInfo(), recurrenceSingle.getId(),
                Option.<Instant>empty(), false, ActionInfo.webTest(recurrenceSingleStart.minus(88888)));
        Assert.hasSize(1, sendingSmsDao.findByUid(attendee.getUid()));

        sendingSmsDao.deleteByUid(attendee.getUid());
        eventWebManager.deleteEvent(organizer.getUserInfo(), master.getId(),
                Option.of(instanceSingleStart), false, ActionInfo.webTest(instanceSingleStart.minus(88888)));
        Assert.hasSize(1, sendingSmsDao.findByUid(attendee.getUid()));

        sendingSmsDao.deleteByUid(attendee.getUid());
        eventWebManager.deleteEvent(organizer.getUserInfo(), master.getId(),
                Option.of(instanceTailStart), true, ActionInfo.webTest(instanceTailStart.minus(88888)));
        Assert.hasSize(1, sendingSmsDao.findByUid(attendee.getUid()));

        Assert.none(genericBeanDao.findBean(EventHelper.INSTANCE, SqlCondition.condition("id = ?", master.getId())));
    }

    @Test
    public void deleteOrphanedRecurrence() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(12334);

        Event master = testManager.createDefaultEvent(creator.getUid(), "deleteOrphanedRecurrence");
        testManager.createDailyRepetitionAndLinkToEvent(master.getId());

        ListF<Event> recurrences = Cf.list(1, 2).map(days -> {
            Event recurrence = testManager.createDefaultRecurrence(
                    creator.getUid(), master.getId(), master.getStartTs().plus(Duration.standardDays(days)));

            testManager.createEventUser(creator.getUid(), recurrence.getId(), Decision.YES, Option.empty());
            testManager.createEventLayer(creator.getDefaultLayerId(), recurrence.getId(), true);

            return recurrence;
        });

        eventRoutines.deleteEvents(Option.of(creator.getUserInfo()), Cf.list(master.getId()),
                InvitationProcessingMode.SAVE_ONLY, ActionInfo.webTest());

        eventWebManager.deleteEvent(
                creator.getUserInfo(), recurrences.first().getId(), Option.empty(), true, ActionInfo.webTest());

        Assert.isEmpty(eventDao.findEventsByIdsSafe(Cf.list(recurrences.first().getId())));
        Assert.notEmpty(eventDao.findEventsByIdsSafe(Cf.list(recurrences.last().getId())));
    }
}
