package ru.yandex.market.api.util.httpclient.spi;

public class UnmatchedHttpRequestException extends RuntimeException {
    public UnmatchedHttpRequestException(String message) {
        super(message);
    }
}
