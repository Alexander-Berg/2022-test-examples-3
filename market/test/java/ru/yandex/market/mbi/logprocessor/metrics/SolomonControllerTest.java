package ru.yandex.market.mbi.logprocessor.metrics;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.common.cache.memcached.cacheable.MemCacheable;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.checkout.pushapi.proto.PushApiRequest;
import ru.yandex.market.core.logbroker.ApiLogOuterClass;
import ru.yandex.market.mbi.logprocessor.FunctionalTest;
import ru.yandex.market.mbi.logprocessor.TestUtil;
import ru.yandex.market.mbi.logprocessor.logbroker.PushApiLogLogbrokerDataProcessor;
import ru.yandex.market.mbi.logprocessor.logbroker.apilog.ApiLogFastAccessLogbrokerDataProcessor;
import ru.yandex.market.mbi.logprocessor.logbroker.apilog.ApiLogLogbrokerDataProcessor;
import ru.yandex.market.mbi.logprocessor.storage.yt.dao.ApiLogRepository;
import ru.yandex.market.mbi.logprocessor.storage.yt.dao.PushApiLogRepository;
import ru.yandex.market.mbi.util.MbiAsserts;

class SolomonControllerTest extends FunctionalTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private MemCachingService memCachingService;

    @Autowired
    private MemCacheable<Boolean, Long> partnerErrorsWrite;

    @Test
    void testFailedInsertsSensors() {
        checkYtApiLogFailedInsertsSensor();
        checkYtPushApiFailedInsertsSensor();
        checkYtApiLogFastAccessFailedInsertsSensor();

        ResponseEntity<String> response = testRestTemplate.getForEntity("/solomon", String.class);

        // then
        String expectedResult = TestUtil.readString("asserts/sensors/ytFailedInsertsSensorsResponse.json");
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        MbiAsserts.assertJsonEquals(expectedResult, response.getBody());
    }

    void checkYtPushApiFailedInsertsSensor() {
        // given
        PushApiLogRepository pushApiLogRepository = Mockito.mock(PushApiLogRepository.class);
        PushApiLogLogbrokerDataProcessor pushApiLogLogbrokerDataProcessor =
                new PushApiLogLogbrokerDataProcessor(
                        pushApiLogRepository,
                        "mbi-log-processor",
                        true,
                        30,
                        memCachingService,
                        partnerErrorsWrite);
        PushApiRequest pushApiRequest = PushApiRequest.newBuilder()
                .setShopId(1L)
                .setUserId(1L)
                .setSuccess(true)
                .setSandbox(false)
                .setRequest("request")
                .setUrl("http://path/to")
                .setArgs("")
                .setResponseTime(42L)
                .setEventTime(System.currentTimeMillis())
                .setHost("0.0.0.0")
                .setRequestHeaders("Accept: application/xml")
                .setRequestBody("")
                .setResponseHeaders("Content-Type: application/xml")
                .setResponseBody("")
                .setResponseError("")
                .setResponseSuberror("")
                .setErrorDescription("")
                .setRequestMethod("POST")
                .setPartnerInterface(false)
                .setContext("")
                .setUserGroup("")
                .setActionId("1")
                .setOrderId(2L)
                .setRequestId("3")
                .build();

        Mockito.doThrow(new RuntimeException("test")).when(pushApiLogRepository).saveAll(Mockito.anyList());

        // when

        Assertions.assertThrows(RuntimeException.class, () ->
                pushApiLogLogbrokerDataProcessor.process(new MessageBatch("topic", 1,
                        List.of(new MessageData(pushApiRequest.toByteArray(), 0,
                                new MessageMeta("test".getBytes(), 0, 0, 0, "::1",
                                        CompressionCodec.RAW, Collections.emptyMap()))))));
    }

    void checkYtApiLogFailedInsertsSensor() {
        // given
        ApiLogRepository apiLogRepository = Mockito.mock(ApiLogRepository.class);
        ApiLogLogbrokerDataProcessor apiLogLogbrokerDataProcessor =
                new ApiLogLogbrokerDataProcessor(apiLogRepository,
                        "mbi-log-processor");

        ApiLogOuterClass.ApiLog logFull = ApiLogOuterClass.ApiLog.newBuilder()
                .setIsFastAccess(true)
                .setTraceId("2/1")
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


        Mockito.doThrow(new RuntimeException("test")).when(apiLogRepository).saveAll(Mockito.anyList());

        // when

        Assertions.assertThrows(RuntimeException.class, () ->
                apiLogLogbrokerDataProcessor.process(new MessageBatch("topic", 1,
                        List.of(new MessageData(logBatch.toByteArray(), 0,
                                new MessageMeta("test".getBytes(), 0, 0, 0, "::1",
                                        CompressionCodec.RAW, Collections.emptyMap()))))));
    }

    void checkYtApiLogFastAccessFailedInsertsSensor() {
        // given
        ApiLogRepository apiLogRepository = Mockito.mock(ApiLogRepository.class);
        ApiLogFastAccessLogbrokerDataProcessor apiLogFastAccessLogbrokerDataProcessor =
                new ApiLogFastAccessLogbrokerDataProcessor(apiLogRepository,
                        "mbi-log-processor");

        var logBatch = ApiLogOuterClass.ApiLogBatch.newBuilder()
                .addApiLogs(ApiLogOuterClass.ApiLog.newBuilder()
                        .setIsFastAccess(true)
                        .setTraceId("2/2")
                        .build())
                .addApiLogs(ApiLogOuterClass.ApiLog.newBuilder().build())
                .build();

        Mockito.doThrow(new RuntimeException("test")).when(apiLogRepository).saveAll(Mockito.anyList());

        // when
        Assertions.assertThrows(RuntimeException.class, () ->
                apiLogFastAccessLogbrokerDataProcessor.process(new MessageBatch("topic", 1,
                        List.of(new MessageData(logBatch.toByteArray(), 0,
                                new MessageMeta("test".getBytes(), 0, 0, 0, "::1",
                                        CompressionCodec.RAW, Collections.emptyMap()))))));
    }

}
