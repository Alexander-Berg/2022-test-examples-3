package ru.yandex.market.delivery.mdbapp.integration.router;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;

import static org.junit.Assert.assertEquals;
import static steps.orderSteps.OrderEventSteps.cancelledEvent;

@RunWith(Parameterized.class)
public class PostOrderEventsRouterTest {

    private static final String TEST_CODE = "Code";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Parameterized.Parameter
    public OrderHistoryEvent orderEvent;

    @Parameterized.Parameter(1)
    public String expectedChannel;

    private PostOrderEventsRouter router = new PostOrderEventsRouter();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {
                createOrderEvent(),
                PostOrderEventsRouter.CHANNEL_POST_CREATE_ORDER
            },
            {
                canceledOrderEvent(),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                cancellationRequestParcelEvent(),
                OrderEventsRouter.CHANNEL_CANCEL_PARCEL_DELAYED
            },
            {
                updateOrderEvent(),
                PostOrderEventsRouter.CHANNEL_POST_UPDATE_ORDER
            },
            {
                cancelledEvent(),
                OrderEventsRouter.CHANNEL_FIRST_TIME_CANCELLED_ORDER
            },
        });
    }

    @Test
    public void testRouting() {
        assertEquals(
            "Result channel is not correct",
            expectedChannel,
            router.route(orderEvent)
        );
    }

    private static OrderHistoryEvent createOrderEvent() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();
        orderEvent.setOrderBefore(createPostOrder());
        orderEvent.setOrderAfter(createPostOrder(createParcel(), OrderStatus.PROCESSING));
        return orderEvent;
    }

    private static OrderHistoryEvent canceledOrderEvent() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();
        Track track = new Track();
        track.setTrackCode(TEST_CODE);

        orderEvent.setOrderBefore(createPostOrder());
        orderEvent.setOrderAfter(createPostOrder(createParcel(track), OrderStatus.CANCELLED));
        return orderEvent;
    }

    private static OrderHistoryEvent cancellationRequestParcelEvent() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();
        orderEvent.setType(HistoryEventType.PARCEL_CANCELLATION_REQUESTED);
        Track track = new Track();
        track.setTrackCode(TEST_CODE);

        orderEvent.setOrderBefore(createPostOrder());
        orderEvent.setOrderAfter(createPostOrder(createParcel(track), OrderStatus.PROCESSING));
        return orderEvent;
    }

    private static OrderHistoryEvent updateOrderEvent() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();

        Track track = new Track();
        track.setTrackCode(TEST_CODE);

        Parcel parcel = createParcel(track);
        parcel.setWeight(2L);
        orderEvent.setOrderBefore(createPostOrder(createParcel(), OrderStatus.PROCESSING));
        orderEvent.setOrderAfter(createPostOrder(parcel, OrderStatus.PROCESSING));
        return orderEvent;
    }

    private static Order createPostOrder() {
        Order order = OrderSteps.getNotFakeOrder();
        order.setFulfilment(true);
        order.setRgb(Color.BLUE);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        delivery.setType(DeliveryType.POST);
        order.setDelivery(delivery);
        return order;
    }

    private static Order createPostOrder(Parcel parcel, OrderStatus status) {
        Order order = createPostOrder();
        order.setStatus(status);
        order.getDelivery().setParcels(Collections.singletonList(parcel));
        return order;
    }

    private static Parcel createParcel() {
        Parcel parcel = new Parcel();
        parcel.setDepth(1L);
        parcel.setWidth(1L);
        parcel.setHeight(1L);
        parcel.setWeight(1L);
        return parcel;
    }

    private static Parcel createParcel(Track track) {
        Parcel parcel = createParcel();
        parcel.setTracks(Collections.singletonList(track));
        return parcel;
    }
}
