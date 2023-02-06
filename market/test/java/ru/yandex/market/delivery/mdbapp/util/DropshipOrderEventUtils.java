package ru.yandex.market.delivery.mdbapp.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import steps.ParcelSteps;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;

public final class DropshipOrderEventUtils {

    public static final Date TRAN_DATE = Date.from(Instant.parse("2019-07-20T00:00:00Z"));

    private DropshipOrderEventUtils() {
    }

    @Nonnull
    public static OrderHistoryEvent blueDropshipCreateOrderEventWithRoute() {
        return blueDropshipCreateOrderEventWithRoute(null);
    }

    @Nonnull
    public static Parcel getParcel() {
        ArrayList<Track> tracks = new ArrayList<>();
        Track track = new Track();
        track.setOrderId(OrderSteps.getFilledOrder().getId());
        track.setTrackCode("trackcode");
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        track.setDeliveryServiceId(123L);
        tracks.add(track);
        Parcel parcel = new Parcel();
        parcel.setTracks(tracks);
        return parcel;
    }

    @Nonnull
    public static OrderHistoryEvent blueDropshipCreateOrderEventWithRoute(ChangeRequest changeRequest) {
        OrderHistoryEvent event = blueDropshipCreateOrderEvent();
        Parcel parcel = getParcel();
        ParcelSteps.addRoute(parcel);
        event.getOrderAfter().getDelivery().setParcels(Collections.singletonList(parcel));
        Optional.ofNullable(changeRequest)
            .ifPresent(cr -> event.getOrderAfter().setChangeRequests(List.of(cr)));
        return event;
    }

    @Nonnull
    public static OrderHistoryEvent blueDropshipSortingCenterCreateOrderEvent(boolean isOrderWithRoute) {
        OrderHistoryEvent orderHistoryEvent = isOrderWithRoute
            ? blueDropshipCreateOrderEventWithRoute()
            : blueDropshipCreateOrderEvent();

        orderHistoryEvent.getOrderBefore().getItems().stream().findFirst().get().setWarehouseId(111);
        orderHistoryEvent.getOrderAfter().getItems().stream().findFirst().get().setWarehouseId(111);

        return orderHistoryEvent;
    }

    @Nonnull
    public static OrderHistoryEvent blueDropshipCreateOrderEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_SUBSTATUS_UPDATED);
        Order blueDropshipOrderBefore = createBlueDropshipOrder();
        blueDropshipOrderBefore.setSubstatus(OrderSubstatus.AWAIT_CONFIRMATION);

        Order blueDropshipOrderAfter = createBlueDropshipOrder();
        blueDropshipOrderAfter.setSubstatus(OrderSubstatus.STARTED);

        event.setOrderBefore(blueDropshipOrderBefore);
        event.setOrderAfter(blueDropshipOrderAfter);
        event.setTranDate(TRAN_DATE);

        return event;
    }

    @Nonnull
    public static OrderHistoryEvent blueDropshipCreateOrderEventWithParcel() {
        OrderHistoryEvent event = blueDropshipCreateOrderEvent();
        event.getOrderBefore().getDelivery().setParcels(Collections.singletonList(getParcel()));
        event.getOrderAfter().getDelivery().setParcels(Collections.singletonList(getParcel()));
        return event;
    }

    @Nonnull
    public static Order createBlueDropshipOrder() {
        Order order = OrderSteps.getFilledOrder();

        order.setRgb(Color.BLUE);
        order.setFulfilment(false);
        order.setStatus(OrderStatus.PROCESSING);

        Delivery delivery = order.getDelivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setParcels(Collections.emptyList());

        return order;
    }

    @Nonnull
    public static OrderHistoryEvent blueDropshipSortingCenterEventWithRoute(ChangeRequest changeRequest) {
        OrderHistoryEvent orderHistoryEvent = blueDropshipCreateOrderEventWithRoute(changeRequest);

        orderHistoryEvent.getOrderBefore().getItems().stream().findFirst().get().setWarehouseId(111);
        orderHistoryEvent.getOrderAfter().getItems().stream().findFirst().get().setWarehouseId(111);

        return orderHistoryEvent;
    }

    @Nonnull
    public static OrderHistoryEvent blueDropshipCreateRegisterEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_DELIVERY_UPDATED);
        Order blueDropshipOrderBefore = createBlueDropshipOrder();
        blueDropshipOrderBefore.setSubstatus(OrderSubstatus.READY_TO_SHIP);

        Order blueDropshipOrderAfter = createBlueDropshipOrder();
        blueDropshipOrderAfter.setSubstatus(OrderSubstatus.READY_TO_SHIP);
        blueDropshipOrderAfter.getDelivery().setParcels(Collections.singletonList(getParcel()));
        event.setOrderBefore(blueDropshipOrderBefore);
        event.setOrderAfter(blueDropshipOrderAfter);

        return event;
    }

    @Nonnull
    public static OrderHistoryEvent dropshipCheckpointChanged() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();

        Order order1 = DropshipOrderEventUtils.createBlueDropshipOrder();
        order1.setSubstatus(OrderSubstatus.AWAIT_CONFIRMATION);
        order1.getDelivery().setParcels(Collections.singletonList(getParcel()));

        Order order2 = DropshipOrderEventUtils.createBlueDropshipOrder();
        order2.setSubstatus(OrderSubstatus.READY_TO_SHIP);
        order2.getDelivery().setParcels(Collections.singletonList(getParcel()));

        orderEvent.setOrderBefore(order1);
        orderEvent.setOrderAfter(order2);

        Track track1 = new Track();
        track1.setTrackCode("123");
        track1.setDeliveryServiceId(999L);

        Track track2 = new Track();
        track2.setTrackCode("234");
        track2.setDeliveryServiceId(888L);

        orderEvent.getOrderAfter().getDelivery().getParcels().get(0).setTracks(List.of(track1, track2));
        orderEvent.getOrderAfter().getDelivery().getParcels().get(0).setStatus(ParcelStatus.CREATED);

        orderEvent.setType(HistoryEventType.TRACK_CHECKPOINT_CHANGED);
        return orderEvent;
    }
}
