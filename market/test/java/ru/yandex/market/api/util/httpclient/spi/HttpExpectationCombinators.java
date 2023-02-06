package ru.yandex.market.api.util.httpclient.spi;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import ru.yandex.market.api.util.ApiCollections;
import ru.yandex.market.http.HttpRequestUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HttpExpectationCombinators {

    /**
     * Ожидание http запроса с заданной схемой
     */
    public static final Function<String, HttpRequestExpectation> BY_SCHEME =
        (scheme) -> (new HttpRequestExpectation((actual) -> scheme.equalsIgnoreCase(actual.getUri().getScheme()),
            d -> d.setScheme(scheme)));
    /**
     * Ожидание http запроса на заданный хост
     */
    public static final Function<String, HttpRequestExpectation> BY_HOST =
        (host) -> new HttpRequestExpectation((request) -> host.equalsIgnoreCase(request.getUri().getHost()),
            b -> b.setHost(host));
    /**
     * Ожидание http запроса на заданный порт
     */
    public static final Function<Integer, HttpRequestExpectation> BY_PORT =
        (port) -> new HttpRequestExpectation((request) -> port == request.getUri().getPort(),
            b -> b.setPort(port));
    /**
     * Ожидание http запроса на заданный путь (ручку)
     */
    public static final Function<String, HttpRequestExpectation> BY_SERVER_METHOD =
        (serverMethod) -> new HttpRequestExpectation((request) -> serverMethod.equalsIgnoreCase(request.getUri().getPath()),
            b -> b.setServerMethod(serverMethod));
    /**
     * Ожидание http запроса c заданным методом
     */
    public static final Function<HttpMethod, HttpRequestExpectation> BY_HTTP_METHOD =
        (method) -> new HttpRequestExpectation((request) -> method == request.getMethod(),
            b -> b.setHttpMethod(method));
    /**
     * Ожидание http запроса c заданным параметром запроса
     */
    public static final BiFunction<String, String, HttpRequestExpectation> BY_QUERY_STRING_PARAMETER =
        (String name, String value) -> new HttpRequestExpectation((request) -> request.hasParameter(name, value),
            b -> b.withParameter(name, value));

    /**
     * Ожидание http запроса c заданным параметром запроса
     */
    public static final BiFunction<String, String, HttpRequestExpectation> BY_HTTP_HEADER =
        (String name, String value) -> new HttpRequestExpectation(
            (request) -> request.getHttpHeaders().contains(name, value, true),
            b -> b.withHeader(name, value));

    /**
     * Ожидание http запроса без заданного значения параметра запроса
     */
    public static final Function<String, HttpRequestExpectation> BY_PARAMETER_NOT_IN_QUERY_STRING =
        (String name) -> new HttpRequestExpectation((request) -> !request.hasParameter(name),
            b -> b.withoutParameter(name));

    /**
     * Ожидание http запроса c заданным телом запроса
     */
    public static final BiFunction<Function<byte[], Boolean>, String, HttpRequestExpectation> BY_BODY =
        (Function<byte[], Boolean> predicate, String description) -> new HttpRequestExpectation((request) -> predicate.apply(request.getBody()),
            b -> b.withBodyCondition(description));
    /**
     * Ожидание http запроса c заданным запросом
     */
    public static final Function<String, HttpRequestExpectation> BODY_HASH =
        (String value) -> new HttpRequestExpectation((request) -> value.equalsIgnoreCase(request.getHttpHeaders().get(HttpHeaderNames.CONTENT_MD5)),
            b -> b.withBodyCondition(String.format("request body md5 hash is equal to '%s'", value)));
    /**
     *
     */
    public static final HttpRequestExpectation DUMMY = new HttpRequestExpectation((expected) -> true);
    /**
     *
     */
    public static final Function<URI, HttpRequestExpectation> BY_URI =
        (uri) -> and(BY_SCHEME.apply(uri.getScheme()),
            BY_HOST.apply(uri.getHost()),
            BY_PORT.apply(uri.getPort()),
            BY_SERVER_METHOD.apply(uri.getPath()),
            ByParameters(uri));

    public static HttpRequestExpectation and(HttpRequestExpectation... items) {
        return and(Arrays.asList(items));
    }

    public static HttpRequestExpectation and(Collection<HttpRequestExpectation> items) {
        return ApiCollections.reduce(DUMMY, items, HttpExpectationCombiners::and);
    }

    /**
     * @param uri
     * @return
     */
    private static HttpRequestExpectation ByParameters(URI uri) {
        return and(HttpRequestUtils.getParametersFromUri(uri)
            .entrySet()
            .stream()
            .map(x -> and(x.getValue()
                .stream()
                .map(v -> BY_QUERY_STRING_PARAMETER.apply(x.getKey(), v)).collect(Collectors.toList())))
            .collect(Collectors.toList()));
    }
}
