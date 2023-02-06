package ru.yandex.market.delivery.mdbapp.integration.router;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrderMilestoneTimingsTskvLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.EventFlowParametersHolder;
import ru.yandex.market.delivery.mdbapp.util.DropshipOrderEventUtils;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.delivery.mdbapp.integration.router.BlueDropshipFlowOrdersEventRouter.CHANNEL_CREATE_DROPSHIP_WITHOUT_SORTING_CENTER_ORDER_WITH_ROUTE;
import static ru.yandex.market.delivery.mdbapp.integration.router.BlueDropshipFlowOrdersEventRouter.CHANNEL_CREATE_ORDER_DROPSHIP_SORTING_CENTER_WITH_ROUTE;
import static ru.yandex.market.delivery.mdbapp.integration.router.CrossDockFlowOrdersEventRouterTest.discardOrderEmptyEvent;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderCheckpointEventsRouter.GET_ORDER_FROM_FF;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsByTypeRouter.CHANNEL_DISCARDED;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsRouter.CHANNEL_CHANGE_REQUEST_CREATED;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsRouter.CHANNEL_CHECKPOINT_CHANGED;
import static ru.yandex.market.delivery.mdbapp.integration.router.OrderEventsRouter.CHANNEL_FIRST_TIME_CANCELLED_ORDER;
import static ru.yandex.market.delivery.mdbapp.util.DropshipOrderEventUtils.TRAN_DATE;
import static steps.orderSteps.OrderEventSteps.cancelledEvent;
import static steps.orderSteps.OrderEventSteps.createCancellationEvent;
import static steps.orderSteps.OrderEventSteps.deliveryOptionChangeRequest;
import static steps.orderSteps.OrderEventSteps.userMovedDeliveryDatesDeliveryOptionChangeRequest;

@DisplayName("Роутинг синих заказов")
@RunWith(Parameterized.class)
public class BlueDropshipFlowOrdersEventRouterTest {

    BackLogOrderMilestoneTimingsTskvLogger tskvLogger = new BackLogOrderMilestoneTimingsTskvLogger(
        new EventFlowParametersHolder(),
        new TestableClock()
    );
    private final BlueDropshipFlowOrdersEventRouter blueDropshipFlowOrdersEventRouter =
        new BlueDropshipFlowOrdersEventRouter(tskvLogger);

    @Parameterized.Parameter
    public OrderHistoryEvent orderHistoryEvent;

