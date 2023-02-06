package ru.yandex.market.notifier.util.providers;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;

public abstract class TrackProvider {

    public static final String TRACK_CODE = "asdasd";
    public static final long DELIVERY_SERVICE_ID = 99L;

    public static Track createTrack(String trackCode, long deliveryServiceId) {
        Track track = new Track();
        track.setDeliveryServiceId(deliveryServiceId);
        track.setTrackCode(trackCode);
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        return track;
    }
}
