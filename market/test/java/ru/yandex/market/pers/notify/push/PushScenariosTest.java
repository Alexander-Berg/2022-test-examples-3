package ru.yandex.market.pers.notify.push;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.pers.notify.PushConsumerTest;
import ru.yandex.market.pers.notify.ems.configuration.NotificationConfigFactory;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.ems.service.MailerNotificationEventService;
import ru.yandex.market.pers.notify.external.carter.CarterService;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.UserModel;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.model.push.PushTemplateType;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.pricealert.PriceAlertService;
import ru.yandex.market.pers.notify.pricealert.model.PriceAlertSubscription;
import ru.yandex.market.pers.notify.push.generator.CarterPushGenerator;
import ru.yandex.market.pers.notify.push.generator.UserOfflinePushGenerator;
import ru.yandex.market.pers.notify.push.generator.util.CarterUserReceiver;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.notify.mock.MarketMailerMockFactory.generateChangeRequest;

/**
 * @author artemmz
 * created on 02.02.16.
 * https://wiki.yandex-team.ru/market/mobile/push/
 * P.S. http://pusher-test.yandex.net работает через раз,
 * лучше использовать продовский http://pusher.yandex.net
 */
public class PushScenariosTest extends PushConsumerTest {


    private static final String UUID = "ba617f2509adf9bd8027c2944f349b4e";

    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private MailerNotificationEventService mailerNotificationEventService;
    @Autowired
    private UserOfflinePushGenerator userOfflinePushGenerator;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;
    @Autowired
    private CarterPushGenerator carterPushGenerator;
    @Autowired
    private CarterUserReceiver carterUserReceiver;
    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;
    @Autowired
    private PriceAlertService priceAlertService;
    @Autowired
    private JdbcTemplate ytJdbcTemplate;

//    Long UID = 348214245L;
//    String UUID = "f8abd9e64e2ca30d49351c22d1c472c8";

    @BeforeEach
    public void init() {
        clearDb();
        mobileAppInfoDAO.add(new MobileAppInfo(1L, UUID, "app", "none", MobilePlatform.ANY, false));
    }

    /*========================Scenario 1 - order waits for pickup===========================*/

