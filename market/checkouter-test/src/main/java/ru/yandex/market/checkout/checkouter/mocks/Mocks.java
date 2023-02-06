package ru.yandex.market.checkout.checkouter.mocks;

import org.mockito.internal.stubbing.answers.ThrowsException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public final class Mocks {

    private Mocks() {
    }

    public static <T> T createMock(Class<T> clazz) {
        return mock(
                clazz,
                withSettings().defaultAnswer(new ThrowsException(new RuntimeException(clazz.getSimpleName() +
                        "'s method is not defined")))
        );
    }
}
