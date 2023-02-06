package ru.yandex.market.api.listener.expectations;

@FunctionalInterface
public interface MatchHttpRequestFunction {
    Boolean match(HttpRequest request);
}
