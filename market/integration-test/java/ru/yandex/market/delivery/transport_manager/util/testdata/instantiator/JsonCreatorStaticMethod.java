package ru.yandex.market.delivery.transport_manager.util.testdata.instantiator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;

import ru.yandex.market.delivery.transport_manager.util.testdata.ObjectTestFieldValuesUtils;

public class JsonCreatorStaticMethod implements Instantiator {
    @Override
    @Nonnull
    public <T> Optional<T> newInstance(Class<T> dtoClass, boolean jsonOnly) {
        // Если есть статический метод с аннотацией @JsonCreator, используем его
        return Stream.of(dtoClass.getDeclaredMethods())
            .filter(m -> Modifier.isStatic(m.getModifiers()))
            .filter(m -> m.getAnnotation(JsonCreator.class) != null)
            .findFirst()
            .map((Method m1) -> ObjectTestFieldValuesUtils.callStaticMethod(m1, jsonOnly));
    }
}
