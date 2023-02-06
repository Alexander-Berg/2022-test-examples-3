package ru.yandex.market.pers.notify.subscription;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

import ru.yandex.market.pers.notify.export.ChangedSubscriptionDao;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.subscription.Subscription;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionHistoryItem;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.notify.model.NotificationTransportType.MAIL;
import static ru.yandex.market.pers.notify.model.NotificationTransportType.PUSH;
import static ru.yandex.market.pers.notify.model.NotificationType.ADVERTISING;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_PUSH_GENERAL_ADVERTISING;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_PUSH_LIVE_STREAM;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_PUSH_ORDER_STATUS;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_PUSH_PERSONAL_ADVERTISING;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_PUSH_UGC;
import static ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus.SUBSCRIBED;
import static ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus.UNSUBSCRIBED;

/**
 * @author vtarasoff
 * @since 08.07.2021
 */
public class SubscriptionServiceTest extends MockedDbTest {
    private static final Uid PUID = new Uid(123L);
    private static final Uuid UUID = new Uuid("456");

    private static final String PARAM_STATUS = "status";

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionDao subscriptionDao;

    @Autowired
    private SubscriptionHistoryDao subscriptionHistoryDao;

    @Autowired
    private ChangedSubscriptionDao changedSubscriptionDao;

    @Test
    public void shouldSaveAndUpdateCorrectSubscriptions() {
        var advertising = new Subscription(PUID, PUSH, STORE_PUSH_GENERAL_ADVERTISING, SUBSCRIBED);
        var liveStream = new Subscription(PUID, PUSH, STORE_PUSH_LIVE_STREAM, SUBSCRIBED);
        var personal = new Subscription(PUID, PUSH, STORE_PUSH_PERSONAL_ADVERTISING, UNSUBSCRIBED);

        subscriptionService.saveOrUpdate(List.of(advertising, liveStream, personal));

        liveStream.setStatus(UNSUBSCRIBED);
        personal.setStatus(SUBSCRIBED);

        var ugc = new Subscription(PUID, PUSH, STORE_PUSH_UGC, SUBSCRIBED);
        var orderStatus = new Subscription(PUID, PUSH, STORE_PUSH_ORDER_STATUS, UNSUBSCRIBED);

        subscriptionService.saveOrUpdate(List.of(advertising, liveStream, personal, ugc, orderStatus));

        assertThat(subscriptionDao.count(), is(5L));
        assertThat(changedSubscriptionDao.findAll().size(), is(7));

        assertSubscriptionAndHistoryAndChanged(PUID, PUSH, STORE_PUSH_GENERAL_ADVERTISING, SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(0L, PARAM_STATUS, String.valueOf(SUBSCRIBED.ordinal()))
        ));

