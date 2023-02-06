package ru.yandex.direct.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AsyncConsumerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void test() throws InterruptedException {
        Object obj = new Object();
        try (AsyncConsumer<Object> consumer = new AsyncConsumer<>(o -> Assert.assertEquals(o, obj), 10)) {
            consumer.accept(obj);
        }
    }

    @Test
    public void testInterruptWhileConsuming() throws InterruptedException {
        AsyncConsumer.AsyncConsumerException exc = null;
        try (AsyncConsumer<Object> consumer = new AsyncConsumer<>(o -> Thread.currentThread().interrupt(), 10)) {
            consumer.accept(new Object());
            consumer.awaitStop(Duration.ofSeconds(5));
            Assert.assertTrue(consumer.isStopped());

            AsyncConsumer.AsyncConsumerException exc2 = null;
            try {
                consumer.accept(new Object());
            } catch (AsyncConsumer.AsyncConsumerException e) {
                exc2 = e;
            }
            Assert.assertNotNull(exc2);
            Assert.assertEquals(InterruptedException.class, exc2.getCause().getClass());
        } catch (AsyncConsumer.AsyncConsumerException e) {
            exc = e;
        }
        Assert.assertNotNull(exc);
        Assert.assertEquals(InterruptedException.class, exc.getCause().getClass());
    }

    @Test
    public void testCancel() throws InterruptedException {
        try (AsyncConsumer<Object> consumer = new AsyncConsumer<>(o -> {
        }, 10)) {
            Thread.sleep(100);
            consumer.cancel();
            consumer.awaitStop(Duration.ofSeconds(5));
            Assert.assertTrue(consumer.isStopped());

            thrown.expect(AsyncConsumer.AsyncConsumerException.class);
            thrown.expectMessage("Consumer is cancelled");

            consumer.accept(new Object());
        }
    }

    @Test
    public void testClose() throws InterruptedException {
        AsyncConsumer<Object> consumer = new AsyncConsumer<>(o -> {
        }, 10);
        consumer.close();

        thrown.expect(AsyncConsumer.AsyncConsumerException.class);
        thrown.expectMessage("Consumer is closed");

        consumer.accept(new Object());
    }

    @Test
    public void testUnexpectedStop() throws InterruptedException {
        try (AsyncConsumer<Object> consumer = new AsyncConsumer<>((c, f) -> {
        }, 10, "Consumer")) {
            consumer.awaitStop(Duration.ofSeconds(5));
            Assert.assertTrue(consumer.isStopped());

            thrown.expect(AsyncConsumer.AsyncConsumerException.class);
            thrown.expectMessage("Consumer thread is unexpectedly stopped without an error");

            consumer.accept(new Object());
        }
    }

    @Test
    public void testException() throws InterruptedException {
        AsyncConsumer.AsyncConsumerException exc = null;
        try (AsyncConsumer<Object> consumer = new AsyncConsumer<>(o -> {
            throw new RuntimeException("Fail");
        }, 10)) {
            consumer.accept(new Object());
            consumer.awaitStop(Duration.ofSeconds(5));
            Assert.assertTrue(consumer.isStopped());

            AsyncConsumer.AsyncConsumerException exc2 = null;
            try {
                consumer.accept(new Object());
            } catch (AsyncConsumer.AsyncConsumerException e) {
                exc2 = e;
            }
            Assert.assertNotNull(exc2);
            Assert.assertEquals("Consumer thread is failed", exc2.getMessage());
            Assert.assertNotNull(exc2.getCause());
            Assert.assertEquals(RuntimeException.class, exc2.getCause().getClass());
            Assert.assertEquals("Fail", exc2.getCause().getMessage());
        } catch (AsyncConsumer.AsyncConsumerException e) {
            exc = e;
        }
        Assert.assertNotNull(exc);
        Assert.assertEquals("Consumer thread is failed", exc.getMessage());
        Assert.assertNotNull(exc.getCause());
        Assert.assertEquals(RuntimeException.class, exc.getCause().getClass());
        Assert.assertEquals("Fail", exc.getCause().getMessage());
    }

    @Test
    public void testProducerWait() throws InterruptedException {
        List<Integer> ints = new ArrayList<>();

        try (AsyncConsumer<Integer> consumer = new AsyncConsumer<>(
                o -> {
                    ints.add(o);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException exc) {
                        Thread.currentThread().interrupt();
                    }
                },
                2)
        ) {
            for (int i = 0; i < 10; i++) {
                consumer.accept(i);
            }
        }
        Assert.assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), ints);
    }
}
