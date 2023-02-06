package ru.yandex.market.checkout.test.providers;

import java.util.Date;

import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;

public abstract class TrackCheckpointProvider {

    public static final int DEFAULT_CHECKPOINT_STATUS = 123;

    private TrackCheckpointProvider() {
    }

    public static TrackCheckpoint createCheckpoint() {
        return createCheckpoint(DEFAULT_CHECKPOINT_STATUS);
    }

    public static TrackCheckpoint createLegacyCheckpoint() {
        return createCheckpoint(null);
    }

    public static TrackCheckpoint createCheckpoint(Integer deliveryCheckpointStatus) {
        return createCheckpoint(deliveryCheckpointStatus, 123L);
    }

    public static TrackCheckpoint createCheckpoint(Integer deliveryCheckpointStatus, long trackerCheckpointId) {
        return new TrackCheckpoint(
                trackerCheckpointId,
                "Russia",
                "Moscow",
                "Office",
                "Message",
                CheckpointStatus.DELIVERED,
                "123123",
                new Date(),
                deliveryCheckpointStatus
        );
    }

    public static TrackCheckpoint createCheckpointWithTrackerId(long trackerCheckpointId) {
        return createCheckpoint(DEFAULT_CHECKPOINT_STATUS, trackerCheckpointId);
    }
}
