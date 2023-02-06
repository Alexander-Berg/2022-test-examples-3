package ru.yandex.market.saas.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.internal.matchers.And;
import org.mockito.internal.matchers.Not;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SaasMockUtils {
    private SaasMockUtils() {

    }

    public static void mockSaasResponse(HttpClient client, Answer<InputStream> responseAnswer,
                                        Matcher<HttpUriRequest> saasRequestMatcher) {
        mockSaasResponse(client, HttpStatus.SC_OK, responseAnswer, saasRequestMatcher);
    }

    public static void mockSaasResponse(HttpClient client,
                                        int statusCode,
                                        Answer<InputStream> responseAnswer,
                                        Matcher<HttpUriRequest> saasRequestMatcher) {
        try {
            HttpResponse response = mock(HttpResponse.class);
            StatusLine statusLine = mock(StatusLine.class);
            HttpEntity httpEntity = mock(HttpEntity.class);

            when(client.execute(argThat(saasRequestMatcher))).thenReturn(response);

            when(response.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(statusCode);

            when(response.getEntity()).thenReturn(httpEntity);
            when(httpEntity.getContent()).then(responseAnswer);
            when(httpEntity.isStreaming()).thenReturn(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockSaasResponseWithFile(HttpClient client, String responseFile,
                                                Matcher<HttpUriRequest> requestQueryMatcher) {
        mockSaasResponseWithFile(client, responseFile, HttpStatus.SC_OK, requestQueryMatcher);
    }

    public static void mockSaasResponseWithFile(HttpClient client, String responseFile, int statusCode,
                                                Matcher<HttpUriRequest> requestQueryMatcher) {
        mockSaasResponse(client,
                statusCode, req -> SaasMockUtils.class.getResourceAsStream(responseFile),
                requestQueryMatcher);
    }

    public static Matcher<HttpUriRequest> withSearchAttribute(String name, String... values) {
        if (values.length == 0) {
            throw new RuntimeException("no values");
        }
        String expectedQuery = Arrays.stream(values)
                .map(x -> name + ":" + x)
                .collect(Collectors.joining(" \\| "));
        return withQueryParam(".*?text=.*?\\(?" + expectedQuery + "\\)?.*?");
    }

    public static Matcher<HttpUriRequest> withTextBody(String query) {
        return new SaasRequestBodyMatcher(query);
    }

    public static Matcher<HttpUriRequest> withPartialText(String text) {
        if (StringUtils.isEmpty(text)) {
            throw new RuntimeException("no values");
        }
        return withQueryParam(".*?text=.*?\\(?" + text + "\\)?.*?");
    }

    public static Matcher<HttpUriRequest> withSearchExcludeAttribute(String name, String value) {
        return withQueryParam(".*?text=.*? ~~ \\(?" + name + ":" + value + "\\)?.*?");
    }

    public static Matcher<HttpUriRequest> withNoCache() {
        return withQueryParam("nocache=1");
    }

    @SafeVarargs
    public static Matcher<HttpUriRequest> and(Matcher<HttpUriRequest>... matchers) {
        return new And(Arrays.asList(matchers));
    }

    public static Matcher<HttpUriRequest> not(Matcher<HttpUriRequest> matcher) {
        return new Not(matcher);
    }

    public static Matcher<HttpUriRequest> withDescSort(String sort) {
        return and(
                withRegex("(?!.*?asc=1.*).*"),
                withQueryParam("how=" + sort)
        );
    }

    public static Matcher<HttpUriRequest> withRegex(String regex) {
        return new SaasRequestMatcher(regex);
    }

    public static Matcher<HttpUriRequest> withAscSort(String sort) {
        return withQueryParam("how=" + sort + "&asc=1");
    }

    public static Matcher<HttpUriRequest> withQueryParam(String nameAndValue) {
        return withRegex("(.*?&|\\?)" + nameAndValue + "(&.*|$)");
    }

    public static Matcher<HttpUriRequest> withHeader(String header, String expectedValue){
        return new SaasRequestHeaderMatcher(header, expectedValue);
    }

    private static class SaasRequestHeaderMatcher extends BaseMatcher<HttpUriRequest> {
        private final String header;
        private final String expectedValue;

        private SaasRequestHeaderMatcher(String header, String expectedValuevalue) {
            this.header = header;
            this.expectedValue = expectedValuevalue;
        }

        @Override
        public boolean matches(Object item) {
            if (item instanceof HttpUriRequest) {
                String actualValue = ((HttpUriRequest) item).getFirstHeader(header).getValue();
                return expectedValue.equals(actualValue);
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(header + " : " + expectedValue);
        }
    }

    private static class SaasRequestMatcher extends BaseMatcher<HttpUriRequest> {
        private final String queryRegex;

        private SaasRequestMatcher(String queryRegex) {
            this.queryRegex = queryRegex;
        }

        @Override
        public boolean matches(Object item) {
            if (item instanceof HttpUriRequest) {
                URI uri = ((HttpUriRequest) item).getURI();
                try {
                    return queryMatches(URLDecoder.decode(uri.getQuery(), "UTF-8"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            return false;
        }

        private boolean queryMatches(String query) {
            return Pattern.matches(queryRegex, query);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Query regex: " + queryRegex);
        }
    }

    private static class SaasRequestBodyMatcher extends BaseMatcher<HttpUriRequest> {
        private final String queryInBody;

        private SaasRequestBodyMatcher(String queryInBody) {
            this.queryInBody = queryInBody;
        }


        @Override
        public boolean matches(Object item) {
            if (item instanceof HttpEntityEnclosingRequest) {
                HttpEntity body = ((HttpEntityEnclosingRequest) item).getEntity();
                try {
                    InputStream inputStream = body.getContent();
                    String content = IOUtils.toString(new InputStreamReader(inputStream));
                    return String.format("text=%s", queryInBody).equals(content);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            return false;
        }

        public void describeTo(Description description) {
            description.appendText("Query in body: " + queryInBody);
        }

    }
}
