package ru.yandex.market.checkout.providers;

import java.util.Collections;
import java.util.Date;

import com.google.common.collect.Lists;

import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackId;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrack;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrackCheckpoint;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrackMeta;
import ru.yandex.market.checkout.test.providers.TrackProvider;

public final class DeliveryTrackProvider {

    public static final long TRACKER_CHECKPOINT_ID = 123L;
    private static final int DELIVERY_AT_START_RAW_STATUS = 10;

    private DeliveryTrackProvider() {
    }

    public static DeliveryTrack getDeliveryTrack(long trackerId) {
        return getDeliveryTrack(trackerId, DELIVERY_AT_START_RAW_STATUS);
    }

    public static DeliveryTrack getDeliveryTrack(long trackerId, int rawStatus) {
        return getDeliveryTrack(trackerId, rawStatus, TRACKER_CHECKPOINT_ID);
    }

    public static DeliveryTrack getDeliveryTrack(long trackerId, int rawStatus, long trackerCheckpointId) {
        return getDeliveryTrack(TrackProvider.TRACK_ID, trackerId, rawStatus, trackerCheckpointId);
    }

    public static DeliveryTrack getDeliveryTrack(TrackId trackId, long trackerId, int rawStatus) {
        return getDeliveryTrack(trackId, trackerId, rawStatus, TRACKER_CHECKPOINT_ID);
    }

    public static DeliveryTrack getDeliveryTrack(TrackId trackId, long trackerId, int rawStatus,
                                                 long trackerCheckpointId) {
        DeliveryTrackMeta deliveryTrackMeta = new DeliveryTrackMeta(
                trackId.getTrackCode(),
                trackId.getDeliveryService(),
                1L,
                "123"
        );
        deliveryTrackMeta.setId(trackerId);

        return new DeliveryTrack(
                deliveryTrackMeta,
                Collections.singletonList(
                        getDeliveryTrackCheckpoint(rawStatus, trackerCheckpointId))
        );
    }

    public static DeliveryTrackCheckpoint getDeliveryTrackCheckpoint(int rawStatus, long trackerCheckpointId) {
        return new DeliveryTrackCheckpoint(
                trackerCheckpointId,
                "Russia",
                "Moscow",
                "Leo Tolstoy st, 16",
                "Delivery",
                CheckpointStatus.OUT_FOR_DELIVERY,
                "111111",
                new Date(),
                rawStatus
        );
    }

    public static DeliveryTrack getDeliveryTrack(String order) {
        DeliveryTrack deliveryTrack = new DeliveryTrack();
        deliveryTrack.setDeliveryTrackMeta(DeliveryTrackMetaProvider.getDeliveryTrackMeta(order));
        deliveryTrack.setDeliveryTrackCheckpoints(Collections.singletonList(
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint()
        ));
        return deliveryTrack;
    }

    public static DeliveryTrack getDeliveryTrack(String order, DeliveryTrackCheckpoint... checkpoints) {
        DeliveryTrack deliveryTrack = new DeliveryTrack();
        deliveryTrack.setDeliveryTrackMeta(DeliveryTrackMetaProvider.getDeliveryTrackMeta(order));
        deliveryTrack.setDeliveryTrackCheckpoints(Lists.newArrayList(checkpoints));
        return deliveryTrack;
    }

    public static DeliveryTrack getDeliveryTrack(String order,
                                                 long trackId,
                                                 String trackCode,
                                                 DeliveryTrackCheckpoint... checkpoints) {
        DeliveryTrack deliveryTrack = new DeliveryTrack();
        deliveryTrack.setDeliveryTrackMeta(DeliveryTrackMetaProvider.getDeliveryTrackMeta(order, trackId, trackCode));
        deliveryTrack.setDeliveryTrackCheckpoints(Lists.newArrayList(checkpoints));
        return deliveryTrack;
    }
}
