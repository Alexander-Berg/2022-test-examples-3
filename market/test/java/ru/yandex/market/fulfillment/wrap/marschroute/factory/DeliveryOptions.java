package ru.yandex.market.fulfillment.wrap.marschroute.factory;

import ru.yandex.market.fulfillment.wrap.marschroute.model.base.DeliveryId;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.delivery.city.DeliveryOption;

import java.time.LocalDate;

import static ru.yandex.market.fulfillment.wrap.marschroute.factory.MarschrouteDates.marschrouteDates;

public class DeliveryOptions {

    public static final String COURIER_OPTION = "COURIER";
    public static final String PICKUP_OPTION = "PICKUP";
    public static final String POST_OPTION = "POST";

    private DeliveryOptions() {
    }

    public static DeliveryOption courierDeliveryOption() {
        return courierDeliveryOption(
                "COURIER_EXAMPLE",
                LocalDate.of(1999, 9, 9),
                100
        );
    }

    public static DeliveryOption courierDeliveryOption(String name, LocalDate deliveryDate, int price) {
        return new DeliveryOption()
                .setName(name)
                .setCost(price)
                .setDeliveryId(DeliveryId.OUTSOURCE_COURIER.getValue())
                .setPossibleDates(marschrouteDates(deliveryDate));
    }

    public static DeliveryOption postDeliveryOption(DeliveryId deliveryId) {
        return new DeliveryOption()
                .setName("Почта РФ")
                .setCost(200)
                .setDeliveryCode("RUPOST")
                .setDeliveryId(deliveryId.getValue());
    }

    public static DeliveryOption pickupDeliveryOption() {
        return new DeliveryOption()
                .setName("Boxberry ПВЗ")
                .setCost(2000)
                .setDeliveryCode("BXBRY")
                .setDeliveryId(DeliveryId.PICKUP_POINT.getValue());
    }
}
