package ru.yandex.market.api.listener.expectations;

import ru.yandex.market.api.util.ApiCollections;
import ru.yandex.market.api.util.Result;

import java.util.ArrayList;
import java.util.List;

public class HttpTestClientConfiguration {

    private final List<PredefinedHttpResponse> configuration = new ArrayList<>();

    public HttpResponseConfigurer register(HttpRequestExpectation expectation) {
        synchronized (this) {
            PredefinedHttpResponse e = new PredefinedHttpResponse(expectation);
            configuration.add(e);
            return e.getResponseConfigurer();
        }
    }

    public void reset() {
        synchronized (this) {
            configuration.clear();
        }
    }

    public void throwIfUnmatchedRequestExists() {
        synchronized (this) {
            if (!configuration.isEmpty()) {
                throw new UnmatchedHttpExpectationExistsException(configuration);
            }
        }
    }

    private Result<PredefinedHttpResponse, String> findAndRemoveAppropriateResponseDescriptor(HttpRequest request) {
        synchronized (this) {
            List<Integer> candidatesPosition = new ArrayList<>();
            for (int i = 0; i < configuration.size(); ++i) {
                if (configuration.get(i).getHttpRequestExpectation().match(request)) {
                    candidatesPosition.add(i);
                }
            }
            if (ApiCollections.isEmpty(candidatesPosition)) {
                return Result.newError(String.format("Cant find match for request [%s / %s]",
                                                     request.getMethod().toString().toUpperCase(),
                                                     request.getUri().toString()));
            }
            if (candidatesPosition.size() > 1) {
                return Result.newError(String.format("More than one candidate found for request [%s / %s]",
                                                     request.getMethod().toString().toUpperCase(),
                                                     request.getUri().toString()));
            }
            PredefinedHttpResponse response = configuration.get(candidatesPosition.get(0));
            int count = response.decrementAndGetCount();
            if (count == 0) {
                configuration.remove((int) candidatesPosition.get(0));
            }
            return Result.newResult(response);
        }
    }

    public Result<PredefinedHttpResponse, String> tryResolve(HttpRequest request) {
        return findAndRemoveAppropriateResponseDescriptor(request);
    }
}
