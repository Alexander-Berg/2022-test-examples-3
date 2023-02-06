package ru.yandex.calendar.logic.notification;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventNotification;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.web.EventWebManager;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class NotificationRoutinesRecalcRemoveTest extends AbstractDbDataTest {

    @Autowired
    protected EventRoutines eventRoutines;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    protected NotificationRoutines notificationRoutines;
    @Autowired
    private UserManager userManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private NotificationDbManager notificationDbManager;


    private Option<Instant> getNextNotifyEventInstanceStart(PassportUid uid, long eventId) {
        ListF<Long> euIds = Cf.list(eventUserDao.findEventUserByEventIdAndUid(eventId, uid).get().getId());
        EventNotifications notifications = notificationDbManager.getNotificationsByEventUserIds(euIds).single();

        EventNotification notification = notifications.getEventNotifications().first();
        Duration negOffset = Duration.standardMinutes(-notification.getOffsetMinute());

        return notification.getNextSendTs().map(ts -> ts.plus(negOffset));
    }

    /**
     * Remove only next instance of event
     */
    @Test
    public void repeatedEvent1() {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10211").getUserInfo();
        PassportUid uid = user.getUid();

        ActionInfo actionInfo = ActionInfo.webTest();
        Event event = testManager.createDailyRepeatedEventWithNotification(uid, actionInfo.getNow());
        Instant nextInstMs = TestDateTimes.addDaysMoscow(actionInfo.getNow(), 1);
        eventWebManager.deleteUserEvent(user, event.getId(), Option.of(nextInstMs), false, actionInfo);

        Instant expected = TestDateTimes.addDaysMoscow(actionInfo.getNow(), 2);
        Assert.some(expected, getNextNotifyEventInstanceStart(uid, event.getId()));
    }

    /**
     * Remove only next-next instance of event
     */
    @Test
    public void repeatedEvent2() {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10212").getUserInfo();
        PassportUid uid = user.getUid();

        ActionInfo actionInfo = ActionInfo.webTest();
        Event event = testManager.createDailyRepeatedEventWithNotification(uid, actionInfo.getNow());
        Instant nextInstMs = TestDateTimes.addDaysMoscow(actionInfo.getNow(), 2);
        eventWebManager.deleteUserEvent(user, event.getId(), Option.of(nextInstMs), false, actionInfo);

        Instant expected = TestDateTimes.addDaysMoscow(actionInfo.getNow(), 1);
        Assert.some(expected, getNextNotifyEventInstanceStart(uid, event.getId()));
    }
}
