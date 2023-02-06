package ru.yandex.calendar.logic.notification;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventNotification;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.web.EventWebManager;
import ru.yandex.calendar.logic.event.web.EventWebUpdater;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.misc.time.TimeUtils;

public class NotificationRoutinesRecalcOnUpdateLiteTest extends AbstractDbDataTest {

    @Autowired
    protected NotificationRoutines notificationRoutines;
    @Autowired
    protected EventRoutines eventRoutines;
    @Autowired
    protected EventLayerDao eventLayerDao;
    @Autowired
    private TestManager testManager;
    @Autowired
    private UserManager userManager;
    @Autowired
    private EventWebUpdater eventUpdater;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private NotificationDbManager notificationDbManager;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private EventWebManager eventWebManager;

    private EventData createEventData(Instant instanceStartTs, Instant newStartTs, long eventId) {
        long layerId = eventLayerDao.findEventLayersByEventId(eventId).single().getLayerId();

        EventData eventData = new EventData();
        eventData.getEvent().setStartTs(newStartTs);
        eventData.getEvent().setEndTs(TestManager.addHours(newStartTs, 1, TimeUtils.EUROPE_MOSCOW_TIME_ZONE));
        eventData.getEvent().setIsAllDay(false);
        eventData.getEvent().setId(eventId);
        eventData.setLayerId(layerId);
        eventData.setPrevLayerId(layerId);
        eventData.setInstanceStartTs(instanceStartTs);
        eventData.setTimeZone(MoscowTime.TZ);

        return eventData;
    }

    private Option<Instant> getNextNotifyEventInstanceStart(PassportUid uid, long eventId) {
        ListF<Long> euIds = Cf.list(eventUserDao.findEventUserByEventIdAndUid(eventId, uid).get().getId());
        EventNotifications notifications = notificationDbManager.getNotificationsByEventUserIds(euIds).single();

        EventNotification notification = notifications.getEventNotifications().first();
        Duration negOffset = Duration.standardMinutes(-notification.getOffsetMinute());

        return notification.getNextSendTs().map(ts -> ts.plus(negOffset));
    }

    private EventNotification getSingleEventNotPanelNotification(PassportUid uid, long eventId) {
        return notificationDbManager
                .getEventUsersWithNotificationsByUidAndEventIds(uid, Cf.list(eventId)).single()
                .getNotifications().getEventNotifications()
                .filter(EventNotification.getChannelF().andThenEquals(Channel.PANEL).notF()).single();
    }

    @Test
    public void singleEvent() {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10201").getUserInfo();
        PassportUid uid = user.getUid();

        DateTime nowTs = new DateTime(2011, 3, 26, 20, 17, 22, 0, DateTimeZone.UTC);
        Event event = testManager.createSingleEventWithNotification(uid, nowTs.toInstant());
        DateTime newStartTs = nowTs.plusHours(3);
        EventData eventData = createEventData(nowTs.toInstant(), newStartTs.toInstant(), event.getId());
        eventUpdater.update(user, eventData, NotificationsData.notChanged(), false, ActionInfo.webTest());

        // first check event is really updated
        Assert.equals(newStartTs.toInstant(), eventDao.findEventById(event.getId()).getStartTs());

        Assert.some(newStartTs.toInstant(), getNextNotifyEventInstanceStart(uid, event.getId()));
    }

    /**
     * Move next instance of event
     */
    @Test
    public void repeatedEvent1() {

        UserInfo user = testManager.prepareUser("yandex-team-mm-10202").getUserInfo();
        PassportUid uid = user.getUid();

        Instant now = TestDateTimes.utc(2020, 12, 11, 20, 5);
        Event event = testManager.createDailyRepeatedEventWithNotification(uid, now);
        // Update next day instance
        Instant nextInstanceStart = TestDateTimes.addDaysMoscow(now, 1);
        Instant newStart = TestManager.addHours(nextInstanceStart, 3, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        EventData eventData = createEventData(nextInstanceStart, newStart, event.getId());

        long newEventId = eventUpdater.update(user, eventData, NotificationsData.notChanged(), false,
                ActionInfo.webTest(now.plus(1))).getNewEventId().single();

        // check notification in main event
        Instant expected = TestDateTimes.addDaysMoscow(now, 2);
        Assert.some(expected, getNextNotifyEventInstanceStart(uid, event.getId()));

        // check notification in recurrence event
        Assert.some(newStart, getNextNotifyEventInstanceStart(uid, newEventId));

    }

    /**
     * Move next-next instance of event
     */
    @Test
    public void repeatedEvent2() {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10203").getUserInfo();
        PassportUid uid = user.getUid();

        final ActionInfo actionInfo = ActionInfo.webTest();
        Instant nowTs = actionInfo.getNow();
        Event event = testManager.createDailyRepeatedEventWithNotification(uid, nowTs);
        // Update next day instance
        Instant nextInstTs = TestDateTimes.addDaysMoscow(nowTs, 2);
        Instant newStartTs = TestManager.addHours(nextInstTs, 3, TimeUtils.EUROPE_MOSCOW_TIME_ZONE);
        EventData eventData = createEventData(nowTs, newStartTs, event.getId());

        long newEventId = eventUpdater.update(user, eventData,
                NotificationsData.notChanged(), false, actionInfo).getNewEventId().single();

        // check notification in main event
        Instant expected = TestDateTimes.addDaysMoscow(nowTs, 1);
        Assert.some(expected, getNextNotifyEventInstanceStart(uid, event.getId()));

        // check notification in recurrence event
        Assert.some(newStartTs, getNextNotifyEventInstanceStart(uid, newEventId));
    }

    @Test
    public void allDayEvent() {
        DateTimeZone userTz = DateTimeZone.forID("Europe/Zurich");
        DateTimeZone eventTz = DateTimeZone.forID("Europe/Moscow");

        UserInfo user = testManager.prepareUser("yandex-team-mm-10204").getUserInfo();

        LocalDateTime localStart = LocalDate.now(eventTz).plusDays(1).toLocalDateTime(LocalTime.MIDNIGHT);
        LocalDateTime localEnd = localStart.plusDays(1);

        EventData data = testManager.createDefaultEventData(user.getUid(), "All Day Event");
        data.getEvent().setStartTs(localStart.toDateTime(eventTz).toInstant());
        data.getEvent().setEndTs(localEnd.toDateTime(eventTz).plusDays(1).toInstant());
        data.getEvent().setIsAllDay(true);
        data.setTimeZone(eventTz);

        settingsRoutines.updateTimezone(user.getUid(), userTz.getID());

        long eventId = eventWebManager.createUserEvent(
                user.getUid(), data,
                NotificationsData.create(Cf.list(Notification.email(Duration.ZERO))),
                InvitationProcessingMode.SAVE_ONLY, ActionInfo.webTest()).getEventId();

        EventNotification notification = getSingleEventNotPanelNotification(user.getUid(), eventId);

        Assert.notEquals(data.getEvent().getStartTs(), notification.getNextSendTs().get());
        Assert.equals(localStart, new LocalDateTime(notification.getNextSendTs().get(), userTz));
    }
}
