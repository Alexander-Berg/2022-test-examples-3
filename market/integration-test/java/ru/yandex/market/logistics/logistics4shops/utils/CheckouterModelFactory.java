package ru.yandex.market.logistics.logistics4shops.utils;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;

@UtilityClass
@ParametersAreNonnullByDefault
public class CheckouterModelFactory {
    @Nonnull
    public Order buildOrderWithParcels(List<Parcel> parcels) {
        Delivery delivery = new Delivery();
        delivery.setParcels(parcels);

        Order resultOrder = new Order();
        resultOrder.setDelivery(delivery);
        return resultOrder;
    }

    @Nonnull
    public Order buildOrderWithDefaultParcel() {
        Parcel parcel = new Parcel();
        parcel.setId(101L);

        return buildOrderWithParcels(List.of(parcel));
    }

    @Nonnull
    public RequestClientInfo requestClientInfo(ClientRole clientRole) {
        return RequestClientInfo.builder(clientRole).build();
    }

    @Nonnull
    public OrderRequest orderRequest(long orderId) {
        return OrderRequest.builder(orderId).build();
    }
}
