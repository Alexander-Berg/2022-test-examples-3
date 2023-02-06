package ru.yandex.market.checkout.test.providers;

import java.util.function.Consumer;

import com.google.common.collect.Lists;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;

/**
 * Создает Delivery для запросов POST /orders/{}/delivery.
 */
public abstract class DeliveryUpdateProvider {

    public static Delivery createDeliveryUpdate(Consumer<Delivery> configurer) {
        Delivery delivery = new Delivery();
        configurer.accept(delivery);
        return delivery;
    }

    public static Delivery createDeliveryUpdateWithShipments(Parcel... shipments) {
        return createDeliveryUpdate(d -> {
            d.setParcels(Lists.newArrayList(shipments));
        });
    }

    public static Delivery createDeliveryUpdateWithParcels(Parcel... parcels) {
        return createDeliveryUpdate(d -> {
            d.setParcels(Lists.newArrayList(parcels));
        });
    }

}
