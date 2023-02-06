package ru.yandex.market.mboc.common.infrastructure.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RetryHelperTest {
    @Test
    public void testPositive() {
        AtomicInteger test = new AtomicInteger();
        try {
            RetryHelper.retry("test", 2, attempt -> {
                test.incrementAndGet();
                throw new TestException();
            });
            fail("Must throw exception in the end");
        } catch (RetryHelper.RetriesExhausted e) {
            assertTrue(e.getCause() instanceof TestException);
        }

        assertEquals(2, test.get());
    }

    @Test
    public void testNoCatch() {
        AtomicInteger test = new AtomicInteger();
        try {
            RetryHelper.retry("test", 2, attempt -> {
                test.incrementAndGet();
                throw new RuntimeException(new RuntimeException());
            });
            fail("Must throw exception in the end");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertTrue(e.getCause().getCause() instanceof RuntimeException);
            assertNull(e.getCause().getCause().getCause());
        }

        assertEquals(2, test.get());
    }

    @Test
    public void testOk() {
        AtomicInteger test = new AtomicInteger();
        int result = RetryHelper.retry("test", 2, attempt -> {
            return test.incrementAndGet();
        });
        assertEquals(1, result);
        assertEquals(1, test.get());
    }

    @Test
    public void testOkEventually() {
        AtomicInteger test = new AtomicInteger();
        int result = RetryHelper.retry("test", 2, attempt -> {
            if (test.incrementAndGet() <= 1) {
                throw new TestException();
            }
            return test.get();
        });

        assertEquals(2, result);
        assertEquals(2, test.get());
    }


    public static class TestException extends Exception {
    }
}
