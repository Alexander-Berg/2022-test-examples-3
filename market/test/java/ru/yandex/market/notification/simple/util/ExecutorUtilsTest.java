package ru.yandex.market.notification.simple.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.notification.simple.util.ExecutorUtils.constantCallable;
import static ru.yandex.market.notification.simple.util.ExecutorUtils.getFuture;
import static ru.yandex.market.notification.simple.util.ExecutorUtils.namedCallable;
import static ru.yandex.market.notification.simple.util.ExecutorUtils.shutdown;

/**
 * Unit-тесты для {@link ExecutorUtils}.
 *
 * @author Vladislav Bauer
 */
public class ExecutorUtilsTest {

    private static final int VALUE_ONE = 1;


    @Test
    public void testConstructor() {
        ClassUtils.checkConstructor(ExecutorUtils.class);
    }

    @Test
    public void testGetFuture() {
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        assertThat(getFuture(executor.submit(() -> true)).orElse(false), equalTo(true));
        assertThat(getFuture(executor.submit(() -> { throw new RuntimeException(); })).orElse(false), equalTo(false));
        assertThat(shutdown(executor), equalTo(true));
    }

    @Test
    public void testConstantCallable() throws Exception {
        assertThat(nullCallable().call(), nullValue());
        assertThat(oneCallable().call(), equalTo(VALUE_ONE));
    }

    @Test
    public void testNamedCallable() throws Exception {
        final String name = "test";
        assertThat(namedCallable(nullCallable(), name).call(), nullValue());
        assertThat(namedCallable(oneCallable(), name).call(), equalTo(VALUE_ONE));
    }

    @Test
    public void testShutdownNegative() {
        final ExecutorService executorService = mock(ExecutorService.class);
        doThrow(RuntimeException.class).when(executorService).shutdown();
        doThrow(RuntimeException.class).when(executorService).shutdownNow();

        assertThat(shutdown(executorService), equalTo(false));
    }


    private Callable<Integer> oneCallable() {
        return constantCallable(VALUE_ONE);
    }

    private Callable<Object> nullCallable() {
        return constantCallable(null);
    }

}
