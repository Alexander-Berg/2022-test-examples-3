package ru.yandex.market.request.trace;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import ru.yandex.market.request.HttpMethod;
import ru.yandex.market.request.Protocol;
import ru.yandex.market.request.RequestType;

import java.util.Arrays;
import java.time.Clock;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class RequestLogRecordBuilderTest {

    private static final long TEST_TIMESTAMP = 1470064678042L;
    private static final ZoneId TEST_ZONE = ZoneId.of("UTC+3");

    @Mock
    Clock testClock;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(testClock.getZone()).thenReturn(TEST_ZONE);
    }

    @Test
    public void shouldBuildRecord() throws Exception {
        RequestLogRecordBuilder builder = new RequestLogRecordBuilder(testClock);

        builder.setEndTimestampMillis(TEST_TIMESTAMP);
        builder.setRequestId("Test-Request-Id");
        builder.setSource(Module.MARKET_CONTENT_API_CLIENT);
        builder.setSourceHost("123.123.123.123");
        builder.setProtocol(Protocol.HTTPS);
        builder.setHttpMethod(HttpMethod.GET);
        builder.setResourceName("someResource");
        builder.setQuery("resource?q1=1&q2=abc");
        builder.setHttpCode(200);
        builder.setDurationMillis(100);
        builder.addKeyValue("custom-int", 123);
        builder.addKeyValue("custom-string", "abc");
        builder.addEvent("custom-event", TEST_TIMESTAMP);
        builder.setType(RequestType.OUT);
        builder.setSourceVirtualHost("checkouter.http.vs.market.yandex.net");
        builder.setSourceHttpMethod(HttpMethod.POST);
        builder.setSourceResourceName("/checkout");
        builder.setTestIds(Arrays.asList(1L,2L,3L));

        assertEquals(
            "tskv\t" +
                "date=2016-08-01T18:17:58.042+03:00\t" +
                "type=OUT\t" +
                "request_id=Test-Request-Id\t" +
                "source_module=market_content-api-client\t" +
                "source_host=123.123.123.123\t" +
                "protocol=https\t" +
                "http_method=GET\t" +
                "request_method=someResource\t" +
                "query_params=resource?q1=1&q2=abc\t" +
                "time_millis=100\t" +
                "http_code=200\t" +
                "source_vhost=checkouter.http.vs.market.yandex.net\t" +
                "source_http_method=POST\t" +
                "source_request_method=/checkout\t" +
                "test_ids=1,2,3\t" +
                "event.custom-event=1470064678042\t" +
                "kv.custom-int=123\t" +
                "kv.custom-string=abc",
            builder.build()
        );
    }
}
