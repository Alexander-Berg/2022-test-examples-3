package ru.yandex.market.logistics.lom.service.tracker.listener.implementation;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrack;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackCheckpoint;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrackMeta;
import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdDeliveryTrackPayload;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@ParametersAreNonnullByDefault
public abstract class AbstractCheckpointListenerTest extends AbstractContextualTest {
    protected static final Instant CHECKPOINT_INSTANT = Instant.parse("2021-09-21T12:30:00Z");

    @Nonnull
    protected LomSegmentCheckpoint createCheckpoint(
        SegmentStatus segmentStatus,
        long trackerId,
        long trackerCheckpointId
    ) {
        return LomSegmentCheckpoint.builder()
            .segmentStatus(segmentStatus)
            .trackerCheckpointId(trackerCheckpointId)
            .trackerId(trackerId)
            .build();
    }

    @Nonnull
    protected LomSegmentCheckpoint createCheckpoint(SegmentStatus segmentStatus) {
        return createCheckpoint(segmentStatus, CHECKPOINT_INSTANT);
    }

    @Nonnull
    protected LomSegmentCheckpoint createCheckpoint(
        SegmentStatus segmentStatus,
        Instant checkpointDate
    ) {
        return LomSegmentCheckpoint.builder()
            .segmentStatus(segmentStatus)
            .trackerId(100L)
            .date(checkpointDate)
            .build();
    }

    @Nonnull
    protected OrderIdDeliveryTrackPayload preparePayload(List<DeliveryTrackCheckpoint> checkpoints) {
        return PayloadFactory.createOrderIdDeliveryTrackPayload(
            1L,
            new DeliveryTrack(
                new DeliveryTrackMeta(100L, "track-code", "1", CHECKPOINT_INSTANT),
                checkpoints
            ),
            "1",
            1
        );
    }
}
