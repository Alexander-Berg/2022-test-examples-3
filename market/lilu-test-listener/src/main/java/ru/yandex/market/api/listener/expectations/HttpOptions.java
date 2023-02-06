package ru.yandex.market.api.listener.expectations;

public class HttpOptions {
    private final Long timeoutInMilliseconds;
    private final HttpErrorType error;
    private Integer count;

    public HttpOptions(Long timeoutInMilliseconds, HttpErrorType error, Integer count) {
        this.timeoutInMilliseconds = timeoutInMilliseconds;
        this.error = error;
        this.count = count;
    }

    public int decrementAndGetCount() {
        if (count == null) {
            return 0;
        }
        return --count;
    }

    public Integer getCount() {
        return count;
    }

    public HttpErrorType getError() {
        return error;
    }

    public long getTimeout() {
        return timeoutInMilliseconds;
    }

    public boolean hasTimeout() {
        return null != timeoutInMilliseconds && timeoutInMilliseconds > 0;
    }
}
