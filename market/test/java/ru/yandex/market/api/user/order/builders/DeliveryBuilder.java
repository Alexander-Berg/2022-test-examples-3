package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;

import java.util.Collections;
import java.util.List;

public class DeliveryBuilder extends RandomBuilder<Delivery> {
    private Delivery delivery = new Delivery();

    @Override
    public DeliveryBuilder random() {
        delivery.setDeliveryDates(new DeliveryDatesBuilder().random().build());
        return this;
    }

    public DeliveryBuilder courier() {
        delivery.setType(DeliveryType.DELIVERY);
        return this;
    }

    public DeliveryBuilder outlet(ShopOutletBuilder builder) {
        delivery.setType(DeliveryType.PICKUP);
        ShopOutlet outlet = builder.build();
        delivery.setOutlet(outlet);
        delivery.setOutlets(Collections.singletonList(outlet));
        return this;
    }

    public DeliveryBuilder outletId(long id) {
        delivery.setType(DeliveryType.PICKUP);
        delivery.setOutletId(id);
        return this;
    }

    public DeliveryBuilder shipments(List<Parcel> shipments) {
        delivery.setParcels(shipments);
        return this;
    }

    @Override
    public Delivery build() {
        return delivery;
    }
}
