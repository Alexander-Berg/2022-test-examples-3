package ru.yandex.market.fulfillment.wrap.marschroute.factory;

import ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking.TrackingInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking.TrackingStatus;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDateTime;

import java.time.LocalDateTime;

import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteDateTimes.marschrouteDateTime;

public class TrackingInfos {

    public static TrackingInfo[] trackingInfos(TrackingInfo... trackingInfos) {
        return trackingInfos;
    }

    public static TrackingInfo trackingInfo(String status,
                                            LocalDateTime localDateTime,
                                            TrackingStatus trackingStatus) {
        return trackingInfo(
                status,
                marschrouteDateTime(localDateTime),
                trackingStatus);
    }

    public static TrackingInfo trackingInfo(String status,
                                            MarschrouteDateTime marschrouteDateTime,
                                            TrackingStatus trackingStatus) {

        TrackingInfo trackingInfo = new TrackingInfo();
        trackingInfo.setStatus(status);
        trackingInfo.setDate(marschrouteDateTime);
        trackingInfo.setTrackingStatus(trackingStatus);

        return trackingInfo;
    }
}
