package ru.yandex.market.delivery.transport_manager.util.testdata.instantiator;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.RequiredArgsConstructor;

import ru.yandex.market.delivery.transport_manager.util.testdata.ObjectTestFieldValuesUtils;

@RequiredArgsConstructor
public class JsonBuilderWithDefaultConstructor implements Instantiator {
    private final Instantiator builderInstantiator;
    @Override
    @Nonnull
    public <T> Optional<T> newInstance(Class<T> dtoClass, boolean jsonOnly) {
        return Optional.ofNullable(dtoClass.getAnnotation(JsonDeserialize.class))
            .map(JsonDeserialize::builder)
            .flatMap(c -> builderInstantiator.newInstance(c, false))
            .map(ObjectTestFieldValuesUtils::callBuilder);
    }
}
