package ru.yandex.market.mbi.logprocessor.logbroker;

import java.util.Collections;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.common.cache.memcached.cacheable.MemCacheable;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.checkout.pushapi.proto.PushApiRequest;
import ru.yandex.market.mbi.logprocessor.storage.yt.dao.PushApiLogRepository;
import ru.yandex.market.mbi.logprocessor.storage.yt.model.PushApiLogEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PushApiLogLogbrokerDataProcessorTest {

    @Captor
    ArgumentCaptor<List<PushApiLogEntity>> captor;

    @Mock
    PushApiLogRepository pushApiLogRepository;

    @Autowired
    private MemCachingService memCachingService;

    @Autowired
    private MemCacheable<Boolean, Long> partnerErrorsWrite;

    @Test
    void processTest() {
        var processor = new PushApiLogLogbrokerDataProcessor(pushApiLogRepository, "",
                true, 30, memCachingService, partnerErrorsWrite);

        var logForPAPI = createLog(false);
        var logForPI = createLog(true);

        // Вызываем парсер сообщения
        processor.process(new MessageBatch("topic", 1, List.of(
                createMessageData(logForPAPI),
                createMessageData(logForPI))));

        verify(pushApiLogRepository).saveAll(captor.capture());

        // Проверяем, что распарсилось именно то, что было передано.
        var val = captor.getValue();
        assertEquals(1, val.size());
        PushApiLogEntity entity = val.get(0);
        assertEquals(logForPAPI.getShopId(), entity.getShop_id());
        assertEquals(logForPAPI.getUserId(), entity.getUser_id());
        assertEquals(logForPAPI.getSuccess(), entity.isSuccess());
        assertEquals(logForPAPI.getSandbox(), entity.isSandbox());
        assertEquals(logForPAPI.getRequest(), entity.getRequest());
        assertEquals(logForPAPI.getUrl(), entity.getUrl());
        assertEquals(logForPAPI.getArgs(), entity.getArgs());
        assertEquals(logForPAPI.getResponseTime(), entity.getResponse_time());
        assertEquals(-logForPAPI.getEventTime(), entity.getEvent_time());
        assertEquals(logForPAPI.getHost(), entity.getHost());
        assertEquals(logForPAPI.getRequestHeaders(), entity.getRequest_headers());
        assertEquals(logForPAPI.getResponseTime(), entity.getResponse_time());
        assertEquals(logForPAPI.getRequestBody(), entity.getRequest_body());
        assertEquals(logForPAPI.getResponseHeaders(), entity.getResponse_headers());
        assertEquals(logForPAPI.getResponseBody(), entity.getResponse_body());
        assertEquals(logForPAPI.getResponseError(), entity.getResponse_error());
        assertEquals(logForPAPI.getResponseSuberror(), entity.getResponse_sub_error());
        assertEquals(logForPAPI.getErrorDescription(), entity.getError_description());
        assertEquals(logForPAPI.getRequestMethod(), entity.getRequest_method());
        assertEquals(logForPAPI.getPartnerInterface(), entity.isPartner_interface());
        assertEquals(logForPAPI.getContext(), entity.getContext());
        assertEquals(logForPAPI.getUserGroup(), entity.getUser_group());
        assertEquals(logForPAPI.getActionId(), entity.getAction_id());
        assertEquals(logForPAPI.getOrderId(), entity.getOrder_id());
        assertEquals(logForPAPI.getRequestId(), entity.getRequest_id());

        // Проверяем создание event_id
        assertNotNull(entity.getEvent_id());
    }

    private PushApiRequest createLog(boolean isPartnerInterface) {
        return PushApiRequest.newBuilder()
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
                .setPartnerInterface(isPartnerInterface)
                .setContext("")
                .setUserGroup("")
                .setActionId("1")
                .setOrderId(2L)
                .setRequestId("3")
                .build();
    }

    private MessageData createMessageData(PushApiRequest log) {
        return new MessageData(log.toByteArray(), 0, new MessageMeta("test".getBytes(), 0, 0, 0, "::1",
                CompressionCodec.RAW, Collections.emptyMap()));
    }
}
