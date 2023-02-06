package ru.yandex.market.mbi.logprocessor.logbroker;

import java.util.Collections;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.core.logbroker.ApiLogOuterClass;
import ru.yandex.market.mbi.logprocessor.logbroker.apilog.ApiLogFastAccessLogbrokerDataProcessor;
import ru.yandex.market.mbi.logprocessor.logbroker.apilog.ApiLogLogbrokerDataProcessor;
import ru.yandex.market.mbi.logprocessor.storage.yt.dao.ApiLogRepository;
import ru.yandex.market.mbi.logprocessor.storage.yt.model.ApiLogEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiLogLogbrokerDataProcessorTest {

    @Captor
    ArgumentCaptor<List<ApiLogEntity>> captor;

    @Mock
    ApiLogRepository apiLogRepository;

    @Test
    void processTest() {
        var processor = new ApiLogLogbrokerDataProcessor(apiLogRepository, "");

        ApiLogOuterClass.ApiLog logFull = ApiLogOuterClass.ApiLog.newBuilder()
                .setIsFastAccess(true)
                .setTraceId("1/0")
                .setArgs("args")
                .setRequestDate(1L)
                .setPartnerId(2L)
                .setDebugKey("debug key")
                .setOauthClientId("client id")
                .setResourceId(3)
                .setUid(4L)
                .setHost("host")
                .setResponseTime(5L)
                .setResponseStatusCode(6)
                .setResponseBody("resp body")
                .setResponseHeaders("resp headers")
                .setRequestBody("req body")
                .setRequestHeaders("req headers")
                .setMethod("method")
                .setResource("resource")
                .setVersion("version")
                .setCampaignId(7L)
                .setCampaignType("campaign type")
                .setIp("0:0:0:0")
                .setMethod("method")
                .setUrl("url")
                .setUid(8L)
                .build();

        var logBatch = ApiLogOuterClass.ApiLogBatch.newBuilder()
                .addApiLogs(logFull)
                .addApiLogs(ApiLogOuterClass.ApiLog.newBuilder().build())
                .build();

        // Вызываем парсер сообщения
        processor.process(new MessageBatch("topic", 1,
                Collections.singletonList(createMessageData(logBatch))));


        verify(apiLogRepository).saveAll(captor.capture());

        var val = captor.getValue();
        assertEquals(2, val.size());

        ApiLogEntity message = val.get(0);

        assertEquals(logFull.getArgs(), message.getArgs());
        assertEquals(logFull.getCampaignId(), message.getCampaignId());
        assertEquals(logFull.getCampaignType(), message.getCampaignType());
        assertEquals(logFull.getDebugKey(), message.getDebugKey());
        assertEquals(logFull.getRequestDate(), message.getEventTime());
        assertEquals(logFull.getHost(), message.getHost());
        assertEquals(logFull.getIp(), message.getIp());
        assertEquals(logFull.getOauthClientId(), message.getOauthClientId());
        assertEquals(logFull.getPartnerId(), message.getPartnerId());
        assertEquals(logFull.getResource(), message.getPath());
        assertEquals(logFull.getRequestBody(), message.getRequestBody());
        assertEquals(logFull.getRequestHeaders(), message.getRequestHeaders());
        assertEquals(logFull.getMethod(), message.getRequestMethod());
        assertEquals(logFull.getResourceId(), message.getResourceId());
        assertEquals(logFull.getResponseBody(), message.getResponseBody());
        assertEquals(logFull.getResponseStatusCode(), message.getResponseCode());
        assertEquals(logFull.getResponseHeaders(), message.getResponseHeaders());
        assertEquals(logFull.getResponseTime(), message.getResponseTime());
        assertEquals(logFull.getTraceId(), message.getTraceId());
        assertEquals(logFull.getUrl(), message.getUrl());
        assertEquals(logFull.getUid(), message.getUserId());
        assertEquals(logFull.getVersion(), message.getVersion());
    }

    @Test
    void testApiLogLogbrokerDataProcessor() {
        var ApiLogProcessor = new ApiLogLogbrokerDataProcessor(apiLogRepository, "");
        ApiLogProcessor.process(new MessageBatch("topic", 1,
                Collections.singletonList(createMessageData(getTestLogBatch()))));
        verify(apiLogRepository).saveAll(captor.capture());

        var val = captor.getValue();
        assertEquals(3, val.size());

        ApiLogEntity message1 = val.get(0);
        ApiLogEntity message2 = val.get(1);
        ApiLogEntity message3 = val.get(2);
        assertEquals("1/1", message1.getTraceId());
        assertEquals("1/2", message2.getTraceId());
        assertEquals("1/3", message3.getTraceId());
    }

    @Test
    void testApiLogFastAccessLogbrokerDataProcessor() {
        var ApiLogProcessor = new ApiLogFastAccessLogbrokerDataProcessor(apiLogRepository, "");
        ApiLogProcessor.process(new MessageBatch("topic", 1,
                Collections.singletonList(createMessageData(getTestLogBatch()))));
        verify(apiLogRepository).saveAll(captor.capture());

        // Не взяли лог с traceId = "1/2" (он не fastAccess)
        var val = captor.getValue();
        assertEquals(2, val.size());

        ApiLogEntity message1 = val.get(0);
        ApiLogEntity message2 = val.get(1);
        assertEquals("1/1", message1.getTraceId());
        assertEquals("1/3", message2.getTraceId());
    }

    private MessageData createMessageData(ApiLogOuterClass.ApiLogBatch log) {
        return new MessageData(log.toByteArray(), 0, new MessageMeta("test".getBytes(), 0, 0, 0, "::1",
                CompressionCodec.RAW, Collections.emptyMap()));
    }

    private ApiLogOuterClass.ApiLogBatch getTestLogBatch() {

        ApiLogOuterClass.ApiLog logFast = ApiLogOuterClass.ApiLog.newBuilder()
                .setIsFastAccess(true)
                .setTraceId("1/1")
                .build();
        ApiLogOuterClass.ApiLog logNotFast = ApiLogOuterClass.ApiLog.newBuilder()
                .setIsFastAccess(false)
                .setTraceId("1/2")
                .build();
        ApiLogOuterClass.ApiLog logFast2 = ApiLogOuterClass.ApiLog.newBuilder()
                .setIsFastAccess(true)
                .setTraceId("1/3")
                .build();

        return ApiLogOuterClass.ApiLogBatch.newBuilder()
                .addApiLogs(logFast)
                .addApiLogs(logNotFast)
                .addApiLogs(logFast2)
                .build();

    }

}
