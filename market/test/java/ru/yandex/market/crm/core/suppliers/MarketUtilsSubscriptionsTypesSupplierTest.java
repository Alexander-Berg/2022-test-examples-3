package ru.yandex.market.crm.core.suppliers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType.Channel;
import ru.yandex.market.crm.core.services.pers.MarketUtilsClient;
import ru.yandex.market.crm.core.services.pers.MarketUtilsService;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author vtarasoff
 * @since 21.07.2021
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MarketUtilsSubscriptionsTypesSupplierTest {
    private static final SubscriptionType SUBSCRIPTION_1
            = new SubscriptionType(1, "name1", "descr1", false, false, Set.of(Channel.EMAIL));
    private static final SubscriptionType SUBSCRIPTION_2
            = new SubscriptionType(2, "name2", "descr2", true, false, Set.of(Channel.EMAIL));
    private static final SubscriptionType SUBSCRIPTION_3
            = new SubscriptionType(2, "name2", "descr2", true, false, Set.of(Channel.PUSH));
    private static final SubscriptionType SUBSCRIPTION_4
            = new SubscriptionType(4, "name4", "descr4", true, true, Set.of(Channel.PUSH));

    private static final List<SubscriptionType> TYPES = List.of(
            SUBSCRIPTION_1, SUBSCRIPTION_2, SUBSCRIPTION_3, SUBSCRIPTION_4
    );

    @Mock
    private MarketUtilsClient client;

    private MarketUtilsService service;
    private MarketUtilsSubscriptionsTypesSupplier subscriptionsTypesSupplier;

    @BeforeEach
    void setUp() {
        subscriptionsTypesSupplier = new MarketUtilsSubscriptionsTypesSupplier(() -> service, Map.of());
        service = new MarketUtilsService(client, subscriptionsTypesSupplier);
    }

    @Test
    void shouldThrowsErrorIfMarketUtilsErrorAndFirstRequest() {
        when(client.getSubscriptionTypes()).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> subscriptionsTypesSupplier.resolve(SUBSCRIPTION_1.getId()));
        assertThrows(RuntimeException.class, () -> subscriptionsTypesSupplier.resolve(SUBSCRIPTION_1.getName()));
        assertThrows(RuntimeException.class, () -> subscriptionsTypesSupplier.apply(Set.of()));
        assertThrows(RuntimeException.class, () -> subscriptionsTypesSupplier.applyAll(Set.of()));
    }

    @Test
    void shouldReturnCachedIfMarketUtilsErrorAndFirstRequest() {
        when(client.getSubscriptionTypes()).thenReturn(TYPES);

        subscriptionsTypesSupplier.resolve(1L);

        when(client.getSubscriptionTypes()).thenThrow(new RuntimeException());

        assertEquals(SUBSCRIPTION_1, subscriptionsTypesSupplier.resolve(SUBSCRIPTION_1.getId()));
        assertEquals(SUBSCRIPTION_1, subscriptionsTypesSupplier.resolve(SUBSCRIPTION_1.getName()));
        assertEquals(nonDeprecatedTypes(TYPES), subscriptionsTypesSupplier.apply(Set.of()));
        assertEquals(TYPES, subscriptionsTypesSupplier.applyAll(Set.of()));
    }

    @Test
    void shouldFilterReturnedByChannels() {
        when(client.getSubscriptionTypes()).thenReturn(TYPES);

        assertEquals(
                nonDeprecatedTypes(TYPES),
                subscriptionsTypesSupplier.apply(Set.of(Channel.EMAIL, Channel.PUSH))
        );
        assertEquals(TYPES, subscriptionsTypesSupplier.applyAll(Set.of(Channel.EMAIL, Channel.PUSH)));

        List<SubscriptionType> emailTypes = List.of(SUBSCRIPTION_1, SUBSCRIPTION_2);
        List<SubscriptionType> pushTypes = List.of(SUBSCRIPTION_3, SUBSCRIPTION_4);

        assertEquals(nonDeprecatedTypes(emailTypes), subscriptionsTypesSupplier.apply(Set.of(Channel.EMAIL)));
        assertEquals(emailTypes, subscriptionsTypesSupplier.applyAll(Set.of(Channel.EMAIL)));

        assertEquals(nonDeprecatedTypes(pushTypes), subscriptionsTypesSupplier.apply(Set.of(Channel.PUSH)));
        assertEquals(pushTypes, subscriptionsTypesSupplier.applyAll(Set.of(Channel.PUSH)));
    }

    @Test
    void shouldReturnNullIfNotExists() {
        when(client.getSubscriptionTypes()).thenReturn(TYPES);

        assertNull(subscriptionsTypesSupplier.resolve(-1L));
        assertNull(subscriptionsTypesSupplier.resolve("some name"));
    }

    @Test
    void shouldReturnCachedIfNotTimeToReload() {
        when(client.getSubscriptionTypes()).thenReturn(TYPES);

        subscriptionsTypesSupplier.resolve(1L);

        when(client.getSubscriptionTypes()).thenReturn(List.of());

        assertEquals(SUBSCRIPTION_1, subscriptionsTypesSupplier.resolve(SUBSCRIPTION_1.getId()));
        assertEquals(SUBSCRIPTION_1, subscriptionsTypesSupplier.resolve(SUBSCRIPTION_1.getName()));
        assertEquals(nonDeprecatedTypes(TYPES), subscriptionsTypesSupplier.apply(Set.of()));
        assertEquals(TYPES, subscriptionsTypesSupplier.applyAll(Set.of()));
    }

    @Test
    void shouldReturnFreshIfTimeToReload() {
        when(client.getSubscriptionTypes()).thenReturn(TYPES);

        subscriptionsTypesSupplier.resolve(1L);

        SubscriptionType type1
                = new SubscriptionType(-1, "name-1", "descr-1", false, false, Set.of(Channel.values()));
        SubscriptionType type2
                = new SubscriptionType(-2, "name-2", "descr-2", true, false, Set.of(Channel.values()));
        SubscriptionType type3
                = new SubscriptionType(-3, "name-3", "descr-3", false, true, Set.of(Channel.values()));
        SubscriptionType type4
                = new SubscriptionType(-4, "name-4", "descr-4", true, true, Set.of(Channel.values()));

        List<SubscriptionType> newTypes = List.of(type1, type2, type3, type4);

        when(client.getSubscriptionTypes()).thenReturn(newTypes);

        subscriptionsTypesSupplier.setMinCacheUpdatingInterval(Duration.ofMinutes(-1));

        assertEquals(type1, subscriptionsTypesSupplier.resolve(type1.getId()));
        assertEquals(type1, subscriptionsTypesSupplier.resolve(type1.getName()));
        assertEquals(nonDeprecatedTypes(newTypes), subscriptionsTypesSupplier.apply(Set.of()));
        assertEquals(newTypes, subscriptionsTypesSupplier.applyAll(Set.of()));
    }

    private List<SubscriptionType> nonDeprecatedTypes(List<SubscriptionType> types) {
        return types
                .stream()
                .filter(not(SubscriptionType::isDeprecated))
                .collect(Collectors.toUnmodifiableList());
    }
}
