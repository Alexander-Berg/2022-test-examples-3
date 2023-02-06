package ru.yandex.market.jmf.http.test.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.jmf.http.Http;
import ru.yandex.market.jmf.http.HttpResponse;
import ru.yandex.market.jmf.http.HttpStatus;
import ru.yandex.market.jmf.http.test.ResponseBuilder;
import ru.yandex.market.jmf.utils.test.StatefulHelper;

/**
 * @author apershukov
 */
@Component
public class HttpEnvironment implements StatefulHelper {

    private static final Logger LOG = LoggerFactory.getLogger(HttpEnvironment.class);
    private static final HttpResponse NOT_FOUND_RESPONSE = ResponseBuilder.newBuilder()
            .code(404)
            .body(new byte[0])
            .build();
    private static final Pattern HOST_PATTERN = Pattern.compile("^(https?://[^/]+)/?.*$");
    private final List<Expectation> expectations = new LinkedList<>();

    private static String getHost(String url) {
        Matcher matcher = HOST_PATTERN.matcher(url);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Something is wrong. Cannot extract host from " + url);
    }

    private static String getPath(String url) {
        int queryStart = url.indexOf('?');
        return queryStart < 0 ? url : url.substring(0, queryStart);
    }

    public ExpectationBuilder when(HttpRequest request) {
        return new ExpectationBuilder(request);
    }

    @Override
    public void setUp() {
    }

    @Override
    public void tearDown() {
        expectations.clear();
    }

    HttpResponse execute(Http request, String baseUrl) {
        Optional<HttpResponse> response = doExecute(request, baseUrl);

        String requestDescription = request.getMethod().name() + " " + request.getUrl(baseUrl);

        byte[] requestBody = request.getBody();
        if (requestBody != null) {
            requestDescription += "\n" + new String(requestBody);
        }

        String responseDescription = response
                .map(r -> {
                    HttpStatus status = r.getHttpStatus();
                    return status.value() + " " + status.getReasonPhrase() + "\n" + r.getBodyAsString();
                })
                .orElse("UNKNOWN HOST");

        LOG.info("Request: {}\nResponse: {}", requestDescription, responseDescription);

        return response.orElseThrow(() -> new RuntimeException("Unknown host"));
    }

    private Optional<HttpResponse> doExecute(Http request, String baseUrl) {
        String url = request.getUrl(baseUrl);
        String host = getHost(url);

        List<Expectation> matchingExpectations = expectations.stream()
                .filter(e -> getHost(e.httpRequest.getUrl()).equals(host))
                .filter(e -> e.httpRequest.getMethod().equals(request.getMethod()))
                .filter(e -> getPath(e.httpRequest.getUrl()).equals(url))
                .toList();

        for (Expectation expectation : matchingExpectations) {
            HttpRequest expRequest = expectation.httpRequest;
            List<Http.NamedValue> expParams = expRequest.getParams();
            List<ParamValues> paramsDiff = expParams.stream()
                    .map(p1 -> new ParamValues(
                            p1.getName(),
                            p1.getValue(),
                            request.getQueryParameters().stream()
                                    .filter(p2 -> Objects.equals(p1.getName(), p2.getName()))
                                    .map(Http.NamedValue::getValue)
                                    .collect(Collectors.toList())))
                    .filter(ParamValues::notContaining)
                    .toList();

            if (paramsDiff.isEmpty()) {
                try {
                    return Optional.of(expectation.handler.handle(request));
                } catch (Exception e) {
                    throw Exceptions.sneakyThrow(e);
                }
            }
        }

        LOG.error("Unexpected request " + url);

        boolean expectedOnSameHost = expectations.stream()
                .map(e -> getHost(e.httpRequest.getUrl()))
                .anyMatch(expHost -> expHost.equals(host));
        if (expectedOnSameHost) {
            return Optional.of(NOT_FOUND_RESPONSE);
        }

        return Optional.empty();
    }

    @FunctionalInterface
    public interface RequestHandler {

        HttpResponse handle(Http request) throws Exception;

    }

    private record ParamValues(String paramName, String expectedValue,
                               List<String> actualValues) {

        public boolean notContaining() {
            return actualValues.stream().noneMatch(s -> Objects.equals(s, expectedValue));
        }
    }

    private record Expectation(HttpRequest httpRequest,
                               RequestHandler handler) {

    }

    public class ExpectationBuilder {

        private final HttpRequest request;

        ExpectationBuilder(HttpRequest request) {
            this.request = request;
        }

        public void then(HttpResponse response) {
            then(request -> response);
        }

        public void then(RequestHandler handler) {
            expectations.add(0, new Expectation(request, handler));
        }

    }

}
