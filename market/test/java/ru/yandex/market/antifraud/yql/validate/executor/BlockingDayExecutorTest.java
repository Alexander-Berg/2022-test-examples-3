package ru.yandex.market.antifraud.yql.validate.executor;

import lombok.SneakyThrows;
import org.junit.Test;
import ru.yandex.market.antifraud.util.SleepUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BlockingDayExecutorTest {

    @Test
    public void mustExecuteTimeouted() {
        BlockingDayExecutor executor = new BlockingDayExecutor(1);
        CountDownLatch latch1 = new CountDownLatch(1);

    }

    @Test
    public void mustExecuteFailed() {
        AtomicInteger executionCounter = new AtomicInteger(0);
        BlockingDayExecutor executor = new BlockingDayExecutor(1);
        CountDownLatch latch1 = new CountDownLatch(1);
        executor.execute("20171011", 20171011, () -> {
            try {
                executionCounter.incrementAndGet();
                throw new RuntimeException("Fail executing task");
            } finally {
                latch1.countDown();
            }
        });
        await400ms(latch1);
        SleepUtil.sleep(50);
        CountDownLatch latch2 = new CountDownLatch(1);
        executor.execute("20171011", 20171011, () -> {
            executionCounter.incrementAndGet();
            latch2.countDown();
        });
        await400ms(latch2);

        assertThat(executionCounter.get(), is(2));
    }

    @Test
    public void mustNotExecuteSameDay() {
        AtomicInteger executionCounter = new AtomicInteger(0);
        BlockingDayExecutor executor = new BlockingDayExecutor(2);
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute("20171011-1", 20171011, () -> {
            executionCounter.incrementAndGet();
            executor.execute("20171011-2", 20171011, () -> {
                executionCounter.incrementAndGet();
            });
            latch.countDown();
        });
        await400ms(latch);
        assertThat(executionCounter.get(), is(1));
    }

    @Test
    public void mustExecuteDifferentDays() {
        AtomicInteger executionCounter = new AtomicInteger(0);
        BlockingDayExecutor executor = new BlockingDayExecutor(2);
        CountDownLatch latch = new CountDownLatch(2);
        executor.execute("20171011", 20171011, () -> {
            executionCounter.incrementAndGet();
            latch.countDown();
        });
        executor.execute("20171012",20171012, () -> {
            executionCounter.incrementAndGet();
            latch.countDown();
        });
        await400ms(latch);
        assertThat(executionCounter.get(), is(2));
    }

    @Test
    public void mustExecuteSameDaySequentially() {
        AtomicInteger executionCounter = new AtomicInteger(0);
        BlockingDayExecutor executor = new BlockingDayExecutor(2);
        executor.execute("20171011-1", 20171011, () -> {
            executionCounter.incrementAndGet();
        });
        SleepUtil.sleep(50);
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute("20171011-2", 20171011, () -> {
            executionCounter.incrementAndGet();
            latch.countDown();
        });
        await400ms(latch);
        assertThat(executionCounter.get(), is(2));
    }

    @SneakyThrows
    private static void await400ms(CountDownLatch latch) {
        latch.await(400, TimeUnit.MILLISECONDS);
    }
}
