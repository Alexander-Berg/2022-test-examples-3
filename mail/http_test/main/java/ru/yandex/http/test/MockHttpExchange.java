package ru.yandex.http.test;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.Cancellable;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncResponseProducer;

public class MockHttpExchange implements HttpAsyncExchange {
    private final String uri;

    public MockHttpExchange(final String uri) throws Exception {
        this.uri = uri;
    }

    @Override
    public HttpRequest getRequest() {
        return new HttpGet(uri);
    }

    @Override
    public HttpResponse getResponse() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void submitResponse() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void submitResponse(final HttpAsyncResponseProducer producer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCompleted() {
        return false;
    }

    @Override
    public void setCallback(final Cancellable cancellable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTimeout(final int timeout) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTimeout() {
        return 0;
    }
}
