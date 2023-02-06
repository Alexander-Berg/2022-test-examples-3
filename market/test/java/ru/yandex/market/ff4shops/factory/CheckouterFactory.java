package ru.yandex.market.ff4shops.factory;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;

@ParametersAreNonnullByDefault
public class CheckouterFactory {

    private CheckouterFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static Order createOrder(long orderId, long deliveryServiceId) {
        Order order = new Order();
        order.setId(orderId);
        order.setDelivery(createDelivery(deliveryServiceId));
        return order;
    }

    @Nonnull
    private static Delivery createDelivery(long deliveryServiceId) {
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(deliveryServiceId);
        delivery.setParcels(List.of(createParcel()));
        return delivery;
    }

    @Nonnull
    private static Parcel createParcel() {
        Parcel parcel = new Parcel();
        parcel.setBoxes(List.of(createParcelBox()));
        return parcel;
    }

    @Nonnull
    private static ParcelBox createParcelBox() {
        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(98765L);
        parcelBox.setFulfilmentId("100500-1");
        parcelBox.setDepth(20L);
        parcelBox.setWidth(30L);
        parcelBox.setHeight(40L);
        parcelBox.setWeight(1250L);
        return parcelBox;
    }


}
