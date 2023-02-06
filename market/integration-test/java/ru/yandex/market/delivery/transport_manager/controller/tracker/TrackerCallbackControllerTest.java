package ru.yandex.market.delivery.transport_manager.controller.tracker;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Checkpoint;
import ru.yandex.market.delivery.transport_manager.dto.tracker.CheckpointStatus;
import ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType;
import ru.yandex.market.delivery.transport_manager.dto.tracker.Track;
import ru.yandex.market.delivery.transport_manager.dto.tracker.TrackMeta;
import ru.yandex.market.delivery.transport_manager.dto.tracker.TrackStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.tracker.process.ProcessTrackProducer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

class TrackerCallbackControllerTest extends AbstractContextualTest {
    public static final String[] META_FIELDS_TO_IGNORE = {
        "lastUpdatedDate",
        "backUrl",
        "consumerId",
        "sourceId",
        "startDate",
        "nextRequestDate",
        "lastCheckpointAcquiredDate",
        "stopTrackingDate",
        "lastStatusRequestDate",
        "lastOrdersStatusRequestDate",
        "globalOrder",
        "orderId",
        "checkpointDate",
        "trackId"
    };

    public static final String[] CHECKPOINT_FIELDS_TO_IGNORE = {
        "checkpointDate",
        "trackId"
    };

    @Autowired
    private ProcessTrackProducer producer;

    @Captor
    ArgumentCaptor<Track> trackCaptor;

    @Test
    void testReceiveCheckpoints() throws Exception {
        mockMvc.perform(
            post("/tracker/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/tracker/movement_tracks.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("controller/tracker/notification_results.json"));

        Mockito.verify(producer, Mockito.times(2)).produce(trackCaptor.capture());

        List<Track> tracks = trackCaptor.getAllValues();

        softly.assertThat(tracks.get(0).getDeliveryTrackMeta())
            .usingRecursiveComparison()
            .ignoringFields(META_FIELDS_TO_IGNORE)
            .isEqualTo(firstTrack().getDeliveryTrackMeta());

        softly.assertThat(tracks.get(0).getDeliveryTrackCheckpoints())
            .usingElementComparatorIgnoringFields(CHECKPOINT_FIELDS_TO_IGNORE)
            .containsAll(firstTrack().getDeliveryTrackCheckpoints());

        softly.assertThat(tracks.get(1).getDeliveryTrackMeta())
            .usingRecursiveComparison()
            .ignoringFields(META_FIELDS_TO_IGNORE)
            .isEqualTo(secondTrack().getDeliveryTrackMeta());

        softly.assertThat(tracks.get(1).getDeliveryTrackCheckpoints())
            .usingElementComparatorIgnoringFields(CHECKPOINT_FIELDS_TO_IGNORE)
            .containsAll(secondTrack().getDeliveryTrackCheckpoints());
    }

    private Track firstTrack() {
        return track(
            trackMeta(
                100L,
                "LO1",
                "1807474",
                48L,
                TrackStatus.STARTED,
                EntityType.MOVEMENT
            ),
            List.of(
                checkpoint(
                    1L,
                    150,
                    "тестовое сообщение",
                    CheckpointStatus.IN_TRANSIT,
                    EntityType.MOVEMENT
                ),
                checkpoint(
                    2L,
                    100,
                    "тестовое сообщение",
                    CheckpointStatus.IN_TRANSIT,
                    EntityType.MOVEMENT
                )
            )
        );
    }

    private Track secondTrack() {
        return track(
            trackMeta(
                101L,
                "LO1",
                "1807474",
                48L,
                TrackStatus.STARTED,
                EntityType.MOVEMENT
            ),
            List.of(
                checkpoint(
                    3L,
                    1,
                    "тестовое сообщение",
                    CheckpointStatus.INFO_RECEIVED,
                    EntityType.MOVEMENT
                ),
                checkpoint(
                    4L,
                    0,
                    "тестовое сообщение",
                    CheckpointStatus.PENDING,
                    EntityType.MOVEMENT
                )
            )
        );
    }

    private Track track(TrackMeta trackMeta, List<Checkpoint> checkpoints) {
        return Track.builder()
            .deliveryTrackMeta(trackMeta)
            .deliveryTrackCheckpoints(checkpoints)
            .build();
    }

    private TrackMeta trackMeta(
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

    private Checkpoint checkpoint(
        Long id,
        Integer deliveryCheckpointStatus,
        String message,
        CheckpointStatus checkpointStatus,
        EntityType entityType
    ) {
        return Checkpoint.builder()
            .id(id)
            .deliveryCheckpointStatus(deliveryCheckpointStatus)
            .message(message)
            .checkpointStatus(checkpointStatus)
            .entityType(entityType)
            .build();
    }
}
