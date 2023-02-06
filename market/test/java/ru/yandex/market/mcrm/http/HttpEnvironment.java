package ru.yandex.market.mcrm.http;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.mcrm.utils.test.StatefulHelper;

/**
 * @author apershukov
 */
public class HttpEnvironment implements StatefulHelper {

    private static final Logger LOG = LoggerFactory.getLogger(HttpEnvironment.class);
    private static final HttpResponse NOT_FOUND_RESPONSE = new HttpResponse(
            new ResponseMock(404, new byte[0])
    );
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

        List<Expectation> matchingExpectaions = expectations.stream()
                .filter(e -> getHost(e.httpRequest.getUrl()).equals(host))
                .filter(e -> e.httpRequest.getMethod().equals(request.getMethod()))
                .filter(e -> getPath(e.httpRequest.getUrl()).equals(url))
                .collect(Collectors.toList());

        for (Expectation expectation : matchingExpectaions) {
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
                    .collect(Collectors.toList());

            if (paramsDiff.isEmpty()) {
                try {
                    return Optional.of(expectation.handler.handle(request));
                } catch (Exception e) {
                    throw new RuntimeException(e);
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

    private static class ParamValues {

        private final String paramName;
        private final String expectedValue;
        private final List<String> actualValues;

        public ParamValues(String paramName, String expectedValue, List<String> actualValues) {
            this.paramName = paramName;
            this.expectedValue = expectedValue;
            this.actualValues = actualValues;
        }

        public boolean notContaining() {
            return actualValues.stream().noneMatch(s -> Objects.equals(s, expectedValue));
        }

        @Override
        public String toString() {
            return "ParamValues{" +
                    "paramName='" + paramName + '\'' +
                    ", expectedValue='" + expectedValue + '\'' +
                    ", actualValues=" + actualValues +
                    '}';
        }
    }

    public interface RequestHandler {

        HttpResponse handle(Http request) throws Exception;

    }

    private static class Expectation {

        private final HttpRequest httpRequest;
        private final RequestHandler handler;

        private Expectation(HttpRequest httpRequest, RequestHandler handler) {
            this.httpRequest = httpRequest;
            this.handler = handler;
        }

    }

    public class ExpectationBuilder {

        private final HttpRequest request;

        ExpectationBuilder(HttpRequest request) {
            this.request = request;
        }

        public void then(HttpResponse response) {
            then(new StaticRequestHandler(response));
        }

        public void then(RequestHandler handler) {
            expectations.add(0, new Expectation(request, handler));
        }

    }

    private static class StaticRequestHandler implements RequestHandler {

        private final HttpResponse response;

        private StaticRequestHandler(HttpResponse response) {
            this.response = response;
        }

        @Override
        public HttpResponse handle(Http request) {
            return response;
        }

    }

}
