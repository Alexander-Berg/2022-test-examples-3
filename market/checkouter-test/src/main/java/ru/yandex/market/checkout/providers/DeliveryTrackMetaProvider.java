package ru.yandex.market.checkout.providers;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;

import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrackMeta;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrackStatus;

public abstract class DeliveryTrackMetaProvider {

    public static DeliveryTrackMeta getDeliveryTrackMeta(String orderId) {
        return getDeliveryTrackMeta(orderId, 100500L, "iddqd");
    }

    public static DeliveryTrackMeta getDeliveryTrackMeta(String orderId, long trackId, String trackCode) {
        DeliveryTrackMeta deliveryTrackMeta = new DeliveryTrackMeta();
        deliveryTrackMeta.setId(trackId);
        deliveryTrackMeta.setTrackCode(trackCode);
        deliveryTrackMeta.setBackUrl("http://checkouter.tst.vs.market.yandex.net:39001/");
        deliveryTrackMeta.setDeliveryServiceId(123);
        deliveryTrackMeta.setConsumerId(100502);
        deliveryTrackMeta.setSourceId(1L);
        deliveryTrackMeta.setStartDate(null);
        deliveryTrackMeta.setLastUpdatedDate(Date.from(
                LocalDateTime.of(2017, Month.FEBRUARY, 27, 4, 35, 26)
                        .atZone(ZoneId.of("Europe/Moscow"))
                        .toInstant()));
        deliveryTrackMeta.setLastNotifySuccessDate(Date.from(
                LocalDateTime.of(2017, Month.FEBRUARY, 27, 4, 35, 26)
                        .atZone(ZoneId.of("Europe/Moscow"))
                        .toInstant()));
        deliveryTrackMeta.setNextRequestDate(Date.from(
                LocalDateTime.of(2017, Month.FEBRUARY, 27, 4, 35, 26)
                        .atZone(ZoneId.of("Europe/Moscow"))
                        .toInstant()));
        deliveryTrackMeta.setOrderId(orderId);
        deliveryTrackMeta.setDeliveryTrackStatus(DeliveryTrackStatus.STARTED);
        return deliveryTrackMeta;
    }
}
