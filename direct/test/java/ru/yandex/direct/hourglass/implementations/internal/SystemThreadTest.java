package ru.yandex.direct.hourglass.implementations.internal;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import ru.yandex.direct.hourglass.MonitoringWriter;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;

class SystemThreadTest {

    /**
     * Таймаут для ожидания остановки SystemThread
     * Если это время превышено - значит что-то не так в методах остановки SystemThread
     */
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(15);

    /**
     * Тест проверяет, что при запуске SystemThread создастся нужный поток с правильным именем, а после завршения
     * треда уже не будет
     * Проверяет, что задача будет выполняться преиодически, а после остановки SystemThread завершится
     */
    @Test
    void systemThreadRunTaskPeriodicallyAndStop() {
        var delayInMills = 20;
        var threadFactory =
                new ThreadFactoryBuilder()
                        .setNameFormat("prefix_").build();

        var expectedName = "prefix_name";

        /*
            Latch ожидающий, пока выполнится несколько итераций задач
         */
        var severalIterationsLatch = new CountDownLatch(3);
        Runnable runnable =
                severalIterationsLatch::countDown;
        var systemThread = new SystemThread(runnable, threadFactory, "name", delayInMills, MILLISECONDS,
                mock(MonitoringWriter.class));
        /* В конструкторе тред не должен запускаться */
        assertThat(getCurrentThreadNames()).as("Expected thread should not be started in the constructor").doesNotContain(expectedName);

        systemThread.start();
        /* В методе start тред запустится с правльным именем */
        assertThat(getCurrentThreadNames()).as("Expected thread should be started after start method is called").contains(expectedName);

        /*
            Проверка того, что runnable выполнится как минимум 3 раза, перед тем как остановить тред
         */
        assertTimeoutPreemptively(AWAIT_TIMEOUT, (Executable) severalIterationsLatch::await, "Runnable should be " +
                "invoked 3 " +
                "times, but it is not");
        systemThread.stop();

        assertTimeoutPreemptively(AWAIT_TIMEOUT, systemThread::await, "System thread expected to be stopped in await" +
                "() " +
                "method, but it is not");
        assertThat(getCurrentThreadNames()).as("Expected thread should be stopped after await method is called").doesNotContain(expectedName);
    }

    /**
     * Тест проверяет, задача буде выполняться в правильном треде
     */
    @Test
    void taskRunInRightThread() {
        var delayInMills = 20;
        var threadFactory =
                new ThreadFactoryBuilder()
                        .setNameFormat("prefix_").build();

        var expectedName = "prefix_name";

        var isCorrectName = new AtomicBoolean(true);
        /*
            Latch ожидающий, пока выполнится несколько итераций задач
         */
        var severalIterationsLatch = new CountDownLatch(1);
        Runnable runnable = () -> {
            var threadName = Thread.currentThread().getName();
            if (!threadName.equals(expectedName)) {
                isCorrectName.set(false);
            }
            severalIterationsLatch.countDown();
        };
        var systemThread = new SystemThread(runnable, threadFactory, "name", delayInMills, MILLISECONDS,
                mock(MonitoringWriter.class));

        systemThread.start();
        /*
            Проверка того, что runnable выполнится как минимум 3 раза, перед тем как остановить тред
         */
        assertTimeoutPreemptively(AWAIT_TIMEOUT, (Executable) severalIterationsLatch::await, "Runnable should be " +
                "invoked 3 " +
                "times, but it is not");
        systemThread.stop();

        assertTimeoutPreemptively(AWAIT_TIMEOUT, systemThread::await, "System thread expected to be stopped in await" +
                "() " +
                "method, but it is not");
        assertThat(isCorrectName).isTrue();
    }

    /**
     * Тест проверяет, что если на какой либо итерации runnable бросает Error, то вызовется uncaughtExceptionHandler
     * и тред остановится
     */
    @Test
    void ifErrorStop() {
        var delayInMills = 20;
        AtomicReference<Throwable> uncaughtException = new AtomicReference<>();
        AtomicInteger iterationCnt = new AtomicInteger();
        var threadFactory =
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> uncaughtException.set(e))
                        .build();

