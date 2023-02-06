package ru.yandex.market.pers.notify.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import java.util.List;

import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.subscription.Subscription;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionHistoryItem;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionHistoryItem.EventType;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.pers.notify.model.NotificationTransportType.PUSH;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_PUSH_GENERAL_ADVERTISING;
import static ru.yandex.market.pers.notify.model.subscription.SubscriptionHistoryItem.EventType.CREATED;
import static ru.yandex.market.pers.notify.model.subscription.SubscriptionHistoryItem.EventType.DELETED;
import static ru.yandex.market.pers.notify.model.subscription.SubscriptionHistoryItem.EventType.UPDATED;
import static ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus.SUBSCRIBED;

/**
 * @author vtarasoff
 * @since 08.07.2021
 */
public class SubscriptionHistoryDaoTest extends MockedDbTest {
    @Autowired
    private SubscriptionDao subscriptionDao;

    @Autowired
    private SubscriptionHistoryDao subscriptionHistoryDao;

    private Subscription subscription;

    @BeforeEach
    public void setUp() {
        subscription = subscriptionDao.save(
                new Subscription(new Uuid("123"), PUSH, STORE_PUSH_GENERAL_ADVERTISING, SUBSCRIBED)
        );
    }

	@Test
	public void shouldCreateCorrectHistory() {
        var otherSubscription = subscriptionDao.save(
                new Subscription(new Uuid("456"), PUSH, STORE_PUSH_GENERAL_ADVERTISING, SUBSCRIBED)
        );

        subscriptionHistoryDao.saveAll(List.of(
                SubscriptionHistoryItem.created(subscription.getId(), "paramA", "valueA"),
                SubscriptionHistoryItem.created(subscription.getId(), "paramB", null),
                SubscriptionHistoryItem.updated(subscription.getId(), "paramA", null),
                SubscriptionHistoryItem.updated(subscription.getId(), "paramB", "valueB"),
                SubscriptionHistoryItem.deleted(subscription.getId(), "paramA"),
                SubscriptionHistoryItem.deleted(subscription.getId(), "paramB"),
                SubscriptionHistoryItem.created(otherSubscription.getId(), "paramC", "valueC"),
                SubscriptionHistoryItem.created(otherSubscription.getId(), "paramD", null),
                SubscriptionHistoryItem.updated(otherSubscription.getId(), "paramC", null),
                SubscriptionHistoryItem.updated(otherSubscription.getId(), "paramD", "valueD"),
                SubscriptionHistoryItem.deleted(otherSubscription.getId(), "paramC"),
                SubscriptionHistoryItem.deleted(otherSubscription.getId(), "paramD")
        ));

        assertThat(subscriptionHistoryDao.count(), is(12L));

        List<SubscriptionHistoryItem> items = subscriptionHistoryDao.findAllBy(subscription.getId());
        assertThat(items.size(), is(6));

        assertHistoryItem(items.get(0), subscription.getId(), CREATED, "paramA", "valueA");
        assertHistoryItem(items.get(1), subscription.getId(), CREATED, "paramB", null);
        assertHistoryItem(items.get(2), subscription.getId(), UPDATED, "paramA", null);
        assertHistoryItem(items.get(3), subscription.getId(), UPDATED, "paramB", "valueB");
        assertHistoryItem(items.get(4), subscription.getId(), DELETED, "paramA", null);
        assertHistoryItem(items.get(5), subscription.getId(), DELETED, "paramB", null);

        items = subscriptionHistoryDao.findAllBy(otherSubscription.getId());
        assertThat(items.size(), is(6));

        assertHistoryItem(items.get(0), otherSubscription.getId(), CREATED, "paramC", "valueC");
        assertHistoryItem(items.get(1), otherSubscription.getId(), CREATED, "paramD", null);
        assertHistoryItem(items.get(2), otherSubscription.getId(), UPDATED, "paramC", null);
        assertHistoryItem(items.get(3), otherSubscription.getId(), UPDATED, "paramD", "valueD");
        assertHistoryItem(items.get(4), otherSubscription.getId(), DELETED, "paramC", null);
        assertHistoryItem(items.get(5), otherSubscription.getId(), DELETED, "paramD", null);
	}

	private void assertHistoryItem(SubscriptionHistoryItem item,
                                   long subscriptionId,
                                   EventType eventType,
                                   String name,
                                   String value) {
        assertThat(item.getId(), notNullValue());
        assertThat(item.getSubscriptionId(), is(subscriptionId));
        assertThat(item.getEventDate(), notNullValue());
        assertThat(item.getEventType(), is(eventType));
        assertThat(item.getName(), equalTo(name));
        assertThat(item.getValue(), value != null ? equalTo(value) : nullValue());
    }

    @Test
    public void shouldNotCreateNonExistentSubscriptionHistory() {
        assertThrows(DataAccessException.class, () -> subscriptionHistoryDao.saveAll(List.of(
                SubscriptionHistoryItem.created(subscription.getId() + 1, "param", "value")
        )));

        assertThat(subscriptionHistoryDao.count(), is(0L));
    }

    @Test
    public void shouldNotCreateNullParamNameHistory() {
        assertThrows(DataAccessException.class, () -> subscriptionHistoryDao.saveAll(List.of(
                SubscriptionHistoryItem.created(subscription.getId(), null, "value")
        )));

        assertThat(subscriptionHistoryDao.count(), is(0L));
    }
}
