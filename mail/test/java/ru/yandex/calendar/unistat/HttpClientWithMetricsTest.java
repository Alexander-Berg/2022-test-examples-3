package ru.yandex.calendar.unistat;

import java.net.URI;
import java.util.regex.Pattern;

import lombok.SneakyThrows;
import lombok.val;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.HttpClientWithMetrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpClientWithMetricsTest extends AbstractConfTest {
    private HttpResponse createHttpResponse(int code) {
        val statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(code);

        HttpResponse resp = mock(HttpResponse.class);
        when(resp.getStatusLine()).thenReturn(statusLine);
        return resp;
    }

    @Test
    @SneakyThrows
    public void executeTest() {
        val responseHandler = mock(ResponseHandler.class);
        val requestLine = mock(RequestLine.class);
        val httpContext = mock(HttpContext.class);
        val httpClientMock = mock(HttpClient.class);
        try (val httpClientWithMetrics = new HttpClientWithMetrics(httpClientMock, registry, "testServiceName",
                Pattern.compile("^(/some/path/tostrip)"))) {

            val httpUriRequest = mock(HttpUriRequest.class);
            val httpHost = new HttpHost("host", 88);
            val httpRequest = mock(HttpRequest.class);
            val resp500 = createHttpResponse(500);
            val resp200 = createHttpResponse(200);
            val resp404 = createHttpResponse(404);

            when(requestLine.getUri()).thenReturn("/some/path2");
            when(httpRequest.getRequestLine()).thenReturn(requestLine);
            when(httpUriRequest.getURI()).thenReturn(new URI("/some/path/tostrip/1111111"));

            when(httpClientMock.execute(httpUriRequest)).thenReturn(resp404);
            httpClientWithMetrics.execute(httpUriRequest);
            checkCounterValue("application.request.testServiceName./some/path/tostrip.4xx", 1);
            checkTimer("application.request.time.testServiceName./some/path/tostrip");

            when(httpClientMock.execute(httpUriRequest, httpContext)).thenReturn(resp200);
            httpClientWithMetrics.execute(httpUriRequest, httpContext);
            checkCounterValue("application.request.testServiceName./some/path/tostrip.2xx", 1);
            checkCounterValue("application.request.testServiceName./some/path/tostrip.4xx", 1);

            when(httpClientMock.execute(httpHost, httpRequest)).thenReturn(resp500);
            httpClientWithMetrics.execute(httpHost, httpRequest);
            checkCounterValue("application.request.testServiceName./some/path2.5xx", 1);

            when(httpClientMock.execute(httpHost, httpRequest, httpContext)).thenReturn(resp200);
            httpClientWithMetrics.execute(httpHost, httpRequest, httpContext);
            checkCounterValue("application.request.testServiceName./some/path2.2xx", 1);

            when(httpClientMock.execute(any(HttpUriRequest.class), any(ResponseHandler.class))).thenAnswer(invocation -> {
                Object[] args = invocation.getArguments();
                return ((ResponseHandler) args[1]).handleResponse(resp200);
            });
            httpClientWithMetrics.execute(httpUriRequest, responseHandler);
            checkCounterValue("application.request.testServiceName./some/path/tostrip.2xx", 2);

            when(httpClientMock.execute(any(HttpUriRequest.class), any(ResponseHandler.class), any(HttpContext.class)))
                    .thenAnswer(invocation -> {
                        Object[] args = invocation.getArguments();
                        return ((ResponseHandler) args[1]).handleResponse(resp200);
                    });
            httpClientWithMetrics.execute(httpUriRequest, responseHandler, httpContext);
            checkCounterValue("application.request.testServiceName./some/path/tostrip.2xx", 3);

            when(httpClientMock.execute(any(HttpHost.class), any(HttpRequest.class), any(ResponseHandler.class)))
                    .thenAnswer(invocation -> {
                        Object[] args = invocation.getArguments();
                        return ((ResponseHandler) args[2]).handleResponse(resp200);
                    });
            httpClientWithMetrics.execute(httpHost, httpRequest, responseHandler);
            checkCounterValue("application.request.testServiceName./some/path2.2xx", 2);

            when(httpClientMock.execute(any(HttpHost.class), any(HttpRequest.class), any(ResponseHandler.class),
                    any(HttpContext.class)))
                    .thenAnswer(invocation -> {
                        Object[] args = invocation.getArguments();
                        return ((ResponseHandler) args[2]).handleResponse(resp200);
                    });
            httpClientWithMetrics.execute(httpHost, httpRequest, responseHandler, httpContext);
            checkCounterValue("application.request.testServiceName./some/path2.2xx", 3);
        }
    }
}
