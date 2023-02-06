package ru.yandex.calendar.logic.ics.feed;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.val;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZoraResponseInterceptorTest {
    private static final String METRIC_NAME = "application.test";
    private MeterRegistry registry;

    @Before
    public void setUp() {
        registry = mock(MeterRegistry.class);
        when(registry.counter(anyString())).thenReturn(mock(Counter.class));
    }

    private static HttpResponse getResponse(int status) {
        val statusLine = mock(StatusLine.class);
        val response = mock(HttpResponse.class);

        when(statusLine.getStatusCode()).thenReturn(status);
        when(response.getStatusLine()).thenReturn(statusLine);

        return response;
    }

    @Test
    public void check2xx() {
        check(HttpStatus.SC_CREATED);
    }

    @Test
    public void check4xx() {
        check(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void check503WithoutZoraHeaders() {
        check(HttpStatus.SC_SERVICE_UNAVAILABLE);
        verify(registry, times(1)).counter(anyString());
    }

    @Test
    public void checkQuotaExceeded() {
        val interceptor = new ZoraResponseInterceptor(METRIC_NAME, registry);
        val response = getResponse(HttpStatus.SC_SERVICE_UNAVAILABLE);

        val codeHeader = mock(Header.class);
        when(response.getFirstHeader("X-Yandex-Http-Code")).thenReturn(codeHeader);
        when(codeHeader.getValue()).thenReturn("429");

        interceptor.process(response, null);
        checkDefaultMetric(registry);
        verify(registry, times(1)).counter(String.format("%s.quota", METRIC_NAME));
        verify(registry, times(2)).counter(anyString());
    }

    private void check(int status) {
        val interceptor = new ZoraResponseInterceptor(METRIC_NAME, registry);
        interceptor.process(getResponse(status), null);
        checkDefaultMetric(registry);
    }

    private static void checkDefaultMetric(MeterRegistry registry) {
        verify(registry, times(1)).counter(METRIC_NAME);
    }
}
