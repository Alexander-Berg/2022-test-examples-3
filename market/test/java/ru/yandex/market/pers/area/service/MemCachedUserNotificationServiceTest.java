package ru.yandex.market.pers.area.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.area.model.PersAreaUserId;
import ru.yandex.market.pers.area.model.UserNotification;
import ru.yandex.market.pers.area.model.UserNotificationLinkV1;
import ru.yandex.market.pers.area.model.request.UserNotificationCreatePlainTextRequest;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 10.11.17
 */
public class MemCachedUserNotificationServiceTest extends MarketUtilsMockedDbTest {
    @Autowired
    private MemCachedUserNotificationService memCachedUserNotificationService;

    @Test
    public void addUserNotification() throws Exception {
        UserNotificationCreatePlainTextRequest request = createRequest();
        UserNotification userNotification = memCachedUserNotificationService.addUserNotification(request);
        assertEquals(request.getPlainText(), userNotification.getBody());
        assertEquals(request.getLinkV1(), userNotification.getLinkV1());
        assertTrue(request.getPayload().similar(userNotification.getPayload()));
        assertEquals(request.getType(), userNotification.getType());
        assertEquals(request.getUserId(), userNotification.getUserId());
    }

    @Test
    public void addUserNotificationCleansCache() throws Exception {
        MemCachedUserNotificationService spy = spy(memCachedUserNotificationService);
        UserNotificationCreatePlainTextRequest request = createRequest();
        spy.addUserNotification(request);
        verify(spy).cleanCache(request.getUserId());
    }

    private UserNotificationCreatePlainTextRequest createRequest() {
        JSONObject linkParams = new JSONObject();
        linkParams.put("lpk1", "lpv1");
        linkParams.put("lpk2", "lpv2");
        JSONArray array = new JSONArray(Arrays.asList(1, 2, 3));
        JSONObject payload = new JSONObject();
        payload.put("pk2", array);
        return new UserNotificationCreatePlainTextRequest(
            "my_type",
            new PersAreaUserId(PersAreaUserId.Type.YANDEXUID, "874223"),
            new UserNotificationLinkV1(UserNotificationLinkV1.Target.COLLECTION, false, linkParams),
            payload,
            "my_plain_text"
        );
    }
}
