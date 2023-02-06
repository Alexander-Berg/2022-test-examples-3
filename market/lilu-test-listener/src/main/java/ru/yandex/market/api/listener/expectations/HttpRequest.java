package ru.yandex.market.api.listener.expectations;


import com.google.common.base.Strings;
import ru.yandex.market.api.listener.domain.HttpHeaders;
import ru.yandex.market.api.listener.domain.HttpMethod;
import ru.yandex.market.api.util.ApiCollections;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class HttpRequest {
    private final HttpMethod method;
    private final URI uri;
    private final HttpHeaders httpHeaders;
    private final byte[] body;
    private final Map<String, List<String>> parameters;

    public HttpRequest(HttpMethod method,
                       URI uri,
                       HttpHeaders httpHeaders,
                       byte[] body) {
        this.method = method;
        this.uri = uri;
        this.httpHeaders = httpHeaders;
        this.body = body;
        this.parameters = HttpRequestUtils.getParametersFromUri(uri);
    }

    public byte[] getBody() {
        return body;
    }

    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public URI getUri() {
        return uri;
    }

    public boolean hasParameter(String name, String value) {
        if (ApiCollections.isEmpty(parameters)) {
            return false;
        }
        List<String> values = parameters.get(name);
        if (ApiCollections.isEmpty(values)) {
            return false;
        }
        return values.contains(value);
    }

    public boolean hasParameter(String name) {
        if (ApiCollections.isEmpty(parameters)) {
            return false;
        }
        List<String> values = parameters.get(name);
        if (ApiCollections.isEmpty(values)) {
            return false;
        }
        return values
            .stream()
            .filter(x -> !Strings.isNullOrEmpty(x))
            .findAny()
            .isPresent();
    }
}
