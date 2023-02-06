package ru.yandex.market.delivery.mdbapp.integration.router;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import steps.orderSteps.BuyerSteps;
import steps.orderSteps.OrderSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tariff.TariffData;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrderMilestoneTimingsTskvLogger;
import ru.yandex.market.logistics.logging.backlog.BackLogWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static steps.orderSteps.ChangeRequestSteps.deliveryOptionChangeRequest;
import static steps.orderSteps.OrderEventSteps.itemsRemovalChangeRequest;
import static steps.orderSteps.OrderEventSteps.userMovedDeliveryDatesDeliveryOptionChangeRequest;

@RunWith(Parameterized.class)
public class OrderEventsRouterTest {

    private static final Date TRAN_DATE = Date.from(Instant.parse("2019-07-20T00:00:00Z"));
    private static final long POSTAMAT_PARTNER_ID_1 = 1111L;
    private static final long POSTAMAT_PARTNER_ID_2 = 789L;

    @InjectMocks
    private OrderEventsRouter orderEventsRoute;
    @Mock
    private BackLogOrderMilestoneTimingsTskvLogger tskvLogger;
    @Mock
    private Logger logger;

    @Parameterized.Parameter
    public OrderHistoryEvent event;

    @Parameterized.Parameter(1)
    public String expectedChannelName;

    @Parameterized.Parameter(2)
    public String caseName;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> getParameters() {
        Collection<Object[]> parameters = new ArrayList<>();

        parameters.add(new Object[]{
            getWeightChangedEvent(),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "filled weight"
        });
        parameters.add(new Object[]{
            getFilledDimensionEvent(),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "filled dimension"
        });
        parameters.add(new Object[]{
            getChangeStatusEvent(),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "set PROCESSING status"
        });
        parameters.add(new Object[]{
            getRedCreateOrderEventWithoutPersonalDataGatheredWhenNotNecessary(),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "red order without PD and with needPersonalData=false"
        });
        parameters.add(new Object[]{
            getRepairedOrderEvent(),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "repaired order"
        });
        parameters.add(new Object[]{
            getRedCreateOrderEventWithoutPersonalDataGatheredWhenNecessary(),
            OrderEventsByTypeRouter.CHANNEL_DISCARDED,
            "red order without PD and with needPersonalData=true"
        });
        parameters.add(new Object[]{
            getRedCreateOrderEventWithPersonalDataGathered(),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "red order with PD"
        });
        parameters.add(new Object[]{
            getPersonalDataStatusChangedEvent(),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "set PD status"
        });
        parameters.add(new Object[]{
            getPostamatFlowTrackChange(),
            OrderEventsByTypeRouter.CHANNEL_DISCARDED,
            "discard track changed for postamat flow"
        });
        parameters.add(new Object[]{
            getPostamatCreateOrderEvent(POSTAMAT_PARTNER_ID_2),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "postamat create order event when combinator flow enabled"
        });
        parameters.add(new Object[]{
            getCreateCrossdockOrderEvent(),
            OrderEventsRouter.CHANNEL_CREATE_CROSSDOCK_ORDER_WITH_ROUTE,
            "crossdock create order"
        });
        parameters.add(new Object[]{
            getCreateCrossdockOrderEventWithRoute(),
            OrderEventsRouter.CHANNEL_CREATE_CROSSDOCK_ORDER_WITH_ROUTE,
            "crossdock create order with route"
        });
        parameters.add(new Object[]{
            getCreateCrossdockOrderEventWithRoute(deliveryOptionChangeRequest()),
            OrderEventsRouter.CHANNEL_CREATE_CROSSDOCK_ORDER_WITH_ROUTE,
            "crossdock create order with route and delivery option change request (not by user moved delivery dates)"
        });
        parameters.add(new Object[]{
            getCreateFulfilmentOrderEventWithRoute(OrderStatus.PENDING, deliveryOptionChangeRequest()),
            OrderEventsByTypeRouter.CHANNEL_DISCARDED,
            "fulfilment create order with route and delivery option change request (not by user moved delivery dates)"
        });
        parameters.add(new Object[]{
            getCreateCrossdockOrderEventWithRoute(userMovedDeliveryDatesDeliveryOptionChangeRequest()),
            OrderEventsRouter.CHANNEL_CREATE_CROSSDOCK_ORDER_WITH_ROUTE,
            "crossdock create order with route and delivery option change request by user moved delivery dates"
        });
        parameters.add(new Object[]{
            getCreateFulfilmentOrderEvent(),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "fulfilment create order"
        });
        parameters.add(new Object[]{
            getCreateFulfilmentOrderEventWithRoute(),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "fulfilment create order with route"
        });
        parameters.add(new Object[]{
            getCreateFulfilmentOrderEventWithRoute(deliveryOptionChangeRequest()),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "fulfilment create order with route and delivery option change request (not by user moved delivery dates)"
        });
        parameters.add(new Object[]{
            getCreateFulfilmentOrderEventWithRoute(userMovedDeliveryDatesDeliveryOptionChangeRequest()),
            OrderEventsRouter.CHANNEL_CREATE_FULFILLMENT_ORDER_WITH_ROUTE,
            "fulfilment create order with route and delivery option change request by user moved delivery dates"
        });
        parameters.add(new Object[]{
            getItemsRemovedEvent(),
            OrderEventsRouter.CHANNEL_CHANGE_REQUEST_CREATED,
            "fulfilment change request created"
        });

        return parameters;
    }

