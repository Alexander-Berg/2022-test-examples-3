package ru.yandex.market.common.retrofit;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import retrofit2.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class RetryOperationsTest {

    private final RetryCondition noRetry = new RetryCondition(false, 0);

    @Mock
    private ResponseBody body;

    private RetryOperations retry;

    @BeforeEach
    void init() {
        retry = new RetryOperations();
    }

    @Test
    void testSuccess() {
        assertEquals(noRetry, retry.retry(Response.success("")));
    }


    @ParameterizedTest
    @MethodSource("retryData")
    void testFailureSingleType(int errorCode, int retryCount, int delaySeconds) {
        for (int i = 0; i < retryCount; i++) {
            assertEquals(new RetryCondition(true, TimeUnit.SECONDS.toMillis(delaySeconds)),
                    retry.retry(Response.error(errorCode, body)));
        }
        assertEquals(noRetry, retry.retry(Response.error(errorCode, body)));
    }

    @Test
    void testFailureMultipleTypes() {
        assertEquals(new RetryCondition(true, 5000), retry.retry(Response.error(504, body)));
        assertEquals(new RetryCondition(true, 3000), retry.retry(Response.error(502, body)));
        assertEquals(new RetryCondition(true, 5000), retry.retry(Response.error(420, body)));
        assertEquals(new RetryCondition(true, 5000), retry.retry(Response.error(420, body)));
        assertEquals(new RetryCondition(false, 3000), retry.retry(Response.error(503, body)));

        assertEquals(noRetry, retry.retry(Response.error(503, body)));
    }

    @ParameterizedTest
    @MethodSource("unsupportedExceptions")
    void testFailureWithUnsupportedException(IOException io) {
        assertEquals(noRetry, retry.retry(io));
    }

    @ParameterizedTest
    @MethodSource("supportedExceptions")
    void testFailureWitSupportedException(IOException io) {
        assertEquals(new RetryCondition(true, 5000), retry.retry(new ConnectException()));
    }

    @Test
    void testFailureSupportedExceptionFlow() {
        assertEquals(new RetryCondition(true, 5000), retry.retry(new ConnectException()));
        assertEquals(new RetryCondition(true, 5000), retry.retry(new ConnectException()));
        assertEquals(new RetryCondition(true, 5000), retry.retry(new ConnectException()));

        assertEquals(noRetry, retry.retry(new ConnectException()));
    }

    @Test
    void testCustomRetriesForExceptions() {
        var customRetry = new RetryOperations() {
            @Override
            protected RetryParams onConnect(int currentRetry) {
                return new RetryParams(10, 0);
            }

            @Override
            protected RetryParams onReadTimeout(int currentRetry) {
                return new RetryParams(0, 0);
            }
        };
        assertEquals(new RetryCondition(true, 0), customRetry.retry(new ConnectException()));
        assertEquals(new RetryCondition(true, 0), customRetry.retry(new ConnectException()));
        assertEquals(noRetry, customRetry.retry(new TimeoutException()));
    }

    static Object[] retryData() {
        return new Object[][]{
                {504, 2, 5}, // Дефолтный
                {505, 2, 5},

                {400, 0, 0}, // Нет смысла повторять запрос
                {401, 0, 0},
                {403, 0, 0},
                {404, 0, 0},
                {405, 0, 0},
                {415, 0, 0},
                {501, 0, 0},

                {420, 5, 5}, // Превышено количество одновременных запросов
                {429, 5, 5},

                {500, 3, 3},
                {502, 3, 3}, // Проблемы с нагрузкой на сервер PAPI?
                {503, 3, 3}
        };
    }

    static Object[][] supportedExceptions() {
        return new Object[][]{
                {new ConnectException()},
                {new IOException()},
        };
    }

    static Object[][] unsupportedExceptions() {
        return new Object[][]{
                {new UnknownHostException()},
                {new UnknownServiceException()}
        };
    }
}
