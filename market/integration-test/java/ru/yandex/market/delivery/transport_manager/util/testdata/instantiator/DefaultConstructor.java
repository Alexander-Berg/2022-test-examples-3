package ru.yandex.market.delivery.transport_manager.util.testdata.instantiator;

import java.lang.reflect.Constructor;
import java.util.Optional;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;

import ru.yandex.market.delivery.transport_manager.util.testdata.SetFieldValuesUtil;

public class DefaultConstructor implements Instantiator {
    @Override
    @SneakyThrows
    @Nonnull
    public <T> Optional<T> newInstance(Class<T> aClass, boolean jsonOnly) {
        Constructor<T> defaultConstructor;
        try {
            defaultConstructor = aClass.getDeclaredConstructor();
            defaultConstructor.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            return Optional.empty();
        }

        return Optional.ofNullable(defaultConstructor.newInstance())
            .map(i -> {
                SetFieldValuesUtil.setFieldValues(i, jsonOnly);
                return i;
            });
    }

}
