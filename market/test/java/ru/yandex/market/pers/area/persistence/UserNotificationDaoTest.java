package ru.yandex.market.pers.area.persistence;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.yandex.market.pers.area.db.PersAreaEmbeddedDbUtil;
import ru.yandex.market.pers.area.model.PersAreaUserId;
import ru.yandex.market.pers.area.model.UserNotification;
import ru.yandex.market.pers.area.model.UserNotificationLinkV1;
import ru.yandex.market.pers.area.model.request.UserNotificationCreatePlainTextRequest;
import ru.yandex.market.pers.area.model.request.UserNotificationGetRequest;
import ru.yandex.market.pers.area.model.request.UserNotificationUpdateStatusRequest;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 07.11.17
 */
public class UserNotificationDaoTest {
    private PersAreaEmbeddedDbUtil dbUtil = PersAreaEmbeddedDbUtil.INSTANCE;

    @AfterEach
    public void truncateTables() {
        dbUtil.truncatePersAreaTables();
    }

    @Test
    public void getNotificationsReturnsTimeInSystemTZIfNoTZSpecified() throws Exception {
        UserNotificationDao userNotificationDao = createUserNotificationDao();
        UserNotification notification = userNotificationDao.addUserNotification(createSimpleRequest());
        List<UserNotification> notifications = userNotificationDao.getNotifications(
            new UserNotificationGetRequest(notification.getUserId(), UserNotification.Status.NEW, null, 1, 0));
        ZonedDateTime expectedCreationTs = notification.getCreationTs().withZoneSameInstant(ZoneId.systemDefault());
        ZonedDateTime expectedModificationTs = notification.getModificationTs().withZoneSameInstant(ZoneId.systemDefault());
        assertEquals(expectedCreationTs, notifications.get(0).getCreationTs());
        assertEquals(expectedModificationTs, notifications.get(0).getModificationTs());
    }

    @Test
    public void getNotificationsReturnsTimeInUserTZ() throws Exception {
        UserNotificationDao userNotificationDao = createUserNotificationDao();
        UserNotification notification = userNotificationDao.addUserNotification(createSimpleRequest());
        ZoneId userZone = ZoneId.of("America/New_York");
        List<UserNotification> notifications = userNotificationDao.getNotifications(
            new UserNotificationGetRequest(notification.getUserId(), UserNotification.Status.NEW, userZone, 1, 0));
        ZonedDateTime expectedCreationTs = notification.getCreationTs().withZoneSameInstant(userZone);
        ZonedDateTime expectedModificationTs = notification.getModificationTs().withZoneSameInstant(userZone);
        assertEquals(expectedCreationTs, notifications.get(0).getCreationTs());
        assertEquals(expectedModificationTs, notifications.get(0).getModificationTs());
    }

    @Test
    public void getNotifications() throws Exception {
        UserNotificationDao userNotificationDao = createUserNotificationDao();
        UserNotification expectedNotification = userNotificationDao.addUserNotification(createSimpleRequest());
        List<UserNotification> notifications = userNotificationDao.getNotifications(
            new UserNotificationGetRequest(expectedNotification.getUserId(), UserNotification.Status.NEW, ZoneId.systemDefault(), 1, 0));
        assertEquals(1, notifications.size());
        assertEquals(expectedNotification, notifications.get(0));
    }

    @Test
    public void getUnreadNotificationsCount() throws Exception {
        UserNotificationDao userNotificationDao = createUserNotificationDao();
        userNotificationDao.addUserNotification(createSimpleRequest());
        userNotificationDao.addUserNotification(createSimpleRequest());
        userNotificationDao.addUserNotification(createSimpleRequest());
        assertEquals(3, userNotificationDao.getNotificationsCount(createSimpleRequest().getUserId(), UserNotification.Status.NEW));
    }

    @Test
    public void updateNotificationsStatus() throws Exception {
        UserNotificationDao userNotificationDao = createUserNotificationDao();
        UserNotification notification = userNotificationDao.addUserNotification(createSimpleRequest());
        assertEquals(UserNotification.Status.NEW, notification.getStatus());
        int updated = userNotificationDao.updateNotificationsStatus(new UserNotificationUpdateStatusRequest(
            Collections.singletonList(notification.getId()),
            notification.getUserId(),
            UserNotification.Status.READ
        ));
        List<UserNotification> notifications = userNotificationDao.getNotifications(
            new UserNotificationGetRequest(notification.getUserId(), UserNotification.Status.READ, ZoneId.systemDefault(), 1, 0));
        assertEquals(1, updated);
        assertEquals(1, notifications.size());
        assertEquals(notification.getId(), notifications.get(0).getId());
        assertEquals(UserNotification.Status.READ, notifications.get(0).getStatus());
    }

    @Test
    public void addUserNotificationSimple() throws Exception {
        UserNotificationDao userNotificationDao = createUserNotificationDao();
        UserNotificationCreatePlainTextRequest request = createSimpleRequest();
        UserNotification notification = userNotificationDao.addUserNotification(request);
        assertEquals(request.getUserId(), notification.getUserId());
        assertEquals(request.getType(), notification.getType());
        assertEquals(request.getLinkV1(), notification.getLinkV1());
        assertTrue(request.getPayload().similar(notification.getPayload()));
        assertEquals(request.getPlainText(), notification.getBody());
    }

    private UserNotificationCreatePlainTextRequest createSimpleRequest() {
        return new UserNotificationCreatePlainTextRequest(
            "type",
            new PersAreaUserId(PersAreaUserId.Type.UID, "123213"),
            new UserNotificationLinkV1(UserNotificationLinkV1.Target.LIST, true, generateJsonObject()),
            generateJsonObject(),
            "plain text"
        );
    }

    private JSONObject generateJsonObject() {
        try {
            JSONObject result = new JSONObject();
            result.put("k1", "v1");
            result.put("k2", "v2");
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private UserNotificationDao createUserNotificationDao() {
        return new UserNotificationDao(dbUtil.getPersAreaJdbcTemplate());
    }
}
