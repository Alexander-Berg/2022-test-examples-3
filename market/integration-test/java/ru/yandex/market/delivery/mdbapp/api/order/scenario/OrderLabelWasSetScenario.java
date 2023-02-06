package ru.yandex.market.delivery.mdbapp.api.order.scenario;

import java.util.Collections;
import java.util.List;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbclient.model.request.SetOrderDeliveryShipmentLabel;

public class OrderLabelWasSetScenario {

    private final SetOrderDeliveryShipmentLabel setOrderDeliveryShipmentLabel;
    private final Order order;
    private final List<Parcel> parcels;

    public OrderLabelWasSetScenario(SetOrderDeliveryShipmentLabel setOrderDeliveryShipmentLabel,
                                    Order order,
                                    Parcel parcel) {
        this.setOrderDeliveryShipmentLabel = setOrderDeliveryShipmentLabel;
        this.order = order;
        this.parcels = Collections.singletonList(parcel);
    }

    public SetOrderDeliveryShipmentLabel getSetOrderDeliveryShipmentLabel() {
        return setOrderDeliveryShipmentLabel;
    }

    public Order getOrder() {
        return order;
    }

    public List<Parcel> getOrderParcels() {
        return parcels;
    }
}
