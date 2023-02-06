package ru.yandex.market.api.util.httpclient.spi;

import com.google.common.base.Strings;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

import java.util.*;

class HttpRequestDescriptionBuilder {
    private HttpMethod method;
    private String host;
    private Integer port;
    private String serverMethod;
    private String scheme;
    private Map<String, List<String>> parameters = new HashMap<>();
    private Set<String> withoutParameters = new HashSet<>();
    private HttpHeaders headers = new DefaultHttpHeaders();
    private List<String> bodyConditions = new ArrayList<>();

    public HttpRequestDescription get() {
        return new HttpRequestDescription(
            method,
            scheme,
            host,
            port,
            serverMethod,
            parameters,
            withoutParameters,
            headers,
            bodyConditions);
    }

    public HttpRequestDescriptionBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    HttpRequestDescriptionBuilder setPort(Integer port) {
        this.port = port;
        return this;
    }

    HttpRequestDescriptionBuilder setScheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    public HttpRequestDescriptionBuilder setServerMethod(String serverMethod) {
        this.serverMethod = serverMethod;
        return this;
    }

    HttpRequestDescriptionBuilder setHttpMethod(HttpMethod method) {
        this.method = method;
        return this;
    }

    HttpRequestDescriptionBuilder withBodyCondition(String description) {
        if (!Strings.isNullOrEmpty(description)) {
            bodyConditions.add(description);
        }
        return this;
    }

    HttpRequestDescriptionBuilder withBodyConditions(Collection<String> descriptions) {
        descriptions.forEach(this::withBodyCondition);
        return this;
    }

    HttpRequestDescriptionBuilder withParameter(String name, String value) {
        return addParameter(parameters, name, value);
    }

    HttpRequestDescriptionBuilder withHeader(String name, String value) {
        headers.add(name, value);
        return this;
    }

    HttpRequestDescriptionBuilder withHeaders(HttpHeaders headers) {
        this.headers.setAll(headers);
        return this;
    }

    HttpRequestDescriptionBuilder withParameters(Map<String, List<String>> parameters) {
        parameters.entrySet()
            .forEach(x -> withParameters(x.getKey(), x.getValue()));
        return this;
    }

    HttpRequestDescriptionBuilder withoutParameter(String name) {
        withoutParameters.add(name);
        return this;
    }

    HttpRequestDescriptionBuilder withoutParameters(Set<String> without) {
        this.withoutParameters.addAll(without);
        return this;
    }

    private HttpRequestDescriptionBuilder addParameter(Map<String, List<String>> parameters,
                                                       String name,
                                                       String value) {
        List<String> values = parameters.computeIfAbsent(name, k -> new ArrayList<>());
        values.add(value);
        return this;
    }

    private HttpRequestDescriptionBuilder withParameters(String key, List<String> values) {
        values.forEach(v -> withParameter(key, v));
        return this;
    }
}
