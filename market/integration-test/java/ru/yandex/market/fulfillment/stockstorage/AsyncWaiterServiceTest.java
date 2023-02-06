package ru.yandex.market.fulfillment.stockstorage;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import ru.yandex.market.fulfillment.stockstorage.util.AsyncWaiterService;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AsyncWaiterServiceTest {

    private ThreadPoolTaskExecutor executor;
    private AsyncWaiterService serviceTest;

    @BeforeEach
    public void init() {
        executor = new ThreadPoolTaskExecutor();
        executor.initialize();
        serviceTest = new AsyncWaiterService(ImmutableList.of(executor));
    }

    @Test
    public void waitWithDefaultAndExitByTaskEnded() {
        AtomicInteger value = new AtomicInteger(0);
        executor.execute(() -> value.addAndGet(1));

        serviceTest.awaitTasks();
        assertEquals(1, value.get());
    }

    @Test
    public void waitWithDefaultAndExitByTimeout() {
        AtomicInteger value = new AtomicInteger(0);
        executor.execute(() -> wait(value, 2500));

        serviceTest.awaitTasks();
        assertEquals(0, value.get());
    }

    @Test
    public void waitWithCustomTimeoutAndExitByTaskEnded() {
        AtomicInteger value = new AtomicInteger(0);
        executor.execute(() -> wait(value, 300));

        serviceTest.awaitTasks(TimeUnit.SECONDS.toMillis(30));
        assertEquals(1, value.get());
    }

    public void wait(AtomicInteger value, int sleepTime) {
        try {
            TimeUnit.MILLISECONDS.sleep(sleepTime);
            value.addAndGet(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
