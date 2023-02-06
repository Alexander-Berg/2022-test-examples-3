package ru.yandex.autotests.direct.httpclient.core.exceptions;

/**
 * Created by pashkus on 01.07.15.
 */
public class HttpClientIOException extends  RuntimeException {
    public HttpClientIOException( String message, Throwable cause) {
        super(message, cause);
    }
    public HttpClientIOException( String message) {
        super(message);
    }
}
