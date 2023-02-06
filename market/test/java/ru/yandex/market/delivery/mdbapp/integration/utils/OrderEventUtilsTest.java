package ru.yandex.market.delivery.mdbapp.integration.utils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.delivery.mdbapp.util.OrderEventUtils;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class OrderEventUtilsTest {

    @Test
    public void isCreateOrderRouteTrueTest() {
        assertThat(OrderEventUtils.isCreateOrderRoute(getCreateFullOrderEvent()))
            .as("OrderEventUtils must indicate create full order event")
            .isTrue();
    }

    @Test
    public void isCreateOrderRouteFalseTest() {
        assertThat(OrderEventUtils.isCreateOrderRoute(getGetFullOrderLabel()))
            .as("OrderEventUtils must not indicate create full order event")
            .isFalse();
    }

    @Test
    public void isCreateOrderRouteWithDeletedTrackCodeTrueTest() {
        assertThat(OrderEventUtils.isCreateOrderRoute(getCreateFullOrderWithDeletedTrackEvent()))
            .as("isCreateOrderRoute must indicate create order with deleted track event")
            .isTrue();
    }

    @Test
    public void isCreateOrderRouteWithTrackCodesFalseTest() {
        assertThat(OrderEventUtils.isCreateOrderRoute(getCreateFullOrderWithTracksEvent()))
            .as("isCreateOrderRoute must not indicate create order with tracks in event")
            .isFalse();
    }

    @Test
    public void isGetLabelsRouteTrueTest() {
        assertThat(OrderEventUtils.isDeliveryStatusChanged(getGetFullOrderLabel(), ParcelStatus.CREATED))
            .as("OrderEventUtils must indicate get labels event")
            .isTrue();
    }

    @Test
    public void isGetLabelsRouteFalseTest() {
        assertThat(OrderEventUtils.isDeliveryStatusChanged(getCreateFullOrderEvent(), ParcelStatus.CREATED))
            .as("OrderEventUtils must not indicate get labels event")
            .isFalse();
    }

    @Test
    public void isCreateOrderMultiParcelRouteSingleParcelTrue() {
        assertThat(OrderEventUtils.isCreateParcelOrderRoute(getCreateMultiParcelOrderEvent()))
            .as("isCreateParcelOrderRoute must indicate create multiparcel order event with single parcel")
            .isTrue();
    }

    @Test
    public void getChangeRequestsDiffReturnsEmptyIfNoChange() {
        OrderHistoryEvent testEvent = new OrderHistoryEvent();
        Order testOrder = new Order();
        testOrder.setChangeRequests(List.of(
            mockChangeRequest(1, ChangeRequestType.DELIVERY_OPTION),
            mockChangeRequest(2, ChangeRequestType.DELIVERY_OPTION)
        ));

        testEvent.setOrderAfter(testOrder);
        testEvent.setOrderBefore(testOrder);
        assertThat(OrderEventUtils.getChangeRequestsDiff(testEvent))
            .as("getChangeRequestsDiff must return an empty set if the change requests are the same")
            .isEmpty();
    }

    @Test
    public void getChangeRequestsDiff() {
        Set<ChangeRequest> diff = Set.of(
            mockChangeRequest(11, ChangeRequestType.DELIVERY_OPTION),
            mockChangeRequest(12, ChangeRequestType.CANCELLATION)
        );

        OrderHistoryEvent testEvent = new OrderHistoryEvent();
        Order testOrderBefore = new Order();
        testOrderBefore.setChangeRequests(List.of(
            mockChangeRequest(1, ChangeRequestType.DELIVERY_ADDRESS),
            mockChangeRequest(2, ChangeRequestType.DELIVERY_OPTION)
        ));

        Order testOrderAfter = new Order();
        List<ChangeRequest> newChangeRequests = new ArrayList(testOrderBefore.getChangeRequests());
        newChangeRequests.addAll(diff);
        testOrderAfter.setChangeRequests(newChangeRequests);

        testEvent.setOrderBefore(testOrderBefore);
        testEvent.setOrderAfter(testOrderAfter);
        assertThat(OrderEventUtils.getChangeRequestsDiff(testEvent))
            .as("getChangeRequestsDiff must return the difference in the sets of change requests")
            .isEqualTo(diff);
    }

    @Test
    public void isParcelCancelChangeRequestCreatedReturnEmpty() {
        Set<ChangeRequest> diff = Set.of(
            mockChangeRequest(12, ChangeRequestType.DELIVERY_OPTION)
        );

        OrderHistoryEvent testEvent = new OrderHistoryEvent();
        Order testOrderBefore = new Order();
        testOrderBefore.setChangeRequests(List.of(
            mockChangeRequest(1, ChangeRequestType.PARCEL_CANCELLATION)
        ));

        Order testOrderAfter = new Order();
        List<ChangeRequest> newChangeRequests = new ArrayList(testOrderBefore.getChangeRequests());
        newChangeRequests.addAll(diff);
        testOrderAfter.setChangeRequests(newChangeRequests);

        testEvent.setOrderBefore(testOrderBefore);
        testEvent.setOrderAfter(testOrderAfter);
        assertThat(OrderEventUtils.isParcelCancelChangeRequestCreated(testEvent))
            .as("isCreateParcelOrderRoute must indicate that new change request does not have PARCEL_CANCELLATION")
            .isFalse();
    }

    @Test
    public void isParcelCancelChangeRequestCreated() {
        OrderHistoryEvent testEvent = new OrderHistoryEvent();
        Order testOrderBefore = new Order();
        testOrderBefore.setChangeRequests(List.of(
            mockChangeRequest(1, ChangeRequestType.DELIVERY_OPTION)
        ));

        Order testOrderAfter = new Order();
        List<ChangeRequest> newChangeRequests = new ArrayList(testOrderBefore.getChangeRequests());
        newChangeRequests.add(mockChangeRequest(2, ChangeRequestType.PARCEL_CANCELLATION));
        testOrderAfter.setChangeRequests(newChangeRequests);

        testEvent.setOrderBefore(testOrderBefore);
        testEvent.setOrderAfter(testOrderAfter);
        assertThat(OrderEventUtils.isParcelCancelChangeRequestCreated(testEvent))
            .as("isCreateParcelOrderRoute must indicate that new change request has PARCEL_CANCELLATION")
            .isTrue();
    }

    private static ChangeRequest mockChangeRequest(long id,  ChangeRequestType requestType) {
        return new ChangeRequest(
            id,
            1L,
            new AbstractChangeRequestPayload(requestType) {
            },
            ChangeRequestStatus.PROCESSING,
            Instant.MIN,
            "",
            ClientRole.SYSTEM
        );
    }

    private OrderHistoryEvent getCreateFullOrderEvent() {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(12L);
        orderHistoryEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);

        Order orderBefore = OrderSteps.getFilledOrder();
        orderBefore.setStatus(OrderStatus.PENDING);
        orderBefore.getDelivery().getParcels().get(0).setTracks(Collections.emptyList());
        Order orderAfter = OrderSteps.getFilledOrder();
        orderAfter.setStatus(OrderStatus.PROCESSING);
        orderAfter.getDelivery().getParcels().get(0).setTracks(Collections.emptyList());

        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);

        return orderHistoryEvent;
    }

    private OrderHistoryEvent getCreateFullOrderWithDeletedTrackEvent() {
        OrderHistoryEvent event = getCreateFullOrderEvent();

        event.getOrderBefore().getDelivery().getParcels().get(0)
            .setTracks(Collections.singletonList(new Track("TRACK_CODE", 1L)));
        return event;
    }

    private OrderHistoryEvent getCreateFullOrderWithTracksEvent() {
        OrderHistoryEvent event = getCreateFullOrderEvent();

        List<Track> tracks = Collections.singletonList(new Track("TRACK_CODE", 1L));
        event.getOrderBefore().getDelivery().getParcels().get(0)
            .setTracks(tracks);
        event.getOrderAfter().getDelivery().getParcels().get(0)
            .setTracks(tracks);
        return event;
    }

    private OrderHistoryEvent getCreateMultiParcelOrderEvent() {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(12L);
        orderHistoryEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);

        Order orderBefore = OrderSteps.getRedMultipleOrder();
        orderBefore.setStatus(OrderStatus.PENDING);
        orderBefore.getDelivery().getParcels().forEach(parcel -> parcel.setTracks(Collections.emptyList()));
        Order orderAfter = OrderSteps.getRedMultipleOrder();
        orderAfter.setStatus(OrderStatus.PROCESSING);
        orderAfter.getDelivery().getParcels().forEach(parcel -> parcel.setTracks(Collections.emptyList()));
        orderAfter.getDelivery().getParcels().get(0).setWeight(10L);
        orderAfter.getDelivery().getParcels().get(0).setParcelItems(Collections.singletonList(new ParcelItem(1L, 1)));

        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);

        return orderHistoryEvent;
    }

    private OrderHistoryEvent getGetFullOrderLabel() {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(12L);
        orderHistoryEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);

        Order orderBefore = OrderSteps.getFilledOrder();
        orderBefore.setStatus(OrderStatus.PROCESSING);
        orderBefore.getDelivery().getParcels().get(0).setStatus(ParcelStatus.NEW);
        orderBefore.getDelivery().getParcels().get(0).setTracks(Collections.emptyList());
        Order orderAfter = OrderSteps.getFilledOrder();
        orderAfter.setStatus(OrderStatus.PROCESSING);
        orderAfter.getDelivery().getParcels().get(0).setStatus(ParcelStatus.CREATED);
        orderAfter.getDelivery().getParcels().get(0).setTracks(Collections.emptyList());

        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);

        return orderHistoryEvent;
    }
}
