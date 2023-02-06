package ru.yandex.calendar.logic.notification;

import java.util.Map;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.yamb.Yamb;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventNotificationHelper;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.NotificationSendStat;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.layer.LayerDao;
import ru.yandex.calendar.logic.sending.param.NotificationMessageParameters;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sending.real.PassportSmsServiceMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.perm.EventActionClass;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.commune.bazinga.scheduler.schedule.RescheduleConstant;
import ru.yandex.commune.bazinga.scheduler.schedule.ReschedulePolicy;
import ru.yandex.misc.db.q.SqlCondition;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadLocalTimeout;
import ru.yandex.misc.time.MoscowTime;

public class NotificationSendResultManagerTest extends AbstractConfTest {

    private static final Instant eventStart = MoscowTime.instant(2017, 10, 12, 5, 45);
    private static final String OK = "<?xml version=\"1.0\" encoding=\"windows-1251\"?>\n" +
            "<doc>\n" +
            "    <message-sent id=\"127000000000001\" />\n" +
            "    <gates ids=\"1\" />\n" +
            "</doc>";
    private static final String NOCURRENT = "<?xml version=\"1.0\" encoding=\"windows-1251\"?>\n" +
            "<doc>\n" +
            "    <error>User does not have an active phone to receive messages</error>\n" +
            "    <errorcode>NOCURRENT</errorcode>\n" +
            "</doc>";

    @Autowired
    private NotificationSendManager notificationSendManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private Yamb yamb;
    @Autowired
    private MailSenderMock mailSender;
    @Autowired
    private PassportSmsServiceMock passportSmsService;
    @Autowired
    private NotificationSendStatDao notificationSendStatDao;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private NotificationDbManager notificationDbManager;
    @Autowired
    private LayerDao layerDao;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private GenericBeanDao genericBeanDao;

    @Before
    public void setup() {
        mailSender.clear();
        genericBeanDao.deleteBeans(EventNotificationHelper.INSTANCE, SqlCondition.trueCondition());
    }

    @Test
    public void doNotNotifyDismissedEmployees() {

        TestUserInfo helga = testManager.prepareYandexUser(TestManager.createHelga());
        TestUserInfo ssytnik = testManager.prepareYandexUser(TestManager.createSsytnik());

        SettingsYt data = new SettingsYt();
        data.setIsDismissed(true);

        settingsRoutines.saveEmptySettingsForUid(helga.getUid());
        settingsRoutines.updateSettingsYtByUid(data, helga.getUid());

        Event event = testManager.createDefaultEvent(helga.getUid(), "Top secret event", eventStart);
        testManager.addUserParticipantToEvent(event.getId(), helga.getUid(), Decision.YES, false);
        testManager.addUserParticipantToEvent(event.getId(), ssytnik.getUid(), Decision.YES, false);

        ListF<Email> notified = sendNotifications(event);

        Assert.in(ssytnik.getEmail(), notified);
        Assert.notIn(helga.getEmail(), notified);
    }

    @Test
    public void doNotNotifyIfNoLayerToSeeEvent() {
        TestUserInfo creator = testManager.prepareRandomYaTeamUser(1270);
        TestUserInfo seer = testManager.prepareRandomYaTeamUser(1271);
        TestUserInfo blind = testManager.prepareRandomYaTeamUser(1272);

        Event event = testManager.createDefaultEvent(creator.getUid(), "Public event", eventStart);
        testManager.addUserParticipantToEvent(event.getId(), creator.getUid(), Decision.YES, false);

        eventDao.updateEventPermAll(event.getId(), EventActionClass.VIEW);

        eventRoutines.attachEventOrMeetingToUser(
                seer.getUserInfo(), event.getId(), Option.<Long>empty(), Decision.YES,
                new EventUser(), NotificationsData.createEmpty(), ActionInfo.webTest());
        long notSeeLayerId = eventRoutines.attachEventOrMeetingToUser(
                blind.getUserInfo(), event.getId(), Option.<Long>empty(), Decision.YES,
                new EventUser(), NotificationsData.createEmpty(), ActionInfo.webTest()).layerId.get().getCurrentLayerId();

        eventLayerDao.deleteEventLayersByLayerIds(Cf.list(notSeeLayerId));

        ListF<Email> notified = sendNotifications(event);

        Assert.in(seer.getEmail(), notified);
        Assert.notIn(blind.getEmail(), notified);
    }

