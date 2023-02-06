package ru.yandex.market.delivery.transport_manager.service.logbroker.export;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.market.delivery.transport_manager.config.logbroker.LogbrokerProducerProperties;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.LogbrokerExportable;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.StatusHistoryInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.LogbrokerEventSource;
import ru.yandex.market.delivery.transport_manager.util.BacklogCodes;
import ru.yandex.market.delivery.transport_manager.util.BacklogEntities;

class LogbrokerExporterTest {
    public static final Instant NOW = Instant.ofEpochMilli(500);
    public static final String EVT_2 = "{" +
        "\"id\":2," +
        "\"entityId\":1," +
        "\"type\":\"MOVEMENT\"," +
        "\"oldStatus\":\"NEW\"," +
        "\"newStatus\":\"CANCELLED\"," +
        "\"changedAt\":\"1970-01-01T00:00:02Z\"" +
        "}";
    public static final String EVT_3 = "{" +
        "\"id\":4," +
        "\"entityId\":1," +
        "\"type\":\"TRANSPORTATION_UNIT\"," +
        "\"subType\":\"INBOUND\"," +
        "\"oldStatus\":\"NEW\"," +
        "\"newStatus\":\"ERROR\"," +
        "\"changedAt\":\"1970-01-01T00:00:04Z\"" +
        "}";
    private LogbrokerExporter<LogbrokerExportable> statusHistoryLogbrokerExporter;
    private LogbrokerEventSource<LogbrokerExportable> eventSource;
    private LogbrokerClientFactory logbrokerClientFactory;
    private TestableClock clock;
    private AsyncProducer asyncProducer;
    private LogbrokerProducerProperties logbrokerProducerProperties;

    @BeforeEach
    void setUp() {
        eventSource = (LogbrokerEventSource<LogbrokerExportable>) Mockito.mock(LogbrokerEventSource.class);
        logbrokerClientFactory = Mockito.mock(LogbrokerClientFactory.class);
        clock = new TestableClock();
        clock.setFixed(NOW, ZoneId.systemDefault());

        logbrokerProducerProperties = new LogbrokerProducerProperties();
        logbrokerProducerProperties.setEnabled(true);

        statusHistoryLogbrokerExporter = new LogbrokerExporter<>(
            eventSource,
            logbrokerClientFactory,
            null,
            logbrokerProducerProperties,
            clock,
            BacklogEntities.STATUS_HISTORY,
            BacklogCodes.STATUS_HISTORY_PUBLISHED
        );
        asyncProducer = Mockito.mock(AsyncProducer.class);
    }

    @Test
    void testPublish() throws InterruptedException, JsonProcessingException {
        Mockito.when(logbrokerClientFactory.asyncProducer(Mockito.any())).thenReturn(asyncProducer);
        List<LogbrokerExportable> history = List.of(
            new StatusHistoryInfo()
                .setId(1L)
                .setType(EntityType.TRANSPORTATION)
                .setOldStatus(TransportationStatus.NEW.name())
                .setNewStatus(TransportationStatus.CANCELLED.name())
                .setEntityId(1L)
                .setPublished(true)
                .setChangedAt(Instant.ofEpochMilli(1000L)),
            new StatusHistoryInfo()
                .setId(2L)
                .setType(EntityType.MOVEMENT)
                .setOldStatus(MovementStatus.NEW.name())
                .setNewStatus(MovementStatus.CANCELLED.name())
                .setEntityId(1L)
                .setPublished(false)
                .setChangedAt(Instant.ofEpochMilli(2000L)),
            new StatusHistoryInfo()
                .setId(4L)
                .setType(EntityType.TRANSPORTATION_UNIT)
                .setSubType(TransportationUnitType.INBOUND.name())
                .setOldStatus(TransportationUnitStatus.NEW.name())
                .setNewStatus(TransportationUnitStatus.ERROR.name())
                .setEntityId(1L)
                .setPublished(false)
                .setChangedAt(Instant.ofEpochMilli(4000L))
        );
        Mockito
            .when(eventSource.findUnpublished(Mockito.anyInt()))
            .thenReturn(history);
        Mockito
            .when(asyncProducer.init())
            .thenReturn(CompletableFuture.completedFuture(new ProducerInitResponse(1, null, 1, null)));

        statusHistoryLogbrokerExporter.publishToLogbroker();

        Mockito.verify(asyncProducer).init();
        Mockito.verify(asyncProducer).write(
            AdditionalMatchers.aryEq(EVT_2.getBytes(StandardCharsets.UTF_8))
        );
        Mockito.verify(asyncProducer).write(
            AdditionalMatchers.aryEq(EVT_3.getBytes(StandardCharsets.UTF_8))
        );
        Mockito.verify(asyncProducer).close();

        Mockito.verify(eventSource).findUnpublished(
            Mockito.eq(LogbrokerExporter.LIMIT)
        );
        Mockito.verify(eventSource).setPublished(Mockito.eq(List.of(2L, 4L)));

        Mockito.verifyNoMoreInteractions(asyncProducer, eventSource);
    }

    @Test
    void disabled() {
        logbrokerProducerProperties.setEnabled(false);
        statusHistoryLogbrokerExporter.publishToLogbroker();
        Mockito.verifyNoMoreInteractions(asyncProducer, eventSource);
    }
}
