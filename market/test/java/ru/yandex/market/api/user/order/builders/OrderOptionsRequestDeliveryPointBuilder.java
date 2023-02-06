package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.api.user.order.checkout.DeliveryPointId;
import ru.yandex.market.api.user.order.preorder.OrderOptionsRequest;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OrderOptionsRequestDeliveryPointBuilder extends RandomBuilder<OrderOptionsRequest.DeliveryPoint> {

    OrderOptionsRequest.DeliveryPoint deliveryPoint;

    public OrderOptionsRequestDeliveryPointBuilder(Class<? extends OrderOptionsRequest.DeliveryPoint> clazz) {
        if (clazz.isAssignableFrom(OrderOptionsRequest.AddressDeliveryPoint.class)) {
            this.deliveryPoint = new OrderOptionsRequest.AddressDeliveryPoint();
        } else  if (clazz.isAssignableFrom(OrderOptionsRequest.OutletDeliveryPoint.class)) {
            this.deliveryPoint = new OrderOptionsRequest.OutletDeliveryPoint();
        } else {
            throw new IllegalArgumentException("Unknown delivery class");
        }
    }

    @Override
    public OrderOptionsRequestDeliveryPointBuilder random() {
        if (deliveryPoint instanceof OrderOptionsRequest.AddressDeliveryPoint) {
            OrderOptionsRequest.AddressDeliveryPoint addressDeliveryPoint = (OrderOptionsRequest.AddressDeliveryPoint) this.deliveryPoint;

            addressDeliveryPoint.setCountry(random.getString());
            addressDeliveryPoint.setCity(random.getString());
            addressDeliveryPoint.setStreet(random.getString());
            addressDeliveryPoint.setBlock(random.getString());
            addressDeliveryPoint.setHouse(random.getString());
            addressDeliveryPoint.setFloor(random.getString());
            addressDeliveryPoint.setPostCode(random.getString());
            addressDeliveryPoint.setSubway(random.getString());
        } else if (deliveryPoint instanceof OrderOptionsRequest.OutletDeliveryPoint) {
            OrderOptionsRequest.OutletDeliveryPoint outletDeliveryPoint = (OrderOptionsRequest.OutletDeliveryPoint) this.deliveryPoint;

            outletDeliveryPoint.setOutletId(random.getInt(10000));
        } else {
            throw new IllegalArgumentException("Unknown delivery class");
        }

        return this;
    }

    public OrderOptionsRequestDeliveryPointBuilder withId(DeliveryPointId id) {
        deliveryPoint.setId(id);
        return this;
    }

    public OrderOptionsRequestDeliveryPointBuilder withOutletId(long outletId) {
        if (deliveryPoint instanceof OrderOptionsRequest.OutletDeliveryPoint) {
            ((OrderOptionsRequest.OutletDeliveryPoint) deliveryPoint).setOutletId(outletId);
        } else {
            throw new IllegalArgumentException("deliveryPoint is not instance of OutletDeliveryPoint");
        }
        return this;
    }

    @Override
    public OrderOptionsRequest.DeliveryPoint build() {
        return deliveryPoint;
    }
}
