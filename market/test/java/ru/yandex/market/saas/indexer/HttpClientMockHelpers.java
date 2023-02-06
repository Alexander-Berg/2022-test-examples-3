package ru.yandex.market.saas.indexer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.mockito.Mockito;
import org.springframework.util.StreamUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

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

    public static StatusLine mockStatusLineWithCode(int code) {
        StatusLine result = Mockito.mock(StatusLine.class);
        Mockito.when(result.getStatusCode()).thenReturn(code);
        return result;
    }

    public static HttpEntity httpEntityMock(String content) throws IOException {
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        ByteArrayInputStream responseStream = new ByteArrayInputStream(content.getBytes("UTF-8"));
        // required to handle correct stream closing
        BufferedInputStream resultStream = new BufferedInputStream(responseStream);
        Mockito.when(httpEntity.getContent()).thenReturn(resultStream);
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
}
