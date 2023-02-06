package ru.yandex.market.checkout.pushapi.providers;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;

public abstract class ParcelProvider {
    static Parcel buildShipment() {
        Parcel shipment = new Parcel();
        shipment.setWeight(10L);
        shipment.setWidth(10L);
        shipment.setHeight(10L);
        shipment.setDepth(10L);
        shipment.setStatus(ParcelStatus.NEW);
        return shipment;
    }
}
