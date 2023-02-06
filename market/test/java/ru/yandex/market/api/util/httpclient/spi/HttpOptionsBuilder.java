package ru.yandex.market.api.util.httpclient.spi;

public class HttpOptionsBuilder {

    private Long timeoutInMilliseconds = null;
    private HttpErrorType error;
    private Integer count;

    public HttpOptionsBuilder error(HttpErrorType error) {
        this.error = error;
        return this;
    }

    public HttpOptions get() {
        return new HttpOptions(timeoutInMilliseconds, error, count);
    }

    public HttpOptionsBuilder timeout(Long timeoutInMilliseconds) {
        this.timeoutInMilliseconds = timeoutInMilliseconds;
        return this;
    }

    public HttpOptionsBuilder times(int count) {
        this.count = count;
        return this;
    }
}
