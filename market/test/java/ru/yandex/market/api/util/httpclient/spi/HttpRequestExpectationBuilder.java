package ru.yandex.market.api.util.httpclient.spi;

import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpMethod;

import ru.yandex.market.api.util.ApiStrings;
import ru.yandex.market.api.util.Urls;

import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.BODY_HASH;
import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.BY_BODY;
import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.BY_HOST;
import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.BY_HTTP_HEADER;
import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.BY_HTTP_METHOD;
import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.BY_PARAMETER_NOT_IN_QUERY_STRING;
import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.BY_PORT;
import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.BY_QUERY_STRING_PARAMETER;
import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.BY_SCHEME;
import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.BY_SERVER_METHOD;
import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.BY_URI;
import static ru.yandex.market.api.util.httpclient.spi.HttpExpectationCombinators.DUMMY;

public class HttpRequestExpectationBuilder {
    private HttpRequestExpectation expectation;
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpRequestExpectationBuilder() {
        this.expectation = DUMMY;
    }

    private HttpRequestExpectationBuilder(HttpRequestExpectation expectation) {
        this.expectation = expectation;
    }

    public static HttpRequestExpectationBuilder localhost() {
        return new HttpRequestExpectationBuilder().host("localhost");
    }

    public static HttpRequestExpectationBuilder url(String url) {
        URI uri = Urls.toUri(url);
        return new HttpRequestExpectationBuilder()
            .host(uri.getHost())
            .port(uri.getPort())
            .scheme(uri.getScheme());
    }

    public HttpRequestExpectationBuilder apply(Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> fn) {
        if (null == fn) {
            return this;
        }
        return fn.apply(this);
    }

    public HttpRequestExpectationBuilder body(Function<byte[], Boolean> bodyMatcher, String description) {
        return and(this.expectation, BY_BODY.apply(bodyMatcher, description));
    }

    public HttpRequestExpectationBuilder jsonBody(Function<JsonNode, Boolean> bodyMatcher, String description) {
        return body(body -> {

            try {
                return bodyMatcher.apply(mapper.readTree(body));
            } catch (IOException e) {
                return false;
            }
        }, description);
    }

    public HttpRequestExpectationBuilder bodyHash(String hash) {
        return and(this.expectation, BODY_HASH.apply(hash));
    }

    public HttpRequestExpectation build() {
        return this.expectation;
    }

    public HttpRequestExpectationBuilder delete() {
        return httpMethod(HttpMethod.DELETE);
    }

    public HttpRequestExpectationBuilder get() {
        return httpMethod(HttpMethod.GET);
    }

    public HttpRequestExpectationBuilder head() {
        return httpMethod(HttpMethod.HEAD);
    }

    public HttpRequestExpectationBuilder host(String host) {
        return and(this.expectation, BY_HOST.apply(host));
    }

    public HttpRequestExpectationBuilder options() {
        return httpMethod(HttpMethod.OPTIONS);
    }

    public HttpRequestExpectationBuilder param(String name, String value) {
        return and(this.expectation, BY_QUERY_STRING_PARAMETER.apply(name, value));
    }

    public HttpRequestExpectationBuilder header(String name, String value) {
        return and(this.expectation, BY_HTTP_HEADER.apply(name, value));
    }

    public HttpRequestExpectationBuilder withoutParam(String name) {
        return and(this.expectation, BY_PARAMETER_NOT_IN_QUERY_STRING.apply(name));
    }

    public HttpRequestExpectationBuilder port(int port) {
        return and(this.expectation, BY_PORT.apply(port));
    }

    public HttpRequestExpectationBuilder post() {
        return httpMethod(HttpMethod.POST);
    }

    public HttpRequestExpectationBuilder put() {
        return httpMethod(HttpMethod.PUT);
    }

    public HttpRequestExpectationBuilder scheme(String scheme) {
        return and(this.expectation, BY_SCHEME.apply(scheme));
    }

    public HttpRequestExpectationBuilder serverMethod(String serverMethod) {
        return and(this.expectation, BY_SERVER_METHOD.apply(ApiStrings.forceLeadingSlash(serverMethod)));
    }

    public HttpRequestExpectationBuilder uri(URI uri) {
        return and(this.expectation, BY_URI.apply(uri));
    }

    public HttpRequestExpectationBuilder patch() {
        return httpMethod(HttpMethod.PATCH);
    }

    private HttpRequestExpectationBuilder and(HttpRequestExpectation... items) {
        return new HttpRequestExpectationBuilder(HttpExpectationCombinators.and(items));
    }

    private HttpRequestExpectationBuilder httpMethod(HttpMethod method) {
        return and(expectation, BY_HTTP_METHOD.apply(method));
    }
}
