package ru.yandex.market.ff.dbqueue.producer;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryTrackStatus;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.dbqueue.UpdateTrackerStatusPayload;
import ru.yandex.market.ff.model.dto.tracker.Track;
import ru.yandex.market.ff.model.dto.tracker.TrackCheckpoint;
import ru.yandex.market.ff.model.dto.tracker.TrackMeta;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class UpdateTrackerQueueProducerSerializationTest extends IntegrationTest {

    private static final String PAYLOAD_STRING_VAR_1 = extractFileContent(
            "db-queue/producer/update-tracker/payload1.json").trim();
    private static final String PAYLOAD_STRING_VAR_2 = extractFileContent(
            "db-queue/producer/update-tracker/payload2.json").trim();

    @Autowired
    private UpdateTrackerQueueProducer updateTrackQueueProducer;

    @Test
    public void testSerializeWorks() {
        UpdateTrackerStatusPayload payload = new UpdateTrackerStatusPayload(1, Set.of(101L, 102L),
                createTrackList());
        String result = updateTrackQueueProducer.getPayloadTransformer().fromObject(payload);
        assertions.assertThat(result).isIn(PAYLOAD_STRING_VAR_1, PAYLOAD_STRING_VAR_2);
    }

    private List<Track> createTrackList() {
        Track track1 = Track.builder()
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
                                .build()
                ))
                .build();

        Track track2 = Track.builder()
                .deliveryTrackMeta(TrackMeta.builder()
                        .id(102)
                        .entityId(2)
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
                                .build()
                ))
                .build();
        return List.of(track1, track2);
    }
}
