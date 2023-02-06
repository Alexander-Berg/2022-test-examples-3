package ru.yandex.market.logistics.lom.jobs.processor;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.LogbrokerProperties;
import ru.yandex.market.logistics.lom.entity.enums.LogbrokerSourceLockType;
import ru.yandex.market.logistics.lom.jobs.consumer.PublishLogbrokerHistoryEventsConsumer;
import ru.yandex.market.logistics.lom.jobs.model.LogbrokerSourceIdPayload;
import ru.yandex.market.logistics.lom.model.dto.AuthorDto;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.lom.service.order.history.LogbrokerSourceService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PublishLogbrokerHistoryEventsProcessorTest extends AbstractContextualTest {
    private static final Long ORDER_ID_HASH = -2582693687464317919L;
    private static final Long EVENT_ID_HASH = 2721502588582550493L;
    @Autowired
    private LogbrokerClientFactory lomLogbrokerClientFactory;
    @Autowired
    private PublishLogbrokerHistoryEventsConsumer publishLogbrokerHistoryEventsConsumer;
    @Autowired
    private PublishLogbrokerHistoryEventsProcessor publishLogbrokerHistoryEventsProcessor;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private LogbrokerSourceService logbrokerSourceService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private LogbrokerProperties lomLogbrokerProperties;

    @Mock
    private AsyncProducer asyncProducer;

    @Captor
    private ArgumentCaptor<byte[]> eventCaptor;

    private static final LogbrokerSourceIdPayload PAYLOAD = PayloadFactory.logbrokerSourceIdPayload(1, "1", 1L);
    private static final Task<LogbrokerSourceIdPayload> TASK = TaskFactory.createTask(PAYLOAD);

    private int propagation;

    @BeforeEach
    void setUp() throws Exception {
        clock.setFixed(Instant.parse("2018-01-01T12:10:05Z"), ZoneId.systemDefault());
        propagation = transactionTemplate.getPropagationBehavior();
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        when(asyncProducer.write(eventCaptor.capture(), anyLong(), anyLong()))
            .thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1L, 135135L, false)));
        when(lomLogbrokerClientFactory.asyncProducer(any()))
            .thenReturn(asyncProducer);
        when(asyncProducer.init())
            .thenReturn(
                CompletableFuture.completedFuture(new ProducerInitResponse(1L, "topic", 0, "sessionId")),
                CompletableFuture.completedFuture(new ProducerInitResponse(4L, "topic", 0, "sessionId")),
                CompletableFuture.completedFuture(new ProducerInitResponse(5L, "topic", 0, "sessionId"))
            );
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.setPropagationBehavior(propagation);
        lomLogbrokerProperties.getExport().setBatchSize(200);
    }

    @Test
    @DisplayName("Успешный экспорт списка событий по source_id")
    @DatabaseSetup("/jobs/processor/publish_logbroker_history_events/before/events_list.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/publish_logbroker_history_events/after/exported_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @JpaQueriesCount(10)
    void testMultipleEventsPushed() throws Exception {
        lomLogbrokerProperties.getExport().setBatchSize(3);
        publishLogbrokerHistoryEventsConsumer.execute(TASK);

        ArgumentCaptor<AsyncProducerConfig> captor = ArgumentCaptor.forClass(AsyncProducerConfig.class);
        verify(lomLogbrokerClientFactory, times(3)).asyncProducer(captor.capture());
        softly.assertThat(captor.getValue().getSourceId()).isEqualTo("1".getBytes(StandardCharsets.US_ASCII));
        verify(asyncProducer, times(3)).init();
        verify(asyncProducer, times(3)).close();
        verify(asyncProducer).write(any(), eq(2L), eq(Instant.parse("2018-01-01T12:00:00Z").toEpochMilli()));
        verify(asyncProducer).write(any(), eq(3L), eq(Instant.parse("2018-01-01T12:09:00Z").toEpochMilli()));
        verify(asyncProducer).write(any(), eq(4L), eq(Instant.parse("2018-01-01T12:09:00Z").toEpochMilli()));
        verify(asyncProducer).write(any(), eq(5L), eq(Instant.parse("2018-01-01T12:10:00Z").toEpochMilli()));
        verifyNoMoreInteractions(asyncProducer);

        assertBacklogDelay(0L, 1L, 1);
        assertBacklogDelay(1L, 2L, 1);
        assertBacklogDelay(10L, 1L, 1);
        assertBacklogLastEvent(5L, 1);
    }

    @Test
    @DisplayName("Нет событий для экспорта")
    @DatabaseSetup("/jobs/processor/publish_logbroker_history_events/before/empty.xml")
    void testEmpty() {
        publishLogbrokerHistoryEventsConsumer.execute(TASK);

        verify(asyncProducer).init();
        verify(asyncProducer).close();
        verifyNoMoreInteractions(asyncProducer);
        assertBacklogNoEventsExported();
    }

    @Test
    @DisplayName("Исключение во время транзакции")
    @DatabaseSetup("/jobs/processor/publish_logbroker_history_events/before/events_list.xml")
    void testExceptionFirstTransaction() throws Exception {
        when(asyncProducer.init()).thenThrow(
            new RuntimeException("Exception happened during events processing for sourceId 1")
        );

        softly.assertThatThrownBy(() -> publishLogbrokerHistoryEventsProcessor.processPayload(PAYLOAD))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("java.lang.RuntimeException: Exception happened during events processing for sourceId 1");
        verify(lomLogbrokerClientFactory).asyncProducer(any());
        assertFailedEventBacklog(1);
        assertBacklogNoEventsExported();
    }

    @Test
    @DisplayName("Тестируем что если другая транзакция держит строку из logbroker_source - задача сразу завершиться")
    @DatabaseSetup("/jobs/processor/publish_logbroker_history_events/before/events_list.xml")
    void testSelectForUpdate() {
        transactionTemplate.execute(tc -> {
            logbrokerSourceService.getRowLock(1, LogbrokerSourceLockType.EXPORT_EVENTS);
            publishLogbrokerHistoryEventsConsumer.execute(TASK);
            return null;
        });
        verifyZeroInteractions(asyncProducer);
    }

    @Test
    @DisplayName("Событие экспортируется, проверка сериализации")
    @DatabaseSetup("/jobs/processor/publish_logbroker_history_events/before/event_logbroker_id_present.xml")
    void testEventExportExecutorEventIsPublished() throws Exception {
        publishLogbrokerHistoryEventsConsumer.execute(TASK);

        verify(asyncProducer).write(any(), eq(2L), eq(Instant.parse("2018-01-01T12:00:00Z").toEpochMilli()));

        String eventDtoJson = new String(eventCaptor.getValue(), StandardCharsets.UTF_8);

        JSONAssert.assertEquals(
            IntegrationTestUtils.extractFileContent("jobs/processor/publish_logbroker_history_events/dto/event_2.json"),
            eventDtoJson,
            true
        );

        assertEventDto(
            2L,
            AuthorDto.builder()
                .abcServiceId(1L)
                .yandexUid(BigDecimal.ONE)
                .build(),
            objectMapper.createObjectNode().set("id", new IntNode(1))
        );
        assertBacklogDelay(10L, 1L, 1);
        assertBacklogLastEvent(2L, 1);
    }

    private void assertFailedEventBacklog(Integer sourceId) {
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=EVENT_EXPORT_FAILED\t" +
                String.format("payload=Failed export event for sourceId %s\t", sourceId) +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=EXPORTED_HISTORY_EVENTS_STATS\t" +
                "extra_keys=sourceId\t" +
                String.format("extra_values=%s", sourceId)
        );
    }

    private void assertBacklogNoEventsExported() {
        softly.assertThat(backLogCaptor.getResults().toString()).doesNotContain("code=EVENT_EXPORT_DELAY");
        softly.assertThat(backLogCaptor.getResults().toString()).doesNotContain("code=LOGBROKER_LAST_EVENT_ID");
    }

    private void assertBacklogDelay(Long delay, Long count, Integer sourceId) {
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=EVENT_EXPORT_DELAY\t" +
                String.format("payload=Exported %s events in %s minutes\t", count, delay) +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "tags=EXPORTED_HISTORY_EVENTS_STATS\t" +
                "extra_keys=sourceId,delayTimeMinutes,eventsCount\t" +
                String.format("extra_values=%s,%s,%s", sourceId, delay, count)
        );
    }

    private void assertBacklogLastEvent(Long maxSeqNo, Integer sourceId) {
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=LOGBROKER_LAST_EVENT_ID\t" +
                String.format(
                    "payload=Last exported message for sourceId %s logbrokerId is %s\t",
                    sourceId,
                    maxSeqNo
                ) +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t" +
                "extra_keys=sourceId,logbrokerId\t" +
                String.format("extra_values=%s,%s", sourceId, maxSeqNo)
        );
    }

    private void assertEventDto(long logbrokerId, AuthorDto author, JsonNode snapshot) throws IOException {
        EventDto actual = objectMapper.readValue(eventCaptor.getValue(), EventDto.class);

        EventDto expected = new EventDto()
            .setId(2)
            .setEntityCreated(ZonedDateTime.of(2020, 2, 2, 12, 0, 0, 0, ZoneId.of("GMT")).toInstant())
            .setCreated(ZonedDateTime.of(2018, 1, 1, 12, 0, 0, 0, ZoneId.of("GMT")).toInstant())
            .setEntityType(EntityType.ORDER)
            .setEntityId(1L)
            .setAuthor(author)
            .setDiff(objectMapper.createArrayNode())
            .setSnapshot(snapshot)
            .setLogbrokerId(logbrokerId)
            .setOrderIdHash(ORDER_ID_HASH)
            .setEventIdHash(EVENT_ID_HASH);

        softly.assertThat(actual).isEqualToComparingFieldByField(expected);
    }
}
