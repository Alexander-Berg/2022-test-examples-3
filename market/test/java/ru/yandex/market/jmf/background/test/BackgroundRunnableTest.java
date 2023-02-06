package ru.yandex.market.jmf.background.test;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.jmf.background.impl.BackgroundRunnable;

public class BackgroundRunnableTest {
    @Test
    public void testException() throws InterruptedException {
        var backgroundRunnable = new BackgroundRunnable(TimeUnit.MILLISECONDS, 100L,
                () -> {
                    throw new RuntimeException();
                });

        Thread thread = new Thread(backgroundRunnable);
        thread.setDaemon(true);
        thread.start();

        Thread.sleep(300);

        try {
            Assertions.assertTrue(thread.isAlive());
        } finally {
            thread.interrupt();
        }
    }

    @Test
    public void testThrowableButNotException() throws InterruptedException {
        var backgroundRunnable = new BackgroundRunnable(TimeUnit.MILLISECONDS, 100L,
                () -> {
                    throw Exceptions.sneakyThrow(new Throwable());
                });

        Thread thread = new Thread(backgroundRunnable);
        thread.setDaemon(true);
        thread.start();

        Thread.sleep(300);

        Assertions.assertFalse(thread.isAlive());
    }
}
