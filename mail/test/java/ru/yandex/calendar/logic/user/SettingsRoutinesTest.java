package ru.yandex.calendar.logic.user;

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
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Settings;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.web.EventWebManager;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.base.Cf2;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class SettingsRoutinesTest extends AbstractConfTest {
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private TestManager testManager;
    @Autowired
    private UserManager userManager;
    @Autowired
    private UserDao userDao;
    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private NotificationDbManager notificationDbManager;

    @Test
    public void updateTimezoneWorksWithEmptyOldTimezone() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-10481").getUid();

        // TODO can enhance with adding testManager.createDefaultAllDayEvent
        // and verifying that its start instant moves along with zone changes

        settingsRoutines.clearTimezoneForTest(uid);

        final String newGeoTzId = "Asia/Yekaterinburg";
        final String newTzId = "Europe/Madrid";
        settingsRoutines.updateTimezones(uid, Option.of(newTzId), Option.of(newGeoTzId));

        Settings s = settingsRoutines.getSettingsByUid(uid).getCommon();
        Assert.A.equals(newGeoTzId, s.getGeoTzJavaid());
        Assert.A.equals(newTzId, s.getTimezoneJavaid());
    }

    @Test
    public void updateTimezoneUpdatesAllDayNextSendTsForFixedOffset() {
        updateTimezoneUpdatesAllDayNextSendTs(
                DateTimeZone.forOffsetHours(1),
                DateTimeZone.forOffsetHours(2),
                DateTimeZone.forOffsetHours(3));
    }

    @Test
    public void updateTimezoneUpdatesAllDayNextSendTsForFloatingOffset() {
        updateTimezoneUpdatesAllDayNextSendTs(
                DateTimeZone.forID("Asia/Yekaterinburg"),
                DateTimeZone.forID("Europe/Moscow"),
                DateTimeZone.forID("America/Anchorage"));
    }

    private void updateTimezoneUpdatesAllDayNextSendTs(
            DateTimeZone eventTz, DateTimeZone userOldTz, DateTimeZone userNewTz)
    {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-10482").getUid();

        LocalDateTime start = LocalDate.now(DateTimeZone.UTC).toLocalDateTime(LocalTime.MIDNIGHT).plusDays(1);

        settingsRoutines.updateTimezone(uid, userOldTz.getID());

        EventData data = new EventData();
        data.getEvent().setStartTs(start.toDateTime(eventTz).toInstant());
        data.getEvent().setEndTs(start.plusDays(1).toDateTime(eventTz).toInstant());
        data.getEvent().setIsAllDay(true);
        data.setTimeZone(eventTz);

        Notification notification = Notification.sms(Duration.ZERO);

        long eventId = eventWebManager.createUserEvent(
                uid, data, NotificationsData.create(Cf.list(notification)),
                InvitationProcessingMode.SAVE_ONLY, ActionInfo.webTest()).getEventId();

        Option<Instant> nextSendTs = notificationDbManager.getEventUserWithNotificationsByUidAndEventId(uid, eventId)
                .get().getEventNotifications(notification.getChannel()).single().getNextSendTs();
        Assert.some(start.toDateTime(userOldTz).toInstant(), nextSendTs);

        settingsRoutines.updateTimezone(uid, userNewTz.getID());

        nextSendTs = notificationDbManager.getEventUserWithNotificationsByUidAndEventId(uid, eventId)
                .get().getEventNotifications(notification.getChannel()).single().getNextSendTs();

        Assert.some(start.toDateTime(userNewTz).toInstant(), nextSendTs);
    }

    @Test
    public void getSettingsByUidIfExistsBatch() {
        ListF<TestUserInfo> infos = testManager.prepareUsers(
                Cf.<String>list("yandex-team-mm-10483", "yandex-team-mm-10484", "yandex-team-mm-10485"));

        ListF<PassportUid> uids = infos.map(TestUserInfo.getUidF())
                .plus(Cf.list(1l, 2l).map(PassportUid::cons))
                .plus(infos.first().getUid());

        MapF<PassportUid, Settings> uidToSettings = settingsRoutines.getSettingsCommonByUidIfExistsBatch(uids);

        Assert.A.hasSize(3, uidToSettings);
        uidToSettings.forEach((uid, settings) -> Assert.equals(uid, settings.getUid()));
    }

    @Test
    public void getOrCreateSettingsByUidBatch() {
        ListF<PassportUid> newbies = userManager.getUidByLoginForTestBatch(
                Cf.list("yandex-team-mm-10486", "yandex-team-mm-10487").map(PassportLogin::cons)).get2();
        testManager.cleanUsers(newbies);

        PassportUid elder = testManager.prepareUser("yandex-team-mm-10488").getUid();

        ListF<PassportUid> uids = newbies.plus(elder);

        MapF<PassportUid, Settings> fromRoutines =
                settingsRoutines.getOrCreateSettingsByUidBatch(uids).mapValues(SettingsInfo.getCommonF());

        final ListF<PassportUid> fromDb = Cf2.flatBy2(userDao.findSettingsByUids(uids)).get2().map(SettingsInfo.getUidF());

        Assert.A.hasSize(3, fromDb);
        Assert.A.hasSize(3, fromRoutines);

        fromRoutines.forEach((uid, settings) -> {
            Assert.equals(uid, settings.getUid());
            Assert.isTrue(fromDb.containsTs(uid));
        });
    }
}
