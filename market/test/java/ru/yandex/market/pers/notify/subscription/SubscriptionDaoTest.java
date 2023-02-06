package ru.yandex.market.pers.notify.subscription;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.Uuid;
import ru.yandex.market.pers.notify.model.subscription.Subscription;
import ru.yandex.market.pers.notify.model.subscription.SubscriptionStatus;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author vtarasoff
 * @since 08.07.2021
 */
public class SubscriptionDaoTest extends MockedDbTest {
    private static final Uuid UUID = new Uuid("123");
    private static final Uid PUID = new Uid(456L);

    private static final NotificationTransportType PUSH = NotificationTransportType.PUSH;
    private static final NotificationTransportType EMAIL = NotificationTransportType.MAIL;

    private static final Set<NotificationType> ALL_TYPES = Set.of(NotificationType.values());

    private static final NotificationType PUSH_GENERAL_ADVERTISING = NotificationType.STORE_PUSH_GENERAL_ADVERTISING;
    private static final NotificationType PUSH_LIVE_STREAM = NotificationType.STORE_PUSH_LIVE_STREAM;

    private static final NotificationType EMAIL_ADVERTISING = NotificationType.ADVERTISING;

    private static final SubscriptionStatus SUBSCRIBED = SubscriptionStatus.SUBSCRIBED;
    private static final SubscriptionStatus UNSUBSCRIBED = SubscriptionStatus.UNSUBSCRIBED;

    @Autowired
    private SubscriptionDao subscriptionDao;

    @Test
    public void shouldSaveCorrectSubscriptions() {
        var subscriptions = new Subscription[] {
                new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED),
                new Subscription(PUID, EMAIL, EMAIL_ADVERTISING, SUBSCRIBED),
                new Subscription(UUID, PUSH, PUSH_GENERAL_ADVERTISING, UNSUBSCRIBED)
        };

        asList(subscriptions).forEach(subscriptionDao::save);

        var savedSubscriptions = subscriptionDao.findAllBy(PUID, Set.of(PUSH), ALL_TYPES);
        assertThat(savedSubscriptions.size(), is(1));
        assertSavedSubscription(savedSubscriptions.get(0), PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED);

        savedSubscriptions = subscriptionDao.findAllBy(PUID, Set.of(EMAIL), ALL_TYPES);
        assertThat(savedSubscriptions.size(), is(1));
        assertSavedSubscription(savedSubscriptions.get(0), PUID, EMAIL, EMAIL_ADVERTISING, SUBSCRIBED);

