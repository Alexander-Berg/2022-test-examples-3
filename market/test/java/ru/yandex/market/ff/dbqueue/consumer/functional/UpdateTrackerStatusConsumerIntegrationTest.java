package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryTrackStatus;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.UpdateTrackerQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.UpdateTrackerStatusPayload;
import ru.yandex.market.ff.model.dto.tracker.Track;
import ru.yandex.market.ff.model.dto.tracker.TrackCheckpoint;
import ru.yandex.market.ff.model.dto.tracker.TrackMeta;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;

public class UpdateTrackerStatusConsumerIntegrationTest extends IntegrationTestWithDbQueueConsumers {

    @Autowired
    private UpdateTrackerQueueConsumer consumer;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    public void resetMocks() {
        super.resetMocks();

    }

    @Test
    @DatabaseSetup("classpath:db-queue/consumer/update-tracker/before-1.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/update-tracker/after-1.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void successValidation() {
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
        Task<UpdateTrackerStatusPayload> task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        TaskExecutionResult result = transactionTemplate.execute(status -> consumer.execute(task));
        assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }

    @Test
    @DatabaseSetup("classpath:db-queue/consumer/update-tracker/before-2.xml")
    void handleInconsistentRequestChangeExceptionWithFinishedStatus() {

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
        Task<UpdateTrackerStatusPayload> task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        TaskExecutionResult result = transactionTemplate.execute(status -> consumer.execute(task));
        assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }
}
