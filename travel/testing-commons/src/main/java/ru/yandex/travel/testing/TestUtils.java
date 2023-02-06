package ru.yandex.travel.testing;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.BooleanSupplier;

import org.mockito.Mockito;

public final class TestUtils {
    public static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(30);
    public static final Duration DEFAULT_RETRY_DELAY = Duration.ofMillis(50);

    private TestUtils() {
    }

    public static void sleep(Duration duration) {
        sleep(duration.toMillis());
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // preserving the flag
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T genericsFriendlyMock(Class<?> clazz) {
        return (T) Mockito.mock(clazz);
    }

    public static void waitForState(
            String description,
            BooleanSupplier predicate
    ) {
        waitForState(description, DEFAULT_WAIT_TIMEOUT, predicate);
    }

    public static void waitForState(
            String description,
            Duration timeout,
            BooleanSupplier predicate
    ) {
        waitForState(description, timeout, DEFAULT_RETRY_DELAY, predicate);
    }

    public static void waitForState(
            String description,
            Duration timeout,
            Duration retryDelay,
            BooleanSupplier stateChecker
    ) {
        long startWaitingTime = System.currentTimeMillis();
        while (!stateChecker.getAsBoolean()) {
            if ((System.currentTimeMillis() - startWaitingTime) > timeout.toMillis()) {
                throw new RuntimeException("The expected state hasn't been reached in " +
                        timeout.truncatedTo(ChronoUnit.SECONDS).toString() + " : " + description);
            }
            sleep(retryDelay);
        }
    }

    public static void waitForCheck(
            Duration timeout,
            Runnable stateChecker
    ) {
        waitForCheck(timeout, DEFAULT_RETRY_DELAY, stateChecker);
    }

    public static void waitForCheck(
            Duration timeout,
            Duration retryDelay,
            Runnable stateChecker
    ) {
        long startWaitingTime = System.currentTimeMillis();
        while (true) {
            try {
                stateChecker.run();
                return;
            } catch (AssertionError ex) {
                if ((System.currentTimeMillis() - startWaitingTime) > timeout.toMillis()) {
                    throw ex;
                }
                sleep(retryDelay);
            }
        }
    }
}
