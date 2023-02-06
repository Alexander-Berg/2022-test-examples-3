package ru.yandex.market.api.tests.feature.clients;

import ru.yandex.market.api.listener.expectations.HttpExpectations;
import ru.yandex.market.api.listener.expectations.HttpRequestExpectationBuilder;
import ru.yandex.market.api.listener.expectations.HttpResponseConfigurer;

import java.util.function.Function;

/**
 * @author dimkarp93
 */
public class BlackboxTestClient {
    private HttpExpectations httpExpectations;

    public BlackboxTestClient(HttpExpectations httpExpectations) {
        this.httpExpectations = httpExpectations;
    }

    private HttpResponseConfigurer configure(
        Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> fn
    ) {
        return httpExpectations.configure(
            fn.apply(new HttpRequestExpectationBuilder())
        );
    }

    public void userByOAuth(String oauth, String response) {
        configure(x -> x.serverMethod("blackbox")
            .param("oauth_token", oauth))
            .ok()
            .body(response);
    }
}
