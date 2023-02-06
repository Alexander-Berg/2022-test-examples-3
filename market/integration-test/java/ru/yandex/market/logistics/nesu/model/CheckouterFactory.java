package ru.yandex.market.logistics.nesu.model;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.common.rest.Pager;

@ParametersAreNonnullByDefault
public final class CheckouterFactory {

    private CheckouterFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static OrderSearchRequest checkouterSearchRequest(Long... orderIds) {
        return OrderSearchRequest.builder()
            .withPageInfo(Pager.atPage(1, 50))
            .withRgbs(new Color[]{Color.BLUE})
            .withPartials(new OptionalOrderPart[]{OptionalOrderPart.DELIVERY_PARCELS})
            .withOrderIds(orderIds)
            .build();
    }

    @Nonnull
    public static Order checkouterOrder(long id, OrderSubstatus status) {
        Parcel parcel = new Parcel();
        parcel.setBoxes(List.of(new ParcelBox()));

        Delivery delivery = new Delivery();
        delivery.setParcels(List.of(parcel));

        Order checkouterOrder = new Order();
        checkouterOrder.setId(id);
        checkouterOrder.setSubstatus(status);
        checkouterOrder.setDelivery(delivery);
        return checkouterOrder;
    }
}
