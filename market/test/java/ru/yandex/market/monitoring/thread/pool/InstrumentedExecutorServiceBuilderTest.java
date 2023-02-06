package ru.yandex.market.monitoring.thread.pool;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;


class InstrumentedExecutorServiceBuilderTest {

    @Test
    void doesNotThrowException() throws InterruptedException {
        final UncaughtExceptionHandler uncaughtExceptionHandler = (t, e) -> {
        };
        final ExecutorService service1 = InstrumentedBuilders.threadPool("test-1")
                .setDaemon(true)
                .setPriority(Thread.NORM_PRIORITY)
                .setUncaughtExceptionHandler(uncaughtExceptionHandler)
                .setCorePoolSize(5)
                .setMaximumPoolSize(Integer.MAX_VALUE)
                .setKeepAliveTime(1, TimeUnit.MINUTES)
                .setWorkQueue(new SynchronousQueue<>())
                .setAllowCoreThreadTimeOut(true)
                .setPrestartAllCoreThreads(true)
                .withErrorHandling()
                .build();

        final ExecutorService service2 = InstrumentedBuilders.scheduledThreadPool("test-2")
                .setDaemon(true)
                .setPriority(Thread.NORM_PRIORITY)
                .setUncaughtExceptionHandler(uncaughtExceptionHandler)
                .setCorePoolSize(5)
                .setKeepAliveTime(1, TimeUnit.MINUTES)
                .setAllowCoreThreadTimeOut(true)
                .setPrestartAllCoreThreads(true)
                .withErrorHandling()
                .build();
        service1.shutdown();
        service2.shutdown();
        service1.awaitTermination(1, TimeUnit.MINUTES);
        service2.awaitTermination(1, TimeUnit.MINUTES);
    }
}
