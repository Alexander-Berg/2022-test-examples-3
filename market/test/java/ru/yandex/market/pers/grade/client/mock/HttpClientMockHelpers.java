package ru.yandex.market.pers.grade.client.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.util.StreamUtils;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpClientMockHelpers {
    public static ArgumentMatcher<HttpUriRequest> withQuery(String path, String params) {
        return new QueryRegexMatcher(path, params);
    }

    public static ArgumentMatcher<HttpUriRequest> withPath(String path) {
        return new QueryRegexMatcher(path, null);
    }

    public static ArgumentMatcher<HttpUriRequest> withQueryParams(String params) {
        return new QueryRegexMatcher(null, params);
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

    public static HttpResponse getHttpResponseMock(HttpEntity httpEntity, StatusLine statusLine) {
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
        return httpResponse;
    }

    public static HttpResponse getHttpResponseMock(String content, int code) throws IOException {
        return getHttpResponseMock(httpEntityMock(content), mockStatusLineWithCode(code));
    }

    public static StatusLine mockStatusLineWithCode(int code) {
        StatusLine result = Mockito.mock(StatusLine.class);
        Mockito.when(result.getStatusCode()).thenReturn(code);
        return result;
    }

    public static HttpEntity httpEntityMock(String content) throws IOException {
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        ByteArrayInputStream responseStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
        Mockito.when(httpEntity.getContent()).thenReturn(responseStream);
        Mockito.when(httpEntity.isStreaming()).thenReturn(true);
        return httpEntity;
    }

    public static HttpClient mockHttpClient(InputStream responseInputStream) throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        String content = StreamUtils.copyToString(responseInputStream, Charset.forName("UTF-8"));
        HttpResponse mockHttpResponse = getHttpResponseMock(content, 200);
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        return httpClient;
    }

    public static void mockResponseWithFile(HttpClient client,
                                            int code,
                                            String responseFile) {
        mockResponseWithFile(client, code, responseFile, invocation -> true);
    }

    public static void mockResponseWithFile(HttpClient client,
                                            int code,
                                            String responseFile,
                                            ArgumentMatcher<HttpUriRequest> requestQueryMatcher) {
        mockResponse(client,
            code,
            req -> HttpClientMockHelpers.class.getResourceAsStream(responseFile),
            requestQueryMatcher);
    }

    public static void mockResponse(HttpClient client,
                                    int status,
                                    Answer<InputStream> responseAnswer) {
        mockResponse(client, status, responseAnswer, argument -> true);
    }

    public static void mockResponse(HttpClient client,
                                    int status,
                                    Answer<InputStream> responseAnswer,
                                    ArgumentMatcher<HttpUriRequest> requestQueryMatcher) {
        try {
            HttpResponse response = mock(HttpResponse.class);
            StatusLine statusLine = mock(StatusLine.class);
            HttpEntity httpEntity = mock(HttpEntity.class);

            when(client.execute(argThat(requestQueryMatcher), any(HttpContext.class))).thenReturn(response);

            when(response.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(status);

            when(response.getEntity()).thenReturn(httpEntity);
            when(httpEntity.getContent()).then(responseAnswer);
            when(httpEntity.isStreaming()).thenReturn(true);
            when(response.getAllHeaders()).thenReturn(new Header[]{
                new BasicHeader("Content-Type", "application/json")
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
