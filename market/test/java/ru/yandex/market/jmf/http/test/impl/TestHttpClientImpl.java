package ru.yandex.market.jmf.http.test.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.yandex.market.jmf.http.Http;
import ru.yandex.market.jmf.http.HttpClient;
import ru.yandex.market.jmf.http.HttpResponse;
import ru.yandex.market.jmf.http.HttpUtils;
import ru.yandex.market.jmf.utils.serialize.SerializationService;

/**
 * @author apershukov
 */
public class TestHttpClientImpl implements HttpClient {

    private final HttpEnvironment environment;
    private final String baseUrl;
    private final ExecutorService executor;
    private final SerializationService serializationService;

    TestHttpClientImpl(HttpEnvironment environment, String baseUrl, SerializationService serializationService) {
        this.environment = environment;
        this.baseUrl = baseUrl;
        this.executor = Executors.newCachedThreadPool();
        this.serializationService = serializationService;
    }

    @Override
    public <T> T execute(Http request, Class<T> responseCls) {
        HttpResponse response = execute(request);
        HttpUtils.throwIfError(response);
        return serializationService.deserialize(response.getBodyAsBytes(), responseCls);
    }

    @Override
    public CompletableFuture<HttpResponse> executeAsync(Http http) {
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                future.complete(environment.execute(http, baseUrl));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
