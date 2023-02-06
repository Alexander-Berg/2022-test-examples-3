package ru.yandex.market.delivery.transport_manager.service.movement.tracking.entity;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Checkpoint;
import ru.yandex.market.delivery.transport_manager.dto.tracker.CheckpointStatus;
import ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Track;
import ru.yandex.market.delivery.transport_manager.dto.tracker.TrackMeta;
import ru.yandex.market.delivery.transport_manager.dto.tracker.TrackStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.movement.get.GetMovementProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.outbound.PutOutboundProducer;
import ru.yandex.market.delivery.transport_manager.service.AxaptaStatusEventService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DatabaseSetup({
    "/repository/service/movement/with_several_transportations.xml",
    "/repository/service/movement/methods.xml"
})
class MovementTrackerCheckpointProcessorTest extends AbstractContextualTest {
    @Autowired
    private MovementTrackerCheckpointProcessor movementTrackerCheckpointProcessor;

    @Autowired
    private CommonMovementProcessor commonMovementProcessor;

    @Autowired
    private GetMovementProducer getMovementProducer;

    @Autowired
    private AxaptaStatusEventService axaptaStatusEventService;

    @Autowired
    private PutOutboundProducer putOutboundProducer;

    @Test
    @ExpectedDatabase(
        value = "/repository/service/movement/after/after_success_completed_callback.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void correctlyReceivedCheckpoint() {
        movementTrackerCheckpointProcessor.process(
            4L,
            someTrack().getDeliveryTrackMeta(),
            someTrack().getDeliveryTrackCheckpoints()
        );
    }

    @Test
    void processCarAssignedSuccess() {
        movementTrackerCheckpointProcessor.process(
            4L,
            trackWithCarAssigned().getDeliveryTrackMeta(),
            trackWithCarAssigned().getDeliveryTrackCheckpoints()
        );
        verify(getMovementProducer).enqueue(any(Transportation.class));
        verify(axaptaStatusEventService).createNewTransportationEvent(Mockito.any());
    }

    @Test
    void testRemoveExtraCheckpoint() {
        List<Checkpoint> checkpoints = List.of(
            getCheckpointByCode(95, 2022),
            getCheckpointByCode(100, 2022)
        );

        Assertions.assertThat(commonMovementProcessor.remove95If100Exists(checkpoints))
            .extracting(Checkpoint::getDeliveryCheckpointStatus)
            .containsOnly(100);
    }

    @Test
    void testRemoveUnknownCheckpoint() {
        List<Checkpoint> checkpoints = List.of(
            getCheckpointByCode(95, 2022),
            getCheckpointByCode(-1, 2022),
            getCheckpointByCode(400, 2022)
        );

        Assertions.assertThat(commonMovementProcessor.filterCheckpoints(-1L, checkpoints))
            .extracting(Checkpoint::getDeliveryCheckpointStatus)
            .containsOnly(95);
    }

    @Test
    @DatabaseSetup("/repository/service/movement/transportation_checkpoint_95_100.xml")
    void testInvokeOnlyOnePutOutboundAfterReceiving95And100() {
        movementTrackerCheckpointProcessor.process(
            5L,
            trackMeta(102L, "5", "M5", 3L, TrackStatus.STARTED, EntityType.MOVEMENT),
            List.of(getCheckpointByCode(95, 2022), getCheckpointByCode(100, 2022))
        );

        verify(putOutboundProducer, times(1)).enqueue(any(Transportation.class));
    }

    @Test
    @DatabaseSetup("/repository/service/movement/transportation_checkpoint_95_100_les_cp_exists.xml")
    void noExtraPutOutboundIfFasterCheckpointArrived() {
        movementTrackerCheckpointProcessor.process(
            5L,
            trackMeta(102L, "5", "M5", 3L, TrackStatus.STARTED, EntityType.MOVEMENT),
            List.of(getCheckpointByCode(95, 2022), getCheckpointByCode(100, 2022))
        );

        verify(putOutboundProducer, times(0)).enqueue(any(Transportation.class));
    }

    private static Track trackWithCarAssigned() {
        return track(
            trackMeta(
                101L,
                "4",
                "M4",
                3L,
                TrackStatus.STARTED,
                EntityType.MOVEMENT
            ),
            List.of(
                getCheckpointByCode(100, 1636555369),
                getCheckpointByCode(250, 1636555369)
            )
        );
    }

    private static Track someTrack() {
        return track(
            trackMeta(
                101L,
                "4",
                "M4",
                3L,
                TrackStatus.STARTED,
                EntityType.MOVEMENT
            ),
            List.of(
                getCheckpointByCode(250, 1636555369)
            )
        );
    }

    private static Track track(TrackMeta trackMeta, List<Checkpoint> checkpoints) {
        return Track.builder()
            .deliveryTrackMeta(trackMeta)
            .deliveryTrackCheckpoints(checkpoints)
            .build();
    }

    private static TrackMeta trackMeta(
        Long id,
        String entityId,
        String trackCode,
        Long deliveryServiceId,
        TrackStatus trackStatus,
        EntityType entityType
    ) {
        return TrackMeta.builder()
            .id(id)
            .entityType(entityType)
            .deliveryTrackStatus(trackStatus)
            .deliveryServiceId(deliveryServiceId)
            .trackCode(trackCode)
            .entityId(entityId)
            .build();
    }

    private static Checkpoint getCheckpointByCode(int code, int timestamp) {
        return Checkpoint.builder()
            .deliveryCheckpointStatus(code)
            .entityType(EntityType.MOVEMENT)
            .checkpointStatus(CheckpointStatus.PENDING)
            .checkpointDate(Instant.ofEpochSecond(timestamp))
            .build();
    }


}
