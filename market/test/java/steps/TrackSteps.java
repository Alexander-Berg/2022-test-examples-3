package steps;

import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.json.Names;

public class TrackSteps {
    private static final Long ID = 123L;
    private static final Long ORDER_ID = 123L;
    private static final Long DELIVERY_ID = 666L;
    private static final String TRACK_CODE = "track code";
    private static final Long DELIVERY_SERVICE_ID = 333L;
    private static final Long TRACKER_ID = 222L;
    private static final TrackStatus STATUS = TrackStatus.STARTED;

    private TrackSteps() {
    }

    public static Track getTrack() {
        Track track = new Track();

        track.setId(ID);
        track.setOrderId(ORDER_ID);
        track.setTrackCode(TRACK_CODE);
        track.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        track.setTrackerId(TRACKER_ID);
        track.setStatus(STATUS);
        track.setDeliveryId(DELIVERY_ID);

        return track;
    }

    public static JSONObject getTrackJson() throws JSONException {
        JSONObject trackJson = new JSONObject();

        trackJson.put(Names.Track.ID, ID);
        trackJson.put("orderId", ORDER_ID);
        trackJson.put("deliveryId", DELIVERY_ID);
        trackJson.put(Names.Track.TRACK_CODE, TRACK_CODE);
        trackJson.put(Names.Track.DELIVERY_SERVICE_ID, DELIVERY_SERVICE_ID);
        trackJson.put(Names.Track.TRACKER_ID, TRACKER_ID);
        trackJson.put(Names.Track.STATUS, STATUS.name());

        return trackJson;
    }
}
