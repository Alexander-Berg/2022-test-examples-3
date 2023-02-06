package ru.yandex.market.mbo.mdm.common.service.queue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.Test;

import static ru.yandex.market.mbo.mdm.common.service.queue.MultiThreadingProcessingProperties.MultiThreadingProcessingPropertiesBuilder.multiThreadingProcessingProperties;

public class MultiThreadingProcessorTest {


    @Test
    public void shouldRunTaskAtLeastTenTimes() {
        // given
        AtomicInteger counter = new AtomicInteger();
        Runnable task = counter::incrementAndGet;
        var properties = multiThreadingProcessingProperties()
            .build();

        // when run processor async
        CompletableFuture.runAsync(() -> MultiThreadingProcessor.process(task, properties));

        // then
        Awaitility
            .waitAtMost(1, TimeUnit.SECONDS)
            .until(() -> counter.get() > 10);
    }

    @Test
    public void shouldRunTaskEvenIfExceptionOccurs() {
        // given
        AtomicInteger counter = new AtomicInteger();
        Runnable task = () -> {
            counter.incrementAndGet();
            throw new RuntimeException();
        };
        var properties = multiThreadingProcessingProperties()
            .build();

        // when run processor async
        CompletableFuture.runAsync(() -> MultiThreadingProcessor.process(task, properties));

        // then
        Awaitility
            .waitAtMost(1, TimeUnit.SECONDS)
            .until(() -> counter.get() > 100);
    }

    @Test
    public void shouldNotRunAdditionalTaskIfNoMemory() {
        // given
        AtomicInteger counter = new AtomicInteger();
        Runnable task = counter::incrementAndGet;

        // No enough memory to run task
        var properties = multiThreadingProcessingProperties()
            .initialNumberOfThreads(0)
            .minMemoryToRunTaskInBytes(Long.MAX_VALUE)
            .build();


        // when run processor async
        CompletableFuture.runAsync(() -> MultiThreadingProcessor.process(task, properties));

        // then
        Awaitility
            .waitAtMost(1, TimeUnit.SECONDS)
            .atLeast(100, TimeUnit.MILLISECONDS)
            .until(() -> counter.get() == 0);
    }
}
