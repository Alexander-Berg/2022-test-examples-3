package ru.yandex.market.delivery.mdbapp.integration.router;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tariff.TariffData;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.delivery.mdbapp.components.service.InternalVariableService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.delivery.mdbapp.integration.router.CrossDockFlowOrdersEventRouter.CHANNEL_CROSS_DOCK_CREATE_ORDER;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsByTypeRouter.CHANNEL_DISCARDED;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsRouter.CHANNEL_CHECKPOINT_CHANGED;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsRouter.CHANNEL_FIRST_TIME_CANCELLED_ORDER;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsRouter.CHANNEL_GET_LABELS;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsRouter.CHANNEL_GET_TARIFF_DATA;
import static steps.orderSteps.OrderEventSteps.cancelledEvent;
import static steps.orderSteps.OrderSteps.getFilledOrder;

@RunWith(Parameterized.class)
public class CrossDockFlowOrdersEventRouterTest {

    private CrossDockFlowOrdersEventRouter crossDockFlowOrdersEventRouter = new CrossDockFlowOrdersEventRouter(
        mock(InternalVariableService.class)
    );

    @Parameterized.Parameter
    public OrderHistoryEvent orderHistoryEvent;

    @Parameterized.Parameter(1)
    public String channel;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            {
                createOrderSingleParcelEvent(),
                CHANNEL_CROSS_DOCK_CREATE_ORDER
            },
            {
                createOrderSingleParcelEventWithoutItemsInParcel(),
                CHANNEL_DISCARDED
            },
            {
                createOrderMultiParcelEvent(),
                CHANNEL_CROSS_DOCK_CREATE_ORDER
            },
            {
                createOrderMultiParcelWithTracksEvent(),
                CHANNEL_DISCARDED
            },
            {
                cancelParcelEvent(),
                CHANNEL_DISCARDED
            },
            {
                getLabelsOrderSingleParcelEvent(),
                CHANNEL_GET_LABELS
            },
            {
                getLabelsOrderMultiParcelEvent(),
                CHANNEL_GET_LABELS
            },
            {
                discardOrderEmptyEvent(),
                CHANNEL_DISCARDED
            },
            {
                getTariffDataEvent(),
                CHANNEL_GET_TARIFF_DATA
            },
            {
                createOrderWithTariffData(),
                CHANNEL_CROSS_DOCK_CREATE_ORDER
            },
            {
                cancelledEvent(),
                CHANNEL_FIRST_TIME_CANCELLED_ORDER
            },
            {
                checkpointChanged(),
                CHANNEL_CHECKPOINT_CHANGED
            }
        });
    }

    @Test
    public void routeTest() {
        assertEquals("Route to correct channel", channel, crossDockFlowOrdersEventRouter.route(orderHistoryEvent));
    }

    private static OrderHistoryEvent createOrderSingleParcelEvent() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();

        Order orderBefore = createRedOrderSingleParcel();
        Order orderAfter = createRedOrderSingleParcel();
        orderAfter.getDelivery().getParcels().get(0).setWeight(10L);
        orderAfter.getDelivery().getParcels().get(0).setParcelItems(Collections.singletonList(new ParcelItem(1L, 1)));
        orderAfter.setStatus(OrderStatus.PROCESSING);

        orderEvent.setOrderBefore(orderBefore);
        orderEvent.setOrderAfter(orderAfter);

        return orderEvent;
    }

    private static OrderHistoryEvent createOrderSingleParcelEventWithoutItemsInParcel() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();

        Order orderBefore = createRedOrderSingleParcel();
        Order orderAfter = createRedOrderSingleParcel();
        orderAfter.getDelivery().getParcels().get(0).setWeight(10L);
        orderAfter.setStatus(OrderStatus.PROCESSING);

        orderEvent.setOrderBefore(orderBefore);
        orderEvent.setOrderAfter(orderAfter);

        return orderEvent;
    }

    private static OrderHistoryEvent createOrderMultiParcelEvent() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();

        Order orderBefore = createRedOrderMultiParcel();
        Order orderAfter = createRedOrderMultiParcel();
        orderAfter.getDelivery().getParcels().get(0).setWeight(10L);
        orderAfter.getDelivery().getParcels().get(0).setParcelItems(Collections.singletonList(new ParcelItem(1L, 1)));
        orderAfter.setStatus(OrderStatus.PROCESSING);

        orderEvent.setOrderBefore(orderBefore);
        orderEvent.setOrderAfter(orderAfter);

        return orderEvent;
    }

    private static OrderHistoryEvent createOrderMultiParcelWithTracksEvent() {
        OrderHistoryEvent orderEvent = createOrderMultiParcelEvent();

        Track track = new Track();
        track.setTrackCode("123");
        orderEvent.getOrderAfter().getDelivery().getParcels().get(0).setTracks(Collections.singletonList(track));

        return orderEvent;
    }

    private static OrderHistoryEvent cancelParcelEvent() {
        OrderHistoryEvent orderEvent = createOrderMultiParcelWithTracksEvent();
        orderEvent.setType(HistoryEventType.PARCEL_CANCELLATION_REQUESTED);

        return orderEvent;
    }

    private static OrderHistoryEvent getLabelsOrderSingleParcelEvent() {
        OrderHistoryEvent orderEvent = createOrderSingleParcelEvent();

        Track track = new Track();
        track.setTrackCode("123");
        orderEvent.getOrderAfter().getDelivery().getParcels().get(0).setTracks(Collections.singletonList(track));
        orderEvent.getOrderAfter().getDelivery().getParcels().get(0).setStatus(ParcelStatus.CREATED);

        return orderEvent;
    }

    private static OrderHistoryEvent getLabelsOrderMultiParcelEvent() {
        OrderHistoryEvent orderEvent = createOrderMultiParcelEvent();

        Track track = new Track();
        track.setTrackCode("123");
        orderEvent.getOrderAfter().getDelivery().getParcels().get(0).setTracks(Collections.singletonList(track));
        orderEvent.getOrderAfter().getDelivery().getParcels().get(0).setStatus(ParcelStatus.CREATED);

        return orderEvent;
    }

    private static OrderHistoryEvent getTariffDataEvent() {
        OrderHistoryEvent orderEvent = createOrderSingleParcelEvent();

        orderEvent.getOrderAfter().getDelivery().setTariffId(2234562L);
        orderEvent.getOrderBefore().getDelivery().setTariffId(2234562L);

        return orderEvent;
    }

    private static OrderHistoryEvent createOrderWithTariffData() {
        OrderHistoryEvent orderEvent = createOrderSingleParcelEvent();

        Delivery afterDelivery = orderEvent.getOrderAfter().getDelivery();
        afterDelivery.setTariffId(2234562L);
        afterDelivery.setTariffData(new TariffData());

        Delivery beforeDelivery = orderEvent.getOrderBefore().getDelivery();
        beforeDelivery.setTariffId(2234562L);

        return orderEvent;
    }

    static OrderHistoryEvent discardOrderEmptyEvent() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();
        orderEvent.setOrderBefore(new Order());
        orderEvent.setOrderAfter(new Order());

        return orderEvent;
    }

    private static OrderHistoryEvent checkpointChanged() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();

        Order order = createRedOrderSingleParcel();
        order.setStatus(OrderStatus.PROCESSING);

        orderEvent.setOrderBefore(order);
        orderEvent.setOrderAfter(order);

        Track track = new Track();
        track.setTrackCode("123");
        orderEvent.getOrderAfter().getDelivery().getParcels().get(0).setTracks(Collections.singletonList(track));
        orderEvent.getOrderAfter().getDelivery().getParcels().get(0).setStatus(ParcelStatus.CREATED);

        orderEvent.setType(HistoryEventType.TRACK_CHECKPOINT_CHANGED);
        return orderEvent;
    }

    private static Order createRedOrderSingleParcel() {
        Order order = getFilledOrder();

        order.setRgb(Color.RED);
        order.setGlobal(true);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        delivery.setType(DeliveryType.POST);
        Parcel parcel = new Parcel();
        parcel.setId(1L);
        delivery.addParcel(parcel);
        order.setDelivery(delivery);

        return order;
    }

    private static Order createRedOrderMultiParcel() {
        Order order = createRedOrderSingleParcel();

        Parcel parcel = new Parcel();
        parcel.setId(2L);
        order.getDelivery().addParcel(parcel);

        return order;
    }
}
