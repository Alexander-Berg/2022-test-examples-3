package ru.yandex.market.api.util.httpclient.spi;

import java.util.ArrayList;

import io.netty.handler.codec.http.HttpResponseStatus;

import ru.yandex.market.api.util.ResourceHelpers;
import ru.yandex.market.http.HttpResponse;

public class HttpResponseConfigurer {
    private final HttpOptionsBuilder optionsBuilder = new HttpOptionsBuilder();
    private HttpResponseStatus httpStatus;
    private byte[] body;

    public HttpResponseConfigurer body(byte[] body) {
        return configure(() -> this.body = body);
    }

    public HttpResponseConfigurer body(String resourcePath) {
        byte[] data = ResourceHelpers.getResource(resourcePath);
        return configure(() -> this.body = data);
    }

    public HttpResponseConfigurer emptyResponse() {
        return configure(() -> this.body = new byte[0]);
    }

    public HttpResponseConfigurer error(HttpErrorType errorType) {
        return configure(() -> optionsBuilder.error(errorType));
    }

    public HttpOptions getOptions() {
        return optionsBuilder.get();
    }

    public HttpResponse getResponse() {
        return HttpResponse.of(httpStatus == null ? HttpResponseStatus.OK.code() : httpStatus.code(), new ArrayList<>(), body);
    }

    public HttpResponseConfigurer ok() {
        return configure(() -> status(HttpResponseStatus.OK));
    }

    public HttpResponseConfigurer status(HttpResponseStatus status) {
        return configure(() -> httpStatus = status);
    }

    public HttpResponseConfigurer timeout(long timeoutInMilliseconds) {
        return configure(() -> optionsBuilder.timeout(timeoutInMilliseconds));
    }

    public HttpResponseConfigurer times(int count) {
        return configure(() -> optionsBuilder.times(count));
    }

    private HttpResponseConfigurer configure(Runnable consumer) {
        consumer.run();
        return this;
    }

}
