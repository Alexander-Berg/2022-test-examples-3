package ru.yandex.market.api.util.httpclient.spi;

import org.springframework.stereotype.Service;

import ru.yandex.market.api.common.Result;
import ru.yandex.market.http.HttpRequest;

@Service
public class HttpExpectations {

    private final HttpTestClientConfiguration configuration = new HttpTestClientConfiguration();

    public HttpResponseConfigurer configure(HttpRequestExpectationBuilder requestBuilder) {
        return configuration.register(requestBuilder.build());
    }

    public void reset() {
        configuration.reset();
    }

    public Result<PredefinedHttpResponse, String> tryResolve(HttpRequest request) {
        return configuration.tryResolve(request);
    }

    public void verify() {
        verifyInternal(configuration);
    }

    private void verifyInternal(HttpTestClientConfiguration configuration) {
        configuration.throwIfUnmatchedRequestExists();
    }
}
