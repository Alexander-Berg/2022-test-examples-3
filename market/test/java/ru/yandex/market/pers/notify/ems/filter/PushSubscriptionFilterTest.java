package ru.yandex.market.pers.notify.ems.filter;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.TimeUnit;

import ru.yandex.market.pers.notify.ems.NotificationEventConsumer;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.filter.config.PushSubscriptionFilterConfig;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.model.subscription.Subscription;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus;
import ru.yandex.market.pers.notify.push.MobileAppInfoDAO;
import ru.yandex.market.pers.notify.subscription.SubscriptionService;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pers.notify.ems.filter.PushSubscriptionFilter.DO_NOT_SEND_CONSUMER;

/**
 * @author vtarasoff
 * @since 24.08.2021
 */
public class PushSubscriptionFilterTest extends MarketMailerMockedDbTest {
    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private PushSubscriptionFilter pushSubscriptionFilter;

    @Mock
    private NotificationEventConsumer defaultConsumer;

    @Test
    void shouldSkipNonPushEvent() {
        var event = createEvent(NotificationSubtype.BLUE_PROMO_MISC, null, null, null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(defaultConsumer, consumer);
    }

    @Test
    void shouldSkipNonStoreEvent() {
        var event = createEvent(NotificationSubtype.PUSH_SIMPLE, null, null, null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(defaultConsumer, consumer);
    }

    @Test
    void shouldFilterIfNoIdentifiersEvent() {
        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, null, null, null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldFilterIfSubscriptionByUuidNotExists() {
        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, null, "someUuid", null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldFilterIfRequiredSubscriptionByUuidNotExists() {
        addSubscription("someUuid", NotificationType.STORE_PUSH_GENERAL_ADVERTISING, SubscriptionStatus.SUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, null, "someUuid", null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldFilterIfRequiredSubscriptionByUuidNotSubscribed() {
        addSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.UNSUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, null, "someUuid", null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldFilterIfRequiredSubscriptionByUuidSubscribedButMobileAppInfoNotExists() {
        addSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, null, "someUuid", null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldPassIfRequiredSubscriptionByUuidSubscribed() {
        addMobileAppInfo(null, "someUuid", null);
        addSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, null, "someUuid", null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(defaultConsumer, consumer);
    }

    @Test
    void shouldFilterIfSubscriptionByYandexUidNotExists() {
        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, null, null, "someYandexUid");

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldFilterIfRequiredSubscriptionByYandexUidNotExists() {
        addMobileAppInfo(null, "someUuid", "someYandexUid");
        addSubscription("someUuid", NotificationType.STORE_PUSH_GENERAL_ADVERTISING, SubscriptionStatus.SUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, null, null, "someYandexUid");

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldFilterIfRequiredSubscriptionByYandexUidNotSubscribed() {
        addMobileAppInfo(null, "someUuid", "someYandexUid");
        addSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.UNSUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, null, null, "someYandexUid");

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldFilterIfRequiredSubscriptionByYandexUidSubscribedButMobileAppInfoNotExists() {
        addSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, null, null, "someYandexUid");

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldPassIfRequiredSubscriptionByYandexUidSubscribed() {
        addMobileAppInfo(null, "someUuid", "someYandexUid");
        addSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, null, null, "someYandexUid");

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(defaultConsumer, consumer);
    }

    @Test
    void shouldFilterIfSubscriptionByUidNotExists() {
        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, 1L, null, null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldFilterIfRequiredSubscriptionByUidNotExists() {
        addMobileAppInfo(1L, "someUuid", null);
        addSubscription("someUuid", NotificationType.STORE_PUSH_GENERAL_ADVERTISING, SubscriptionStatus.SUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, 1L, null, null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldFilterIfRequiredSubscriptionByUidExistsButMobileAppInfoNotExists() {
        addSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, 1L, null, null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldFilterIfLastUpdatedRequiredSubscriptionByUidUnsubscribed() throws InterruptedException {
        addMobileAppInfo(1L, "someUuid", null);
        TimeUnit.SECONDS.sleep(1L);
        addMobileAppInfo(1L, "otherUuid", null);

        addSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED);
        addSubscription("otherUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.UNSUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, 1L, null, null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldPassIfLastUpdatedRequiredSubscriptionByUidSubscribed() throws InterruptedException {
        addMobileAppInfo(1L, "someUuid", null);
        TimeUnit.SECONDS.sleep(1L);
        addMobileAppInfo(1L, "otherUuid", null);

        addSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.UNSUBSCRIBED);
        addSubscription("otherUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, 1L, null, null);

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(defaultConsumer, consumer);
    }

    @Test
    void shouldFindSubscriptionsByUuidFirst() {
        addMobileAppInfo(1L, "someUuid", "someYandexUid");
        addMobileAppInfo(1L, "otherUuid", "otherYandexUid");

        addSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED);
        addSubscription("otherUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.UNSUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, 1L, "otherUuid", "someYandexUid");

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    @Test
    void shouldFindSubscriptionsByYandexUidFirstIfNoUuid() {
        addMobileAppInfo(1L, "someUuid", null);
        addMobileAppInfo(2L, "otherUuid", "someYandexUid");

        addSubscription("someUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.SUBSCRIBED);
        addSubscription("otherUuid", NotificationType.STORE_PUSH_ORDER_STATUS, SubscriptionStatus.UNSUBSCRIBED);

        var event = createEvent(NotificationSubtype.PUSH_STORE_DELIVERED, 1L, "anotherUuid", "someYandexUid");

        var consumer = pushSubscriptionFilter.filter(event, PushSubscriptionFilterConfig.SUBSCRIBED_BY_TYPE, defaultConsumer);

        assertEquals(DO_NOT_SEND_CONSUMER, consumer);
    }

    private void addMobileAppInfo(Long uid, String uuid, String yandexUid) {
        var info = new MobileAppInfo(uid, uuid, "someAppName", "somePushToken", MobilePlatform.ANDROID, true);
        info.setYandexUid(yandexUid);

        mobileAppInfoDAO.add(info);
    }

    private void addSubscription(String uuid, NotificationType type, SubscriptionStatus status) {
        subscriptionService.saveOrUpdate(List.of(
                new Subscription(new Uuid(uuid), NotificationTransportType.PUSH, type, status)
        ));
    }

    private NotificationEvent createEvent(NotificationSubtype subtype, Long uid, String uuid, String yandexUid) {
        var event = new NotificationEvent();
        event.setNotificationSubtype(subtype);
        event.setUid(uid);
        event.setUuid(uuid);
        event.setYandexUid(yandexUid);

        return event;
    }
}