        assertSubscriptionAndHistoryAndChanged(PUID, PUSH, STORE_PUSH_LIVE_STREAM, UNSUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(0L, PARAM_STATUS, String.valueOf(SUBSCRIBED.ordinal())),
                SubscriptionHistoryItem.updated(0L, PARAM_STATUS, String.valueOf(UNSUBSCRIBED.ordinal()))
        ));

        assertSubscriptionAndHistoryAndChanged(PUID, PUSH, STORE_PUSH_PERSONAL_ADVERTISING, SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(0L, PARAM_STATUS, String.valueOf(UNSUBSCRIBED.ordinal())),
                SubscriptionHistoryItem.updated(0L, PARAM_STATUS, String.valueOf(SUBSCRIBED.ordinal()))
        ));

        assertSubscriptionAndHistoryAndChanged(PUID, PUSH, STORE_PUSH_UGC, SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(0L, PARAM_STATUS, String.valueOf(SUBSCRIBED.ordinal()))
        ));

        assertSubscriptionAndHistoryAndChanged(PUID, PUSH, STORE_PUSH_ORDER_STATUS, UNSUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(0L, PARAM_STATUS, String.valueOf(UNSUBSCRIBED.ordinal()))
        ));
    }

    private void assertSubscriptionAndHistoryAndChanged(Identity<?> identity,
                                                        NotificationTransportType channel,
                                                        NotificationType type,
                                                        SubscriptionStatus status,
                                                        List<SubscriptionHistoryItem> historyItems) {
        List<Subscription> subscriptions = subscriptionService.get(identity, Set.of(channel), Set.of(type));
        assertThat(subscriptions.size(), is(1));

        var subscription = subscriptions.get(0);
        assertThat(subscription.getIdentity(), equalTo(identity));
        assertThat(subscription.getChannel(), is(channel));
        assertThat(subscription.getType(), is(type));
        assertThat(subscription.getStatus(), is(status));

        assertSubscriptionHistory(subscription.getId(), historyItems);
        assertChangedSubscription(subscription.getId());
    }

    private void assertSubscriptionHistory(long subscriptionId, List<SubscriptionHistoryItem> expected) {
        List<SubscriptionHistoryItem> actual = subscriptionHistoryDao.findAllBy(subscriptionId);
        assertThat(actual.size(), is(expected.size()));

        for (int i = 0; i < actual.size(); i++) {
            var actualItem = actual.get(i);
            var expectedItem = expected.get(i);

            assertThat(actualItem.getEventDate(), notNullValue());
            assertThat(actualItem.getEventType(), is(expectedItem.getEventType()));
            assertThat(actualItem.getName(), equalTo(expectedItem.getName()));
            assertThat(actualItem.getValue(), equalTo(expectedItem.getValue()));
        }
    }

    private void assertChangedSubscription(long subscriptionId) {
        long actualCount = changedSubscriptionDao.findAll()
                .stream()
                .filter(id -> id.equals(subscriptionId))
                .count();

        assertThat(actualCount, greaterThan(0L));
    }

    @Test
    public void shouldSaveOrUpdateMultiIdentitySubscriptions() {
        var puidPushAdvertising = new Subscription(PUID, PUSH, STORE_PUSH_GENERAL_ADVERTISING, SUBSCRIBED);
        var puidEmailAdvertising = new Subscription(PUID, MAIL, ADVERTISING, SUBSCRIBED);
        var uuidAdvertising = new Subscription(UUID, PUSH, STORE_PUSH_GENERAL_ADVERTISING, SUBSCRIBED);

        subscriptionService.saveOrUpdate(List.of(puidPushAdvertising, puidEmailAdvertising, uuidAdvertising));

        assertThat(subscriptionDao.count(), is(3L));
        assertThat(changedSubscriptionDao.findAll().size(), is(3));

        assertSubscriptionAndHistoryAndChanged(PUID, PUSH, STORE_PUSH_GENERAL_ADVERTISING, SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(0L, PARAM_STATUS, String.valueOf(SUBSCRIBED.ordinal()))
        ));

        assertSubscriptionAndHistoryAndChanged(PUID, MAIL, ADVERTISING, SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(0L, PARAM_STATUS, String.valueOf(SUBSCRIBED.ordinal()))
        ));

        assertSubscriptionAndHistoryAndChanged(UUID, PUSH, STORE_PUSH_GENERAL_ADVERTISING, SUBSCRIBED, List.of(
                SubscriptionHistoryItem.created(0L, PARAM_STATUS, String.valueOf(SUBSCRIBED.ordinal()))
        ));
    }

    @Test
    public void shouldNotSaveAndUpdateSubscriptionsIfNotCorrectAny() {
        var puidAdvertising = new Subscription(PUID, PUSH, STORE_PUSH_GENERAL_ADVERTISING, SUBSCRIBED);
        var uuidAdvertising = new Subscription(UUID, MAIL, STORE_PUSH_GENERAL_ADVERTISING, SUBSCRIBED);

        assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionService.saveOrUpdate(List.of(puidAdvertising, uuidAdvertising))
        );

        assertThat(subscriptionDao.count(), is(0L));
        assertThat(subscriptionHistoryDao.count(), is(0L));
        assertTrue(changedSubscriptionDao.findAll().isEmpty());
    }
}
