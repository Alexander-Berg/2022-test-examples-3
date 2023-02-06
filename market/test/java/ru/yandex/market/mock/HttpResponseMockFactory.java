package ru.yandex.market.mock;

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
import org.mockito.stubbing.Answer;
import org.springframework.util.StreamUtils;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author korolyov
 * 21.06.16
 */
public class HttpResponseMockFactory {
    private HttpResponseMockFactory() {

    }

    public static HttpResponse getHttpResponseMock(HttpEntity httpEntity, StatusLine statusLine) {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[] {
                new BasicHeader("Content-Type", "application/json")
        });
        return httpResponse;
    }

    public static HttpResponse getHttpResponseMock(String content, int code) throws IOException {
        return getHttpResponseMock(HttpEntityMockFactory.httpEntityMock(content),
                StatusLineMockFactory.mockStatusLineWithCode(code));
    }

    public static HttpClient mockHttpClient(InputStream responseInputStream) throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        mockResponse(httpClient, responseInputStream, 200);
        return httpClient;
    }

    public static void mockResponse(HttpClient client, InputStream responseInputStream, int code) throws IOException {
        String content = StreamUtils.copyToString(responseInputStream, Charset.forName("UTF-8"));
        Answer<Object> handleResult = invocation -> getHttpResponseMock(content, code);
        when(client.execute(any())).then(handleResult);
        when(client.execute(any(HttpUriRequest.class), any(HttpContext.class)))
                .then(handleResult);
    }

}
