package ru.yandex.market.checkout.checkouter.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.config.lock.YtLockConfiguration;
import ru.yandex.market.checkout.checkouter.config.lock.YtLockConfigurationProperties;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.order.OrderUpdater;
import ru.yandex.market.checkout.checkouter.storage.ShopScheduleEntityGroup;
import ru.yandex.market.checkout.storage.err.StorageException;
import ru.yandex.market.checkout.storage.impl.LockCallback;
import ru.yandex.market.checkout.storage.impl.LockService;
import ru.yandex.market.checkout.stub.lock.LockStorageStub;

//работает только, если указан локальный YT_TOKEN
public class SimpleGeneralLockServiceTest {
    private static LockService lockService = null;
    private static final boolean USE_REAL_YT = false;

    @BeforeAll
    public static void beforeAll() {
        // Используется в процессе отладки взаимодействия с YT
        // берём таймаут как на проде
        lockService = createLockService(30000, 60000);
    }

    private static LockService createLockService(int timeout, long infiniteTimeout) {
        var ytConfigurationProperties = new YtLockConfigurationProperties();
        ytConfigurationProperties.setLockTimeout(timeout);
        ytConfigurationProperties.setLockInfiniteTimeout(infiniteTimeout);
        var ytConfiguration = new YtLockConfiguration();
        var retryTemplate = ytConfiguration.ytLockRetryTemplate(ytConfigurationProperties, timeout);
        var infiniteRetryTemplate = ytConfiguration.ytLockInfiniteRetryTemplate(ytConfigurationProperties);
        LockStorage lockStorage = new LockStorageStub();
        if (USE_REAL_YT) {
            ytConfigurationProperties.setToken("paste token from clipboard");
            ytConfigurationProperties.setRootPath("//home/checkouter/load");
            var entities = List.of("order", "order-return", ShopScheduleEntityGroup.NAME);
            lockStorage = new YtLockStorage(ytConfigurationProperties, entities, new CheckouterFeatureResolverStub());
        }
        return new GeneralLockService(lockStorage, retryTemplate, infiniteRetryTemplate);
    }

    @Test
    public void lockEntityGroupTest() throws ExecutionException, InterruptedException {
        var executor = Executors.newFixedThreadPool(2);

        var count = new AtomicInteger(0);
        LockCallback<?> callback = foo -> count.getAndIncrement();
        var customLockService = createLockService(1000, 1000);
        Callable<?> callable = () -> {
            customLockService.lockEntityGroup(OrderUpdater.entityGroup(1L), 1000L, callback);

            return null;
        };
        var result1 = executor.submit(callable);
        var result2 = executor.submit(callable);

        result1.get();
        result2.get();

        // Оба потока дошли до обработки
        Assertions.assertEquals(2, count.get());
    }

    @Test
    public void lockEntityGroupWithTimeoutTest() throws ExecutionException, InterruptedException {
        var executor = Executors.newFixedThreadPool(2);

        var count = new AtomicInteger(0);
        LockCallback<?> callback = foo -> {
            count.getAndIncrement();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Assertions.fail(e);
            }
            return null;
        };
        var customLockService = createLockService(100, 0);
        Runnable runnable = () -> {
            try {
                customLockService.lockEntityGroup(OrderUpdater.entityGroup(1L), 0, callback);
            } catch (StorageException e) {
                Assertions.assertEquals(e.getCause().getClass(), TimeoutException.class);
            } catch (TimeoutException e) {
              // its ok
            } catch (Exception e) {
                Assertions.fail(e);
            }
        };

        var result1 = executor.submit(runnable);
        var result2 = executor.submit(runnable);

        result1.get();
        result2.get();

        Assertions.assertEquals(1, count.get());
    }

    @Test
    public void lockEntityGroupWithoutTimeoutTest() throws ExecutionException, InterruptedException {
        var executor = Executors.newFixedThreadPool(2);

        var count = new AtomicInteger(0);
        LockCallback<?> callback = foo -> {
            count.getAndIncrement();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Assertions.fail(e);
            }
            return null;
        };
        Runnable runnable = () -> lockService.lockEntityGroup(OrderUpdater.entityGroup(1L), callback);

        var result1 = executor.submit(runnable);
        var result2 = executor.submit(runnable);

        result1.get();
        result2.get();

        Assertions.assertEquals(2, count.get());
    }

    @Test
    public void lockTwiceInOneThread() throws ExecutionException, InterruptedException {
        var executor = Executors.newFixedThreadPool(1);

        var count = new AtomicInteger(0);
        LockCallback<?> callback = foo -> {
            count.getAndIncrement();
            lockService.lockEntityGroup(OrderUpdater.entityGroup(1L), mutex -> {
                count.getAndIncrement();
                return null;
            });
            return null;
        };
        Runnable runnable = () -> lockService.lockEntityGroup(OrderUpdater.entityGroup(1L), callback);

        var result1 = executor.submit(runnable);

        result1.get();

        Assertions.assertEquals(2, count.get());
    }

    // Тест нужен в процессе отладки взаимодействия с YT
    @Test
    @Disabled
    public void loadTest() throws ExecutionException, InterruptedException {
        var threadCount = 100;
        var executor = Executors.newFixedThreadPool(threadCount);

        var count = new AtomicInteger(0);
        LockCallback<?> callback = foo -> {
            count.getAndIncrement();
            longRunningTask();
            return null;
        };

        var futures = new ArrayList<Future<Void>>();
        for (var i = 0; i < threadCount; i++) {
            final var index = (long) i;
            futures.add(executor.submit(() -> {
                lockService.lockEntityGroup(OrderUpdater.entityGroup(index), callback);
                return null;
            }));
        }
        for (var future: futures) {
            future.get();
        }
        Assertions.assertEquals(threadCount, count.get());
    }

    private void longRunningTask() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
