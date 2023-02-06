package ru.yandex.market.delivery.transport_manager.service.movement.tracking;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Checkpoint;
import ru.yandex.market.delivery.transport_manager.dto.tracker.CheckpointStatus;
import ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Track;
import ru.yandex.market.delivery.transport_manager.dto.tracker.TrackMeta;
import ru.yandex.market.delivery.transport_manager.dto.tracker.TrackStatus;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;
import ru.yandex.market.delivery.transport_manager.service.PartnerInfoService;
import ru.yandex.market.delivery.transport_manager.service.external.tracker.TrackProcessorService;
import ru.yandex.market.delivery.transport_manager.service.movement.tracking.entity.MovementTrackerCheckpointProcessor;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@DatabaseSetup({
    "/repository/service/movement/with_several_transportations.xml",
    "/repository/service/movement/methods.xml",
    "/repository/service/movement/processed_checkpoints.xml"
})
class TrackProcessorServiceTest extends AbstractContextualTest {

    @Autowired
    private TrackProcessorService trackProcessorService;

    @Autowired
    private MovementTrackerCheckpointProcessor movementTrackerCheckpointProcessor;

    @Autowired
    private TransportationUnitMapper transportationUnitMapper;

    @Autowired
    private PartnerInfoService partnerInfoService;

    @Test
    @ExpectedDatabase(
        value = "/repository/checkpoint_log/expected/after_processing_track.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void processMovementTrackWithSeveralCheckpoints() {
        trackProcessorService.process(trackWithOldCheckpoints());

        Mockito.verify(movementTrackerCheckpointProcessor).process(
            4L,
            trackWithOldCheckpoints().getDeliveryTrackMeta(),
            List.of(
                getCheckpointByCode(100, 1636555269),
                getCheckpointByCode(200, 1636555369)
            )
        );
    }

    @Test
    void unitProcessedCheckpointReceived() {
        Mockito.doReturn(PartnerType.FULFILLMENT).when(partnerInfoService)
            .getPartnerType(2L, 1L);
        trackProcessorService.process(unitProcessed());

        softly.assertThat(transportationUnitMapper.getById(3L).getStatus())
            .isEqualTo(TransportationUnitStatus.PROCESSED);
    }

    private static Track unitProcessed() {
        return track(
            trackMeta(
                111L,
                "3",
                "blabla",
                2L,
                TrackStatus.STARTED,
                EntityType.OUTBOUND
            ),
            List.of(
                getUnitCheckpoint(40, 1636555369)
            )
        );
    }

    private static Track trackWithOldCheckpoints() {
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
                getCheckpointByCode(1, 10),
                getCheckpointByCode(100, 1636555269),
                getCheckpointByCode(200, 1636555369),
                getCheckpointByCode(250, 10)
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

    private static Checkpoint getUnitCheckpoint(int code, int timestamp) {
        return Checkpoint.builder()
            .deliveryCheckpointStatus(code)
            .entityType(EntityType.OUTBOUND)
            .checkpointStatus(CheckpointStatus.PENDING)
            .checkpointDate(Instant.ofEpochSecond(timestamp))
            .build();
    }
}