        var error = new Error("Unexpected throwable");
        Runnable runnable = () -> {
            if (iterationCnt.get() > 3) {
                throw error;
            }
            iterationCnt.incrementAndGet();
        };
        var systemThread = new SystemThread(runnable, threadFactory, "name", delayInMills, MILLISECONDS,
                mock(MonitoringWriter.class));

        systemThread.start();

        assertTimeoutPreemptively(AWAIT_TIMEOUT, systemThread::await, "System thread expected to be stopped in await" +
                "() " +
                "method, but it is not");

        assertThat(uncaughtException).hasValue(error);
    }

    /**
     * Тест проверяет, что если runnable бросает RuntimeException, то uncaughtExceptionHandler не вызовется, тред
     * продолжит запускать Runnable
     */
    @Test
    void continueIfRuntimeException() {
        var delayInMills = 20;
        AtomicReference<Throwable> uncaughtException = new AtomicReference<>();

        var threadFactory =
                new ThreadFactoryBuilder()
                        .setUncaughtExceptionHandler((t, e) -> uncaughtException.set(e))
                        .build();

    /*
        Latch ожидающий, пока выполнится несколько итераций задач
     */
        var severalIterationsLatch = new CountDownLatch(3);
        Runnable runnable = () -> {
            severalIterationsLatch.countDown();
            throw new RuntimeException("Runtime exception");
        };
        var systemThread = new SystemThread(runnable, threadFactory, "name", delayInMills, MILLISECONDS,
                mock(MonitoringWriter.class));

        systemThread.start();

    /*
        Проверка того, что runnable выполнится как минимум 3 раза, перед тем как остановить тред
     */
        assertTimeoutPreemptively(AWAIT_TIMEOUT, (Executable) severalIterationsLatch::await, "Runnable should be " +
                "invoked several " +
                "times, but it is not");

        systemThread.stop();

        assertTimeoutPreemptively(AWAIT_TIMEOUT, systemThread::await, "System thread expected to be stopped in await" +
                "() " +
                "method, but it is not");

        assertThat(uncaughtException.get()).

                isNull();

    }

    /**
     * Тест проверяет, что delay между итерациями правильный
     */
    @Test
    void systemThreadDelayTest() {
        long delayInMills = 2000;
        var threadFactory =
                new ThreadFactoryBuilder()
                        .build();

        var severalIterationsLatch = new CountDownLatch(2);

        var startTimes = new CopyOnWriteArrayList<Long>();
        Runnable runnable = () -> {
            startTimes.add(System.nanoTime());
            severalIterationsLatch.countDown();
        };
        var systemThread = new SystemThread(runnable, threadFactory, "name", delayInMills, MILLISECONDS,
                mock(MonitoringWriter.class));

        systemThread.start();

        /*
            Проверка того, что runnable выполнится как минимум 3 раза, перед тем как остановить тред
         */
        assertTimeoutPreemptively(AWAIT_TIMEOUT, (Executable) severalIterationsLatch::await, "Runnable should be " +
                "invoked several " +
                "times, but it is not");

        systemThread.stop();

        assertTimeoutPreemptively(AWAIT_TIMEOUT, systemThread::await, "System thread expected to be stopped in await" +
                "() " +
                "method, but it is not");

        var gotDelayBetweenIterations = (startTimes.get(1) - startTimes.get(0)) / 1_000_000;
        /*
         * Из за непредсказуемых остановок треда(например, gc) не очень правильно рассчитывать, что sleep между
         * итерациями будет очень близок к указанному преиоду. Поэтому проверятеся, что sleep между
         * итерацями больше или равен указанному периоду, но не сильно превосходит его
         */
        assertThat(gotDelayBetweenIterations).isBetween(delayInMills, delayInMills * 2);
    }

    private Set<String> getCurrentThreadNames() {
        return Thread.getAllStackTraces().keySet().stream().map(Thread::getName).collect(toSet());
    }
}
