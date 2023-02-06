package ru.yandex.market.api.util.httpclient.spi;

import ru.yandex.market.http.HttpRequest;

@FunctionalInterface
public interface MatchHttpRequestFunction {
    Boolean match(HttpRequest request);
}
