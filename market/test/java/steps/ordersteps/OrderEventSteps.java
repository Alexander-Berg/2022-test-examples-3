package steps.orderSteps;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import steps.utils.DateUtils;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryOptionChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemsRemovalChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.parcel.ParcelCancelChangeRequestPayload;
import ru.yandex.market.delivery.mdbapp.integration.converter.ChangeRequestStatusConverter;
import ru.yandex.market.delivery.mdbapp.integration.payload.change.request.CancellationChangeRequestFactory;
import ru.yandex.market.delivery.mdbapp.integration.payload.change.request.ChangeRequestInternal;

public class OrderEventSteps {
    private static final boolean FAKE = false;

    private static final Long ID = 67903L;
    private static final String HOST = "host";

    private OrderEventSteps() {
    }

    public static OrderHistoryEvent getOrderHistoryEvent() {
        Order order = OrderSteps.getFilledOrder();
        order.setFake(FAKE);

        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(ID);
        orderHistoryEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        orderHistoryEvent.setAuthor(new ClientInfo(ClientRole.SYSTEM, 123L));
        orderHistoryEvent.setFromDate(DateUtils.getDate());
        orderHistoryEvent.setToDate(DateUtils.getDate());
        orderHistoryEvent.setTranDate(DateUtils.getDate());
        orderHistoryEvent.setHost(HOST);
        orderHistoryEvent.setOrderBefore(order);
        orderHistoryEvent.setOrderAfter(order);

        return orderHistoryEvent;
    }

    public static OrderHistoryEvent cancelledEvent() {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setType(HistoryEventType.ORDER_STATUS_UPDATED);

        Order orderBefore = new Order();
        orderBefore.setStatus(OrderStatus.PROCESSING);
        Order orderAfter = new Order();
        orderAfter.setStatus(OrderStatus.CANCELLED);

        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);

        return orderHistoryEvent;
    }

    @Nonnull
    public static OrderHistoryEvent createCancellationEvent() {
        return createChangeRequestCreatedEvent(List.of(
            deliveryOptionChangeRequest(),
            createCancelChangeRequest()
        ));
    }

    @Nonnull
    public static OrderHistoryEvent createChangeRequestCreatedEvent(@Nullable List<ChangeRequest> changeRequests) {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setType(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED);

        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(51L);
        delivery.setRegionId(123L);
        Parcel parcel = new Parcel();
        parcel.setId(5L);
        delivery.addParcel(parcel);

        Order orderBefore = new Order();
        Order orderAfter = new Order();
        orderBefore.setDelivery(delivery);
        orderAfter.setDelivery(delivery);
        orderAfter.setStatus(OrderStatus.PROCESSING);
        orderAfter.setChangeRequests(changeRequests);
        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);

        return orderHistoryEvent;
    }

    @Nonnull
    public static ChangeRequest createCancelChangeRequest() {
        return new ChangeRequest(
            1L,
            1L,
            new ParcelCancelChangeRequestPayload(5L, OrderSubstatus.AWAIT_CONFIRMATION, "note", null),
            ChangeRequestStatus.APPLIED,
            Instant.now(),
            "Any text",
            ClientRole.SYSTEM
        );
    }

    @Nonnull
    public static ChangeRequest createChangeRequest(AbstractChangeRequestPayload payload) {
        return createChangeRequest(1, payload);
    }

    public static ChangeRequest createChangeRequest(long id, AbstractChangeRequestPayload payload) {
        return new ChangeRequest(
            id,
            1L,
            payload,
            ChangeRequestStatus.APPLIED,
            Instant.now(),
            "Any text",
            ClientRole.SYSTEM
        );
    }

    @Nonnull
    public static ChangeRequest deliveryOptionChangeRequest() {
        return createChangeRequest(new DeliveryOptionChangeRequestPayload());
    }

    @Nonnull
    public static ChangeRequest userMovedDeliveryDatesDeliveryOptionChangeRequest() {
        DeliveryOptionChangeRequestPayload changeRequestPayload = new DeliveryOptionChangeRequestPayload();
        changeRequestPayload.setReason(HistoryEventReason.USER_MOVED_DELIVERY_DATES);
        return createChangeRequest(changeRequestPayload);
    }

    @Nonnull
    public static ChangeRequestInternal<?> createInternalCancelChangeRequest() {
        return new CancellationChangeRequestFactory(new ChangeRequestStatusConverter())
            .create(createCancellationEvent(), createCancelChangeRequest());
    }

    @Nonnull
    public static Order buildBeruDropshipOrderWithCancelRequest(
        Long orderId,
        Long shopId,
        Long parcelId,
        Long cancelRequestId
    ) {
        return buildBeruDropshipOrderWithCancelRequest(
            orderId,
            shopId,
            parcelId,
            cancelRequestId,
            OrderSubstatus.CUSTOM
        );
    }

    @Nonnull
    public static Order buildBeruDropshipOrderWithCancelRequest(
        Long orderId,
        Long shopId,
        Long parcelId,
        Long cancelRequestId,
        OrderSubstatus substatus
    ) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.PROCESSING);
        order.setFulfilment(false);
        order.setRgb(Color.BLUE);
        order.setShopId(shopId);

        OrderItem item = new OrderItem();
        item.setFulfilmentWarehouseId(1L);
        item.setWarehouseId(2);
        order.addItem(item);

        Delivery delivery = new Delivery();

        Parcel parcel = new Parcel();
        parcel.setId(parcelId);
        Track track = new Track();
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        parcel.addTrack(track);
        delivery.addParcel(parcel);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        order.setDelivery(delivery);

        order.setChangeRequests(List.of(
            new ChangeRequest(
                cancelRequestId,
                orderId,
                new ParcelCancelChangeRequestPayload(parcelId, substatus, "", Collections.emptyList()),
                ChangeRequestStatus.NEW,
                Instant.MIN,
                "",
                ClientRole.UNKNOWN
            )
        ));

        return order;
    }

    @Nonnull
    public static ChangeRequest itemsRemovalChangeRequest(long id) {
        return createChangeRequest(id, new ItemsRemovalChangeRequestPayload(List.of(), List.of()));
    }
}
