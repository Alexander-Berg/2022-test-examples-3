package ru.yandex.travel.testing.mockito;

import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.mockito.Mockito;

@RequiredArgsConstructor
public final class ThreadSafeMockBuilder<T> {
    private final Class<T> type;
    private Consumer<T> defaultInitializer;

    public static <T> ThreadSafeMockBuilder<T> newThreadSafeMockBuilder(Class<T> type) {
        return new ThreadSafeMockBuilder<>(type);
    }

    public ThreadSafeMockBuilder<T> withDefaultInitializer(Consumer<T> defaultInitializer) {
        this.defaultInitializer = defaultInitializer;
        return this;
    }

    // cannot use here an expression like "<ET extends T & ThreadSafeMock<T>> ET"
    @SuppressWarnings("unchecked")
    public <BT extends T> BT build() {
        return (BT) Mockito.mock(type, Mockito.withSettings()
                .defaultAnswer(new ThreadSafeMockAnswer<>(type, defaultInitializer))
                .extraInterfaces(ThreadSafeMock.class));
    }
}
