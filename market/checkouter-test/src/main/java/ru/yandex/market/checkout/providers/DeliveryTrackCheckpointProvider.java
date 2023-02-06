package ru.yandex.market.checkout.providers;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrackCheckpoint;

public final class DeliveryTrackCheckpointProvider {

    public static final int DEFAULT_DELIVERY_CHECKPOINT_STATUS = 1;
    private static final AtomicLong CHECKPOINT_ID = new AtomicLong(112212L);

    private DeliveryTrackCheckpointProvider() {
    }

    public static DeliveryTrackCheckpoint deliveryTrackCheckpoint() {
        return deliveryTrackCheckpoint(DEFAULT_DELIVERY_CHECKPOINT_STATUS);
    }

    public static DeliveryTrackCheckpoint deliveryTrackCheckpoint(int deliveryCheckpointStatus) {
        return deliveryTrackCheckpoint(CHECKPOINT_ID.getAndIncrement(), deliveryCheckpointStatus);
    }

    public static DeliveryTrackCheckpoint deliveryTrackCheckpoint(
            long id, int deliveryCheckpointStatus) {
        return deliveryTrackCheckpoint(id, CheckpointStatus.DELIVERED, deliveryCheckpointStatus);
    }

    public static DeliveryTrackCheckpoint deliveryTrackCheckpoint(
            long id, CheckpointStatus status, int deliveryCheckpointStatus) {
        DeliveryTrackCheckpoint deliveryTrackCheckpoint = new DeliveryTrackCheckpoint();
        deliveryTrackCheckpoint.setId(id);
        deliveryTrackCheckpoint.setCountry("RUSSIA");
        deliveryTrackCheckpoint.setCity("MOSCOW");
        deliveryTrackCheckpoint.setLocation("MOSCOW, TX, 7976");
        deliveryTrackCheckpoint.setMessage("bla-bla");
        deliveryTrackCheckpoint.setCheckpointStatus(status);
        deliveryTrackCheckpoint.setZipCode("123456");
        deliveryTrackCheckpoint.setCheckpointDate(Date.from(
                LocalDateTime.of(2017, Month.FEBRUARY, 27, 4, 35, 26)
                        .atZone(ZoneId.of("Europe/Moscow"))
                        .toInstant()
        ));
        deliveryTrackCheckpoint.setDeliveryCheckpointStatus(deliveryCheckpointStatus);
        return deliveryTrackCheckpoint;
    }
}
