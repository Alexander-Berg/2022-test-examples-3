package ru.yandex.market.jmf.background.test;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.jmf.background.impl.BackgroundServiceImpl;

@SpringJUnitConfig(InternalBackgroundTestConfiguration.class)
public class BackgroundServiceTest {
    private final BackgroundServiceImpl backgroundService;

    @Autowired
    public BackgroundServiceTest(BackgroundServiceImpl backgroundService) {
        this.backgroundService = backgroundService;
    }

    @Test
    public void testThreadStop() throws InterruptedException {
        var atomicInteger = new AtomicInteger();
        Thread thread = backgroundService.doBackground(1, (Exceptions.TrashRunnable) () -> {
            Thread.sleep(200);
            atomicInteger.incrementAndGet();
        });

        Thread.sleep(100);
        backgroundService.stopByThread(thread);

        Assertions.assertTrue(backgroundService.getThreads().isEmpty());
        Assertions.assertTrue(backgroundService.getThreadsByName().isEmpty());

        Thread.sleep(150);

        Assertions.assertEquals(0, atomicInteger.get());
    }

    @Test
    public void testThreadStopByName() throws InterruptedException {
        var atomicInteger = new AtomicInteger();
        String threadName = "name";
        backgroundService.doBackground(1, (Exceptions.TrashRunnable) () -> {
            Thread.sleep(200);
            atomicInteger.incrementAndGet();
        }, threadName);

        Thread.sleep(100);
        backgroundService.stopByName(threadName);

        Assertions.assertTrue(backgroundService.getThreads().isEmpty());
        Assertions.assertTrue(backgroundService.getThreadsByName().isEmpty());

        Thread.sleep(150);

        Assertions.assertEquals(0, atomicInteger.get());
    }

    @Test
    public void testStopService() throws InterruptedException {
        var atomicInteger = new AtomicInteger();
        String threadName = "name";
        backgroundService.doBackground(1, (Exceptions.TrashRunnable) () -> {
            Thread.sleep(200);
            atomicInteger.incrementAndGet();
        }, threadName);
        backgroundService.doBackground(1, (Exceptions.TrashRunnable) () -> {
            Thread.sleep(200);
            atomicInteger.incrementAndGet();
        });

        Thread.sleep(100);
        backgroundService.stop();

        Assertions.assertTrue(backgroundService.getThreads().isEmpty());
        Assertions.assertTrue(backgroundService.getThreadsByName().isEmpty());

        Thread.sleep(150);

        Assertions.assertEquals(0, atomicInteger.get());
    }
}
