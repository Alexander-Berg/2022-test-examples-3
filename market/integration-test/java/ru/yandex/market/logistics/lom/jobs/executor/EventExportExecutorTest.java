package ru.yandex.market.logistics.lom.jobs.executor;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import javax.persistence.EntityManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.LogbrokerProperties;
import ru.yandex.market.logistics.lom.converter.EventConverter;
import ru.yandex.market.logistics.lom.model.dto.AuthorDto;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.lom.service.order.OrderService;
import ru.yandex.market.logistics.lom.service.order.history.OrderHistoryService;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

import static org.mockito.Mockito.times;

public class EventExportExecutorTest extends AbstractContextualTest {
    private static final Long ORDER_ID_HASH = -2582693687464317919L;
    private static final Long EVENT_ID_HASH = 2721502588582550493L;
    @Autowired
    private LogbrokerClientFactory lomLogbrokerClientFactory;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private AsyncProducerConfig lomAsyncProducerConfig;
    @Autowired
    private OrderHistoryService orderHistoryService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private LogbrokerProperties lomLogbrokerProperties;
    @Autowired
    private DataFieldMaxValueIncrementer logbrokerIdSequence;
    @Autowired
    private TransactionOperations transactionTemplate;
    @Autowired
    private EventConverter eventConverter;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private AsyncProducer asyncProducer;

    @Captor
    private ArgumentCaptor<byte[]> eventCaptor;

    private EventExportExecutor eventExportExecutor;

    @SneakyThrows
    @BeforeEach
    void setUp() {
        eventExportExecutor = new EventExportExecutor(
            lomLogbrokerClientFactory,
            lomAsyncProducerConfig,
            orderHistoryService,
            orderService,
            objectMapper,
            lomLogbrokerProperties,
            logbrokerIdSequence,
            transactionTemplate,
            eventConverter,
            clock
        );

        Mockito.when(asyncProducer.write(eventCaptor.capture(), Mockito.anyLong()))
            .thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1L, 135135L, false)));
        Mockito.when(asyncProducer.init())
            .thenReturn(CompletableFuture.completedFuture(new ProducerInitResponse(1L, "topic", 0, "sessionId")));
        Mockito.when(lomLogbrokerClientFactory.asyncProducer(Mockito.any()))
            .thenReturn(asyncProducer);
        Mockito.when(logbrokerIdSequence.nextLongValue()).thenReturn(1L);
    }

    @SneakyThrows
    @Test
    @DisplayName("Экспорт списка событий")
    @DatabaseSetup("/jobs/executor/eventExport/before/events_list.xml")
    void testMultipleEventsPushed() {
        clock.setFixed(Instant.parse("2018-01-01T12:10:05Z"), ZoneId.systemDefault());
        eventExportExecutor.doJob(jobExecutionContext);

        Mockito.verify(asyncProducer, times(6)).write(Mockito.any(), Mockito.anyLong());

        assertBacklog(0L, 3L);
        assertBacklog(1L, 2L);
        assertBacklog(10L, 1L);
    }

    @SneakyThrows
    @Test
    @DisplayName("События экспортируются, если logbrokerId проставлен")
    @DatabaseSetup("/jobs/executor/eventExport/before/event_logbroker_id_present.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/eventExport/after/event_published_id_present.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testEventExportExecutorEventIsPublished() {
        clock.setFixed(Instant.parse("2018-01-01T12:00:05Z"), ZoneId.systemDefault());
        eventExportExecutor.doJob(jobExecutionContext);

        Mockito.verify(asyncProducer).write(Mockito.any(), Mockito.eq(2L));

        String eventDtoJson = new String(eventCaptor.getValue(), StandardCharsets.UTF_8);

        JSONAssert.assertEquals(
            IntegrationTestUtils.extractFileContent("jobs/executor/eventExport/dto/event_2.json"),
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
        assertBacklog(0L, 1L);
    }

    @SneakyThrows
    @Test
    @DisplayName("События экспортируются, если logbrokerId не проставлен")
    @DatabaseSetup("/jobs/executor/eventExport/before/event_logbroker_id_null.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/eventExport/after/event_published_id_null.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @Transactional
    public void testEventExportExecutorEventIsNotPublished() {
        clock.setFixed(Instant.parse("2018-01-01T12:00:05Z"), ZoneId.systemDefault());
        eventExportExecutor.doJob(jobExecutionContext);
        entityManager.flush();

        Mockito.verify(asyncProducer, times(1))
            .write(Mockito.any(), Mockito.eq(1L));

        String eventDtoJson = new String(eventCaptor.getValue(), StandardCharsets.UTF_8);

        JSONAssert.assertEquals(
            IntegrationTestUtils.extractFileContent("jobs/executor/eventExport/dto/event_1.json"),
            eventDtoJson,
            true
        );

        assertEventDto(
            1L,
            AuthorDto.builder()
                .abcServiceId(1L)
                .yandexUid(BigDecimal.ONE)
                .build(),
            NullNode.getInstance()
        );
        assertBacklog(0L, 1L);
    }

    @SneakyThrows
    @Test
    @DisplayName("События экспортируются, если поле author не проставлено")
    @DatabaseSetup("/jobs/executor/eventExport/before/event_author_null_logbroker_Id_null.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/eventExport/after/event_published_author_null_id_null.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @Transactional
    public void testEventExportExecutorEventAuthorIsNull() {
        clock.setFixed(Instant.parse("2018-01-01T12:00:05Z"), ZoneId.systemDefault());
        eventExportExecutor.doJob(jobExecutionContext);
        entityManager.flush();

        Mockito.verify(asyncProducer, times(1))
            .write(Mockito.any(), Mockito.eq(1L));

        String eventDtoJson = new String(eventCaptor.getValue(), StandardCharsets.UTF_8);

        JSONAssert.assertEquals(
            IntegrationTestUtils.extractFileContent("jobs/executor/eventExport/dto/event_1_null_author.json"),
            eventDtoJson,
            true
        );

        assertEventDto(1L, null, NullNode.getInstance());
        assertBacklog(0L, 1L);
    }

    private void assertBacklog(Long delay, Long count) {
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=EVENT_EXPORT_DELAY\t" +
                String.format("payload=Exported %s events in %s minutes\t", count, delay) +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=EXPORTED_HISTORY_EVENTS_STATS\t" +
                "extra_keys=sourceId,delayTimeMinutes,eventsCount\t" +
                String.format("extra_values=%s,%s,%s", "market-lom", delay, count)
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