        savedSubscriptions = subscriptionDao.findAllBy(UUID, Set.of(PUSH), ALL_TYPES);
        assertThat(savedSubscriptions.size(), is(1));
        assertSavedSubscription(savedSubscriptions.get(0), UUID, PUSH, PUSH_GENERAL_ADVERTISING, UNSUBSCRIBED);
    }

    private void assertSavedSubscription(Subscription subscription,
                                         Identity<?> identity,
                                         NotificationTransportType channel,
                                         NotificationType type,
                                         SubscriptionStatus status) {
        assertThat(subscription.getId(), notNullValue());
        assertThat(subscription.getIdentity(), equalTo(identity));
        assertThat(subscription.getChannel(), is(channel));
        assertThat(subscription.getType(), is(type));
        assertThat(subscription.getStatus(), is(status));
        assertThat(subscription.getCreatedAt(), notNullValue());
        assertThat(subscription.getModifiedAt(), notNullValue());
        assertThat(subscription.getParams(), equalTo(Map.of()));
    }

    @Test
    public void shouldNotSaveNullValueSubscriptions() {
        assertThrows(NullPointerException.class, () ->
                subscriptionDao.save(new Subscription(new Uid(null), PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED))
        );
        assertThrows(NullPointerException.class, () ->
                subscriptionDao.save(new Subscription(PUID, null, PUSH_GENERAL_ADVERTISING, SUBSCRIBED))
        );
        assertThrows(NullPointerException.class, () ->
                subscriptionDao.save(new Subscription(PUID, PUSH, null, SUBSCRIBED))
        );
        assertThrows(NullPointerException.class, () ->
                subscriptionDao.save(new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, null))
        );
        assertThat(subscriptionDao.count(), is(0L));
    }

    @Test
    public void shouldNotSaveDuplicateTypeSubscription() {
        var subscription = new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED);
        subscriptionDao.save(subscription);

        assertThrows(DataAccessException.class, () -> subscriptionDao.save(subscription));
        assertThat(subscriptionDao.count(), is(1L));
    }

	@Test
    public void shouldSaveDifferentTypeSubscriptions() {
        subscriptionDao.save(new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED));
        subscriptionDao.save(new Subscription(PUID, PUSH, PUSH_LIVE_STREAM, SUBSCRIBED));

        assertThat(subscriptionDao.count(), is(2L));
    }

    @Test
    public void shouldReturnSaveSubscriptionId() {
        var savedSubscription = subscriptionDao.save(new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED));
        var savedSubscriptions = subscriptionDao.findAllBy(PUID, Set.of(PUSH), ALL_TYPES);

        assertThat(savedSubscriptions.size(), is(1));
        assertThat(savedSubscriptions.get(0).getId(), equalTo(savedSubscription.getId()));
    }

    @Test
    public void shouldNotIgnoreChannelForMultiChannelSubscription() {
        subscriptionDao.save(new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED));
        subscriptionDao.save(new Subscription(PUID, EMAIL, EMAIL_ADVERTISING, SUBSCRIBED));

        var savedSubscriptions = subscriptionDao.findAllBy(PUID, ALL_TYPES);
        assertThat(savedSubscriptions.size(), is(2));

        savedSubscriptions = subscriptionDao.findAllBy(PUID, Set.of(PUSH), ALL_TYPES);
        assertThat(savedSubscriptions.size(), is(1));
        assertSavedSubscription(savedSubscriptions.get(0), PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED);

        savedSubscriptions = subscriptionDao.findAllBy(PUID, Set.of(EMAIL), ALL_TYPES);
        assertThat(savedSubscriptions.size(), is(1));
        assertSavedSubscription(savedSubscriptions.get(0), PUID, EMAIL, EMAIL_ADVERTISING, SUBSCRIBED);
    }

    @Test
    public void shouldNotSaveIncompatibleChannelSubscription() {
        assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionDao.save(new Subscription(UUID, EMAIL, PUSH_GENERAL_ADVERTISING, SUBSCRIBED))
        );
        assertThat(subscriptionDao.count(), is(0L));
    }

    @Test
    public void shouldNotUpdateNonExistentSubscription() {
        Optional<Subscription> updatedSubscription = subscriptionDao.update(
                new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED).withId(1L)
        );
        assertTrue(updatedSubscription.isEmpty());
    }

    @Test
    public void shouldUpdateOnlySubscriptionStatus() {
        var savedSubscription = subscriptionDao.save(new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED));
        subscriptionDao.update(
                new Subscription(PUID, EMAIL, EMAIL_ADVERTISING, UNSUBSCRIBED).withId(savedSubscription.getId())
        );

        var updatedSubscriptions = subscriptionDao.findAllBy(PUID, Set.of(PUSH), ALL_TYPES);
        assertThat(updatedSubscriptions.size(), is(1));
        assertSavedSubscription(updatedSubscriptions.get(0), PUID, PUSH, PUSH_GENERAL_ADVERTISING, UNSUBSCRIBED);
    }

    @Test
    public void shouldSaveCorrectSubscriptionList() {
        var subscriptions = List.of(
                new Subscription(PUID, EMAIL, EMAIL_ADVERTISING, SUBSCRIBED),
                new Subscription(UUID, PUSH, PUSH_GENERAL_ADVERTISING, UNSUBSCRIBED)
        );

        subscriptionDao.saveAll(subscriptions);

        var savedSubscriptions = subscriptionDao.findAllBy(PUID, Set.of(EMAIL), ALL_TYPES);
        assertThat(savedSubscriptions.size(), is(1));
        assertSavedSubscription(savedSubscriptions.get(0), PUID, EMAIL, EMAIL_ADVERTISING, SUBSCRIBED);

        savedSubscriptions = subscriptionDao.findAllBy(UUID, Set.of(PUSH), ALL_TYPES);
        assertThat(savedSubscriptions.size(), is(1));
        assertSavedSubscription(savedSubscriptions.get(0), UUID, PUSH, PUSH_GENERAL_ADVERTISING, UNSUBSCRIBED);
    }

    @Test
    public void shouldNotSaveDuplicateTypeSubscriptionList() {
        var subscriptions = List.of(
                new Subscription(PUID, PUSH, EMAIL_ADVERTISING, SUBSCRIBED),
                new Subscription(PUID, PUSH, EMAIL_ADVERTISING, UNSUBSCRIBED)
        );

        assertThrows(DataAccessException.class, () -> subscriptionDao.saveAll(subscriptions));
        assertThat(subscriptionDao.count(), is(0L));
    }

    @Test
    public void shouldSaveDifferentTypeSubscriptionList() {
        var subscriptions = List.of(
                new Subscription(PUID, PUSH, EMAIL_ADVERTISING, SUBSCRIBED),
                new Subscription(PUID, PUSH, PUSH_LIVE_STREAM, SUBSCRIBED)
        );

        subscriptionDao.saveAll(subscriptions);
        assertThat(subscriptionDao.count(), is(2L));
    }

    @Test
    public void shouldNotSaveIncompatibleChannelSubscriptionList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> subscriptionDao.saveAll(List.of(
                        new Subscription(UUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED),
                        new Subscription(UUID, EMAIL, PUSH_GENERAL_ADVERTISING, SUBSCRIBED)
                ))
        );
        assertThat(subscriptionDao.count(), is(0L));
    }

    @Test
    public void shouldReturnSaveSubscriptionIds() {
        var subscriptions = List.of(
                new Subscription(PUID, EMAIL, EMAIL_ADVERTISING, SUBSCRIBED),
                new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, UNSUBSCRIBED)
        );

        subscriptionDao.saveAll(subscriptions);
        List<Subscription> savedSubscriptions = subscriptionDao.findAllBy(PUID, ALL_TYPES);

        assertThat(savedSubscriptions.size(), is(2));
        assertThat(savedSubscriptions.get(0).getId(), notNullValue());
        assertThat(savedSubscriptions.get(1).getId(), notNullValue());
    }

    @Test
    public void shouldNotIgnoreChannelForMultiChannelSubscriptionList() {
        var subscriptions = List.of(
                new Subscription(PUID, EMAIL, EMAIL_ADVERTISING, SUBSCRIBED),
                new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED)
        );

        subscriptionDao.saveAll(subscriptions);

        var savedSubscriptions = subscriptionDao.findAllBy(PUID, ALL_TYPES);
        assertThat(savedSubscriptions.size(), is(2));

        savedSubscriptions = subscriptionDao.findAllBy(PUID, Set.of(PUSH), ALL_TYPES);
        assertThat(savedSubscriptions.size(), is(1));
        assertSavedSubscription(savedSubscriptions.get(0), PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED);

        savedSubscriptions = subscriptionDao.findAllBy(PUID, Set.of(EMAIL), ALL_TYPES);
        assertThat(savedSubscriptions.size(), is(1));
        assertSavedSubscription(savedSubscriptions.get(0), PUID, EMAIL, EMAIL_ADVERTISING, SUBSCRIBED);
    }

    @Test
    public void shouldNotUpdateNonExistentSubscriptionList() {
        List<Subscription> updatedSubscriptions = subscriptionDao.updateAll(List.of(
                new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED).withId(1L),
                new Subscription(PUID, EMAIL, EMAIL_ADVERTISING, SUBSCRIBED).withId(2L)
        ));
        assertTrue(updatedSubscriptions.isEmpty());
    }

    @Test
    public void shouldUpdateOnlySubscriptionListStatus() {
        List<Subscription> savedSubscriptions = subscriptionDao.saveAll(
                List.of(new Subscription(PUID, PUSH, PUSH_GENERAL_ADVERTISING, SUBSCRIBED))
        );

        assertThat(savedSubscriptions.size(), is(1));

        subscriptionDao.updateAll(List.of(
                new Subscription(PUID, EMAIL, EMAIL_ADVERTISING, UNSUBSCRIBED).withId(savedSubscriptions.get(0).getId())
        ));

        var updatedSubscriptions = subscriptionDao.findAllBy(PUID, Set.of(PUSH), ALL_TYPES);
        assertThat(updatedSubscriptions.size(), is(1));
        assertSavedSubscription(updatedSubscriptions.get(0), PUID, PUSH, PUSH_GENERAL_ADVERTISING, UNSUBSCRIBED);
    }
}
