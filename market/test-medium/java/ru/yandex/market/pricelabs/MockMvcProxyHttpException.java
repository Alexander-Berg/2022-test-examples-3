package ru.yandex.market.pricelabs;

public class MockMvcProxyHttpException extends RuntimeException {
    public MockMvcProxyHttpException(String message) {
        super(message);
    }

    public MockMvcProxyHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
