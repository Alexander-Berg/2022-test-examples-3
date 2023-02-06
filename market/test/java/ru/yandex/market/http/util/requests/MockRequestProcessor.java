package ru.yandex.market.http.util.requests;

import java.net.URI;
import java.util.function.IntFunction;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;

import ru.yandex.market.http.HttpResponse;
import ru.yandex.market.http.RequestProcessor;

/**
 * @author dimkarp93
 */
public class MockRequestProcessor implements RequestProcessor {
    private final IntFunction<Future<HttpResponse>> callExecutor;
    private int counter = 0;

    public MockRequestProcessor(IntFunction<Future<HttpResponse>> callExecutor) {
        this.callExecutor = callExecutor;
    }

    @Override
    public Future<HttpResponse> doCall(EventLoop executor,
                                       URI uri,
                                       HttpRequest request,
                                       String requestId,
                                       Object chunks) {
        return callExecutor.apply(++counter);
    }
}
