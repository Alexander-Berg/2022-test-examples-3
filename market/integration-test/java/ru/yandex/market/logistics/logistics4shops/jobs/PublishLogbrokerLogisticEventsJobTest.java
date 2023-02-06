package ru.yandex.market.logistics.logistics4shops.jobs;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.protobuf.Timestamp;
import com.google.type.Date;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.config.properties.LogbrokerProducerProperties;
import ru.yandex.market.logistics.logistics4shops.event.model.DropshipOrderCreatedPayload;
import ru.yandex.market.logistics.logistics4shops.event.model.ExpressOrderCreatedPayload;
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent;
import ru.yandex.market.logistics.logistics4shops.event.model.OrderBindToShipmentPayload;
import ru.yandex.market.logistics.logistics4shops.event.model.RegularReturnStatusData;
import ru.yandex.market.logistics.logistics4shops.event.model.RegularReturnStatusData.Box;
import ru.yandex.market.logistics.logistics4shops.event.model.RegularReturnStatusData.Box.RecipientType;
import ru.yandex.market.logistics.logistics4shops.event.model.RegularReturnStatusData.Item;
import ru.yandex.market.logistics.logistics4shops.event.model.ReturnStatusChangedPayload;
import ru.yandex.market.logistics.logistics4shops.event.model.ReturnStatusChangedPayload.ReturnSource;
import ru.yandex.market.logistics.logistics4shops.event.model.ReturnStatusChangedPayload.ReturnStatus;
import ru.yandex.market.logistics.logistics4shops.logging.code.LogisticEventCode;
import ru.yandex.market.logistics.logistics4shops.repository.LogisticEventRepository;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecord.Level;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecordFormat;
import ru.yandex.market.logistics.logistics4shops.utils.logging.TskvLogRecordFormat.ExceptionPayload;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logEqualsTo;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logHasFormat;
import static ru.yandex.market.logistics.logistics4shops.utils.logging.BackLogAssertions.logHasLevel;

@DisplayName("Тест эскпорта логистических событий в логброкер")
@ParametersAreNonnullByDefault
class PublishLogbrokerLogisticEventsJobTest extends AbstractIntegrationTest {
    private static final Predicate<TskvLogRecord<?>> EXPORT_FAILED_LOG_MATCHER = logEqualsTo(
        TskvLogRecord.exception(ExceptionPayload.of(
            "Failed export of events to logbroker",
            "RuntimeException: I'm exception"
        )).setLoggingCode(LogisticEventCode.EVENT_EXPORT_FAILED)
    );

    @Autowired
    private LogbrokerClientFactory logbrokerClientFactory;
    @Autowired
    private LogbrokerProducerProperties logbrokerProducerProperties;
    @Autowired
    private PublishLogbrokerLogisticEventsJob job;
    @Autowired
    private LogisticEventRepository repository;

    @Mock
    private AsyncProducer asyncProducer;

    @Captor
    private ArgumentCaptor<byte[]> eventCaptor;

    private final List<LogisticEvent> expectedLogisticEvents = expectedEvents();

