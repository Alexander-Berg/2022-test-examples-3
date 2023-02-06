package ru.yandex.market.http.util.requests;

import java.util.function.IntFunction;

import io.netty.util.concurrent.Future;

import ru.yandex.market.http.CacheMode;
import ru.yandex.market.http.HttpResponse;
import ru.yandex.market.http.RequestProcessor;
import ru.yandex.market.http.RequestProcessorFactory;
import ru.yandex.market.http.listeners.RequestProcessorEventListener;

/**
 * @author dimkarp93
 */
public class MockRequestProcessorFactory implements RequestProcessorFactory {
    private final IntFunction<Future<HttpResponse>> callExecutor;

    public MockRequestProcessorFactory(IntFunction<Future<HttpResponse>> callExecutor) {
        this.callExecutor = callExecutor;
    }


    @Override
    public RequestProcessor create(String configuration, int timeout, int connectTimeout, int maxPacketSize,
                                   RequestProcessorEventListener listener) {
        return new MockRequestProcessor(callExecutor);
    }

}
