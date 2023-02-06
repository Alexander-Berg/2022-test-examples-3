package ru.yandex.market.request.httpclient.trace;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.apache.http.HttpException;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.request.trace.RequestTraceUtil;

import static org.apache.http.protocol.HttpCoreContext.HTTP_REQUEST;

public class HttpClientTraceUtilTest {
    private static final String URL = "http://report.tst.vs.market.yandex.net:17051/yandsearch?place=mainreport&pp=18";
    private static final String BLACKBOX_URL = "http://blackbox?sessionid=11111";
    private static final TestLogger logger = TestLoggerFactory.getTestLogger("requestTrace");

    private HttpResponse httpResponse;
    private TraceHttpResponseInterceptor traceHttpResponseInterceptor;

    @Before
    public void setUp() {
        this.httpResponse = mockHttpResponse();
        logger.clear();
    }

    @Test
    public void testTraceHttpResponseInterceptor() throws IOException, HttpException {
        HttpContext httpContext = mockRequest(BLACKBOX_URL);
        traceHttpResponseInterceptor = new TraceHttpResponseInterceptor(ImmutableSet.of("sessionid", "sslsessionid"));
        traceHttpResponseInterceptor.process(httpResponse, httpContext);
        Map<String, String> attributes = extractLogAttributes();
        Assert.assertEquals("http://blackbox?sessionid=************", attributes.get("query_params"));
    }

    @Test
    public void testTraceHttpResponseInterceptorWithNullParams() throws IOException, HttpException {
        HttpContext httpContext = mockRequest(BLACKBOX_URL);
        traceHttpResponseInterceptor = new TraceHttpResponseInterceptor(null);
        traceHttpResponseInterceptor.process(httpResponse, httpContext);
        Map<String, String> attributes = extractLogAttributes();
        Assert.assertEquals(BLACKBOX_URL, attributes.get("query_params"));
    }

    @Test
    public void testQueryHeaders() {
        HttpContext httpContext = mockRequest(URL);

        HttpClientTraceUtil.logRecord(httpContext, httpResponse, null);

        HttpRequest httpRequest = (HttpRequest) httpContext.getAttribute(HTTP_REQUEST);

        Assert.assertEquals("123/deadbeaf", httpRequest.getHeaders(RequestTraceUtil.REQUEST_ID_HEADER)[0].getValue());
        Assert.assertEquals("Test-User-Ticket", httpRequest.getHeaders(RequestTraceUtil.USER_TICKET_HEADER)[0].getValue());
    }

    @Test
    public void testQueryParams() {
        HttpContext httpContext = mockRequest(URL);

        HttpClientTraceUtil.logRecord(httpContext, httpResponse, null);

        Map<String, String> attributes = extractLogAttributes();

        Assert.assertEquals("/yandsearch?place=mainreport&pp=18", attributes.get("query_params"));
    }

    @Test
    public void testQueryParamsWithoutQuery() {
        HttpContext httpContext = mockRequest("http://checkouter.market.http.yandex.net:39001/orders/123");

        HttpClientTraceUtil.logRecord(httpContext, httpResponse, null);

        Map<String, String> attributes = extractLogAttributes();

        Assert.assertEquals("/orders/123", attributes.get("query_params"));
    }

    private static HttpContext mockRequest(String uri) {
        RequestContextHolder.setContext(new RequestContext("123/456"));

        RequestLine requestLine = Mockito.mock(RequestLine.class);
        Mockito.when(requestLine.getUri())
                .thenReturn(uri);
        Mockito.when(requestLine.getMethod())
                .thenReturn("GET");

        HttpRequest httpRequest = Mockito.mock(HttpRequest.class);
        Mockito.when(httpRequest.getRequestLine())
                .thenReturn(requestLine);
        Mockito.when(httpRequest.getHeaders(RequestTraceUtil.REQUEST_ID_HEADER))
                .thenReturn(new Header[]{new BasicHeader(RequestTraceUtil.REQUEST_ID_HEADER, "123/deadbeaf")});
        Mockito.when(httpRequest.getHeaders(RequestTraceUtil.USER_TICKET_HEADER))
                .thenReturn(new Header[]{new BasicHeader(RequestTraceUtil.USER_TICKET_HEADER, "Test-User-Ticket")});
        Mockito.when(httpRequest.getProtocolVersion())
                .thenReturn(new ProtocolVersion("http", 1, 1));

        HttpContext httpContext = Mockito.mock(HttpContext.class);
        Mockito.when(httpContext.getAttribute("TRACE_START_TIME_MILLIS"))
                .thenReturn(123L);
        Mockito.when(httpContext.getAttribute("TRACE_TARGET_MODULE"))
                .thenReturn(Module.REPORT);
        Mockito.when(httpContext.getAttribute(HTTP_REQUEST))
                .thenReturn(httpRequest);
        return httpContext;
    }

    private static HttpResponse mockHttpResponse() {
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        Mockito.when(statusLine.getStatusCode())
                .thenReturn(200);

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.getStatusLine())
                .thenReturn(statusLine);
        return httpResponse;
    }

    private static Map<String, String> extractLogAttributes() {
        LoggingEvent event = Iterables.getOnlyElement(logger.getLoggingEvents());
        Assert.assertEquals(Level.TRACE, event.getLevel());

        String message = event.getMessage();
        return Stream.of((String[]) message.split("\\t"))
                // пропускаем метку tskv
                .skip(1)
                .map(kv -> kv.split("=", 2))
                .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
    }
}
