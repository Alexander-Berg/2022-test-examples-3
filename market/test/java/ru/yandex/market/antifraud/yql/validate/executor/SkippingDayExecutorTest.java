package ru.yandex.market.antifraud.yql.validate.executor;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import lombok.SneakyThrows;
import org.junit.Test;
import ru.yandex.market.antifraud.util.SleepUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SkippingDayExecutorTest {

    @Test
    public void mustExecuteFailed() {
        AtomicInteger executionCounter = new AtomicInteger(0);
        SkippingDayExecutor executor = new SkippingDayExecutor(1);
        CountDownLatch latch1 = new CountDownLatch(1);
        executor.submit("20171011", 20171011, () -> {
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
        executor.submit("20171011", 20171011, () -> {
            executionCounter.incrementAndGet();
            latch2.countDown();
        });
        await400ms(latch2);

        assertThat(executionCounter.get(), is(2));
    }

    @Test
    public void mustNotExecuteSameDay() {
        AtomicInteger executionCounter = new AtomicInteger(0);
        SkippingDayExecutor executor = new SkippingDayExecutor(2);
        CountDownLatch latch = new CountDownLatch(1);
        executor.submit("20171011-1", 20171011, () -> {
            executionCounter.incrementAndGet();
            executor.submit("20171011-2", 20171011, () -> {
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
        SkippingDayExecutor executor = new SkippingDayExecutor(2);
        CountDownLatch latch = new CountDownLatch(2);
        executor.submit("20171011", 20171011, () -> {
            executionCounter.incrementAndGet();
            latch.countDown();
        });
        executor.submit("20171012",20171012, () -> {
            executionCounter.incrementAndGet();
            latch.countDown();
        });
        await400ms(latch);
        assertThat(executionCounter.get(), is(2));
    }

    @Test
    public void mustExecuteSameDaySequentially() {
        AtomicInteger executionCounter = new AtomicInteger(0);
        SkippingDayExecutor executor = new SkippingDayExecutor(2);
        executor.submit("20171011-1", 20171011, () -> {
            executionCounter.incrementAndGet();
        });
        SleepUtil.sleep(50);
        CountDownLatch latch = new CountDownLatch(1);
        executor.submit("20171011-2", 20171011, () -> {
            executionCounter.incrementAndGet();
            latch.countDown();
        });
        await400ms(latch);
        assertThat(executionCounter.get(), is(2));
    }

    @Test
    public void mustNotExecuteTaskOverTheLimit() {
        Set<Integer> executedTasks = new ConcurrentSkipListSet<>();
        SkippingDayExecutor executor = new SkippingDayExecutor(5);
        AtomicInteger cnt = new AtomicInteger();

        for(int day = 20181001; day < 20181031; day++) {
            int currentDay = day;
            executor.submit("t1", day, () -> {
                executedTasks.add(currentDay);
                cnt.incrementAndGet();
                SleepUtil.sleep(50);
            });
        }

        while(cnt.get() < 5) {
            SleepUtil.sleep(50);
        }

        assertEquals(ImmutableSet.of(
            20181001, 20181002, 20181003, 20181004, 20181005),
            executedTasks);
    }

    @SneakyThrows
    private static void await400ms(CountDownLatch latch) {
        latch.await(400, TimeUnit.MILLISECONDS);
    }
}
