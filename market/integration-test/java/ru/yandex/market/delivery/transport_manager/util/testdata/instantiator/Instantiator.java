package ru.yandex.market.delivery.transport_manager.util.testdata.instantiator;

import java.util.Optional;

import javax.annotation.Nonnull;

public interface Instantiator {
    default boolean isJsonOnly() {
        return true;
    }

    @Nonnull
    <T> Optional<T> newInstance(Class<T> dtoClass, boolean jsonOnly);
}
