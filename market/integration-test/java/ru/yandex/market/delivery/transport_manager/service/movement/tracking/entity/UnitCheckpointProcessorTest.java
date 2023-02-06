package ru.yandex.market.delivery.transport_manager.service.movement.tracking.entity;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Checkpoint;
import ru.yandex.market.delivery.transport_manager.dto.tracker.CheckpointStatus;
import ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Track;
import ru.yandex.market.delivery.transport_manager.dto.tracker.TrackMeta;
import ru.yandex.market.delivery.transport_manager.dto.tracker.TrackStatus;
import ru.yandex.market.delivery.transport_manager.event.unit.status.UnitStatusReceivedEvent;
import ru.yandex.market.delivery.transport_manager.event.unit.status.listener.UpdateEntitiesStatusListener;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;
import ru.yandex.market.delivery.transport_manager.service.PartnerInfoService;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@DatabaseSetup({
    "/repository/service/movement/with_several_transportations.xml",
    "/repository/service/movement/methods.xml",
})
class UnitCheckpointProcessorTest extends AbstractContextualTest {
    @Autowired
    private UnitCheckpointProcessor unitCheckpointProcessor;

    @Autowired
    private UpdateEntitiesStatusListener updateEntitiesStatusListener;

    @Autowired
    private TransportationUnitMapper transportationUnitMapper;

    @Autowired
    private PartnerInfoService partnerInfoService;

    @Captor
    ArgumentCaptor<UnitStatusReceivedEvent> eventCaptor;

    @Test
    void process() {
        Mockito.doReturn(PartnerType.DELIVERY).when(partnerInfoService)
            .getPartnerType(2L, 1L);

        unitCheckpointProcessor.process(
            3L,
            track().getDeliveryTrackMeta(),
            track().getDeliveryTrackCheckpoints()
        );

        Mockito.verify(updateEntitiesStatusListener, Mockito.times(3))
            .listen(eventCaptor.capture());
        List<UnitStatusReceivedEvent> allValues = eventCaptor.getAllValues();

        softly.assertThat(allValues.get(0).getOldStatus()).isEqualTo(TransportationUnitStatus.NEW);
        softly.assertThat(allValues.get(0).getNewStatus()).isEqualTo(TransportationUnitStatus.ARRIVED);
        softly.assertThat(allValues.get(1).getOldStatus()).isEqualTo(TransportationUnitStatus.ARRIVED);
        softly.assertThat(allValues.get(1).getNewStatus()).isEqualTo(TransportationUnitStatus.IN_PROGRESS);
        softly.assertThat(allValues.get(2).getOldStatus()).isEqualTo(TransportationUnitStatus.IN_PROGRESS);
        softly.assertThat(allValues.get(2).getNewStatus()).isEqualTo(TransportationUnitStatus.PROCESSED);

        softly.assertThat(transportationUnitMapper.getById(3L).getStatus())
            .isEqualTo(TransportationUnitStatus.PROCESSED);
    }

    @Test
    @DatabaseSetup("/repository/service/movement/unit_accepted_checkpoint.xml")
    void processAcceptedUnit() {
        unitCheckpointProcessor.process(
            3L,
            trackAccepted().getDeliveryTrackMeta(),
            trackAccepted().getDeliveryTrackCheckpoints()
        );

        TransportationUnit unit = transportationUnitMapper.getById(3L);
        softly.assertThat(unit.getStatus()).isEqualTo(TransportationUnitStatus.ACCEPTED);
    }

    private static Track track() {
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
                checkpoint(100500, 1636555369),
                checkpoint(20, 1636555371),
                checkpoint(30, 1636555372),
                checkpoint(40, 1636555380)
            )
        );
    }

    private static Track trackAccepted() {
        return track(
                trackMeta(
                    111L,
                    "3",
                    "blabla",
                    3L,
                    TrackStatus.STARTED,
                    EntityType.OUTBOUND
                ),
                List.of(
                        checkpoint(1, 1636555369)
                )
        );
    }

    private static Checkpoint checkpoint(int code, int timestamp) {
        return Checkpoint.builder()
            .deliveryCheckpointStatus(code)
            .entityType(EntityType.OUTBOUND)
            .checkpointStatus(CheckpointStatus.PENDING)
            .checkpointDate(Instant.ofEpochSecond(timestamp))
            .build();
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
}
