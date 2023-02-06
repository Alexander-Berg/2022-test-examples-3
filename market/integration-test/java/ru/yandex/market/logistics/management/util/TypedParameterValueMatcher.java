package ru.yandex.market.logistics.management.util;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.hibernate.jpa.TypedParameterValue;
import org.mockito.ArgumentMatcher;

@RequiredArgsConstructor
public class TypedParameterValueMatcher implements ArgumentMatcher<TypedParameterValue> {
    private final TypedParameterValue expected;

    @Override
    public boolean matches(TypedParameterValue argument) {
        return expected.getType().equals(argument.getType())
            && Objects.deepEquals(expected.getValue(), argument.getValue());
    }
}