    @BeforeEach
    void setUp() throws Exception {
        logbrokerProducerProperties.setBatchSize(2);
        when(logbrokerClientFactory.asyncProducer(any()))
            .thenReturn(asyncProducer);
        when(asyncProducer.write(eventCaptor.capture(), anyLong(), anyLong()))
            .thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1L, 135135L, false)));
        when(asyncProducer.init())
            .thenReturn(
                CompletableFuture.completedFuture(new ProducerInitResponse(-1L, "topic", 0, "sessionId"))
            );
    }

    @AfterEach
    void tearDown() {
        logbrokerProducerProperties.setBatchSize(200);
        verifyNoMoreInteractions(asyncProducer);
    }

    @Test
    @DisplayName("Успешный экспорт событий")
    @DatabaseSetup("/jobs/publish_logbroker_logistic_events/before/multiple_events.xml")
    void exportSuccess() throws JobExecutionException {
        saveSerializedEventsToBase(repository.findAllById(List.of(1L, 2L, 3L, 4L)));
        job.execute(null);
        verify(asyncProducer).init();
        verify(asyncProducer).close();
        verify(asyncProducer).write(any(), eq(0L), eq(Instant.parse("2022-02-02T11:00:00Z").toEpochMilli()));
        verify(asyncProducer).write(any(), eq(1L), eq(Instant.parse("2022-02-02T11:00:00Z").toEpochMilli()));
        verify(asyncProducer).write(any(), eq(2L), eq(Instant.parse("2022-02-02T11:00:00Z").toEpochMilli()));
        verify(asyncProducer).write(any(), eq(3L), eq(Instant.parse("2022-02-02T11:00:00Z").toEpochMilli()));
        verifySentEvents();
        assertLogs().noneMatch(logHasLevel(Level.ERROR)).noneMatch(logHasFormat(TskvLogRecordFormat.JSON_EXCEPTION));
    }

    @Test
    @DisplayName("Нет событий для экспорта")
    @DatabaseSetup("/jobs/publish_logbroker_logistic_events/before/single_event.xml")
    void exportNoEvents() throws JobExecutionException {
        job.execute(null);
        verify(asyncProducer).init();
        verify(asyncProducer).close();
        assertLogs().noneMatch(logHasLevel(Level.ERROR)).noneMatch(logHasFormat(TskvLogRecordFormat.JSON_EXCEPTION));
    }

    @Test
    @DisplayName("Исключение во время экспорта")
    void exportException() {
        when(asyncProducer.init())
            .thenThrow(new RuntimeException("I'm exception"));
        softly.assertThatThrownBy(() -> job.execute(null))
            .isInstanceOf(JobExecutionException.class)
            .hasMessage("java.lang.RuntimeException: I'm exception");
        verify(asyncProducer).init();
        verify(asyncProducer).close();

        assertLogs().anyMatch(EXPORT_FAILED_LOG_MATCHER);
    }

    @Test
    @DisplayName("Исключение во время экспорта, после проставления logbroker_id")
    @DatabaseSetup("/jobs/publish_logbroker_logistic_events/before/multiple_events.xml")
    @ExpectedDatabase(
        value = "/jobs/publish_logbroker_logistic_events/after/multiple_events.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void exportExceptionAfterLogbrokerIdWrite() {
        saveSerializedEventsToBase(repository.findAllById(List.of(1L, 2L, 3L, 4L)));
        when(asyncProducer.write(eventCaptor.capture(), anyLong(), anyLong()))
            .thenThrow(new RuntimeException("I'm exception"));
        softly.assertThatThrownBy(() -> job.execute(null))
            .isInstanceOf(JobExecutionException.class)
            .hasMessage("java.lang.RuntimeException: I'm exception");
        verify(asyncProducer).init();
        verify(asyncProducer).write(any(), eq(0L), eq(Instant.parse("2022-02-02T11:00:00Z").toEpochMilli()));
        verify(asyncProducer).close();

        assertLogs().anyMatch(EXPORT_FAILED_LOG_MATCHER);
    }

    @SneakyThrows
    private void verifySentEvents() {
        List<LogisticEvent> actual = eventCaptor.getAllValues().stream()
            .map(this::deserializeEvent)
            .toList();
        softly.assertThat(actual).containsExactlyElementsOf(expectedLogisticEvents);
    }

    @Nonnull
    @SneakyThrows
    private LogisticEvent deserializeEvent(byte[] eventBytes) {
        return LogisticEvent.parseFrom(eventBytes);
    }

    @Nonnull
    private List<LogisticEvent> expectedEvents() {
        return List.of(
            eventBase(1L)
                .setExpressOrderCreatedPayload(
                    ExpressOrderCreatedPayload.newBuilder()
                        .setOrderId(123L)
                        .setShopId(456L)
                        .setPackagingDeadline(
                            Timestamp.newBuilder().setSeconds(Instant.parse("2022-02-02T11:00:01Z").getEpochSecond())
                        )
                        .build()
                )
                .build(),
            eventBase(2L)
                .setReturnStatusChangedPayload(
                    ReturnStatusChangedPayload.newBuilder()
                        .setReturnEventId(200600)
                        .setReturnEventTimestamp(
                            Timestamp.newBuilder().setSeconds(Instant.parse("2022-02-02T11:00:01Z").getEpochSecond())
                        )
                        .setReturnId(300700)
                        .setOrderId(400800)
                        .setReturnSource(ReturnSource.CLIENT)
                        .setClientReturnId(500900)
                        .setReturnStatus(ReturnStatus.IN_TRANSIT)
                        .setRegularReturnStatusData(
                            RegularReturnStatusData.newBuilder()
                                .addBoxes(
                                    Box.newBuilder()
                                        .setExternalId("lkjpoi-098")
                                        .setRecipientType(RecipientType.SHOP)
                                        .setDestinationLogisticPointId(1000L)
                                        .build()
                                )
                                .addItems(
                                    Item.newBuilder()
                                        .setSupplierId(2000)
                                        .setVendorCode("jhgytr654")
                                        .putInstances("CIS", "098987876")
                                        .build()
                                )
                        )
                )
                .build(),
            eventBase(3L)
                .setDropshipOrderCreatedPayload(
                    DropshipOrderCreatedPayload.newBuilder()
                        .setOrderId(124L)
                        .setShopId(457L)
                        .setShipmentDate(Date.newBuilder().setYear(2022).setMonth(2).setDay(2).build())
                )
                .build(),
            eventBase(4L)
                .setOrderBindToShipmentPayload(
                    OrderBindToShipmentPayload.newBuilder()
                        .setOrderId(124L)
                        .setShopId(457L)
                        .setShipmentId(222L)
                )
                .build()
        );
    }

    private void saveSerializedEventsToBase(
        List<ru.yandex.market.logistics.logistics4shops.model.entity.LogisticEvent> innerEvents
    ) {
        StreamEx.of(innerEvents)
            .zipWith(expectedEvents().stream())
            .forKeyValue((inner, proto) -> {
                inner.setEventBytes(proto.toByteArray());
                repository.save(inner);
            });
    }

    @Nonnull
    private static LogisticEvent.Builder eventBase(Long id) {
        return LogisticEvent.newBuilder()
            .setId(id)
            .setCreated(Timestamp.newBuilder().setSeconds(Instant.parse("2022-02-02T11:00:00Z").getEpochSecond()))
            .setRequestId("1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd");
    }

}
