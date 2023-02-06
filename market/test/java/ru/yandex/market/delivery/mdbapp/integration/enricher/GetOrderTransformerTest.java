package ru.yandex.market.delivery.mdbapp.integration.enricher;

import java.util.LinkedList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.integration.payload.GetLgwFfOrder;

@RunWith(Parameterized.class)
public class GetOrderTransformerTest {
    private static final String SC_OR_FF_TRACK_CODE = "scOrFfId";
    private static final String DS_TRACK_CODE = "dsId";
    private static final long SC_OR_FF_DELIVERY_SERVICE_ID = 131L;
    private static final long DS_DELIVERY_SERVICE_ID = 106L;
    private static final long YANDEX_ID = 123456L;

    private final GetOrderTransformer getOrderTransformer = new GetOrderTransformer();

    @Parameterized.Parameter
    public OrderHistoryEvent event;

    @Parameterized.Parameter(1)
    public String testName;

    @Parameterized.Parameters(name = "{1}")
    public static Object[][] data() {
        return new Object[][] {
            {
                getOrderHistoryEvent(DeliveryServiceType.SORTING_CENTER), "SC order"
            },
            {
                getOrderHistoryEvent(DeliveryServiceType.FULFILLMENT), "FF order"
            }
        };
    }

    @Test
    public void yandexIdTransform() {
        OrderHistoryEvent orderHistoryEvent = event;
        GetLgwFfOrder getOrder = getOrderTransformer.transform(orderHistoryEvent);

        Assert.assertEquals(
            "wrong yandexId",
            String.valueOf(YANDEX_ID),
            getOrder.getOrderId().getYandexId()
        );
    }

    @Test
    public void trackCodeTransform() {
        OrderHistoryEvent orderHistoryEvent = event;
        GetLgwFfOrder getOrder = getOrderTransformer.transform(orderHistoryEvent);

        Assert.assertEquals(
            "trackCode from wrong partner",
            SC_OR_FF_TRACK_CODE,
            getOrder.getOrderId().getPartnerId()
        );
    }

    @Test
    public void deliveryServiceIdTransform() {
        OrderHistoryEvent orderHistoryEvent = event;
        GetLgwFfOrder getOrder = getOrderTransformer.transform(orderHistoryEvent);

        Assert.assertEquals(
            "deliveryServiceId from wrong partner",
            String.valueOf(SC_OR_FF_DELIVERY_SERVICE_ID),
            getOrder.getPartner().getId().toString()
        );
    }

    private static OrderHistoryEvent getOrderHistoryEvent(DeliveryServiceType type) {
        final OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();

        Order orderAfter = new Order();
        Delivery delivery = new Delivery();

        LinkedList<Parcel> parcels = new LinkedList<>();
        Parcel parcel = new Parcel();

        Track trackFromFF = new Track();
        trackFromFF.setDeliveryServiceType(type);
        trackFromFF.setTrackCode(SC_OR_FF_TRACK_CODE);
        trackFromFF.setDeliveryServiceId(SC_OR_FF_DELIVERY_SERVICE_ID);

        Track trackFromDS = new Track();
        trackFromDS.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        trackFromDS.setTrackCode(DS_TRACK_CODE);
        trackFromDS.setDeliveryServiceId(DS_DELIVERY_SERVICE_ID);

        parcel.addTrack(trackFromFF);
        parcel.addTrack(trackFromDS);
        parcels.add(parcel);

        delivery.setParcels(parcels);

        orderAfter.setDelivery(delivery);
        orderAfter.setId(YANDEX_ID);

        orderHistoryEvent.setOrderAfter(orderAfter);

        return orderHistoryEvent;
    }
}
