package ru.yandex.market.mcrm.http;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author apershukov
 */
public class TestHttpClientImpl implements HttpClient {

    private final HttpEnvironment environment;
    private final String baseUrl;
    private final ExecutorService executor;

    TestHttpClientImpl(HttpEnvironment environment, String baseUrl) {
        this.environment = environment;
        this.baseUrl = baseUrl;
        this.executor = Executors.newCachedThreadPool();
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

    @Override
    public void executeBackground(Http request, String handler) {

    }
}
