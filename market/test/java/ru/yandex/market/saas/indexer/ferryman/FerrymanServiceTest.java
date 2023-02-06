package ru.yandex.market.saas.indexer.ferryman;

import java.net.URLDecoder;
import java.time.Instant;
import java.util.function.Consumer;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.saas.indexer.ferryman.model.CommonFerrymanFormat;
import ru.yandex.market.saas.indexer.ferryman.model.YtTableRef;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.util.Assert.isInstanceOf;
import static ru.yandex.market.saas.indexer.HttpClientMockHelpers.getHttpResponseMock;

public class FerrymanServiceTest {

    private FerrymanService ferrymanServiceSpy;
    private HttpClient httpClient;

    private Consumer<Long> mockDelayer;

    @Before
    public void setUp() throws InterruptedException {
        httpClient = mock(HttpClient.class);
        mockDelayer = mock(Consumer.class);

        //doNothing().when(mockDelayer).accept(anyLong());

        FerrymanService ferrymanService = new FerrymanService("localhost", 80);
        ferrymanService.setHttpClient(httpClient);
        ferrymanService.setDelayer(mockDelayer);

        ferrymanServiceSpy = spy(ferrymanService);
    }

    @Test
    public void testFailAddTableButFindInNamespaces() throws Exception {
        long timestamp = System.currentTimeMillis();
        HttpResponse response500 = getHttpResponseMock("", 500);
        HttpResponse response400 = getHttpResponseMock("", 400);
        HttpResponse response200 = getHttpResponseMock(getNamespaceResponse(timestamp), 200);

        when(httpClient.execute(argThat(new RequestContainsPath("add-full-tables"))))
            .thenReturn(response500)
            .thenReturn(response400);
        when(httpClient.execute(argThat(new RequestContainsPath("get-namespaces"))))
            .thenReturn(response200);

        YtTableRef tableRef = new YtTableRef(getDirPathYtDumper(timestamp), "0", timestamp, false);

        String batch = ferrymanServiceSpy.addTable(tableRef);
        verify(mockDelayer, times(1)).accept(anyLong());
        Assert.assertNull(batch);
    }

    @Test
    public void testAddToTable() throws Exception {
        long timestamp = System.currentTimeMillis();
        long batch = System.currentTimeMillis();
        HttpResponse response202 = getHttpResponseMock(getBatchResponse(batch), 202);

        when(httpClient.execute(argThat(new RequestContainsPath("add-full-tables"))))
            .thenReturn(response202);

        YtTableRef tableRef = new YtTableRef(getDirPathYtDumper(timestamp), "0", timestamp, false);

        String ferrymanBatch = ferrymanServiceSpy.addTable(tableRef);
        verify(httpClient, never()).execute(argThat(new RequestContainsPath("get-namespaces")));
        verify(mockDelayer, never()).accept(anyLong());
        assertEquals(batch, Long.parseLong(ferrymanBatch));

        String expected = String.format(
            "tables=[{\"Path\":\"//home/market/testing/pers-grade/tables/saas_documents/%s\",\"Namespace\":\"0\",\"Timestamp\":%s,\"Delta\":false}]",
            timestamp,
            timestamp);
        assertAddTableCalled(expected);
    }

    @Test
    public void testAddToTableWithFormat() throws Exception {
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli() * 1000;
        long batch = System.currentTimeMillis();
        HttpResponse response202 = getHttpResponseMock(getBatchResponse(batch), 202);

        when(httpClient.execute(argThat(new RequestContainsPath("add-full-tables"))))
            .thenReturn(response202);

        YtTableRef tableRef = new YtTableRef(getDirPathYtDumper(timestamp), "0", now, false)
            .format(CommonFerrymanFormat.SIMPLE);

        String ferrymanBatch = ferrymanServiceSpy.addTable(tableRef);
        verify(httpClient, never()).execute(argThat(new RequestContainsPath("get-namespaces")));
        verify(mockDelayer, never()).accept(anyLong());
        assertEquals(batch, Long.parseLong(ferrymanBatch));

        String expected = String.format(
            "tables=[{\"Path\":\"//home/market/testing/pers-grade/tables/saas_documents/%s\",\"Namespace\":\"0\",\"Timestamp\":%s,\"Delta\":false,\"Format\":\"simple\"}]",
            timestamp,
            timestamp);
        assertAddTableCalled(expected);
    }

