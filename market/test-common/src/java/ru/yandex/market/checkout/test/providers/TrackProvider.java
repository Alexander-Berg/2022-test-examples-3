package ru.yandex.market.checkout.test.providers;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackId;

public abstract class TrackProvider {
    public static final String TRACK_CODE = "asdasd";
    public static final long DELIVERY_SERVICE_ID = 99L;
    public static final TrackId TRACK_ID = new TrackId(TRACK_CODE, DELIVERY_SERVICE_ID);

    public static Track createTrack() {
        return createTrack(DELIVERY_SERVICE_ID);
    }

    public static Track createTrack(long deliveryServiceId) {
        return createTrack(TRACK_CODE, deliveryServiceId);
    }

    public static Track createTrack(String trackCode, long deliveryServiceId) {
        Track track = new Track();
        track.setDeliveryServiceId(deliveryServiceId);
        track.setTrackCode(trackCode);
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        return track;
    }

    public static Track createTrack(String trackCode, long deliveryServiceId, long trackerId) {
        Track track = new Track();
        track.setDeliveryServiceId(deliveryServiceId);
        track.setTrackCode(trackCode);
        track.setTrackerId(trackerId);
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        return track;
    }
}
