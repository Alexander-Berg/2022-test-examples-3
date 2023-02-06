package ru.yandex.market.common.util;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestLogRecordBuilder;

import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class AsyncRetryHttpClientTraceTest {

    private AsyncRetryHttpClient asyncRetryHttpClient = new AsyncRetryHttpClient();
    private String requestId = "111";
    private long durationMs = 10000;
    private HttpGet httpRequest;

    @Before
    public void setUp() {
        asyncRetryHttpClient.setTargetModule(Module.REPORT);
        httpRequest = new HttpGet("http://localhost:123/do/something?param=123");
    }

    /**
     * Проверяем вызов логирования успешного ответа с кодом 200.
     * OK ответ
     */
    @Test
    public void checkLogByRespOKContext() {
        String expectedLog = "tskv\tdate=[0-9+:TZ.-]+\ttype=OUT\trequest_id=111\ttarget_module=market_report\ttarget_host=localhost:123\tprotocol=http\thttp_method=GET\trequest_method=/do/something\tquery_params=/do/something\\?param=123\tretry_num=1\ttime_millis=[0-9]+\thttp_code=200";
        HttpResponse httpResponse = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 200, "OK");

        RequestLogRecordBuilder recordBuilder = asyncRetryHttpClient.createLogRecord(httpResponse, httpRequest, requestId, 1, durationMs, null);
        String result = recordBuilder.build();
        assertTrue(getNotMatchedMessage(result, expectedLog), Pattern.matches(expectedLog, result));
    }

    /**
     * Проверяем вызов логирования по ответу с кодом 400.
     */
    @Test
    public void checkLogByRespFailContext() {
        String expectedLog = "tskv\tdate=[0-9+:TZ.-]+\ttype=OUT\trequest_id=111\ttarget_module=market_report\ttarget_host=localhost:123\tprotocol=http\thttp_method=GET\trequest_method=/do/something\tquery_params=/do/something\\?param=123\tretry_num=1\ttime_millis=[0-9]+\thttp_code=400";
        HttpResponse httpResponse = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 400, "BAD_REQUEST");

        RequestLogRecordBuilder recordBuilder = asyncRetryHttpClient.createLogRecord(httpResponse, httpRequest, requestId, 1, durationMs, null);
        String result = recordBuilder.build();
        assertTrue(getNotMatchedMessage(result, expectedLog), Pattern.matches(expectedLog, result));
    }

    /**
     * Проверяем вызов логирования по запросу, контексту, ретрай контексту
     */
    @Test
    public void checkLogByRequestContextRetryFail() {
        String expectedLog = "tskv\tdate=[0-9+:TZ.-]+\ttype=OUT\trequest_id=111\ttarget_module=market_report\ttarget_host=localhost:123\tprotocol=http\thttp_method=GET\trequest_method=/do/something\tquery_params=/do/something\\?param=123\tretry_num=3\ttime_millis=[0-9]+\terror_code=java.lang.IllegalArgumentException: happens";

        RequestLogRecordBuilder recordBuilder = asyncRetryHttpClient.createLogRecord(null, httpRequest, requestId, 3, durationMs, new IllegalArgumentException("happens"));
        String result = recordBuilder.build();
        assertTrue(getNotMatchedMessage(result, expectedLog), Pattern.matches(expectedLog, result));
    }

    private static String getNotMatchedMessage(String actualString, String pattern) {
        return "Actual string\n" + actualString + "\n" + "doesn't match pattern\n" + pattern ;
    }
}
