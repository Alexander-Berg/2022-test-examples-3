package ru.yandex.market.api.util.httpclient.spi;

import java.util.function.Function;

public class HttpRequestExpectation {

    private final MatchHttpRequestFunction matchFunction;
    private final HttpRequestDescription description;

    public HttpRequestExpectation(MatchHttpRequestFunction matchFunction) {
        this(matchFunction, new HttpRequestDescriptionBuilder());
    }

    public HttpRequestExpectation(MatchHttpRequestFunction matchFunction, HttpRequestDescription description) {
        this.matchFunction = matchFunction;
        this.description = description;
    }

    public HttpRequestExpectation(MatchHttpRequestFunction matchFunction, HttpRequestDescriptionBuilder descriptionBuilder) {
        this.matchFunction = matchFunction;
        this.description = descriptionBuilder.get();
    }

    public HttpRequestExpectation(MatchHttpRequestFunction matchFunction,
                                  Function<HttpRequestDescriptionBuilder, HttpRequestDescriptionBuilder> descriptionBuilderConfigurer) {
        this.matchFunction = matchFunction;
        this.description = descriptionBuilderConfigurer.apply(new HttpRequestDescriptionBuilder()).get();
    }

    public HttpRequestDescription getDescription() {
        return description;
    }

    public MatchHttpRequestFunction getMatchFunction() {
        return matchFunction;
    }
}
