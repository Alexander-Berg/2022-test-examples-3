package ru.yandex.market.delivery.transport_manager.queue.task.tracker.process;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Checkpoint;
import ru.yandex.market.delivery.transport_manager.dto.tracker.CheckpointStatus;
import ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Track;
import ru.yandex.market.delivery.transport_manager.dto.tracker.TrackMeta;
import ru.yandex.market.delivery.transport_manager.queue.base.exception.DbQueueTaskExecutionException;
import ru.yandex.market.delivery.transport_manager.repository.mappers.MovementMapper;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

@DatabaseSetup({
    "/repository/movement/different_status_movements.xml",
})
class ProcessTrackConsumerTest extends AbstractContextualTest {
    @Autowired
    private ProcessTrackConsumer processTrackConsumer;

    @Autowired
    private MovementMapper movementMapper;

    @Test
    void testNewCorrectCheckpoints() {
        processTrackConsumer.execute(
            task(
                "TMM1",
                "movement1",
                10L,
                List.of(
                    checkpoint(0),
                    checkpoint(1),
                    checkpoint(100)
                )
            )
        );

        softly.assertThat(movementMapper.getById(1L).getStatus())
            .isEqualTo(MovementStatus.COURIER_FOUND);
    }

    @Test
    void testOldFormatNewCorrectCheckpoints() {
        processTrackConsumer.execute(
            task(
                "1",
                "movement1",
                10L,
                List.of(
                    checkpoint(0),
                    checkpoint(1),
                    checkpoint(100)
                )
            )
        );

        softly.assertThat(movementMapper.getById(1L).getStatus())
            .isEqualTo(MovementStatus.COURIER_FOUND);
    }

    @Test
    void testNotNewCorrectCheckpoints() {
        processTrackConsumer.execute(
            task(
                "TMM2",
                "movement2",
                20L,
                List.of(
                    checkpoint(0),
                    checkpoint(1),
                    checkpoint(100)
                )
            )
        );

        softly.assertThat(movementMapper.getById(2L).getStatus())
            .isEqualTo(MovementStatus.COURIER_FOUND);
    }

    @Test
    void testNoNewCheckpointsArrived() {
        processTrackConsumer.execute(
            task(
                "TMM3",
                "movement3",
                30L,
                List.of(
                    checkpoint(0),
                    checkpoint(1),
                    checkpoint(100),
                    checkpoint(250)
                )
            )
        );

        softly.assertThat(movementMapper.getById(3L).getStatus())
            .isEqualTo(MovementStatus.COMPLETED);
    }

    @Test
    void testLessCheckpointArrivedLater() {
        processTrackConsumer.execute(
            task(
                "TMM3",
                "movement3",
                30L,
                List.of(
                    checkpoint(0, "2021-01-20T13:00:00.00Z"),
                    checkpoint(3, "2021-01-20T15:00:00.00Z"),
                    checkpoint(95, "2021-01-20T14:00:00.00Z")
                )
            )
        );

        softly.assertThat(movementMapper.getById(3L).getStatus())
            .isEqualTo(MovementStatus.CANCELLED);
    }

    @Test
    void testNoCheckpointsArrived() {
        processTrackConsumer.execute(
            task(
                "TMM3",
                "movement3",
                30L,
                Collections.emptyList()
            )
        );

        softly.assertThat(movementMapper.getById(3L).getStatus())
            .isEqualTo(MovementStatus.PARTNER_CREATED);
    }

    @Test
    void testInvalidTrackCodeArrived() {
        Assertions.assertThrows(DbQueueTaskExecutionException.class, () -> processTrackConsumer.execute(
            task(
                "TMM3",
                "ergerger",
                30L,
                List.of(
                    checkpoint(0),
                    checkpoint(1)
                )
            )
        ));

        softly.assertThat(movementMapper.getById(3L).getStatus())
            .isEqualTo(MovementStatus.PARTNER_CREATED);
    }

    @Test
    void testInvalidPartnerIdArrived() {
        Assertions.assertThrows(DbQueueTaskExecutionException.class, () -> processTrackConsumer.execute(
            task(
                "TMM3",
                "movement3",
                100500L,
                List.of(
                    checkpoint(0),
                    checkpoint(1)
                )
            )
        ));

        softly.assertThat(movementMapper.getById(3L).getStatus())
            .isEqualTo(MovementStatus.PARTNER_CREATED);
    }

    private Task<ProcessTrackDto> task(
        String entityId,
        String trackCode,
        Long partnerId,
        List<Checkpoint> checkpoints
    ) {
        return task(track(trackMeta(entityId, trackCode, partnerId), checkpoints));
    }

    private Track track(TrackMeta trackMeta, List<Checkpoint> checkpoints) {
        return Track.builder()
            .deliveryTrackMeta(trackMeta)
            .deliveryTrackCheckpoints(checkpoints)
            .build();
    }

    private TrackMeta trackMeta(String entityId, String trackCode, Long deliveryServiceId) {
        return TrackMeta.builder()
            .deliveryServiceId(deliveryServiceId)
            .trackCode(trackCode)
            .entityId(entityId)
            .entityType(EntityType.MOVEMENT)
            .build();
    }

    private Checkpoint checkpoint(Integer deliveryCheckpointStatus) {
        return checkpoint(deliveryCheckpointStatus, "2021-01-20T13:00:00.00Z");
    }

    private Checkpoint checkpoint(Integer deliveryCheckpointStatus, String datetime) {
        return Checkpoint.builder()
            .checkpointDate(Instant.parse(datetime))
            .acquiredByTrackerDate(Instant.parse(datetime))
            .deliveryCheckpointStatus(deliveryCheckpointStatus)
            .checkpointStatus(CheckpointStatus.DELIVERED)
            .build();
    }

    private Task<ProcessTrackDto> task(Track track) {
        return Task.<ProcessTrackDto>builder(new QueueShardId("123"))
            .withPayload(new ProcessTrackDto(track))
            .build();
    }
}
