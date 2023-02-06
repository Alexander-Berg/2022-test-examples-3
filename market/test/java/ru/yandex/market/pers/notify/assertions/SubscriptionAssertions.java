package ru.yandex.market.pers.notify.assertions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import ru.yandex.market.pers.notify.export.ChangedSubscriptionDao;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.subscription.Subscription;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionHistoryItem;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus;
import ru.yandex.market.pers.notify.subscription.SubscriptionHistoryDao;
import ru.yandex.market.pers.notify.subscription.SubscriptionService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.notify.subscription.MobileAppSubscriptionsMigrator.DEFAULT_PUSH_TYPES;

/**
 * @author vtarasoff
 * @since 05.10.2021
 */
@Service
public class SubscriptionAssertions {
    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionHistoryDao subscriptionHistoryDao;

    @Autowired
    private ChangedSubscriptionDao changedSubscriptionDao;

    public void assertNoSubscriptions(String uuid) {
        var identity = new Uuid(uuid);

        List<Subscription> subscriptions = subscriptionService.get(identity, DEFAULT_PUSH_TYPES);
        assertTrue(subscriptions.isEmpty());
    }

    public void assertSubscriptions(Set<NotificationType> pushTypes,
                                     String uuid,
                                     SubscriptionStatus status,
                                     List<SubscriptionHistoryItem> historyItems) {
        pushTypes.forEach(type -> assertSubscription(uuid, type, status, historyItems));
    }

    public void assertSubscription(String uuid,
                                    NotificationType type,
                                    SubscriptionStatus status,
                                    List<SubscriptionHistoryItem> historyItems) {
        var identity = new Uuid(uuid);
        var channel = NotificationTransportType.PUSH;

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
}
