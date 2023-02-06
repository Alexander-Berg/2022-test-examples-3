package ru.yandex.calendar.logic.event.web;

import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.CollectorsF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.logic.beans.generated.*;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationUpdateData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.model.EventUserData;
import ru.yandex.calendar.logic.layer.LayerDao;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.Channel;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.resource.schedule.ResourceDaySchedule;
import ru.yandex.calendar.logic.resource.schedule.ResourceScheduleManager;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.participant.ParticipantData;
import ru.yandex.calendar.logic.sharing.participant.ParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.ParticipantsData;
import ru.yandex.calendar.logic.sharing.perm.EventActionClass;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestStatusChecker;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author gutman
 */
public class EventWebUpdaterTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private TestStatusChecker testStatusChecker;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventWebUpdater eventWebUpdater;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private LayerDao layerDao;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private UserManager userManager;
    @Autowired
    private ResourceScheduleManager resourceScheduleManager;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private NotificationDbManager notificationDbManager;

    @Test
    public void participantUpdatesEventFieldsAndSucceeds() {
        participantUpdatesEventFields(true);
    }

    @Test
    public void participantUpdatesEventFieldsAndFails() {
        participantUpdatesEventFields(false);
    }

    private void participantUpdatesEventFields(boolean isAllowed) {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-11501");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11502");

        EventData eventData = createEventData(creator, Cf.list(attendee), "participant updates event fields");

        if (isAllowed) {
            eventData.getEvent().setPermParticipants(EventActionClass.EDIT);
        } else {
            eventData.getEvent().setPermParticipants(EventActionClass.VIEW);
        }

        CreateInfo info = createEvent(creator, eventData);

        eventData.getEvent().setId(info.getEvent().getId());
        EventLayer attendeeEventLayer = eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(info.getEvent().getId(), attendee.getUid()).get();
        eventData.setLayerId(attendeeEventLayer.getLayerId());

        eventData.getEvent().setName("updated by participant");
        eventData.getEvent().setStartTs(eventData.getEvent().getStartTs().plus(Duration.standardHours(1)));
        eventData.getEvent().setEndTs(eventData.getEvent().getEndTs().plus(Duration.standardHours(1)));

        ActionInfo actionInfo = ActionInfo.webTest();
        eventWebUpdater.update(attendee.getUserInfo(), eventData, NotificationsData.notChanged(), true, actionInfo);

        Event updated = eventDao.findEventById(info.getEvent().getId());

        if (isAllowed) {
            Assert.A.equals(eventData.getEvent().getName(), updated.getName());
            Assert.A.equals(eventData.getEvent().getStartTs(), updated.getStartTs());
            Assert.A.equals(eventData.getEvent().getEndTs(), updated.getEndTs());

            testStatusChecker.checkForAttendeeOnWebUpdateOrDelete(attendee.getUid(), info.getEvent(), actionInfo, isAllowed);
        } else {
            Assert.A.notEquals(eventData.getEvent().getName(), updated.getName());
            Assert.A.notEquals(eventData.getEvent().getStartTs(), updated.getStartTs());
            Assert.A.notEquals(eventData.getEvent().getEndTs(), updated.getEndTs());

            // should user version fields change?
        }

    }

    @Test
    public void participantUpdatesParticipantsAndSucceeds() {
        participantUpdatesParticipants(true);
    }

    @Test
    public void participantUpdatesParticipantsAndFails() {
        participantUpdatesParticipants(false);
    }

    private void participantUpdatesParticipants(boolean isAllowed) {
        val creator = testManager.prepareUser("yandex-team-mm-11511");
        val attendee = testManager.prepareUser("yandex-team-mm-11512");
        val attendeeToBeRemoved = testManager.prepareUser("yandex-team-mm-11513");
        val attendeeToBeInvited = testManager.prepareUser("yandex-team-mm-11514");

        val eventData = createEventData(creator, Cf.list(attendee, attendeeToBeRemoved), "participant updates participants");

        eventData.getEvent().setPermParticipants(EventActionClass.VIEW);
        eventData.getEvent().setParticipantsInvite(isAllowed);

        val info = createEvent(creator, eventData);

        eventData.getEvent().setId(info.getEvent().getId());

        eventData.setInvData(new EventInvitationUpdateData(
                Cf.list(attendeeToBeInvited.getEmail()), Cf.list(attendeeToBeRemoved.getEmail())));

        val attendeeEventLayer = eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(info.getEvent().getId(), attendee.getUid()).get();
        eventData.setLayerId(attendeeEventLayer.getLayerId());

        eventWebUpdater.update(attendee.getUserInfo(), eventData,
                NotificationsData.notChanged(), true, ActionInfo.webTest());

        val p = eventInvitationManager.getParticipantsByEventId(info.getEvent().getId());
        val attendeeUids = p.getAllAttendees().flatMap(ParticipantInfo.getUidF());

        if (isAllowed) {
            assertThat(attendeeUids).contains(attendeeToBeInvited.getUid(), attendeeToBeRemoved.getUid());
        } else {
            assertThat(attendeeUids).contains(attendeeToBeRemoved.getUid());
            assertThat(attendeeUids).doesNotContain(attendeeToBeInvited.getUid());
        }
    }

    @Test
    public void participantUpdatesHisOwnFields() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-11521");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-11522");

        EventData eventData = createEventData(creator, Cf.list(attendee), "participant updates his own fields");

        CreateInfo info = createEvent(creator, eventData);

        long createdEventId = info.getEvent().getId();
        eventData.getEvent().setId(createdEventId);

        EventUser eventUser = eventUserDao.findEventUserByEventIdAndUid(createdEventId, attendee.getUid()).get().copy();

        eventUser.setDecision(Decision.NO);
        ListF<Notification> notification = Cf.list(Notification.sms(Duration.standardMinutes(-99)));

        eventUser.unsetField(EventUserFields.ID);
        eventUser.unsetField(EventUserFields.EVENT_ID);

        eventData.setEventUserData(new EventUserData(eventUser, notification));

        EventLayer attendeeEventLayer = eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(createdEventId, attendee.getUid()).get();
        eventData.setLayerId(attendeeEventLayer.getLayerId());

        ActionInfo actionInfo = ActionInfo.webTest();
        eventWebUpdater.update(
                attendee.getUserInfo(), eventData, NotificationsData.updateFromWeb(notification), true, actionInfo);

        EventUser eventUserUpdated = eventUserDao.findEventUserByEventIdAndUid(createdEventId, attendee.getUid()).get();
        ListF<Notification> notificationUpdated = notificationDbManager
                .getNotificationsByEventUserId(eventUserUpdated.getId())
                .getNotifications().filterNot(n -> n.channelIs(Channel.PANEL));

        Assert.equals(Decision.NO, eventUserUpdated.getDecision());
        Assert.equals(Duration.standardMinutes(-99), notificationUpdated.single().getOffset());

        testStatusChecker.checkForAttendeeOnWebUpdateOrDelete(attendee.getUid(), info.getEvent(), actionInfo, false);
    }

    // https://jira.yandex-team.ru/browse/CAL-2780
    // TODO also need 'layerViewerCreatesEventWithNotificationInLayerSharedForEdit' test
    @Test
    public void layerViewerUpdatesHisNotification() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-11531");
        TestUserInfo layerViewer = testManager.prepareUser("yandex-team-mm-11532");
        PassportUid layerViewerUid = layerViewer.getUid();

        long creatorLayerId = layerRoutines.getFirstUserLayerId(creator.getUid(), Option.<String>empty()).get();
        layerRoutines.updateLayerEventsClosedByDefault(creatorLayerId, false);

        EventData eventData = createEventData(creator, Cf.<TestUserInfo>list(), "layer viewer updates his own fields");

        CreateInfo info = createEvent(creator, eventData);
        long eventId = info.getEvent().getId();
        eventData.getEvent().setId(eventId);

        boolean isShared = layerRoutines.startNewSharing(layerViewerUid, creatorLayerId, LayerActionClass.ACCESS);
        Assert.A.isTrue(isShared);


        Option<EventUser> layerViewerEventUserO = eventUserDao.findEventUserByEventIdAndUid(eventId, layerViewerUid);
        Assert.assertNone(layerViewerEventUserO);

        Option<EventLayer> layerViewerEventLayerO = eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(eventId, layerViewerUid);
        Assert.assertNone(layerViewerEventLayerO);


        ListF<Notification> notification = Cf.list(Notification.sms(Duration.standardMinutes(-120)));

        EventUser eventUser = new EventUser();

        eventData.setEventUserData(new EventUserData(eventUser, notification));


        eventWebUpdater.update(layerViewer.getUserInfo(), eventData, NotificationsData.updateFromWeb(notification),
                false, ActionInfo.webTest());

        Option<EventUser> layerViewerEventUserUpdatedO = eventUserDao.findEventUserByEventIdAndUid(eventId, layerViewerUid);
        Assert.assertSome(layerViewerEventUserUpdatedO);

        ListF<Notification> notificationUpdated = notificationDbManager
                .getNotificationsByEventUserId(layerViewerEventUserUpdatedO.get().getId())
                .getNotifications().filterNot(n -> n.channelIs(Channel.PANEL));
        Assert.equals(Duration.standardMinutes(-120), notificationUpdated.single().getOffset());
    }

    @Test
    public void organizerChangesEventToMeetingWithParticipantsFlagsCheck() {
        UserInfo user1 = testManager.prepareUser("yandex-team-mm-11541").getUserInfo();
        PassportUid uid1 = user1.getUid();
        PassportUid uid2 = testManager.prepareUser("yandex-team-mm-11542").getUid();

        final String oldEventName = "Event => Meeting";
        Event oldEventDataWithId = testManager.createDefaultEventWithEventLayerAndEventUser(uid1, oldEventName);
        final long eventId = oldEventDataWithId.getId();

        final Event oldEvent = eventDao.findEventById(eventId);
        { // check step 0 - just event
            Assert.A.equals(oldEventName, oldEvent.getName());
            Assert.assertTrue(eventInvitationManager.getParticipantsByEventId(eventId).isNotMeetingOrIsInconsistent());

            final EventUser orgEventUser = eventUserDao.findEventUserByEventIdAndUid(eventId, uid1).get();
            Assert.assertFalse(orgEventUser.getIsOrganizer());
            Assert.assertFalse(orgEventUser.getIsAttendee());
        }

        EventData newEventDataWithId = new EventData();
        newEventDataWithId.setEvent(oldEventDataWithId);
        newEventDataWithId.setLayerId(layerRoutines.getDefaultLayerId(uid1).get());
        val emails = StreamEx.of(userManager.getEmailByUid(uid2))
            .collect(CollectorsF.toList());
        newEventDataWithId.setInvData(new EventInvitationUpdateData(emails, Cf.list()));
        newEventDataWithId.setTimeZone(MoscowTime.TZ);

        ActionInfo actionInfo = ActionInfo.webTest();
        eventWebUpdater.update(user1, newEventDataWithId, NotificationsData.notChanged(), true, actionInfo);

        { // check step 1 - meeting now
            Assert.assertTrue(eventInvitationManager.getParticipantsByEventId(eventId).isMeeting());

            final EventUser orgEventUser = eventUserDao.findEventUserByEventIdAndUid(eventId, uid1).get();
            Assert.assertTrue(orgEventUser.getIsOrganizer());
            Assert.assertTrue(orgEventUser.getIsAttendee());

            final EventUser attEventUser = eventUserDao.findEventUserByEventIdAndUid(eventId, uid2).get();
            Assert.assertFalse(attEventUser.getIsOrganizer());
            Assert.assertTrue(attEventUser.getIsAttendee());
        }
    }

    @Test
    public void resourceScheduleCacheInvalidatedOnUpdate() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(11551);
        Resource smolny = testManager.cleanAndCreateSmolny();

        Event event = testManager.createDefaultEvent(user.getUid(), "resourceScheduleCacheInvalidatedOnUpdate");
        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(event.getId(), smolny);

        DateTimeZone tz = DateTimeZone.forID(settingsRoutines.getTimeZoneJavaId(user.getUid()));

        // put to cache
        resourceScheduleManager.getResourceScheduleDataForDays(
                Option.empty(), Cf.list(smolny.getId()),
                Cf.list(event.getStartTs().toDateTime(tz).toLocalDate()), tz, Option.empty(), ActionInfo.webTest());


        EventData eventData = new EventData();
        eventData.setEvent(event.clone());
        eventData.getEvent().setStartTs(event.getStartTs().plus(Duration.standardHours(1)));
        eventData.getEvent().setEndTs(event.getEndTs().plus(Duration.standardHours(1)));
        eventData.setLayerId(layerRoutines.getDefaultLayerId(user.getUid()).get());
        eventData.setInvData(new EventInvitationUpdateData(Cf.<Email>list(), Cf.<Email>list()));
        eventData.setInstanceStartTs(event.getStartTs());
        eventData.setTimeZone(MoscowTime.TZ);

        ActionInfo actionInfo = ActionInfo.webTest();
        eventWebUpdater.update(user.getUserInfo(), eventData, NotificationsData.notChanged(), true, actionInfo);

        ListF<ResourceDaySchedule> schedules = resourceScheduleManager.getResourceScheduleDataForDays(
                Option.empty(), Cf.list(smolny.getId()),
                Cf.list(event.getStartTs().toDateTime(tz).toLocalDate()), tz, Option.empty(), ActionInfo.webTest());

        ResourceDaySchedule smolnySchedule = schedules.single();

        InstantInterval eventInSmolny = smolnySchedule.getSchedule().getInstantIntervals().single();

        Assert.A.equals(eventData.getEvent().getStartTs(), eventInSmolny.getStart());
        Assert.A.equals(eventData.getEvent().getEndTs(), eventInSmolny.getEnd());
    }


    private CreateInfo createEvent(TestUserInfo creator, EventData eventData) {
        return eventRoutines.createUserOrFeedEvent(
                UidOrResourceId.user(creator.getUid()), EventType.USER,
                eventRoutines.createMainEvent(creator.getUid(), eventData, ActionInfo.webTest()), eventData,
                NotificationsData.create(eventData.getEventUserData().getNotifications()),
                InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest()
        );
    }

    private EventData createEventData(TestUserInfo creator, ListF<TestUserInfo> attendees, String eventName) {
        EventData eventData = testManager.createDefaultEventData(creator.getUid(), eventName);
        eventData.getEvent().setStartTs(new DateTime(2011, 2, 6, 1, 0, 0, 0, DateTimeZone.UTC).toInstant());
        eventData.getEvent().setEndTs(new DateTime(2011, 2, 6, 2, 0, 0, 0, DateTimeZone.UTC).toInstant()) ;

        ParticipantsData participants;
        if (attendees.isEmpty()) {
            participants = ParticipantsData.notMeeting();
        } else {
            ListF<ParticipantData> attendeesData = attendees.map(new Function<TestUserInfo, ParticipantData>() {
                public ParticipantData apply(TestUserInfo userInfo) {
                    return new ParticipantData(userInfo.getEmail(), userInfo.getLogin().getNormalizedValue(), Decision.YES, true, false, false);
                }
            });

            participants = ParticipantsData.merge(
                new ParticipantData(creator.getEmail(), "creator", Decision.YES, true, true, false), attendeesData);
        }
        eventData.setInvData(participants);

        return eventData;
    }
}
