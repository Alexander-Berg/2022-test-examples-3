package ru.yandex.market.pers.notify.mock;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.Mockito;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpClientMockHelpers {
    public static HttpResponse getHttpResponseMock(HttpEntity httpEntity, StatusLine statusLine) {
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
        return httpResponse;
    }

    public static HttpResponse getHttpResponseMock(String content, int code) throws IOException {
        return getHttpResponseMock(httpEntityMock(content), mockStatusLineWithCode(code));
    }

    private static StatusLine mockStatusLineWithCode(int code) {
        StatusLine result = Mockito.mock(StatusLine.class);
        Mockito.when(result.getStatusCode()).thenReturn(code);
        return result;
    }

    private static HttpEntity httpEntityMock(String content) throws IOException {
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        ByteArrayInputStream responseStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
        Mockito.when(httpEntity.getContent()).thenReturn(responseStream);
        return httpEntity;
    }

    public static HttpClient mockHttpClient(InputStream responseInputStream) throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        String content = StreamUtils.copyToString(responseInputStream, Charset.forName("UTF-8"));
        HttpResponse mockHttpResponse = getHttpResponseMock(content, 200);
        when(httpClient.execute(any())).thenReturn(mockHttpResponse);
        return httpClient;
    }



    public static Matcher<HttpUriRequest> withQueryParam(String nameAndValue) {
        return new HttpRequestMatcher("(.*&|\\?|^)" + nameAndValue + "(&.*|$)", URI::getQuery);
    }

    public static Matcher<HttpUriRequest> withPath(String path) {
        return new HttpRequestMatcher(path, URI::getPath);
    }


    private static class HttpRequestMatcher extends BaseMatcher<HttpUriRequest> {
        private final String regex;
        private final Function<URI, String> extractor;

        private HttpRequestMatcher(String regex, Function<URI, String> extractor) {
            this.regex = regex;
            this.extractor = extractor;
        }

        @Override
        public boolean matches(Object item) {
            if (item instanceof HttpUriRequest) {
                return uriMatches(((HttpUriRequest) item).getURI());
            }

            return false;
        }

        private boolean uriMatches(URI uri) {
            return Pattern.matches(regex, extractor.apply(uri));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("regex: " + regex);
        }
    }
}
