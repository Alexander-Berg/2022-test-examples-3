package ru.yandex.market.http.util.listener;

import java.util.ArrayList;
import java.util.List;

import io.netty.handler.codec.http.HttpMethod;

import ru.yandex.market.http.HttpResponse;
import ru.yandex.market.http.RetryStrategy;
import ru.yandex.market.http.listeners.RetryStrategyEventListener;

/**
 * @author dimkarp93
 */
public class TestRetryStrategyEventListener implements RetryStrategyEventListener, AutoCloseable {
    public int contentCounter;
    public int serviceErrorCounter;
    public int processingErrorCounter;
    public int requestEndCounter;

    public List<Throwable> processingErrors;
    public List<Throwable> requestEndErrors;
    public List<String> marketRequestIds;

    public TestRetryStrategyEventListener() {
        reset();
    }

    @Override
    public void onContent(String requestId, String url, HttpResponse response) {
        ++contentCounter;
    }

    @Override
    public void onServiceError(String requestId, HttpResponse response) {
        ++serviceErrorCounter;
    }

    @Override
    public void onProcessingError(String requestId, Throwable t) {
        ++processingErrorCounter;
        processingErrors.add(t);
    }

    @Override
    public void onRequestEnd(String currentRequestId, String marketRequestId, long duration, HttpResponse response,
                             Throwable cause, RetryStrategy.CallMeta meta, String uriStr, HttpMethod method,
                             int currentRetry) {
        ++requestEndCounter;
        requestEndErrors.add(cause);
        marketRequestIds.add(marketRequestId);
    }

    public void reset() {
        contentCounter = 0;
        serviceErrorCounter = 0;
        processingErrorCounter = 0;
        requestEndCounter = 0;

        processingErrors = new ArrayList<>();
        requestEndErrors = new ArrayList<>();
        marketRequestIds = new ArrayList<>();
    }

    @Override
    public void close() throws Exception {
        reset();
    }
}
