package ru.yandex.market.delivery.mdbapp.api.order.scenario;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.request.CreateOrder;

public class OrderDeliveryWasSetScenario {
    private final CreateOrder createOrder;
    private final Order order;

    public OrderDeliveryWasSetScenario(CreateOrder createOrder, Order order) {
        this.createOrder = createOrder;
        this.order = order;
    }

    public Long getShipmentId() {
        return order.getDelivery().getParcels().get(0).getShipmentId();
    }

    public CreateOrder getCreateOrderRequest() {
        return createOrder;
    }

    public Order getOrder() {
        return order;
    }
}
