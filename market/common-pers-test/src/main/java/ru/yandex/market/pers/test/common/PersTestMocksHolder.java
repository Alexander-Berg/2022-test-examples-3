package ru.yandex.market.pers.test.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 05.03.2021
 */
public class PersTestMocksHolder {
    private static final ConcurrentHashMap<Object, Runnable> MOCKS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Object> MOCK_ADDITION = new ConcurrentHashMap<>();

    public static void resetMocks() {
        MOCK_ADDITION.clear();
        for (Map.Entry<Object, Runnable> entry : MOCKS.entrySet()) {
            Mockito.reset(entry.getKey());
            entry.getValue().run();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAddition(String key, Supplier<T> generator) {
        return (T) MOCK_ADDITION.computeIfAbsent(
            key, k -> generator.get());
    }

    public static <T> T registerMock(Class<T> type) {
        return registerMock(type, mock -> {
            MOCKS.put(mock, () -> {});
        });
    }

    public static <T> T registerMock(Class<T> type, Consumer<T> initializer) {
        T result = mock(type);
        initializer.accept(result);
        MOCKS.put(result, () -> initializer.accept(result));
        return result;
    }
}
