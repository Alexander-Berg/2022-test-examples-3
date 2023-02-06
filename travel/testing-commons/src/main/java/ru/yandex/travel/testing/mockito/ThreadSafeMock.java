package ru.yandex.travel.testing.mockito;

import java.util.function.Consumer;

/**
 * Lets your mocks be safely initialized even under constant calls from other threads.
 * <p>
 * Important for the following cases:
 * - you mock bean is used form a background state poller
 * - the test class contains multiple test methods
 * - the poller keeps polling even during test init phases or initial part of any test method
 * - your code tries to properly initializes the mock
 * - some background call to the same mock breaks the ongoing stubbing chain, replacing it with an unrelated call
 * - your code gets either incorrectly working mock or some sort of class cast exception because of
 * returned mocked result type mismatch.
 * <p>
 * See MockitoConcurrencyFailureTest for an example.
 */
public interface ThreadSafeMock<T> {
    // discards the overridden mocks and restores the default ones
    void resetToDefaultMocks();

    // replaces the mocks with a new copy
    void initNewMocks(Consumer<T> initializer);

    // replaces the mocks with a new copy based on the current one
    void extendCurrentMocks(Consumer<T> initializer);

    T getCurrentMocksHolder();
}