    @Parameterized.Parameter(1)
    public String channel;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
            {
                DropshipOrderEventUtils.blueDropshipCreateOrderEvent(),
                CHANNEL_CREATE_DROPSHIP_WITHOUT_SORTING_CENTER_ORDER_WITH_ROUTE,
            },
            {
                DropshipOrderEventUtils.blueDropshipCreateOrderEventWithRoute(),
                CHANNEL_CREATE_DROPSHIP_WITHOUT_SORTING_CENTER_ORDER_WITH_ROUTE,
            },
            {
                DropshipOrderEventUtils.blueDropshipCreateOrderEventWithRoute(deliveryOptionChangeRequest()),
                CHANNEL_CREATE_DROPSHIP_WITHOUT_SORTING_CENTER_ORDER_WITH_ROUTE,
            },
            {
                DropshipOrderEventUtils.blueDropshipCreateOrderEventWithRoute(
                    userMovedDeliveryDatesDeliveryOptionChangeRequest()),
                CHANNEL_CREATE_DROPSHIP_WITHOUT_SORTING_CENTER_ORDER_WITH_ROUTE,
            },
            {
                DropshipOrderEventUtils.blueDropshipSortingCenterCreateOrderEvent(false),
                CHANNEL_CREATE_ORDER_DROPSHIP_SORTING_CENTER_WITH_ROUTE,
            },
            {
                DropshipOrderEventUtils.blueDropshipSortingCenterCreateOrderEvent(true),
                CHANNEL_CREATE_ORDER_DROPSHIP_SORTING_CENTER_WITH_ROUTE,
            },
            {
                DropshipOrderEventUtils.blueDropshipSortingCenterEventWithRoute(deliveryOptionChangeRequest()),
                CHANNEL_CREATE_ORDER_DROPSHIP_SORTING_CENTER_WITH_ROUTE,
            },
            {
                DropshipOrderEventUtils.blueDropshipSortingCenterEventWithRoute(
                    userMovedDeliveryDatesDeliveryOptionChangeRequest()),
                CHANNEL_CREATE_ORDER_DROPSHIP_SORTING_CENTER_WITH_ROUTE,
            },
            {
                DropshipOrderEventUtils.blueDropshipCreateRegisterEvent(),
                GET_ORDER_FROM_FF,
            },
            {
                cancelParcelEvent(),
                CHANNEL_DISCARDED, // Теперь работаем с change request'ами, т. к. теперь работаем через LOM.
            },
            {
                cancelledEvent(),
                CHANNEL_FIRST_TIME_CANCELLED_ORDER,
            },
            {
                discardOrderEmptyEvent(),
                CHANNEL_DISCARDED,
            },
            {
                createCancellationEvent(),
                CHANNEL_CHANGE_REQUEST_CREATED,
            },
            {
                DropshipOrderEventUtils.dropshipCheckpointChanged(),
                CHANNEL_CHECKPOINT_CHANGED,
            },
            {
                blueDropshipCreateOrderEventStraightFlow(),
                CHANNEL_CREATE_DROPSHIP_WITHOUT_SORTING_CENTER_ORDER_WITH_ROUTE,
            },
            {
                blueDropshipChangeDeliveryInUnpaidEvent(),
                CHANNEL_DISCARDED,
            },
            {
                blueDropshipChangeDeliveryInProcessingEvent(),
                CHANNEL_CREATE_DROPSHIP_WITHOUT_SORTING_CENTER_ORDER_WITH_ROUTE,
            },
            {
                blueDropshipChangeDeliveryInPendingEvent(),
                CHANNEL_DISCARDED,
            },
            {
                blueDropshipChangeDeliveryInPendingStraightEvent(),
                CHANNEL_CREATE_DROPSHIP_WITHOUT_SORTING_CENTER_ORDER_WITH_ROUTE,
            },
        });
    }

    @Test
    public void routeTest() {
        assertEquals("Route to correct channel", channel, blueDropshipFlowOrdersEventRouter.route(orderHistoryEvent));
    }

    @Nonnull
    private static OrderHistoryEvent blueDropshipCreateOrderEventStraightFlow() {
        return buildOrderHistoryEvent(
            HistoryEventType.ORDER_SUBSTATUS_UPDATED,
            OrderStatus.PENDING,
            OrderSubstatus.AWAIT_CONFIRMATION,
            OrderAcceptMethod.PUSH_API,
            null
        );
    }

    @Nonnull
    private static OrderHistoryEvent cancelParcelEvent() {
        OrderHistoryEvent orderEvent = new OrderHistoryEvent();
        orderEvent.setOrderBefore(DropshipOrderEventUtils.createBlueDropshipOrder());
        orderEvent.setOrderAfter(DropshipOrderEventUtils.createBlueDropshipOrder());
        orderEvent.setType(HistoryEventType.PARCEL_CANCELLATION_REQUESTED);
        orderEvent.setTranDate(Date.from(Instant.parse("2019-07-20T00:00:00Z")));
        return orderEvent;
    }

    private static OrderHistoryEvent blueDropshipChangeDeliveryInUnpaidEvent() {
        return buildOrderHistoryEvent(
            HistoryEventType.ORDER_DELIVERY_UPDATED,
            OrderStatus.UNPAID,
            OrderSubstatus.WAITING_USER_INPUT,
            null,
            List.of(deliveryOptionChangeRequest())
        );
    }

    private static OrderHistoryEvent blueDropshipChangeDeliveryInProcessingEvent() {
        return buildOrderHistoryEvent(
            HistoryEventType.ORDER_DELIVERY_UPDATED,
            OrderStatus.PROCESSING,
            OrderSubstatus.STARTED,
            null,
            List.of(deliveryOptionChangeRequest())
        );
    }

    private static OrderHistoryEvent blueDropshipChangeDeliveryInPendingEvent() {
        return buildOrderHistoryEvent(
            HistoryEventType.ORDER_DELIVERY_UPDATED,
            OrderStatus.PENDING,
            OrderSubstatus.AWAIT_CONFIRMATION,
            OrderAcceptMethod.WEB_INTERFACE,
            List.of(deliveryOptionChangeRequest())
        );
    }

    private static OrderHistoryEvent blueDropshipChangeDeliveryInPendingStraightEvent() {
        return buildOrderHistoryEvent(
            HistoryEventType.ORDER_DELIVERY_UPDATED,
            OrderStatus.PENDING,
            OrderSubstatus.AWAIT_CONFIRMATION,
            OrderAcceptMethod.PUSH_API,
            List.of(deliveryOptionChangeRequest())
        );
    }

    private static OrderHistoryEvent buildOrderHistoryEvent(
        HistoryEventType eventType, OrderStatus orderStatus, OrderSubstatus orderSubstatus,
        OrderAcceptMethod orderAcceptMethod, List<ChangeRequest> changeRequests
    ) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(eventType);

        Order blueDropshipOrderBefore = DropshipOrderEventUtils.createBlueDropshipOrder();

        Order blueDropshipOrderAfter = DropshipOrderEventUtils.createBlueDropshipOrder();
        blueDropshipOrderAfter.setStatus(orderStatus);
        blueDropshipOrderAfter.setSubstatus(orderSubstatus);
        Optional.ofNullable(orderAcceptMethod).ifPresent(blueDropshipOrderAfter::setAcceptMethod);
        Optional.ofNullable(changeRequests).ifPresent(blueDropshipOrderAfter::setChangeRequests);

        event.setOrderBefore(blueDropshipOrderBefore);
        event.setOrderAfter(blueDropshipOrderAfter);
        event.setTranDate(TRAN_DATE);

        return event;
    }
}
