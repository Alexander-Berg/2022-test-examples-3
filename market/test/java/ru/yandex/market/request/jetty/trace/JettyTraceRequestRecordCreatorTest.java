package ru.yandex.market.request.jetty.trace;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JettyTraceRequestRecordCreatorTest {

    private static final JettyTraceRequestRecordCreator recordCreator =
            new JettyTraceRequestRecordCreator(Module.TSUM_API);

    private enum RecordField {
        HTTP_METHOD("http_method"),
        PROTOCOL("protocol"),
        RESOURCE_NAME("request_method"),
        QUERY_PARAMS("query_params"),
        TEST_IDS("test_ids");

        private final String key;

        RecordField(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    @Test
    public void simple() {
        String record = createTraceRecord(
            "GET",
            URI.create("http://example.com/serverMethod?a=1&b=2"),
            "1,0,9;2,5,11;3,7,13"
        );
        Map<String, String> parsed = parse(record);
        assertFieldValue(parsed, "GET", RecordField.HTTP_METHOD);
        assertFieldValue(parsed, "http", RecordField.PROTOCOL);
        assertFieldValue(parsed, "/serverMethod", RecordField.RESOURCE_NAME);
        assertFieldValue(parsed, "/serverMethod?a=1&b=2", RecordField.QUERY_PARAMS);
        assertFieldValue(parsed, "1,2,3", RecordField.TEST_IDS);
    }

    @Test
    public void testHideSensitiveParams() {
        String record = createTraceRecord(
                "GET",
                URI.create("http://blackbox.yandex.net/blackbox?" +
                        "user_ticket=ut:userTicket&" +
                        "service_ticket=st:serviceTicket&" +
                        "anotnerParamKey=anotherParamValue&" +
                        "sessionid=my_own_session_id&" +
                        "sslsessionid=my_own_ssl_session_id&" +
                        ""
                ),
                "1,2,3"
        );
        Map<String, String> parsed = parse(record);
        assertFieldValue(
                parsed,
                "/blackbox?" +
                        "user_ticket=*****&" +
                        "service_ticket=*****&" +
                        "anotnerParamKey=anotherParamValue&" +
                        "sessionid=*****&" +
                        "sslsessionid=*****&",
                RecordField.QUERY_PARAMS
        );
    }

    @Test
    public void testHideSensitiveParamsWithCustomValues() {
        JettyTraceRequestRecordCreator custom = new JettyTraceRequestRecordCreator(
                Module.MBI_BILLING,
                ImmutableSet.of("a", "b")
        );
        String record = createTraceRecord(
                custom,
                "GET",
                URI.create("http://blackbox.yandex.net/blackbox?" +
                        "user_ticket=ut:userTicket&" +
                        "service_ticket=st:serviceTicket&" +
                        "a=a_value&" +
                        "b=b_value&" +
                        "sessionid=my_own_session_id&" +
                        "sslsessionid=my_own_ssl_session_id&" +
                        ""
                ),
                "1,2,3"
        );
        Map<String, String> parsed = parse(record);
        assertFieldValue(
                parsed,
                "/blackbox?" +
                        "user_ticket=ut:userTicket&" +
                        "service_ticket=st:serviceTicket&" +
                        "a=*****&" +
                        "b=*****&" +
                        "sessionid=my_own_session_id&" +
                        "sslsessionid=my_own_ssl_session_id&",
                RecordField.QUERY_PARAMS
        );
    }

    private void assertFieldValue(Map<String, String> parsedRecord,
                                  String expectedValue,
                                  RecordField field) {
        Assert.assertEquals(expectedValue, getField(parsedRecord, field));
    }

    private void assertMissingField(Map<String, String> parsedRrcord,
                                    RecordField field) {
        Assert.assertNull(getField(parsedRrcord, field));
    }

    private String getField(Map<String, String> parsed, RecordField httpMethod) {
        return parsed.get(httpMethod.getKey());
    }

    private Map<String, String> parse(String record) {
        return TskvRecordParser.parse(record)
                .stream()
                .collect(Collectors.toMap(
                        TskvRecordParser.Item::getKey,
                        TskvRecordParser.Item::getValue));
    }

    private String createTraceRecord(
            JettyTraceRequestRecordCreator recordCreator,
            String httpMethod,
            URI uri,
            String testIds
    ) {
        return recordCreator.createRecord(
                getRequest(httpMethod, uri, testIds),
                getResponse(),
                RequestContextHolder.getContext())
                .build();
    }

    private String createTraceRecord(String httpMethod,
                                     URI uri, String testIds) {
        return recordCreator.createRecord(
                getRequest(httpMethod, uri, testIds),
                getResponse(),
                RequestContextHolder.getContext())
                .build();
    }

    private Response getResponse() {
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        return response;
    }

    private Request getRequest(String httpMethod, URI uri, String testIds) {
        final Request request = mock(Request.class);
        when(request.getMethod()).thenReturn(httpMethod);
        when(request.getRequestURI()).thenReturn(uri.getPath());
        when(request.getQueryString()).thenReturn(uri.getQuery());
        when(request.getScheme()).thenReturn(uri.getScheme());
        when(request.getHeader("X-Yandex-ExpBoxes")).thenReturn(testIds);
        return request;
    }
}
