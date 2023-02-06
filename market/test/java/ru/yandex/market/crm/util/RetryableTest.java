package ru.yandex.market.crm.util;

import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RetryableTest {

    @Test
    public void nonZeroRetryCount() {
        Counter supplier = new Counter();
        assertResult(1, new Retryable<>(3, supplier));
    }

    @Test
    public void zeroRetryCount() {
        Counter supplier = new Counter();
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Retryable<>(0, supplier));
    }

    @Test
    public void singleErrorThenOk() {
        Counter supplier = new Counter(x -> {
            if (x == 1) {
                throw new RuntimeException("error");
            }
            return x;
        });
        assertResult(2, new Retryable<>(3, supplier));
    }

    @Test
    public void error() {
        Counter supplier = new Counter(x -> {
            throw new RuntimeException("error");
        });
        assertError(new Retryable<>(3, supplier));
    }

    private <T> void assertResult(T expected, Retryable<T> retryable) {
        Result<T, Throwable> result = retryable.get();
        Assertions.assertTrue(result.isOk());
        Assertions.assertEquals(expected, result.getValue());
    }

    private <T> void assertError(Retryable<T> retryable) {
        Result<T, Throwable> result = retryable.get();
        Assertions.assertFalse(result.isOk());
    }

    private static class Counter implements Retryable.RetryableOperation<Integer> {

        private final Function<Integer, Integer> supplier;
        private int callCount;

        public Counter() {
            this(x -> x);
        }

        public Counter(Function<Integer, Integer> supplier) {
            this.supplier = supplier;
            this.callCount = 0;
        }

        @Override
        public Integer apply(int zeroBasedRetryCount) {
            ++this.callCount;
            return supplier.apply(this.callCount);
        }
    }
}
