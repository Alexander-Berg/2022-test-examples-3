package ru.yandex.market.api.listener.expectations;

import ru.yandex.market.api.listener.domain.HttpHeaders;
import ru.yandex.market.api.listener.domain.HttpMethod;
import ru.yandex.market.api.util.ApiCollections;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpRequestDescription {
    private static final String END_OF_LINE = "\n";
    private final HttpMethod method;
    private final String host;
    private final Integer port;
    private final String serverMethod;
    private final Map<String, List<String>> parameters;
    private final List<String> bodyConditions;
    private final String scheme;
    private final Set<String> withoutParameters;
    private final HttpHeaders headers;

    HttpRequestDescription(HttpMethod method,
                           String scheme,
                           String host,
                           Integer port,
                           String serverMethod,
                           Map<String, List<String>> parameters,
                           Set<String> withoutParameters,
                           HttpHeaders headers,
                           List<String> bodyConditions) {
        this.method = method;
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.serverMethod = serverMethod;
        this.parameters = parameters;
        this.withoutParameters = withoutParameters;
        this.bodyConditions = bodyConditions;
        this.headers = headers;
    }

    @Override
    public String toString() {
        return String.format("Request: %s %s%s%s",
                             prettyPrintMethod(),
                             prettyPrintRootUrl(),
                             END_OF_LINE,
                             prettyPrintExtendedAttributes());
    }

    List<String> getBodyConditions() {
        return new ArrayList<>(bodyConditions);
    }

    String getHost() {
        return host;
    }

    HttpMethod getMethod() {
        return method;
    }

    Map<String, List<String>> getParameters() {
        return new HashMap<>(parameters);
    }

    Integer getPort() {
        return port;
    }

    String getScheme() {
        return scheme;
    }

    String getServerMethod() {
        return serverMethod;
    }

    Set<String> getWithoutParameters() {
        return withoutParameters;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    private String prettyPrintBodyConditions(List<String> bodyConditions) {
        if (ApiCollections.isEmpty(bodyConditions)) {
            return "";
        }
        return bodyConditions.stream().map(x -> String.format("request body condition: '%s' ", x))
            .collect(Collectors.joining(END_OF_LINE));
    }

    private String prettyPrintExtendedAttributes() {
        return Stream.of(
            prettyPrintParameters(parameters),
            prettyPrintNotExistingParameters(withoutParameters),
            prettyPrintHeaders(headers),
            prettyPrintBodyConditions(bodyConditions)
        ).collect(Collectors.joining(END_OF_LINE));
    }

    @NotNull
    private String prettyPrintMethod() {
        return (this.method == null ? HttpMethod.GET : this.method).toString().toUpperCase();
    }

    private String prettyPrintNotExistingParameters(Set<String> withoutParameters) {
        return withoutParameters.stream()
            .map(x -> String.format("without parameter: %s", x))
            .collect(Collectors.joining(END_OF_LINE));
    }

    private String prettyPrintParameters(Map<String, List<String>> parameters) {
        if (ApiCollections.isEmpty(parameters)) {
            return "";
        }
        return parameters.entrySet().stream()
            .map(x -> String.format("parameter: '%s' value: '%s'", x.getKey(), prettyPrintParameterValues(x.getValue())))
            .collect(Collectors.joining(END_OF_LINE));
    }

    private String prettyPrintHeaders(HttpHeaders headers) {
        if (0 == headers.size()) {
            return "";
        }
        return headers.entries().stream()
            .map(x -> String.format("header: '%s' value: '%s'", x.getKey(), x.getValue()))
            .collect(Collectors.joining(END_OF_LINE));
    }

    private String prettyPrintParameterValues(List<String> values) {
        if (values.size() == 1) {
            return values.get(0);
        }
        return String.format("[%s]", values
            .stream()
            .collect(Collectors.joining(",")));
    }

    private String prettyPrintRootUrl() {
        return Stream.of((scheme == null ? "http" : scheme) + "://",
                         (host == null ? "localhost" : host),
                         (port == null ? "" : ":" + port.toString()),
                         (serverMethod == null ? "" : serverMethod)
        ).collect(Collectors.joining(""));
    }

}
