package ru.yandex.market.delivery.transport_manager.util.testdata.instantiator;

import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import ru.yandex.market.delivery.transport_manager.util.testdata.ObjectTestFieldValuesUtils;

public class AnyConstructor implements Instantiator {
    @Override
    public boolean isJsonOnly() {
        return false;
    }

    @Override
    @Nonnull
    public <T> Optional<T> newInstance(Class<T> dtoClass, boolean jsonOnly) {
        return Stream.of(dtoClass.getDeclaredConstructors())
            .findFirst()
            .map((Constructor<?> c) -> ObjectTestFieldValuesUtils.callConstructor(c, jsonOnly));
    }
}
