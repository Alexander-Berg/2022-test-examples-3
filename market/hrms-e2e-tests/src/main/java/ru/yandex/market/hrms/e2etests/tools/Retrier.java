package ru.yandex.market.hrms.e2etests.tools;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.qameta.allure.attachment.AttachmentRenderException;
import io.restassured.RestAssured;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.hrms.e2etests.tools.exception.ClientLevelException;
import ru.yandex.market.hrms.e2etests.tools.exception.RetriedAssertionError;

public class Retrier {
    private static final Logger log = LoggerFactory.getLogger(Retrier.class);
    private static final int DEFAULT_RETRIES = 15;
    private static final int DEFAULT_TIMEOUT = 1;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MINUTES;

    private static final int CLIENT_RETRIES = 5;
    private static final int CLIENT_TIMEOUT = 10;
    private static final TimeUnit CLIENT_TIME_UNIT = TimeUnit.SECONDS;

    public static final int RETRIES_BIGGEST = 45;
    public static final int RETRIES_BIG = 25;
    public static final int RETRIES_MEDIUM = 15;
    public static final int RETRIES_SMALL = 5;
    public static final int RETRIES_TINY = 3;

    public static final long TIMEOUT_BIG = 120;
    public static final long TIMEOUT_MEDIUM = 60;
    public static final long TIMEOUT_SMALL = 30;
    public static final long TIMEOUT_TINY = 5;

    private Retrier() {
    }

    public static <T> T clientRetry(Supplier<T> func) {
        return Retrier.clientRetry(func, CLIENT_RETRIES, CLIENT_TIMEOUT, CLIENT_TIME_UNIT);
    }

    public static void clientRetry(Runnable func) {
        Retrier.retry(func, CLIENT_RETRIES, CLIENT_TIMEOUT, CLIENT_TIME_UNIT);
    }

    public static void retry(Runnable func) {
        retry(func, DEFAULT_RETRIES, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public static <T> T retry(Supplier<T> func) {
        return retry(func, DEFAULT_RETRIES, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public static void retry(Runnable func, int retries) {
        retry(func, retries, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public static <T> T retry(Supplier<T> func, int retries) {
        return retry(func, retries, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public static void retry(Runnable func, int retries, long timeout, TimeUnit timeUnit) {
        retry(() -> {
                    func.run();
                    return null;
                }, retries, timeout, timeUnit
        );
    }

    public static void retrySeleniumStep(Runnable func) {
        retrySeleniumStep(() -> {
                    func.run();
                    return null;
                }
        );
    }

    public static <T> T retrySeleniumStep(Supplier<T> func) {
        T result = null;
        for (int i = 1; i <= CLIENT_RETRIES; i++) {
            log.info("Selenium step try {}/{}...", i, CLIENT_RETRIES);
            try {
                result = func.get();
                break;
            } catch (Throwable error) {
                if (error instanceof WebDriverException && i < CLIENT_RETRIES) {
                    log.error(error.getMessage());
                } else throw error;
            }
        }
        return result;
    }

    public static <T> T clientRetry(Supplier<T> func, int retries, long timeout, TimeUnit timeUnit) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(null);
        T result = null;
        for (int i = 1; i <= retries; i++) {
            log.info("Client connection try {}/{}...", i, retries);
            if (i == retries) {
                RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
            }
            try {
                result = func.get();
                break;
            } catch (Throwable error) {
                if (error instanceof AssertionError || error instanceof AttachmentRenderException) {
                    if (i == retries) {
                        throw new ClientLevelException(error);
                    }
                    log.error("Client error: " + error.getMessage());
                } else {
                    throw new ClientLevelException(error);
                }
            }
            Delayer.delay(timeout, timeUnit);
        }
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        return result;
    }

    public static <T> T retry(Supplier<T> func, int retries, long timeout, TimeUnit timeUnit) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(null);
        T result = null;
        for (int i = 1; i <= retries; i++) {
            log.info("try {}/{}...", i, retries);
            if (i == retries) {
                RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
            }
            try {
                result = func.get();
                break;
            } catch (Throwable error) {
                if ((error instanceof AssertionError
                        || error instanceof TimeoutException)
                        && !(error instanceof RetriedAssertionError)) {
                    if (i == retries) {
                        throw new RetriedAssertionError(error);
                    }
                    log.error(error.getMessage());
                } else {
                    throw error;
                }
            }
            Delayer.delay(timeout, timeUnit);
        }
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        return result;
    }

    public static void retryInteractionWithElement(Runnable func) {
        retryInteractionWithElement(() -> {
            func.run();
            return null;
        });
    }

    public static <T> T retryInteractionWithElement(Supplier<T> func) {
        T result = null;
        for (int i = 1; i <= RETRIES_TINY; i++) {
            log.info("Selenium step try {}/{}...", i, RETRIES_TINY);
            try {
                result = func.get();
                break;
            } catch (Throwable error) {
                if ((error instanceof NoSuchElementException
                        || error instanceof TimeoutException) && i < RETRIES_TINY) {
                    log.error(error.getMessage());
                } else throw error;
            }
        }
        return result;
    }
}
