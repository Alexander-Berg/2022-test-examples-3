package ru.yandex.market.crm.triggers.services.subscription;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType.Channel;
import ru.yandex.market.crm.core.services.subscription.SubscriptionResolver;
import ru.yandex.market.crm.core.services.subscription.SubscriptionService;
import ru.yandex.market.crm.core.suppliers.SubscriptionsTypesSupplier;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.models.Subscription;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionResolverTest {

    private static class TestSubscriptionsTypesSupplier implements SubscriptionsTypesSupplier {
        private final List<SubscriptionType> subscriptionTypes = List.of(
                subscriptionType(ADVERTISING_TYPE, false),
                subscriptionType(CART_TYPE, true),
                subscriptionType(NEW_QA_ANSWERS, false)
        );

        private SubscriptionType subscriptionType(long id, boolean isDefault) {
            return new SubscriptionType(id, "", "", isDefault, false, Set.of(Channel.EMAIL));
        }

        @Override
        public SubscriptionType resolve(long id) {
            return resolve(type -> type.getId() == id);
        }

        @Override
        public SubscriptionType resolve(String name) {
            return resolve(type -> type.getName().equals(name));
        }

        private SubscriptionType resolve(Predicate<SubscriptionType> filter) {
            return subscriptionTypes
                    .stream()
                    .filter(filter::test)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<SubscriptionType> applyAll(Set<Channel> channels) {
            return subscriptionTypes
                    .stream()
                    .filter(type -> channels.isEmpty() || type.getChannels().stream().anyMatch(channels::contains))
                    .collect(Collectors.toUnmodifiableList());
        }

        @Override
        public SubscriptionType getTransactional(Channel channel) {
            return null;
        }
    }

    private final static long ADVERTISING_TYPE = 2;
    private final static long CART_TYPE = 12;
    private final static long NEW_QA_ANSWERS = 48;
    private final static String EMAIL1 = "email1@yandex.ru", EMAIL2 = "email2@ya.ru", EMAIL3 = "email3@yandex.ua";

    @Mock
    private SubscriptionService service;

    private SubscriptionResolver resolver;

    @Before
    public void before() {
        SubscriptionsTypesSupplier supplier = new TestSubscriptionsTypesSupplier();
        resolver = new SubscriptionResolver(service, supplier);
    }

    @Test
    public void userHasNoEmailsTest() {
        User user = user();
        assertFalse(resolver.subscriptionsForEmails(user.getEmails(), ADVERTISING_TYPE, null).findFirst().isPresent());
    }

    @Test
    public void userSubscribedTest() {
        User user = user(EMAIL1);
        when(service.getSubscriptions(user.getEmails(), ADVERTISING_TYPE))
                .thenReturn(asList(
                        state(EMAIL1, ADVERTISING_TYPE, null, true, true),
                        state(EMAIL2, ADVERTISING_TYPE, null, false, true)
                ));
        assertTrue(resolver.subscriptionsForEmails(user.getEmails(), ADVERTISING_TYPE, null)
                .map(x -> x.getUid().getStringValue())
                .findFirst().isPresent());
        assertEquals(ImmutableSet.of(EMAIL1), resolver.subscriptionsForEmails(user.getEmails(), ADVERTISING_TYPE, null)
                .map(x -> x.getUid().getStringValue())
                .collect(Collectors.toSet()));
    }

    @Test
    public void userSubscribedWithParameter() {
        User user = user(EMAIL1);
        String questionId = "123321";
        when(service.getSubscriptions(user.getEmails(), NEW_QA_ANSWERS, questionId))
                .thenReturn(Collections.singletonList(state(EMAIL1, NEW_QA_ANSWERS, questionId, true, true)));

        assertTrue(resolver.subscriptionsForEmails(user.getEmails(), NEW_QA_ANSWERS, questionId).findFirst().isPresent());
    }

    @Test
    public void userNotUnsubscribed() {
        User user = user(EMAIL1, EMAIL2, EMAIL3);
        // user has no states for CART
        when(service.getSubscriptions(user.getEmails(), CART_TYPE))
                .thenReturn(emptyList());

        Set<String> subscribedEmails = resolver.subscriptionsForEmails(user.getEmails(), CART_TYPE, null)
                .map(x -> x.getUid().getStringValue())
                .collect(Collectors.toSet());

        assertEquals(user.getEmails(), subscribedEmails);

        // user still has 1 not unsubscribed email
        when(service.getSubscriptions(user.getEmails(), CART_TYPE))
                .thenReturn(asList(
                        // active but invalid
                        state(EMAIL1, CART_TYPE, null, true, false),
                        // valid but inactive
                        state(EMAIL2, CART_TYPE, null, false, true)
                ));

        subscribedEmails = resolver.subscriptionsForEmails(user.getEmails(), CART_TYPE, null)
                .map(x -> x.getUid().getStringValue())
                .collect(Collectors.toSet());

        assertEquals(ImmutableSet.of(EMAIL3), subscribedEmails);
    }

    private Subscription state(String email, long id, @Nullable String parameter, boolean active, boolean valid) {
        Subscription.Builder builder = Subscription.newBuilder()
                .setUid(Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, email))
                .setType(id)
                .setActive(active)
                .setEmailValid(valid);

        if (parameter != null) {
            builder.setParameter(parameter);
        }

        return builder.build();
    }

    private User user(String... emails) {
        IdsGraph graph = new IdsGraph();
        graph.setNodes(
                Stream.of(emails)
                        .map(email -> Uid.of(UidType.EMAIL, email))
                        .collect(Collectors.toList())
        );

        User user = new User("id");
        user.setIdsGraph(graph);
        return user;
    }
}
