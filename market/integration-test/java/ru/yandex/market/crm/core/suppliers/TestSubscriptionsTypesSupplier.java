package ru.yandex.market.crm.core.suppliers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ru.yandex.market.crm.core.domain.subscriptions.SubscriptionType;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;

/**
 * @author vtarasoff
 * @since 20.07.2021
 */
public class TestSubscriptionsTypesSupplier implements SubscriptionsTypesSupplier {
    private final Map<SubscriptionType.Channel, Long> transactionalTypes;

    public TestSubscriptionsTypesSupplier() {
        this(Map.of());
    }

    public TestSubscriptionsTypesSupplier(Map<SubscriptionType.Channel, Long> transactionalTypes) {
        this.transactionalTypes = transactionalTypes;
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
        return SubscriptionTypes.TYPES
                .stream()
                .filter(filter)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<SubscriptionType> applyAll(Set<SubscriptionType.Channel> channels) {
        return SubscriptionTypes.TYPES
                .stream()
                .filter(type -> channels.isEmpty() || type.getChannels().stream().anyMatch(channels::contains))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public SubscriptionType getTransactional(SubscriptionType.Channel channel) {
        return Optional
                .ofNullable(transactionalTypes.get(channel))
                .map(this::resolve)
                .orElse(null);
    }
}
