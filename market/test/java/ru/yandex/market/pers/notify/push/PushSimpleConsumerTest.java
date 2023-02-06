package ru.yandex.market.pers.notify.push;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.pers.notify.PushConsumerTest;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.logging.ExternalLogger;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.model.push.PushDeeplink;
import ru.yandex.market.pers.notify.model.push.PushTemplateType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.notify.mock.MarketMailerMockFactory.generateChangeRequest;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         24.05.17
 */
@ContextConfiguration(classes = PushSimpleConsumerTest.ToStringExternalLogger.class)
class PushSimpleConsumerTest extends PushConsumerTest {
    private static final String UUID = "ba617f2509adf9bd8027c2944f349b4e";

    private static final String MESSAGE_TEMPLATE = "tskv\t" +
        "transitId=some_id\t" +
        "text=\t" +
        "platform=%s\t" +
        "uuid=" + UUID + "\t" +
        "timestamp=...\t" +
        "control=false\t" +
        "globalControl=false\t" +
        "type=3\t" +
        "templateId=" + PushTemplateType.PUSH_STORE_ORDER_STATUS_CANCELLED.getName() + "\t" +
        "notificationId=114\t" +
        "notificationName=PUSH_STORE_CANCELLED_USER_NOT_PAID\t" +
        "payloadJson=[{\\\"name\\\":\\\"ORDER_IDS\\\",\\\"value\\\":\\\"" + ORDER_ID + "\\\"}]";

    @Service
    @Primary
    @Qualifier("sentPushesExternalLogger")
    static class ToStringExternalLogger implements ExternalLogger {
        private String message;

        @Override
        public void log(String message) {
            this.message = message;
        }
    }

    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;

    @Autowired
    private ToStringExternalLogger externalLogger;

    @AfterEach
    void tearDown() {
        externalLogger.message = null;
    }

    @Test
    void getParams() {
        initMobileAppInfo(MobilePlatform.ANDROID);

        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.PRICE_ALERT_ONSTOCK)
            .addTemplateParam(NotificationEventDataName.MODEL_ID, "123")
            .addDeeplink(PushDeeplink.MODEL, 123L)
            .addDataParam(NotificationEventDataName.MODEL_NAME, "my iphone")
            .build());

        Map<String, String> params = pushSimpleConsumer.getParams(event);
        Map<String, String> expectedResult = new HashMap<String, String>() {{
            put(NotificationEventDataName.PUSH_TYPE, "PRICE_ALERT_ONSTOCK");
            put(NotificationEventDataName.PUSH_LINK, "yandexmarket://product/123");
            put(NotificationEventDataName.PUSH_TOUCH_LINK, "yandexmarket://deeplink?pageId=touch:product&productId=123");
        }};
        assertEquals(expectedResult, params);
    }

    @Test
    void testCancellationPushOutdated() {
        Instant threeWeeksOldInstant = Instant.now().minus(29, ChronoUnit.DAYS);
        NotificationEventStatus status = initAndProcessEvent(MobilePlatform.ANY, threeWeeksOldInstant);

        assertEquals(NotificationEventStatus.EXPIRED, status);
    }

    @Test
    void testCancellationPush() {
        NotificationEventStatus status = initAndProcessEvent(MobilePlatform.ANY, Instant.now());

        assertEquals(NotificationEventStatus.SENT, status);
    }

    @Test
    void testLogMessageForAndroid() {
        NotificationEventStatus status = initAndProcessEvent(MobilePlatform.ANDROID, Instant.now());

        assertEquals(NotificationEventStatus.SENT, status);
        assertNotNull(externalLogger.message);
        String expected = String.format(MESSAGE_TEMPLATE, "ANDROID");
        String actual = externalLogger.message.replaceAll("timestamp=\\d+", "timestamp=...");
        assertEquals(expected, actual);
    }

    @Test
    void testLogMessageForIOS() {
        NotificationEventStatus status = initAndProcessEvent(MobilePlatform.IPHONE, Instant.now());

        assertEquals(NotificationEventStatus.SENT, status);
        assertNotNull(externalLogger.message);
        String expected = String.format(MESSAGE_TEMPLATE, "iOS");
        String actual = externalLogger.message.replaceAll("timestamp=\\d+", "timestamp=...");
        assertEquals(expected, actual);
    }

    private void initMobileAppInfo(MobilePlatform platform) {
        mobileAppInfoDAO.add(new MobileAppInfo(1L, UUID, "app", "none", platform, false, new Date()));
    }

    private NotificationEventStatus initAndProcessEvent(MobilePlatform platform, Instant requestInstant) {
        initMobileAppInfo(platform);
        Order order = createOrder();
        setupCheckouterClient(order);
        ChangeRequest changeRequest = generateChangeRequest(
            ChangeRequestType.CANCELLATION,
            ChangeRequestStatus.NEW,
            requestInstant);

        order.setChangeRequests(Collections.singletonList(changeRequest));
        return createCancelledEventAndProcess(PushTemplateType.PUSH_STORE_ORDER_STATUS_CANCELLED);
    }
}
