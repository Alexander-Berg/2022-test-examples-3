package ru.yandex.market.pers.notify.push;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.push.PushDeeplink;
import ru.yandex.market.pers.notify.test.TestUtil;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 30.09.16
 */
class XivaPusherServiceTest {
    private static final String PUSH_TITLE = "Заказ № 5 отменен";
    private static final String PUSH_BODY = "К сожалению, вы не успели его оплатить";
    private static final String DEEPLINK_VALUE = "bluemarket://order/2275086";
    private static final String TRANSBOUNDARY_DEEPLINK_VALUE = "bringly://profile";

    @Test
    void buildPushWithoutGcmData() throws Exception {
        String body = pushWithoutGcmData(PUSH_TITLE);
        JSONAssert.assertEquals(TestUtil.stringFromFile("/data/xiva/push_without_gcm_data.json"), body, true);
    }

    @Test
    void buildPushWithoutGcmDataNoTitle() throws Exception {
        String body = pushWithoutGcmData(null);
        JSONAssert.assertEquals(TestUtil.stringFromFile("/data/xiva/push_without_gcm_data_no_title.json"), body, true);
    }


    @Test
    void buildStorePushWithGcmData() throws Exception {
        String body = pushWithDeepLinkWithGcm(PUSH_TITLE);
        JSONAssert.assertEquals(TestUtil.stringFromFile("/data/xiva/push_with_gcm_data.json"), body, true);
    }

    @Test
    void buildStorePushWithGcmDataNoTitle() throws Exception {
        String body = pushWithDeepLinkWithGcm(null);
        JSONAssert.assertEquals(TestUtil.stringFromFile("/data/xiva/push_with_gcm_data_no_title.json"), body, true);
    }

    private static String pushWithoutGcmData(String pushTitle) {
        XivaPusherService service = new XivaPusherService();
        Map<String, String> params = new HashMap<>();
        params.put("ff?", "ff!");
        params.put("link", PushDeeplink.CART.nativeFormat());

        return service.buildPushMethodBody(PUSH_BODY, pushTitle, params, false);
    }

    private static String pushWithDeepLinkWithGcm(String title) {
        XivaPusherService service = new XivaPusherService();
        Map<String, String> params = new HashMap<>();
        params.put(NotificationEventDataName.STORE_PUSH_DEEPLINK_V1, DEEPLINK_VALUE);
        params.put(NotificationEventDataName.TRANSBOUNDARY_TRADING_PUSH_DEEPLINK_V1, TRANSBOUNDARY_DEEPLINK_VALUE);
        params.put(NotificationEventDataName.PUSH_TYPE, NotificationSubtype.PUSH_STORE_CANCELLED_USER_NOT_PAID.name());

        return service.buildPushMethodBody(XivaPusherServiceTest.PUSH_BODY, title, params, true);
    }
}
