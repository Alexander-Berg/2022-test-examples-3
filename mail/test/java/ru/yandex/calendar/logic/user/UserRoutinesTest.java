package ru.yandex.calendar.logic.user;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.LayerUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class UserRoutinesTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private UserRoutines userRoutines;
    @Autowired
    private UserManager userManager;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private NotificationDbManager notificationDbManager;


    @Test
    public void deleteUserDeletesRepetitions() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(3470);
        Instant startTs = TestDateTimes.moscow(2010, 12, 23, 12, 51);
        Event event = testManager.createDailyRepeatedEventWithNotification(user.getUid(), startTs);
        final long repetitionId = event.getRepetitionId().get();

        userRoutines.deleteUser(user.getUid(), ActionInfo.webTest());

        try {
            eventDao.findRepetitionById(repetitionId);
            Assert.fail("Expected exception when looking for deleted repetition " + repetitionId);
        } catch (EmptyResultDataAccessException e) {
            // ok
        }
    }

    @Test
    public void deleteUserDeletesMainEvents() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(3470);

        Event event = testManager.createDefaultEvent(user.getUid(), "Event for deleteMainEvents()");
        final long mainEventId = event.getMainEventId();

        userRoutines.deleteUser(user.getUid(), ActionInfo.webTest());

        try {
            mainEventDao.findMainEventById(mainEventId);
            Assert.fail("Expected exception when looking for deleted main_event " + mainEventId);
        } catch (EmptyResultDataAccessException e) {
            // ok
        }
    }

    @Test
    public void layerIsDeletedWithNotification() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(3480).getUid();

        LayerUser layerUser = testManager.createLayers(Cf.list(uid)).single().get2();
        ListF<Notification> notifications = testManager.sms25MinutesBefore();
        notificationDbManager.saveLayerNotifications(layerUser.getId(), notifications);

        userRoutines.deleteUser(uid, ActionInfo.webTest());
        Assert.isEmpty(notificationDbManager.getNotificationsByLayerUserId(layerUser.getId()));
    }
}