    @Nonnull
    private static OrderHistoryEvent getWeightChangedEvent() {
        OrderHistoryEvent order = getCreateOrderEvent();

        OrderSteps.setOrderDimension(order.getOrderAfter());

        order.getOrderBefore().setStatus(OrderStatus.PROCESSING);
        order.getOrderAfter().setStatus(OrderStatus.PROCESSING);

        OrderSteps.setOrderWeight(order.getOrderAfter());

        return order;
    }

    @Nonnull
    private static OrderHistoryEvent getRepairedOrderEvent() {
        OrderHistoryEvent event = getCreateOrderEvent();
        event.setType(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED);

        event.getOrderBefore().setStatus(OrderStatus.PROCESSING);
        event.getOrderAfter().setStatus(OrderStatus.PROCESSING);
        event.getOrderAfter().setChangeRequests(Collections.singletonList(deliveryOptionChangeRequest()));

        return event;
    }

    @Nonnull
    private static OrderHistoryEvent getFilledDimensionEvent() {
        OrderHistoryEvent order = getCreateOrderEvent();

        OrderSteps.setOrderDimension(order.getOrderAfter());

        order.getOrderBefore().setStatus(OrderStatus.PROCESSING);
        order.getOrderAfter().setStatus(OrderStatus.PROCESSING);

        OrderSteps.setOrderWeight(order.getOrderBefore());
        OrderSteps.setOrderWeight(order.getOrderAfter());

        return order;
    }

    @Nonnull
    private static OrderHistoryEvent getChangeStatusEvent() {
        OrderHistoryEvent order = getCreateOrderEvent();

        OrderSteps.setOrderDimension(order.getOrderAfter());

        order.getOrderAfter().setStatus(OrderStatus.PROCESSING);

        OrderSteps.setOrderWeight(order.getOrderBefore());
        OrderSteps.setOrderWeight(order.getOrderAfter());

        return order;
    }

    @Nonnull
    private static OrderHistoryEvent getCreateOrderEvent() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();

        Order orderAfter = OrderSteps.getNotFakeOrder();
        Order orderBefore = OrderSteps.getNotFakeOrder();

        // обнуляем треки в тестовом заказе. при создании заказа их нет
        OrderSteps.clearTracks(orderBefore);
        OrderSteps.clearTracks(orderAfter);

