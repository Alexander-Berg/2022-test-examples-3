package ru.yandex.market.delivery.mdbapp.integration.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import steps.orderSteps.OrderEventSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsRouter.CHANNEL_CHECKPOINT_CHANGED;
import static steps.orderSteps.ChangeRequestSteps.createChangeRequest;

@RunWith(Parameterized.class)
public class GoldPartnerOrderEventsRouterTest {

    @Parameterized.Parameter
    public OrderHistoryEvent orderEvent;
    @Parameterized.Parameter(1)
    public String expectedChannel;
    private final GoldPartnerOrderEventsRouter router = new GoldPartnerOrderEventsRouter();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {
                createEvent(null, null, OrderStatus.UNPAID, OrderStatus.PROCESSING),
                GoldPartnerOrderEventsRouter.CHANNEL_GOLD_PARTNER_ORDER_CREATE
            },
            {
                createEventAfterRepairing(null, null, OrderStatus.PROCESSING, OrderStatus.PROCESSING),
                GoldPartnerOrderEventsRouter.CHANNEL_GOLD_PARTNER_ORDER_CREATE
            },
            {
                createEventAfterRepairingWithCancellation(),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                createEvent(null, "someTrack", OrderStatus.UNPAID, OrderStatus.PROCESSING),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                createEvent("someTrack", null, OrderStatus.UNPAID, OrderStatus.PROCESSING),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                createEvent(null, null, OrderStatus.PROCESSING, OrderStatus.PROCESSING),
                OrderEventsByTypeRouter.CHANNEL_DISCARDED
            },
            {
                checkpointChanged(),
                CHANNEL_CHECKPOINT_CHANGED
            },
        });
    }

    @Nonnull
    private static OrderHistoryEvent createEvent(
        String trackCodeBefore,
        String trackCodeAfter,
        OrderStatus statusBefore,
        OrderStatus statusAfter
    ) {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderBefore(createOrder(trackCodeBefore, statusBefore));
        orderHistoryEvent.setOrderAfter(createOrder(trackCodeAfter, statusAfter));

        return orderHistoryEvent;
    }

    @Nonnull
    private static Order createOrder(String trackCode, OrderStatus status) {
        Order order = new Order();
        order.setStatus(status);
        order.setDelivery(createDelivery(trackCode));

        return order;
    }

    @Nonnull
    @SuppressWarnings("SameParameterValue")
    private static OrderHistoryEvent createEventAfterRepairing(
        String trackCodeBefore,
        String trackCodeAfter,
        OrderStatus statusBefore,
        OrderStatus statusAfter
    ) {
        OrderHistoryEvent orderHistoryEvent = createEvent(trackCodeBefore, trackCodeAfter, statusBefore, statusAfter);
        orderHistoryEvent.setType(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED);
        orderHistoryEvent.setOrderAfter(createOrderAfterRepairing(trackCodeAfter, statusAfter));

        return orderHistoryEvent;
    }

    @Nonnull
    private static OrderHistoryEvent createEventAfterRepairingWithCancellation() {
        OrderHistoryEvent orderHistoryEvent = createEvent(null, null, OrderStatus.PROCESSING, OrderStatus.CANCELLED);
        orderHistoryEvent.setType(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED);
        var orderAfter = createOrderAfterRepairing(null, OrderStatus.CANCELLED);
        var changeRequest = createChangeRequest(
            new AbstractChangeRequestPayload(ChangeRequestType.PARCEL_CANCELLATION) {
            }
        );
        orderAfter.setChangeRequests(List.of(changeRequest));
        orderHistoryEvent.setOrderAfter(orderAfter);

        return orderHistoryEvent;
    }

    @Nonnull
    private static OrderHistoryEvent checkpointChanged() {
        OrderHistoryEvent orderEvent = createEvent(null, "someTrack", OrderStatus.UNPAID, OrderStatus.PROCESSING);
        orderEvent.setType(HistoryEventType.TRACK_CHECKPOINT_CHANGED);
        return orderEvent;
    }

    @Nonnull
    private static Order createOrderAfterRepairing(String trackCode, OrderStatus status) {
        Order order = createOrder(trackCode, status);
        order.setChangeRequests(Collections.singletonList(OrderEventSteps.deliveryOptionChangeRequest()));

        return order;
    }

    @Nonnull
    private static Delivery createDelivery(String trackCode) {
        Delivery delivery = new Delivery();
        delivery.setTracks(new ArrayList<>());
        if (trackCode == null) {
            return delivery;
        }
        Track track = new Track();
        track.setTrackCode(trackCode);

        Parcel s = new Parcel();
        s.setTracks(Collections.singletonList(track));
        delivery.setParcels(Collections.singletonList(s));

        return delivery;
    }

    @Test
    public void testRouting() {
        assertThat(router.route(orderEvent)).as("Result channel is not correct").isEqualTo(expectedChannel);
    }

}
