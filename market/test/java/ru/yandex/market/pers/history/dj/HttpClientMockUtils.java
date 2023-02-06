package ru.yandex.market.pers.history.dj;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.mockito.ArgumentMatcher;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpMethod;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author varvara
 * 19.06.2019
 */
public class HttpClientMockUtils {

    public static void mockResponse(HttpClient client, Answer<InputStream> responseAnswer) {
        mockResponse(client, responseAnswer, argument -> true);
    }

    public static void mockResponse(HttpClient client, Answer<InputStream> responseAnswer,
                                    ArgumentMatcher<HttpUriRequest> requestQueryMatcher) {
        mockResponse(client, HttpStatus.SC_OK, responseAnswer, requestQueryMatcher);
    }

    public static void mockResponseWithString(HttpClient client, String answer) {
        mockResponse(client, req -> new ByteArrayInputStream(answer.getBytes()));
    }

    public static void mockResponseWithFile(HttpClient client, String responseFile) {
        HttpClientMockUtils.mockResponseWithFile(
            client,
            responseFile,
            argument -> true);
    }

    public static void mockResponseWithFile(HttpClient client,
                                            String responseFile,
                                            ArgumentMatcher<HttpUriRequest> requestQueryMatcher) {
        HttpClientMockUtils.mockResponseWithFile(
            client,
            HttpStatus.SC_OK,
            responseFile,
            requestQueryMatcher);
    }

    public static void mockResponseWithFile(HttpClient client,
                                            int code,
                                            String responseFile,
                                            ArgumentMatcher<HttpUriRequest> requestQueryMatcher) {
        mockResponse(client,
            code,
            req -> HttpClientMockUtils.class.getResourceAsStream(responseFile),
            requestQueryMatcher);
    }

    public static void mockResponse(HttpClient client,
                                    int status,
                                    Answer<InputStream> responseAnswer,
                                    ArgumentMatcher<HttpUriRequest> requestQueryMatcher) {
        try {
            HttpResponse response = createMockedHttpResponse(status, responseAnswer);
            when(client.execute(argThat(requestQueryMatcher))).thenReturn(response);
            when(client.execute(argThat(requestQueryMatcher), any(HttpContext.class)))
                    .thenReturn(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse createMockedHttpResponse(int status, Answer<InputStream> responseAnswer) {
        try {
            HttpResponse response = mock(HttpResponse.class);
            StatusLine statusLine = mock(StatusLine.class);
            HttpEntity httpEntity = mock(HttpEntity.class);

            when(response.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(status);

            when(response.getEntity()).thenReturn(httpEntity);
            when(httpEntity.getContent()).then(responseAnswer);
            when(httpEntity.isStreaming()).thenReturn(true);
            when(response.getAllHeaders()).thenReturn(new Header[]{
                new BasicHeader("Content-Type", "application/json")
            });
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ArgumentMatcher<HttpUriRequest> withPath(String path) {
        return new PathRegexMatcher(path);
    }

    public static ArgumentMatcher<HttpUriRequest> withQueryParam(String name, Object value) {
        return withQueryParam(String.format("%s=%s", name, value));
    }

    public static ArgumentMatcher<HttpUriRequest> withQueryParam(String nameAndValue) {
        return new QueryRegexMatcher("(^|.*?&|\\?)" + nameAndValue + "(&.*|$)");
    }

    public static ArgumentMatcher<HttpUriRequest> withMethod(HttpMethod method) {
        return new HttpMethodMatcher(method);
    }

    @SafeVarargs
    public static <T> ArgumentMatcher<T> and(ArgumentMatcher<T>... matchers) {
        return x -> {
            for (ArgumentMatcher<T> matcher : matchers) {
                if (!matcher.matches(x)) {
                    return false;
                }
            }
            return true;
        };
    }
}
