package ru.yandex.market.crm.platform.reducers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.GenericSubscription;
import ru.yandex.market.crm.platform.models.GenericSubscription.Channel;
import ru.yandex.market.crm.platform.models.GenericSubscription.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author vtarasoff
 * @since 09.08.2021
 */
public class GenericSubscriptionReducerTest {
    private final static String FACT_ID = "GenericSubscription";

    private final GenericSubscriptionReducer reducer = new GenericSubscriptionReducer();

    private static final GenericSubscription SUBSCRIPTION = GenericSubscription.newBuilder()
            .setUid(Uids.create(UidType.UUID, "123"))
            .setId(Channel.PUSH_VALUE + "$" + 1 + "$params")
            .setChannel(Channel.PUSH)
            .setType(1)
            .setStatus(Status.UNSUBSCRIBED)
            .setCreatedAt(1)
            .setModifiedAt(1)
            .build();

    private static final GenericSubscription OTHER_SUBSCRIPTION = GenericSubscription.newBuilder()
            .setUid(Uids.create(UidType.PUID, 456))
            .setId(Channel.MAIL_VALUE + "$" + 2 + "$other_params")
            .setChannel(Channel.MAIL)
            .setType(2)
            .setStatus(Status.SUBSCRIBED)
            .setCreatedAt(2)
            .setModifiedAt(2)
            .build();

    @Test
    void shouldReduceOneNewSubscriptionCorrectly() {
        var subscriptions = reduce(List.of(), List.of(SUBSCRIPTION));

        assertEquals(Collections.singleton(SUBSCRIPTION), subscriptions);
    }

    @Test
    void shouldReduceOneStoredSubscriptionCorrectly() {
        var subscriptions = reduce(List.of(SUBSCRIPTION), List.of());

        assertEquals(Collections.singleton(SUBSCRIPTION), subscriptions);
    }

    @Test
    void shouldReduceOnlyStatusAndModifiedAtIfNewSubscriptionOlder() {
        var subscriptions = reduce(List.of(SUBSCRIPTION), List.of(OTHER_SUBSCRIPTION));

        assertEquals(
                Collections.singleton(
                        SUBSCRIPTION
                                .toBuilder()
                                .setStatus(Status.SUBSCRIBED)
                                .setModifiedAt(2)
                                .build()
                ),
                subscriptions
        );
    }

    @Test
    void shouldReduceNothingIfNewSubscriptionYounger() {
        var subscriptions = reduce(
                List.of(SUBSCRIPTION),
                List.of(
                        OTHER_SUBSCRIPTION.toBuilder().setModifiedAt(0).build()
        ));

        assertEquals(Collections.singleton(SUBSCRIPTION), subscriptions);
    }

    @Test
    void shouldReduceNothingIfUnsubscribedStatusAndSameAgeSubscriptions() {
        var subscriptions = reduce(
                List.of(SUBSCRIPTION),
                List.of(
                        OTHER_SUBSCRIPTION.toBuilder().setModifiedAt(1).build()
                ));

        assertEquals(Collections.singleton(SUBSCRIPTION), subscriptions);
    }

    @Test
    void shouldReduceToNewIfNewUnsubscribedStatusAndSameAgeSubscriptions() {
        var subscriptions = reduce(
                List.of(OTHER_SUBSCRIPTION),
                List.of(
                        SUBSCRIPTION.toBuilder().setModifiedAt(2).build()
                ));

        assertEquals(
                Collections.singleton(
                        OTHER_SUBSCRIPTION
                                .toBuilder()
                                .setStatus(Status.UNSUBSCRIBED)
                                .build()
                ),
                subscriptions
        );
    }

    @Test
    void shouldReduceToYoungestSubscription() {
        var subscriptions = reduce(
                List.of(SUBSCRIPTION),
                List.of(
                        OTHER_SUBSCRIPTION.toBuilder().setStatus(Status.CONFIRMATION).setModifiedAt(3).build(),
                        OTHER_SUBSCRIPTION
                ));

        assertEquals(
                Collections.singleton(
                        SUBSCRIPTION
                                .toBuilder()
                                .setStatus(Status.CONFIRMATION)
                                .setModifiedAt(3)
                                .build()
                ),
                subscriptions
        );
    }

    @Test
    void shouldReduceToUnsubscribedYoungestSubscription() {
        var subscriptions = reduce(
                List.of(SUBSCRIPTION),
                List.of(
                        OTHER_SUBSCRIPTION.toBuilder().setStatus(Status.UNSUBSCRIBED).build(),
                        OTHER_SUBSCRIPTION
                ));

        assertEquals(
                Collections.singleton(
                        SUBSCRIPTION
                                .toBuilder()
                                .setStatus(Status.UNSUBSCRIBED)
                                .setModifiedAt(2)
                                .build()
                ),
                subscriptions
        );
    }

    private Collection<GenericSubscription> reduce(List<GenericSubscription> stored,
                                                   List<GenericSubscription> newFacts) {
        YieldMock yield = new YieldMock();
        reducer.reduce(stored, newFacts, yield);

        return yield.getAdded(FACT_ID);
    }
}
