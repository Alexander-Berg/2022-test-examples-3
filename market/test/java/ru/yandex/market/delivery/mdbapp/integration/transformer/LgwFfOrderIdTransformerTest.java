package ru.yandex.market.delivery.mdbapp.integration.transformer;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.delivery.mdbapp.components.queue.parcel.OrderParcelDto;
import ru.yandex.market.delivery.mdbapp.integration.payload.CancelLgwFfOrder;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;

import static org.junit.Assert.assertEquals;

public class LgwFfOrderIdTransformerTest {
    private static final long ORDER_ID = 100;

    private static final long FULFILLMENT_ID = 145;

    private static final String TRACK_CODE = "EXT12345";

    private static final ResourceId LGW_ORDER_ID = ResourceId.builder()
        .setYandexId(String.valueOf(ORDER_ID))
        .setPartnerId(TRACK_CODE)
        .build();

    private static final Partner LGW_PARTNER = new Partner(FULFILLMENT_ID);

    private LgwFfOrderIdTransformer lgwFfOrderIdTransformer;

    @Before
    public void init() {
        lgwFfOrderIdTransformer = new LgwFfOrderIdTransformer();
    }

    @Test
    public void testCancelFulfillmentOrderIntegratedWithLgw() {
        CancelLgwFfOrder transformed = lgwFfOrderIdTransformer.transform(
            new OrderParcelDto(createOrder(), createParcel()));

        assertEquals(LGW_ORDER_ID, transformed.getOrderId());
        assertEquals(LGW_PARTNER, transformed.getPartner());
    }

    private static Order createOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setShopId(101L);
        order.setDelivery(createDelivery());
        order.setItems(Collections.singleton(createItem()));
        return order;
    }

    private static OrderItem createItem() {
        OrderItem orderItem = new OrderItem();
        orderItem.setWarehouseId(Math.toIntExact(FULFILLMENT_ID));
        orderItem.setFulfilmentWarehouseId(FULFILLMENT_ID);
        return orderItem;
    }

    private static Delivery createDelivery() {
        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(createParcel()));
        return delivery;
    }

    private static Parcel createParcel() {
        Parcel parcel = new Parcel();
        parcel.setTracks(Collections.singletonList(createTrack()));
        return parcel;
    }

    private static Track createTrack() {
        Track track = new Track();
        track.setDeliveryServiceId(FULFILLMENT_ID);
        track.setTrackCode(TRACK_CODE);
        return track;
    }
}