    @Test
    public void testPickupFromUuid() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.ORDER_STATUS_PICKUP)
            .addTemplateParam(NotificationEventDataName.ORDER_ID, "100500")
            .build();
        testOrderStatusChangeScenario(source);
    }

    /*========================Scenario 2 - order not finished===========================*/

    @Test
    public void testOrderNotFinishedFromUuid() {
        CarterService carterService = mock(CarterService.class);
        testOrderNotFinished(carterService);
    }

    private void testOrderNotFinished(CarterService mockService) {
        carterPushGenerator.setCarterUserReceiver(carterUserReceiver);
        Stream<NotificationEventSource> notifications = carterPushGenerator.generate();
        notifications.forEach(event -> {
            mobileAppInfoDAO.add(new MobileAppInfo(event.getUid(), event.getUuid() == null ? "null" : event.getUuid(),
                "app", "none", MobilePlatform.ANY, false));
            NotificationEvent notificationEvent = notificationEventService.addEvent(event);
            notificationEvent.setConfig(new NotificationConfigFactory().eventConfig(NotificationSubtype.PUSH_CART));
            assertEquals(NotificationEventStatus.SENT,
                pushReplacingConsumer.processEvent(notificationEvent).getStatus());
        });
    }

    /*========================Scenario 3 - order delivered - rate model===========================*/
    @Test
    public void testRateModelFromUuid() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.LEAVE_GRADE_MODEL)
            .addTemplateParam(NotificationEventDataName.MODEL_NAME, "test model")
            .build();
        testOrderStatusChangeScenario(source);
    }

    /*========================Scenario 4 - order delivered - rate shop===========================*/

    @Test
    public void testRateShopFromUuid() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.LEAVE_GRADE_SHOP)
            .addTemplateParam(NotificationEventDataName.SHOP_NAME, "Тестовый шоп")
            .build();
        testOrderStatusChangeScenario(source);
    }
    /*======================== (Deprecated) Scenario 5 - wishlist===========================*/

    /*========================Scenario 6 - order cancelled===========================*/

    @Test
    public void testOrderCancelledFromUuid() {
        Order order = createOrder();
        ChangeRequest changeRequest = generateChangeRequest(
                ChangeRequestType.CANCELLATION,
                ChangeRequestStatus.NEW,
                Instant.now());
        order.setChangeRequests(Collections.singletonList(changeRequest));
        setupCheckouterClient(order);

        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.ORDER_STATUS_CANCELLED)
            .addTemplateParam(NotificationEventDataName.ORDER_ID, ORDER_ID.toString())
            .build();
        mobileAppInfoDAO.add(new MobileAppInfo(source.getUid(), source.getUuid() == null ? "null" : source.getUuid(),
            "app", "none", MobilePlatform.ANY, false));
        assertEquals(NotificationEventStatus.SENT,
            pushReplacingConsumer.processEvent(notificationEventService.addEvent(source)).getStatus());
    }

    /*========================Scenario 7 - push model on stock===========================*/
    @SuppressWarnings("unchecked")
    @Test
    @Disabled("События из priceAlertService больше не шлются")
    public void testModelOnStockFromUuid() {
        long modelId = 2L;
        EmailSubscription subscription = getEmailSubscription(NotificationType.PA_ON_SALE);
        subscription.getParameters().put(EmailSubscriptionParam.PARAM_MODEL_ID, String.valueOf(modelId));
        subscription.setUuid(UUID);
        addSubscriptions(Collections.singletonList(subscription), new Uuid(UUID));
        when(ytJdbcTemplate.query(any(String.class), any(RowMapper.class), anyVararg()))
            .thenReturn(Collections.singletonList(convert(subscription)));
        testModelOnStock(modelId);
    }

    private PriceAlertSubscription convert(EmailSubscription subscription) {
        return new PriceAlertSubscription(
            subscription.getEmail(),
            subscription.getUid(),
            subscription.getUuid(),
            subscription.getYandexUid(),
            Long.parseLong(subscription.getParameters().get(EmailSubscriptionParam.PARAM_MODEL_ID)),
            0.0d,
            null,
            null,
            null,
            NotificationType.PA_ON_SALE
        );
    }

    private void testModelOnStock(long modelId) {
        priceAlertService.scheduleNotifications();

        List<NotificationEvent> events = mailerNotificationEventService.getEventsForProcessing(
            Collections.singletonList(NotificationSubtype.PUSH_PRICE_ALERT), NotificationEventStatus.NEW);
        assertEquals(1, events.size());
        assertEquals(modelId, Long.parseLong(events.get(0).getData().get(
            NotificationEventDataName.PUSH_TEMPLATE_PARAM_PREFIX + NotificationEventDataName.MODEL_ID)));
        events.forEach(event -> assertEquals(NotificationEventStatus.SENT,
            pushReplacingConsumer.processEvent(event).getStatus()));
    }

    private void addSubscriptions(List<EmailSubscription> subscriptions, Identity<?> identity) {
        subscriptionAndIdentityService.createSubscriptions(
            subscriptions.get(0).getEmail(), subscriptions, identity, true);
    }

    /*========================Scenario 8 - user absent for 21 days===========================*/
    @Test
    public void testUserAbsentFromUuid() {
        testUserAbsent(null, UUID);
    }

    private void testUserAbsent(Long uid, String uuid) {
        final int _21_DAYS_AND_1_HOUR_AGO = 3600 * (24 * 21 + 1) * 1000;
        mobileAppInfoDAO.add(new MobileAppInfo(uid, uuid, "app", "FAKE_TOKEN",
            MobilePlatform.ANDROID, false, new Date(System.currentTimeMillis() - _21_DAYS_AND_1_HOUR_AGO)));
        Stream<NotificationEventSource> sourceList = userOfflinePushGenerator.generate();
        long size = sourceList.peek(event -> assertEquals(NotificationEventStatus.SENT,
            pushReplacingConsumer.processEvent(notificationEventService.addEvent(event)).getStatus())).count();
        assertTrue(size > 0);
    }

    /*========================utils===========================*/
    private EmailSubscription getEmailSubscription(NotificationType type) {
        EmailSubscription s = new EmailSubscription();
        s.setEmail("some_email@email.ru");
        s.setSubscriptionType(type);

        Map<String, String> params = new HashMap<>();
        params.put("modelId", "119387");
        params.put("regionId", "213");
        params.put("currency", "RUR");
        params.put("userName", "Иван");
        s.setParameters(params);
        s.setSubscriptionStatus(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        s.setCreationDate(new Date());
        return s;
    }

    private void testOrderStatusChangeScenario(NotificationEventSource source) {
        mobileAppInfoDAO.add(new MobileAppInfo(source.getUid(), source.getUuid() == null ? "null" : source.getUuid(),
            "app", "none", MobilePlatform.ANY, false));
        assertEquals(NotificationEventStatus.SENT,
            pushReplacingConsumer.processEvent(notificationEventService.addEvent(source)).getStatus());
    }

    private void clearDb() {
        jdbcTemplate.update("DELETE FROM MOBILE_APP_INFO");
        jdbcTemplate.update("DELETE FROM EMAIL_SUBSCRIPTION");
        jdbcTemplate.update("DELETE FROM EVENT_SOURCE");
    }

    private UserModel userFromUid(long uid) {
        return new UserModel(uid, null, null);
    }

    private UserModel userFromUuid(String uuid) {
        return new UserModel(null, uuid, null);
    }
}
