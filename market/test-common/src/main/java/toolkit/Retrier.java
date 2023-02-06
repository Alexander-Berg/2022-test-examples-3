package toolkit;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toolkit.exceptions.BreakRetryError;

public class Retrier {
    private static final Logger log = LoggerFactory.getLogger(Retrier.class);
    private static final int DEFAULT_RETRIES = 40;
    private static final int CLIENT_RETRIES = 10;

    private static final int SMALL_TIMEOUT = 5;
    private static final int DEFAULT_TIMEOUT = 15;

    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    public static final int RETRIES_BIG = 60;
    public static final int RETRIES_SMALL = 20;


    private Retrier() {
    }

    public static <T> T clientRetry(Supplier<T> func) {
        return Retrier.retry(func, CLIENT_RETRIES, SMALL_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public static void clientRetry(Runnable func) {
        Retrier.retry(func, CLIENT_RETRIES, SMALL_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public static void retry(Runnable func) {
        retry(func, DEFAULT_RETRIES, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public static <T> T retry(Supplier<T> func) {
        return retry(func, DEFAULT_RETRIES, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public static <T> T retry(Supplier<T> func, int retries) {
        return retry(func, retries, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }


    public static void retry(Runnable func, int retries) {
        retry(func, retries, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public static void retry(Runnable func, int retries, long timeout, TimeUnit timeUnit) {
        retry(() -> {
            func.run();
            return null;
        }, retries, timeout, timeUnit);
    }


    public static <T> T retry(Supplier<T> func, int retries, long timeout, TimeUnit timeUnit) {
        return retrySupplier(func, retries, timeout, timeUnit);
    }

    private static <T> T retrySupplier(Supplier<T> func, int retries, long timeout, TimeUnit timeUnit) {
        T result = null;

        List<StackTraceElement> traces = Arrays.asList(Thread.currentThread().getStackTrace());
        long countRetry = traces.stream().filter(element -> element.getMethodName().equals("retrySupplier")).count();
        boolean multipleRetry = countRetry >= 2;
        for (int i = 1; i <= retries; i++) {
            try {
                result = func.get();
                break;
            } catch (Throwable error) {
                if (i == retries || multipleRetry || error instanceof BreakRetryError) {
                    throw error;
                } else {
                    log.error(error.getMessage());
                }
            }
            Delayer.delay(timeout, timeUnit);
            log.info("try {}/{}...", i, retries);
        }
        return result;
    }
}