        orderEvent.setOrderBefore(orderBefore);
        orderEvent.setOrderAfter(orderAfter);
        orderEvent.setTranDate(TRAN_DATE);
        return orderEvent;
    }

    @Nonnull
    private static OrderHistoryEvent getRedCreateOrderEventWithoutPersonalDataGatheredWhenNotNecessary() {
        OrderHistoryEvent event = getChangeStatusEvent();

        event.getOrderBefore().setRgb(Color.RED);
        event.getOrderAfter().setRgb(Color.RED);

        TariffData tariffData = new TariffData();
        tariffData.setNeedPersonalData(false);
        event.getOrderAfter().getDelivery().setTariffData(tariffData);

        return event;
    }

    @Nonnull
    private static OrderHistoryEvent getRedCreateOrderEventWithoutPersonalDataGatheredWhenNecessary() {
        OrderHistoryEvent event = getChangeStatusEvent();

        event.getOrderBefore().setRgb(Color.RED);
        event.getOrderAfter().setRgb(Color.RED);

        TariffData tariffData = new TariffData();
        tariffData.setNeedPersonalData(true);
        event.getOrderAfter().getDelivery().setTariffData(tariffData);

        return event;
    }

    @Nonnull
    private static OrderHistoryEvent getRedCreateOrderEventWithPersonalDataGathered() {
        OrderHistoryEvent event = getRedCreateOrderEventWithoutPersonalDataGatheredWhenNecessary();

        event.getOrderAfter().setBuyer(BuyerSteps.getBuyerWithPersonalDataGathered());

        return event;
    }

    @Nonnull
    private static OrderHistoryEvent getPersonalDataStatusChangedEvent() {
        OrderHistoryEvent event = getCreateOrderEvent();

        OrderSteps.setOrderDimension(event.getOrderBefore());
        OrderSteps.setOrderDimension(event.getOrderAfter());

        event.getOrderBefore().setStatus(OrderStatus.PROCESSING);
        event.getOrderAfter().setStatus(OrderStatus.PROCESSING);

        event.getOrderBefore().setRgb(Color.RED);
        event.getOrderAfter().setRgb(Color.RED);

        OrderSteps.setOrderWeight(event.getOrderBefore());
        OrderSteps.setOrderWeight(event.getOrderAfter());

        event.getOrderAfter().setBuyer(BuyerSteps.getBuyerWithPersonalDataGathered());

        return event;
    }

    @Nonnull
    private static OrderHistoryEvent getPostamatFlowTrackChange() {
        OrderHistoryEvent event = createPostamatEvent();

        OrderSteps.setOrderDimension(event.getOrderBefore());
        OrderSteps.setOrderDimension(event.getOrderAfter());

        event.getOrderBefore().setStatus(OrderStatus.PROCESSING);
        event.getOrderAfter().setStatus(OrderStatus.PROCESSING);

        event.getOrderBefore().setRgb(Color.BLUE);
        event.getOrderAfter().setRgb(Color.BLUE);

        OrderSteps.setOrderWeight(event.getOrderBefore());
        OrderSteps.setOrderWeight(event.getOrderAfter());

        event.getOrderAfter().setBuyer(BuyerSteps.getBuyerWithPersonalDataGathered());

        return event;
    }

    @Nonnull
    private static OrderHistoryEvent createPostamatEvent() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();
        var before = createPostamatOrder();
        before.getDelivery().addParcel(createParcel());
        orderEvent.setOrderBefore(before);

        Parcel parcel = createParcel();
        parcel.addTrack(new Track(1L, "track", 48L));
        var after = createPostamatOrder();
        after.setStatus(OrderStatus.PROCESSING);
        after.getDelivery().addParcel(parcel);
        orderEvent.setOrderAfter(after);
        orderEvent.setTranDate(TRAN_DATE);
        return orderEvent;
    }

    @Nonnull
    private static Order createPostamatOrder() {
        Order order = OrderSteps.getNotFakeOrder();
        order.setFulfilment(true);
        order.setRgb(Color.BLUE);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        delivery.setType(DeliveryType.PICKUP);
        delivery.setDeliveryServiceId(POSTAMAT_PARTNER_ID_1);
        order.setDelivery(delivery);
        return order;
    }

    @Nonnull
    private static Parcel createParcel() {
        Parcel parcel = new Parcel();
        parcel.setDepth(1L);
        parcel.setWidth(1L);
        parcel.setHeight(1L);
        parcel.setWeight(1L);
        return parcel;
    }

    @Nonnull
    private static OrderHistoryEvent getPostamatCreateOrderEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        Order order = OrderSteps.getFilledOrder();
        order.setStatus(OrderStatus.PROCESSING);
        event.setOrderAfter(order);
        order.getDelivery().setType(DeliveryType.PICKUP);
        order.getDelivery().setDeliveryServiceId(POSTAMAT_PARTNER_ID_1);
        OrderSteps.clearTracks(order);
        event.setTranDate(TRAN_DATE);
        return event;
    }

    @Nonnull
    private static OrderHistoryEvent getPostamatCreateOrderEvent(Long deliveryServiceId) {
        OrderHistoryEvent event = getPostamatCreateOrderEvent();
        event.getOrderAfter().getDelivery().setDeliveryServiceId(deliveryServiceId);
        return event;
    }

    @Nonnull
    private static OrderHistoryEvent getCreateCrossdockOrderEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        Order order = OrderSteps.createFulfilmentOrder();
        OrderSteps.setItemsAtSupplierWarehouse(order);
        event.setOrderAfter(order);
        event.setTranDate(TRAN_DATE);
        return event;
    }

    @Nonnull
    private static OrderHistoryEvent getCreateCrossdockOrderEventWithRoute() {
        return getCreateCrossdockOrderEventWithRoute(null);
    }

    @Nonnull
    private static OrderHistoryEvent getCreateCrossdockOrderEventWithRoute(ChangeRequest changeRequest) {
        OrderHistoryEvent event = getCreateCrossdockOrderEvent();
        OrderSteps.addParcelsRoute(event.getOrderAfter());
        Optional.ofNullable(changeRequest)
            .ifPresent(cr -> event.getOrderAfter().setChangeRequests(List.of(cr)));
        return event;
    }

    @Nonnull
    private static OrderHistoryEvent getCreateFulfilmentOrderEvent() {
        return getCreateFulfilmentOrderEvent(OrderStatus.PROCESSING);
    }

    @Nonnull
    private static OrderHistoryEvent getCreateFulfilmentOrderEvent(OrderStatus orderStatus) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        Order order = OrderSteps.createFulfilmentOrder(orderStatus);
        event.setOrderAfter(order);
        event.setTranDate(TRAN_DATE);
        return event;
    }

    @Nonnull
    private static OrderHistoryEvent getCreateFulfilmentOrderEventWithRoute() {
        return getCreateFulfilmentOrderEventWithRoute(OrderStatus.PROCESSING, null);
    }

    @Nonnull
    private static OrderHistoryEvent getCreateFulfilmentOrderEventWithRoute(ChangeRequest changeRequest) {
        return getCreateFulfilmentOrderEventWithRoute(OrderStatus.PROCESSING, changeRequest);
    }

    @Nonnull
    private static OrderHistoryEvent getCreateFulfilmentOrderEventWithRoute(
        OrderStatus orderStatus,
        ChangeRequest changeRequest
    ) {
        OrderHistoryEvent event = getCreateFulfilmentOrderEvent(orderStatus);
        OrderSteps.addParcelsRoute(event.getOrderAfter());
        Optional.ofNullable(changeRequest)
            .ifPresent(req -> event.getOrderAfter().setChangeRequests(List.of(req)));
        return event;
    }

    @Nonnull
    private static OrderHistoryEvent getItemsRemovedEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED);

        Order orderBefore = OrderSteps.createFulfilmentOrder();
        orderBefore.setChangeRequests(List.of(deliveryOptionChangeRequest()));
        event.setOrderBefore(orderBefore);

        Order orderAfter = OrderSteps.createFulfilmentOrder();
        orderAfter.setChangeRequests(List.of(
            deliveryOptionChangeRequest(),
            itemsRemovalChangeRequest(2)
        ));
        event.setOrderAfter(orderAfter);
        event.setTranDate(TRAN_DATE);

        return event;
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(BackLogWrapper.of(logger)).when(tskvLogger).wrapLogger(any(), any(), any());
    }

    @Test
    public void routeTest() {
        var actualChannelName = orderEventsRoute.route(event);
        assertThat(actualChannelName).isEqualTo(expectedChannelName);
    }
}
