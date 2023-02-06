package ru.yandex.market.ff.dbqueue.service;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryTrackStatus;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.UpdateTrackerStatusPayload;
import ru.yandex.market.ff.model.dto.tracker.Track;
import ru.yandex.market.ff.model.dto.tracker.TrackCheckpoint;
import ru.yandex.market.ff.model.dto.tracker.TrackMeta;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class UpdateTrackerStatusServiceTest extends IntegrationTest {

    @Autowired
    private UpdateTrackerStatusService updateTrackerStatusService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;

/**
 * Проверяем успешное обновление статусов для заявок
 * <p>
 * В БД:
 * <ul>
 *      <li> shop_request: Заявка с id=0 type=SUPPLY в статусе 3
 *      <li> request_status_history: для заявки с id=0 записи статусов 0, 1, 2, 3
 * </ul>
 * В запросе:
 * <ul>
 *      <li> для заявки 0 новые статусы (9, 6, 7)
 * </ul>
 * <p>
 * Проверяем в БД:
 * <ul>
 *     <li> заявка 0 перешла в статус 7
 *     <li> request_status_history: для заявки с id=0 записи статусов 0, 1, 2, 3, 9, 6, 7
 * </ul>
 */
    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-1.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateTrackerStatusSuccessSupply() {
        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(101)
                        .entityId(1)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(100)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857344000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("ARRIVED_TO_SERVICE")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(30)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857345000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("IN_PROGRESS")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(40)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857346000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-2.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-2.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateTrackerStatusSuccessXdoc() {
        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(102)
                        .entityId(2)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(200)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857341000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(102)
                                .message("ARRIVED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(102L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-3.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-3.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateTrackerStatusSuccessTransfer() {
        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(103)
                        .entityId(3)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(100)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.TRANSFER)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(30)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857340000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(103)
                                .message("тестовое сообщение")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857341000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(103)
                                .message("тестовое сообщение")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(103L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-4.xml")
    void shouldHandleExceptionFromShopService() {

        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(101)
                        .entityId(1)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(100)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857344000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("ARRIVED_TO_SERVICE")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(30)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857345000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("IN_PROGRESS")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(40)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857346000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });

    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-5.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-5.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void initiallyAcceptedStatusAsLastStatusInPayload() {
        jdbcTemplate.update("update request_subtype set work_with_initial_acceptance_details = true where id = 1008");

        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(101)
                        .entityId(1)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(300)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(1)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857343000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("ARRIVED_TO_SERVICE")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857344000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("IN_PROGRESS")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(25)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857345000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-6.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-6.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void initiallyAcceptedStatusAsNotLastStatusInPayload() {
        jdbcTemplate.update("update request_subtype set work_with_initial_acceptance_details = true where id = 1008");

        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(101)
                        .entityId(1)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(300)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(1)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857343000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("ARRIVED_TO_SERVICE")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857344000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("IN_PROGRESS")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(25)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857345000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(30)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857346000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-7.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-7.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void otherStatusesWhenRequestInInitiallyAcceptedStatus() {
        jdbcTemplate.update("update request_subtype set work_with_initial_acceptance_details = true where id = 1008");

        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(101)
                        .entityId(1)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(300)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(1)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857343000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("ARRIVED_TO_SERVICE")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857344000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("IN_PROGRESS")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(25)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857345000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(30)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857346000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(40)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857347000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-8.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-8.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void otherStatusesWhenRequestInInitiallyAcceptedDetailsLoadedStatus() {
        jdbcTemplate.update("update request_subtype set work_with_initial_acceptance_details = true where id = 1008");

        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(101)
                        .entityId(1)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(300)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(1)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857343000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("ARRIVED_TO_SERVICE")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857344000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("IN_PROGRESS")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(25)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857345000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(30)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857346000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(40)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857347000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-9.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-9.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void initiallyAcceptedStatusAsNotFirstAndNotLastStatusInUpdatedStatuses() {
        jdbcTemplate.update("update request_subtype set work_with_initial_acceptance_details = true where id = 1008");

        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(101)
                        .entityId(1)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(300)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(1)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857343000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("ARRIVED_TO_SERVICE")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857344000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("IN_PROGRESS")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(25)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857345000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(30)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857346000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-10.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-10.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void initiallyAcceptedStatusForTypeForWhichItIsNotApplicableAndOnlyThisStatusInHistory() {
        jdbcTemplate.update("update request_subtype set work_with_initial_acceptance_details = false where id = 1008");

        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(101)
                        .entityId(1)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(300)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(1)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857343000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("ARRIVED_TO_SERVICE")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857344000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("IN_PROGRESS")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(25)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857345000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-11.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-11.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void initiallyAcceptedStatusForTypeForWhichItIsNotApplicableAndThereIsOtherStatusInHistory() {
        jdbcTemplate.update("update request_subtype set work_with_initial_acceptance_details = false where id = 1008");

        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(101)
                        .entityId(1)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(300)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(1)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857343000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("ARRIVED_TO_SERVICE")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857344000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("IN_PROGRESS")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(25)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857345000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(30)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857346000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-12.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-12.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void noInitiallyAcceptedStatusWhenRequired() {
        jdbcTemplate.update("update request_subtype set work_with_initial_acceptance_details = true where id = 1008");

        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(101)
                        .entityId(1)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(300)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(1)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857343000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("ARRIVED_TO_SERVICE")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857344000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("IN_PROGRESS")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(30)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857345000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/update-tracker-status/before-12.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/update-tracker-status/after-12.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void noInitiallyAcceptedStatusWhenRequiredButAlreadyHasInProgressInHistory() {
        jdbcTemplate.update("update request_subtype set work_with_initial_acceptance_details = true where id = 1008");

        Track track = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(101)
                        .entityId(1)
                        .trackCode("1807474")
                        .lastUpdatedDate(Date.from(Instant.ofEpochMilli(1565092800000L)))
                        .backUrl("https://ffw-api.tst.vs.market.yandex.net/notifyTracks")
                        .deliveryServiceId(300)
                        .consumerId(6)
                        .startDate(Date.from(Instant.ofEpochMilli(1566474905576L)))
                        .nextRequestDate(Date.from(Instant.ofEpochMilli(1566897744312L)))
                        .deliveryTrackStatus(DeliveryTrackStatus.STARTED)
                        .lastCheckpointAcquiredDate(Date.from(Instant.ofEpochMilli(1566475514527L)))
                        .stopTrackingDate(Date.from(Instant.ofEpochMilli(1571659514528L)))
                        .lastStatusRequestDate(Date.from(Instant.ofEpochMilli(1566896844775L)))
                        .globalOrder(false)
                        .entityType(EntityType.INBOUND)
                        .build())
                .deliveryTrackCheckpoints(List.of(
                        TrackCheckpoint.builder()
                                .id(1)
                                .deliveryCheckpointStatus(1)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857343000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("ARRIVED_TO_SERVICE")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(2)
                                .deliveryCheckpointStatus(20)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857344000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("IN_PROGRESS")
                                .build(),
                        TrackCheckpoint.builder()
                                .id(3)
                                .deliveryCheckpointStatus(30)
                                .checkpointDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(936857345000L),
                                        ZoneId.of("UTC+3")))
                                .checkpointStatus(CheckpointStatus.INFO_RECEIVED)
                                .trackId(101)
                                .message("PROCESSED")
                                .build()
                ))
                .build();
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L), List.of(track));
        transactionTemplate.execute(status -> {
            updateTrackerStatusService.processPayload(payload);
            return null;
        });
    }
}
