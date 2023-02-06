package ru.yandex.market.common.mds.s3.client.util;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link CallableOperationUtils}.
 *
 * @author Vladislav Bauer
 */
public class CallableOperationUtilsTest {

    @Test
    public void testConstructorContract() {
        TestUtils.checkConstructor(CallableOperationUtils.class);
    }

    @Test
    public void testDoWithRetryPositive() {
        final Callable<Integer> operation = createOperation();
        final int maxAttempts = 3;

        final int actual = CallableOperationUtils.doWithRetry(operation, maxAttempts);
        assertThat(actual, equalTo(2));
    }

    @Test(expected = RuntimeException.class)
    public void testDoWithRetryNegative() {
        final Callable<Integer> operation = createOperation();
        final int maxAttempts = 1;

        final int actual = CallableOperationUtils.doWithRetry(operation, maxAttempts);
        fail(String.valueOf(actual));
    }


    private Callable<Integer> createOperation() {
        final AtomicInteger counter = new AtomicInteger(0);
        return () -> {
            final int result = counter.incrementAndGet();
            if (result % 2 != 0) {
                throw new RuntimeException();
            }
            return result;
        };
    }

}
