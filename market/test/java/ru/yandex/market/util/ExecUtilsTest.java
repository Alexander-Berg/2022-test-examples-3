package ru.yandex.market.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 08.11.2018
 */
public class ExecUtilsTest {
    private int initial;
    private AtomicInteger testCounter;

    @Before
    public void init() {
        initial = 5;
        testCounter = new AtomicInteger(initial);
    }

    @Test
    public void testLazy() {
        Supplier<Integer> lazy = ExecUtils.lazy(testCounter::incrementAndGet);

        // проверяем, что ленивая инициализация работает
        assertEquals(initial + 1, lazy.get().intValue());

        // проверяем, что работает только один раз
        assertEquals(initial + 1, lazy.get().intValue());
    }

    @Test(expected = ConcurrentException.class)
    public void testLazyChecked() throws Exception {
        // проверяем корректность работы с checked исключениями
        Callable<Integer> callWithException = () -> {
            throw new Exception("invalid value");
        };

        Callable<Integer> lazyChecked = ExecUtils.lazyChecked(callWithException);

        lazyChecked.call();
    }

    @Test(expected = RuntimeException.class)
    public void testLazySilent() {
        // проверяем корректность работы с checked исключениями и их превращением в unchecked
        Callable<Integer> callWithException = () -> {
            throw new Exception("invalid value");
        };

        Supplier<Integer> lazySilent = ExecUtils.lazySilent(callWithException);

        lazySilent.get();
    }

    @Test(expected = ConcurrentException.class)
    public void testLazyUnchecked() throws Exception {
        // проверяем корректность работы с unchecked исключениями
        Callable<Integer> callWithException = () -> {
            throw new RuntimeException("invalid value");
        };

        Callable<Integer> lazyUnchecked = ExecUtils.lazyChecked(callWithException);

        lazyUnchecked.call();
    }

    @Test
    public void testProcessAllOk() {
        StringBuilder builder = new StringBuilder();

        ExecUtils.tryProcessAll(
                () -> builder.append("Test "),
                () -> builder.append("started")
        );

        assertEquals("Test started", builder.toString());
    }

    @Test(expected = RuntimeException.class)
    public void testProcessWithErrors() {
        StringBuilder builder = new StringBuilder();

        ExecUtils.tryProcessAll(
                () -> builder.append("Test "),
                () -> {
                    builder.append("started ");
                    throw new RuntimeException("Failed");
                },
                () -> builder.append("again")
        );

        assertEquals("Test started again", builder.toString());
    }

    @Test
    public void testBatchedConsume() {
        List<String> data = Arrays.asList("some", "text", "to", "test", "this", "method");

        List<String> result = new ArrayList<>();
        ExecUtils.consumeBatched(data, 2, list -> result.add(String.join(" ", list)));
        assertEquals(Arrays.asList("some text", "to test", "this method"),  result);

        result.clear();
        ExecUtils.consumeBatched(data, 4, list -> result.add(String.join(" ", list)));
        assertEquals(Arrays.asList("some text to test", "this method"),  result);

        result.clear();
        ExecUtils.consumeBatched(data, 7, list -> result.add(String.join(" ", list)));
        assertEquals(Arrays.asList("some text to test this method"),  result);
    }

}
