package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;

public class ParcelBuilder extends RandomBuilder<Parcel> {
    Parcel parcel;

    public ParcelBuilder() {
        this.parcel = new Parcel();
    }

    @Override
    public ParcelBuilder random() {
        parcel.setId(random.getLong());
        parcel.setOrderId(random.getLong());
        parcel.setShipmentDate(random.getLocalDate());

        parcel.setDepth(random.getLong());
        parcel.setHeight(random.getLong());
        parcel.setWidth(random.getLong());

        parcel.setWeight(random.getLong());
        return this;
    }

    @Override
    public Parcel build() {
        return parcel;
    }
}
