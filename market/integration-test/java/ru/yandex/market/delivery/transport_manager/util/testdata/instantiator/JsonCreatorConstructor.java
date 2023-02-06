package ru.yandex.market.delivery.transport_manager.util.testdata.instantiator;

import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;

import ru.yandex.market.delivery.transport_manager.util.testdata.ObjectTestFieldValuesUtils;

@RequiredArgsConstructor
public class JsonCreatorConstructor implements Instantiator {

    @Override
    @Nonnull
    public <T> Optional<T> newInstance(Class<T> dtoClass, boolean jsonOnly) {
        // Если есть конструктор с аннотацией @JsonCreator, используем его
        return Stream.of(dtoClass.getDeclaredConstructors())
            .filter(c -> c.getAnnotation(JsonCreator.class) != null)
            .findFirst()
            .map((Constructor<?> c1) -> ObjectTestFieldValuesUtils.callConstructor(c1, jsonOnly));
    }
}