    private void assertAddTableCalled(String expected) throws Exception {
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient, times(1)).execute(requestCaptor.capture());
        assertEquals(1, requestCaptor.getAllValues().size());

        HttpUriRequest request = requestCaptor.getValue();

        assertEquals(expected, URLDecoder.decode(request.getURI().getQuery(), "UTF-8"));
    }

    @Test
    public void testFailAddToTableAndNotFindInNamespaces() throws Exception {
        long timestamp = System.currentTimeMillis();
        HttpResponse response500 = getHttpResponseMock("", 500);
        HttpResponse response400 = getHttpResponseMock("", 400);
        HttpResponse response200 = getHttpResponseMock(getNamespaceResponse(timestamp + 1000), 200);

        when(httpClient.execute(argThat(new RequestContainsPath("add-full-tables"))))
            .thenReturn(response500)
            .thenReturn(response400)
            .thenReturn(response400);
        when(httpClient.execute(argThat(new RequestContainsPath("get-namespaces"))))
            .thenReturn(response200)
            .thenReturn(response200)
            .thenReturn(response200);
        try {
            YtTableRef tableRef = new YtTableRef(getDirPathYtDumper(timestamp), "0", timestamp, false);

            ferrymanServiceSpy.addTable(tableRef);
            Assert.fail("Expected exception to be thrown");
        } catch (Exception e) {
            isInstanceOf(Exception.class, e);
            assertThat(e, hasMessage(equalTo("Did not get any information about posted table after 3 retries")));
        }

        verify(mockDelayer, times(2)).accept(anyLong());
    }

    private String getDirPathYtDumper(long timestamp) {
        return "//home/market/testing/pers-grade" +
            "/tables/saas_documents/" +
            timestamp;
    }

    private class RequestContainsPath extends BaseMatcher<HttpUriRequest> {
        String method;

        RequestContainsPath(String method) {
            this.method = method;
        }

        public boolean matches(Object o) {
            if (o instanceof HttpUriRequest) {
                HttpUriRequest req = (HttpUriRequest) o;
                return req.getURI().getPath().contains(method);
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {

        }
    }

    private static String getNamespaceResponse(Long timestamp) {
        return String.format(
            "{\n" +
                "  \"final\": [\n" +
                "    {\n" +
                "      \"namespace\": \"0\",\n" +
                "      \"timestamp\": \"%s\",\n" +
                "      \"rowCount\": 2,\n" +
                "      \"receiveTimestamp\": \"1527854442981961\",\n" +
                "      \"finalTimestamp\": \"1527854611898679\",\n" +
                "      \"delta\": true\n" +
                "    },\n" +
                "    {\n" +
                "      \"namespace\": \"0\",\n" +
                "      \"timestamp\": \"1527854701063000\",\n" +
                "      \"rowCount\": 2,\n" +
                "      \"receiveTimestamp\": \"1527854750022863\",\n" +
                "      \"finalTimestamp\": \"1527854952581776\",\n" +
                "      \"delta\": true\n" +
                "    },\n" +
                "    {\n" +
                "      \"namespace\": \"0\",\n" +
                "      \"timestamp\": \"1527855300944000\",\n" +
                "      \"rowCount\": 3,\n" +
                "      \"receiveTimestamp\": \"1527855352037456\",\n" +
                "      \"finalTimestamp\": \"1527855540481166\",\n" +
                "      \"delta\": true\n" +
                "    }\n" +
                "  ],\n" +
                "  \"processing\": [],\n" +
                "  \"queue\": []\n" +
                "}", timestamp);
    }

    private static String getBatchResponse(Long batch) {
        return String.format(
            "{\n" +
                "    \"batch\": \"%s\"\n" +
                "}", batch);
    }

}
