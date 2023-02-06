package ru.yandex.calendar.logic.event.web;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function0V;
import ru.yandex.calendar.frontend.web.cmd.run.CommandRunException;
import ru.yandex.calendar.frontend.web.cmd.run.PermissionDeniedUserException;
import ru.yandex.calendar.frontend.web.cmd.run.Situation;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.ModificationInfo;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.model.EventUserData;
import ru.yandex.calendar.logic.notification.Channel;
import ru.yandex.calendar.logic.notification.EventUserWithNotifications;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sending.param.ReplyMessageParameters;
import ru.yandex.calendar.logic.sending.real.MailSender;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.participant.ParticipantData;
import ru.yandex.calendar.logic.sharing.participant.ParticipantsData;
import ru.yandex.calendar.logic.sharing.perm.EventActionClass;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractEwsExportedLoginsTest;
import ru.yandex.calendar.util.dates.DateTimeManager;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author Stepan Koltsov
 */
public class EventWebManagerUpdateTest extends AbstractEwsExportedLoginsTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private MailSender mailSender;
    @Autowired
    private NotificationDbManager notificationDbManager;
    @Autowired
    private DateTimeManager dateTimeManager;

    public EventWebManagerUpdateTest(EwsUsage ewsUsage) {
        super(ewsUsage);
    }

    @Before
    public void setMockEws() {
        setMockEwsProxyWrapper();
    }

    @Test
    public void noNewEventIfNotificationChanges() {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10901").getUserInfo();
        PassportUid uid = user.getUid();

        Event event = testManager.createEventWithDailyRepetition(uid);

        Instant instance = event.getStartTs().toDateTime(DateTimeZone.UTC).plusDays(3).toInstant();

        ListF<Notification> notificationData = Cf.list(Notification.email(Duration.standardMinutes(-44)));
        EventUserData eventUserData = new EventUserData(new EventUser(), notificationData);

        EventData eventData = new EventData();
        eventData.setInstanceStartTs(instance);
        eventData.setEventUserData(eventUserData);
        eventData.setRepetition(TestManager.createDailyRepetitionTemplate());
        eventData.setTimeZone(MoscowTime.TZ);

        eventData.getEvent().setId(event.getId());

        Option<Long> newEventId = eventWebManager.update(
                user, eventData, true, ActionInfo.webTest(event.getStartTs().plus(10000)));
        Assert.A.none(newEventId); // <- test is here

        EventUserWithNotifications p = notificationDbManager
                .getEventUsersWithNotificationsByUidAndEventIds(uid, Cf.list(event.getId())).single();
        Assert.equals(notificationData.unique(),
                p.getNotifications().getNotifications().filterNot(n -> n.channelIs(Channel.PANEL)).unique());
    }

    @Test
    public void participantTriesToUpdateEvent() {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-10911");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10912");

        String eventName = "attendee tries to update event";
        EventData eventData = testManager.createDefaultEventData(creator.getUid(), eventName);
        Instant eventStart = new DateTime(2011, 2, 6, 1, 0, 0, 0, DateTimeZone.UTC).toInstant();
        Instant eventEnd = new DateTime(2011, 2, 6, 2, 0, 0, 0, DateTimeZone.UTC).toInstant();
        eventData.getEvent().setStartTs(eventStart);
        eventData.getEvent().setEndTs(eventEnd) ;

        eventData.setInvData(ParticipantsData.merge(
                        new ParticipantData(creator.getEmail(), "creator", Decision.YES, true, true, false),
                        Cf.list(new ParticipantData(attendee.getEmail(), "attendee", Decision.UNDECIDED, true, false, false))));

        eventData.getEvent().setPermParticipants(EventActionClass.VIEW);

        CreateInfo info = eventRoutines.createUserOrFeedEvent(
                UidOrResourceId.user(creator.getUid()), EventType.USER,
                eventRoutines.createMainEvent(creator.getUid(), eventData, ActionInfo.webTest()), eventData,
                NotificationsData.create(eventData.getEventUserData().getNotifications()),
                InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest()
        );

        eventData.getEvent().setId(info.getEvent().getId());

        eventData.setInvData(ParticipantsData.merge(
                new ParticipantData(creator.getEmail(), "creator", Decision.YES, false, true, false),
                Cf.list(new ParticipantData(attendee.getEmail(), "attendee", Decision.NO, true, false, false))));

        EventLayer attendeeEventLayer = eventLayerDao.findEventLayerByEventIdAndLayerCreatorUid(info.getEvent().getId(), attendee.getUid()).get();
        eventData.setLayerId(attendeeEventLayer.getLayerId());

        eventData.getEvent().setName("ha-ha-ha-ha-ha");
        eventData.getEvent().setStartTs(new DateTime(2010, 1, 7, 1, 0, 0, 0, DateTimeZone.UTC).toInstant());
        eventData.getEvent().setEndTs(new DateTime(2010, 1, 7, 2, 0, 0, 0, DateTimeZone.UTC).toInstant());

        try {
            eventWebManager.update(attendee.getUserInfo(), eventData, true, ActionInfo.webTest());
        } catch (PermissionDeniedUserException e) {
            return;
        }

        // if didn't throw an exception at least ignore changes
        Event updated = eventDao.findEventById(info.getEvent().getId());
        Assert.A.equals(eventName, updated.getName());
        Assert.A.equals(eventStart, updated.getStartTs());
    }

    @Test
    @WantsEws
    public void onlyNotifyOrganizerOnParticipantDetach() {
        onlyNotifyOrganizerOnDetach(true);
    }

    @Test
    @WantsEws
    public void onlyNotifyOrganizerOnNonparticipantDetach() {
        onlyNotifyOrganizerOnDetach(false);
    }

    public void onlyNotifyOrganizerOnDetach(boolean isAttendee) {
        TestUserInfo creator = testManager.prepareUser("yandex-team-mm-10931");
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-10932");
        TestUserInfo detachingUser = testManager.prepareUser("yandex-team-mm-10933");

        setIsEwserIfNeeded(Cf.list(creator, detachingUser));

        Event event = testManager.createDefaultEvent(creator.getUid(), "onlyNotifyOrganizerOnDetach");

        testManager.addUserParticipantToEvent(event.getId(), creator.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);
        setIsExportedWithEwsIfNeeded(event);

        if (isAttendee) {
            testManager.addUserParticipantToEvent(event.getId(), detachingUser.getUid(), Decision.UNDECIDED, false);
        } else {
            testManager.addUserParticipantToEvent(event.getId(), detachingUser.getUid(), Decision.UNDECIDED, false);
            eventUserDao.updateEventUserIsAttendeeByEventIdAndUserId(
                    event.getId(), detachingUser.getUid(), false, ActionInfo.webTest());
        }

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        if (isEwser(creator) && isEwser(detachingUser)) {
            setMockExchangeIdForEventUser(event.getId(), detachingUser.getUid());
        }

        eventRoutines.rejectMeeting(event.getId(), detachingUser.getUid(),
                ActionInfo.webTest(event.getStartTs()), InvitationProcessingMode.SAVE_ATTACH_SEND);

        ListF<ReplyMessageParameters> parameters = mailSenderMock.getReplyMessageParameterss();

        if (isAttendee && !isEwser(creator)) {
            Assert.A.equals(creator.getEmail(), parameters.single().getRecipientEmail());
        } else {
            Assert.A.hasSize(0, parameters);
        }
    }

    @Test
    public void createUpdateDeleteByExternalId() {
        TestUserInfo creator = testManager.prepareRandomYaTeamSuperUser(514312);
        DateTime start = MoscowTime.dateTime(2015, 7, 26, 22, 0);

        EventData data = new EventData();
        data.getEvent().setStartTs(start.toInstant());
        data.getEvent().setEndTs(start.plusHours(1).toInstant());
        data.getEvent().setName("byExternalId");

        data.setTimeZone(MoscowTime.TZ);
        data.setExternalId(Option.of("externalId"));

        Function0V createEventF = () -> eventWebManager.createUserEvent(
                creator.getUid(), data, InvitationProcessingMode.SAVE_ONLY, ActionInfo.webTest());

        createEventF.apply();

        Assert.assertThrows(createEventF::apply,
                CommandRunException.class,
                (e) -> e.getSituation().isSome(Situation.EVENT_ALREADY_EXISTS));

        data.getEvent().setStartTs(start.plusHours(1).toInstant());
        data.getEvent().setEndTs(start.plusHours(2).toInstant());

        ModificationInfo info = eventWebManager.update(
                creator.getUserInfo(), data, Option.empty(), false, ActionInfo.webTest());
        Assert.some(info.getUpdatedEvent());

        eventWebManager.deleteEvent(
                creator.getUserInfo(),
                IdOrExternalId.externalId(data.getExternalId().get()),
                Option.empty(), Option.empty(), false, ActionInfo.webTest());

        Assert.assertThrows(
                () -> eventWebManager.getModificationEvent(
                        Option.of(creator.getUid()),
                        IdOrExternalId.externalId(data.getExternalId().get()), Option.empty()),
                CommandRunException.class, (e) -> e.getSituation().isSome(Situation.EVENT_NOT_FOUND));
    }
} //~

