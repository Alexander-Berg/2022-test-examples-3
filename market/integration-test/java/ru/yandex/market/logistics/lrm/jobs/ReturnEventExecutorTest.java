package ru.yandex.market.logistics.lrm.jobs;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.event_model.ReturnEvent;
import ru.yandex.market.logistics.lrm.event_model.ReturnEventType;
import ru.yandex.market.logistics.lrm.event_model.payload.OrderItemInfo;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnBox;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnCommittedPayload;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnItem;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnReasonType;
import ru.yandex.market.logistics.lrm.event_model.payload.ReturnSubreason;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Экспорт событий по возвратам")
class ReturnEventExecutorTest extends AbstractIntegrationTest {

    @Autowired
    private LogbrokerClientFactory returnEventsClientFactory;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DataFieldMaxValueIncrementer returnEventsLogbrokerIdSequence;

    @Autowired
    private ReturnEventExportExecutor executor;

    private final AsyncProducer asyncProducer = mock(AsyncProducer.class);
    private final ArgumentCaptor<byte[]> eventCaptor = ArgumentCaptor.forClass(byte[].class);
    private final AtomicLong logbrokerId = new AtomicLong();
    private final AtomicLong logbrokerSequenceId = new AtomicLong();

    @BeforeEach
    void setup() throws Exception {
        when(asyncProducer.write(eventCaptor.capture(), anyLong()))
            .then(i -> CompletableFuture.completedFuture(new ProducerWriteResponse(1L, 135135L, false)));
        when(asyncProducer.init()).then(i -> CompletableFuture.completedFuture(
            new ProducerInitResponse(logbrokerId.get(), "topic", 0, "sessionId")
        ));
        when(returnEventsClientFactory.asyncProducer(any())).thenReturn(asyncProducer);
        when(returnEventsLogbrokerIdSequence.nextLongValue()).then(i -> logbrokerSequenceId.incrementAndGet());
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/database/jobs/return-event/export/before/committed.xml")
    @ExpectedDatabase(
        value = "/database/jobs/return-event/export/after/committed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() throws Exception {
        executor.execute(null);

        assertEvent(
            eventCaptor.getValue(),
            ReturnEvent.builder()
                .id(1L)
                .requestId("test-request-id/1")
                .returnId(1L)
                .orderExternalId("order-external-id")
                .eventType(ReturnEventType.RETURN_COMMITTED)
                .payload(
                    new ReturnCommittedPayload()
                        .setExternalId("298347")
                        .setBoxes(List.of(ReturnBox.builder().externalId("box-external-id").build()))
                        .setItems(List.of(
                            ReturnItem.builder()
                                .vendorCode("KJH876")
                                .instances(Map.of("CIS", "98712343"))
                                .returnReason("reason")
                                .returnSubreason(ReturnSubreason.BAD_PACKAGE)
                                .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                .build()
                        ))
                        .setOrderItemsInfo(List.of(
                            OrderItemInfo.builder()
                                .supplierId(765L)
                                .vendorCode("item-vendor-code")
                                .instances(List.of(Map.of(
                                    "CIS", "item-cis",
                                    "UIT", "item-uit"
                                )))
                                .build()
                        ))
                )
                .build()
        );
    }

    @Test
    @DisplayName("Несколько событий")
    @DatabaseSetup("/database/jobs/return-event/export/before/multiple.xml")
    @ExpectedDatabase(
        value = "/database/jobs/return-event/export/after/multiple.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void multiple() throws Exception {
        logbrokerId.set(2L);
        logbrokerSequenceId.set(3L);

        executor.execute(null);

        List<byte[]> allActual = eventCaptor.getAllValues();
        softly.assertThat(allActual).hasSize(2);
        assertEvent(
            allActual.get(0),
            ReturnEvent.builder()
                .id(3L)
                .requestId("test-request-id/3")
                .returnId(1L)
                .orderExternalId("order-external-id-1")
                .eventType(ReturnEventType.RETURN_COMMITTED)
                .build()
        );
        assertEvent(
            allActual.get(1),
            ReturnEvent.builder()
                .id(4L)
                .requestId("test-request-id/4")
                .returnId(2L)
                .orderExternalId("order-external-id-2")
                .eventType(ReturnEventType.RETURN_COMMITTED)
                .build()
        );
    }

    private void assertEvent(byte[] actual, ReturnEvent expected) throws IOException {
        ReturnEvent actualEvent = objectMapper.readValue(actual, ReturnEvent.class);
        softly.assertThat(actualEvent.getCreated()).isNotNull();
        softly.assertThat(actualEvent)
            .usingRecursiveComparison()
            .ignoringFields("created")
            .isEqualTo(expected);
    }

}