    @Test
    public void doNotifySms() {
        passportSmsService.addResponses(Map.of("http://sms.passport.yandex.ru/sendsms?uid=1130000010000001&text=12.10.17+%D0%B2+05%3A45+%22Public+event%22.+%D0%AF.%D0%9A%D0%B0%D0%BB%D0%B5%D0%BD%D0%B4%D0%B0%D1%80%D1%8C&sender=calendar&utf8=1", OK,
                "http://sms.passport.yandex.ru/sendsms?uid=1130000010000002&text=12.10.17+%D0%B2+05%3A45+%22Public+event%22.+%D0%AF.%D0%9A%D0%B0%D0%BB%D0%B5%D0%BD%D0%B4%D0%B0%D1%80%D1%8C&sender=calendar&utf8=1", NOCURRENT));
        TestUserInfo creator = testManager.prepareYandexUser(TestManager.createTestUser(1));
        TestUserInfo seer = testManager.prepareYandexUser(TestManager.createTestUser(2));

        Event event = testManager.createDefaultEvent(creator.getUid(), "Public event", eventStart);
        testManager.addUserParticipantToEvent(event.getId(), creator.getUid(), Decision.YES, false);

        eventDao.updateEventPermAll(event.getId(), EventActionClass.VIEW);

        eventRoutines.attachEventOrMeetingToUser(
                seer.getUserInfo(), event.getId(), Option.empty(), Decision.YES,
                new EventUser(), NotificationsData.createEmpty(), ActionInfo.webTest());

        ActionInfo info = createAndSendNotifications(event, Channel.SMS);
        Option<NotificationSendStat> stat = notificationSendStatDao.findLastNotificationSendStat(info.getRequestIdWithHostId());
        Assert.equals(1, stat.get().getSmsSent());
        Assert.equals(3, stat.get().getTotalProcessed());
        passportSmsService.clear();
    }

    @Test
    public void allDayInOtherTimezone() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-19200");
        settingsRoutines.updateTimezone(user.getUid(), "Europe/Moscow");

        Event event = new Event();
        event.setIsAllDay(true);
        event.setStartTs(MoscowTime.instant(2017, 10, 12, 0, 0));
        event.setEndTs(MoscowTime.instant(2017, 10, 13, 0, 0));

        event = testManager.createDefaultEvent(user.getUid(), "Name", event);
        settingsRoutines.updateTimezone(user.getUid(), "Europe/Samara");

        testManager.createEventLayer(user.getDefaultLayerId(), event.getId());
        long eventUserId = testManager.createEventUser(user.getUid(), event.getId(), Decision.YES, Option.empty());

        ActionInfo actionInfo = ActionInfo.webTest(event.getStartTs());

        notificationDbManager.updateAndRecalcEventNotifications(eventUserId,
                NotificationsData.updateFromWeb(Cf.list(Notification.email(Duration.standardMinutes(660)))), actionInfo);

        notificationSendManager.sendNotifications(ActionInfo.webTest(event.getStartTs().plus(Duration.standardHours(11))));

        NotificationMessageParameters params = mailSender.getMessageParameterss(NotificationMessageParameters.class).single();

        Assert.equals(new LocalDateTime(2017, 10, 12, 0, 0), params.getEventStartTs());
    }

    @Test
    public void tryAgainNotifications() {
        ReschedulePolicy policy = NotificationSendManager.getLocalRetryPolicy();

        Yamb yambMock = Mockito.mock(Yamb.class);
        notificationSendManager.setYambForTest(yambMock);
        try {
            TestUserInfo creator = testManager.prepareRandomYaTeamUser(1270);
            TestUserInfo attendee = testManager.prepareRandomYaTeamUser(1271);

            Event event = testManager.createDefaultEvent(creator.getUid(), "Event", eventStart);

            testManager.addUserParticipantToEvent(event.getId(), creator, Decision.YES, true);
            testManager.addUserParticipantToEvent(event.getId(), attendee, Decision.UNDECIDED, false);

            ArgumentCaptor<String> messagesCaptor = ArgumentCaptor.forClass(String.class);

            Mockito.doThrow(new RuntimeException()).doNothing()
                    .when(yambMock).sendMessage(Mockito.eq(creator.getUid()), messagesCaptor.capture());

            Mockito.doThrow(new RuntimeException()).doThrow(new RuntimeException()).doNothing()
                    .when(yambMock).sendMessage(Mockito.eq(attendee.getUid()), messagesCaptor.capture());

            NotificationSendManager.setLocalRetryPolicyForTest(new RescheduleConstant(Duration.millis(10), 10));

            createAndSendNotifications(event, Channel.YAMB);
            Assert.hasSize(5, messagesCaptor.getAllValues());

            NotificationSendManager.setLocalRetryPolicyForTest(new RescheduleConstant(Duration.standardSeconds(3), 10));
            ThreadLocalTimeout.Handle handle = ThreadLocalTimeout.push(Duration.standardSeconds(3));
            try {
                createAndSendNotifications(event, Channel.YAMB);
                Assert.hasSize(7, messagesCaptor.getAllValues());

            } finally {
                handle.popSafely();
            }

        } finally {
            NotificationSendManager.setLocalRetryPolicyForTest(policy);
            notificationSendManager.setYambForTest(yamb);
        }
    }

    private ActionInfo createAndSendNotifications(Event event, Channel channel) {
        ActionInfo actionInfo = ActionInfo.webTest(event.getStartTs().minus(42));

        ListF<EventUser> eventUsers = eventUserDao.findEventUsersByEventId(event.getId());

        for (EventUser eventUser : eventUsers) {
            notificationDbManager.updateAndRecalcEventNotifications(
                    eventUser.getId(),
                    NotificationsData.updateFromWeb(Cf.list(new Notification(channel, Duration.ZERO))), actionInfo);
        }
        notificationSendManager.sendNotifications(actionInfo);
        return actionInfo;
    }

    private ListF<Email> sendNotifications(Event event) {
        createAndSendNotifications(event, Channel.EMAIL);

        return mailSender
                .getMessageParameterss(NotificationMessageParameters.class)
                .map(NotificationMessageParameters.getRecipientEmailF());
    }
}
