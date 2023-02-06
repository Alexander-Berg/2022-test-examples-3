package ru.yandex.market.api.util.httpclient.spi;

import ru.yandex.market.http.HttpResponse;

public class PredefinedHttpResponse {
    private final HttpResponseConfigurer responseConfigurer;
    private HttpResponse response;
    private HttpOptions options;
    private HttpRequestExpectation httpRequestExpectation;

    public PredefinedHttpResponse(HttpRequestExpectation expectation) {
        this.httpRequestExpectation = expectation;
        this.responseConfigurer = new HttpResponseConfigurer();
    }

    public int decrementAndGetCount() {
        return getOptions().decrementAndGetCount();
    }

    public MatchHttpRequestFunction getHttpRequestExpectation() {
        return httpRequestExpectation.getMatchFunction();
    }

    public HttpRequestDescription getHttpRequestDescription() {
        return httpRequestExpectation.getDescription();
    }

    public HttpOptions getOptions() {
        if (null == options) {
            options = responseConfigurer.getOptions();
        }
        return options;
    }

    public HttpResponse getResponse() {
        if (null == response) {
            response = responseConfigurer.getResponse();
        }
        return response;
    }

    public HttpResponseConfigurer getResponseConfigurer() {
        return responseConfigurer;
    }
}
