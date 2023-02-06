package ru.yandex.market.checkout.checkouter.order.status;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;

public class OrderControllerSubstatusChangeTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;

    @Test
    public void shouldAllowToChangePendingSubstatus() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);

        assertThat(order.getSubstatus(), is(OrderSubstatus.AWAIT_CONFIRMATION));

        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);

        Order updated = orderService.getOrder(order.getId());

        Assertions.assertEquals(OrderStatus.PROCESSING, updated.getStatus());
    }

    @Test
    public void shouldAllowToChangePendingPreorderSubstatus() throws Exception {
        Parameters parameters = new Parameters(OrderProvider.getBlueOrder(o -> {
            o.getItems().forEach(oi -> oi.setPreorder(true));
        }));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PENDING);
        order = orderService.getOrder(order.getId());

        assertThat(order.getSubstatus(), is(OrderSubstatus.PREORDER));

        setFixedTime(getClock().instant().plus(4, ChronoUnit.DAYS));
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();

        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION);

        Order updated = orderService.getOrder(order.getId());

        Assertions.assertEquals(OrderStatus.PENDING, updated.getStatus());
        Assertions.assertEquals(OrderSubstatus.AWAIT_CONFIRMATION, updated.getSubstatus());

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());
        List<OrderHistoryEvent> substatusUpdatedEvents = events.getItems().stream()
                .filter(ohe -> ohe.getType() == HistoryEventType.ORDER_SUBSTATUS_UPDATED)
                .collect(Collectors.toList());
        assertThat(substatusUpdatedEvents, hasSize(1));

        OrderHistoryEvent substatusUpdatedEvent = Iterables.getOnlyElement(substatusUpdatedEvents);
        assertThat(substatusUpdatedEvent.getOrderBefore().getSubstatus(), is(OrderSubstatus.PREORDER));
        assertThat(substatusUpdatedEvent.getOrderAfter().getSubstatus(), is(OrderSubstatus.AWAIT_CONFIRMATION));
    }

    @Test
    public void shouldNotAllowToUpdateNotPreorderOrderToPreorderSubstatus() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);
        Payment payment = orderPayHelper.pay(order.getId());

        transactionTemplate.execute(tc -> {
            masterJdbcTemplate.update(
                    "UPDATE payment SET status = ? WHERE id = ?",
                    PaymentStatus.HOLD.getId(), payment.getId()
            );
            return null;
        });

        assertThat(order.isPreorder(), is(false));

        orderStatusHelper.updateOrderStatusForActions(
                order.getId(), ClientInfo.SYSTEM, OrderStatus.PENDING, OrderSubstatus.PREORDER
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldAllowToChangeFromOldOrderStatusPendingToProcessing() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        transactionTemplate.execute(tc -> {
            masterJdbcTemplate.update("UPDATE ORDERS SET SUBSTATUS = NULL WHERE id = ?", order.getId());
            return null;
        });
        Order patched = orderService.getOrder(order.getId());

        assertThat(patched.getSubstatus(), CoreMatchers.nullValue());

        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);

        Order updated = orderService.getOrder(order.getId());

        Assertions.assertEquals(OrderStatus.PROCESSING, updated.getStatus());
    }

    @Disabled
    @Test
    public void shouldNotAllowToChangePreorderSubstatusToProcessing() {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            Parameters parameters = new Parameters(OrderProvider.getBlueOrder(o -> {
                o.getItems().forEach(oi -> oi.setPreorder(true));
            }));
            parameters.setPaymentMethod(PaymentMethod.YANDEX);

            Order order = orderCreateHelper.createOrder(parameters);
            orderPayHelper.payForOrder(order);
            order = orderService.getOrder(order.getId());

            assertThat(order.getStatus(), is(OrderStatus.PENDING));
            assertThat(order.getSubstatus(), is(OrderSubstatus.PREORDER));

            orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);
        });
    }

    @Test
    public void shouldAllowToChangePickupToPickup() {
        Parameters parameters = new Parameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        Order order = orderCreateHelper.createOrder(parameters);
        processToPickupDefault(order);

        orderStatusHelper.updateOrderStatus(order.getId(), PICKUP, OrderSubstatus.PICKUP_USER_RECEIVED);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), is(PICKUP));
        assertThat(order.getSubstatus(), is(OrderSubstatus.PICKUP_USER_RECEIVED));

        orderStatusHelper.updateOrderStatus(order.getId(), PICKUP);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), is(PICKUP));
        assertThat(order.getSubstatus(), is(OrderSubstatus.PICKUP_SERVICE_RECEIVED));
    }

    @ParameterizedTest
    @EnumSource(value = OrderSubstatus.class, names = {"PICKUP_USER_RECEIVED", "PICKUP_SERVICE_RECEIVED"})
    public void shouldAllowToChangePickupToDelivered(OrderSubstatus substatus) {
        Parameters parameters = new Parameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        Order order = orderCreateHelper.createOrder(parameters);
        processToPickup(order, substatus);

        orderStatusHelper.updateOrderStatus(order.getId(), DELIVERED);
        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), is(DELIVERED));
    }

    @ParameterizedTest
    @EnumSource(value = OrderSubstatus.class, names = {"PICKUP_USER_RECEIVED", "PICKUP_SERVICE_RECEIVED"})
    public void shouldAllowToChangePickupToCanceled(OrderSubstatus substatus) {
        Parameters parameters = new Parameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        Order order = orderCreateHelper.createOrder(parameters);
        processToPickup(order, substatus);

        orderStatusHelper.updateOrderStatus(order.getId(), CANCELLED, OrderSubstatus.USER_CHANGED_MIND);
        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), is(CANCELLED));
    }

    @Test
    public void shouldAllowToChangeDeliveryToCanceledWithFullNotRansom() {
        Parameters parameters = new Parameters();
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.updateOrderStatus(order.getId(), DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        orderStatusHelper.updateOrderStatus(order.getId(), new ClientInfo(ClientRole.DELIVERY_SERVICE, null),
                CANCELLED, OrderSubstatus.FULL_NOT_RANSOM);
        order = orderService.getOrder(order.getId());

        assertThat(order.getSubstatus(), is(OrderSubstatus.FULL_NOT_RANSOM));
    }

    @Test
    public void shouldAllowToChangePickupToCanceledWithFullNotRansom() {
        Parameters parameters = new Parameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        Order order = orderCreateHelper.createOrder(parameters);
        processToPickup(order, OrderSubstatus.PICKUP_SERVICE_RECEIVED);

        orderStatusHelper.updateOrderStatus(order.getId(), new ClientInfo(ClientRole.PICKUP_SERVICE, null),
                CANCELLED, OrderSubstatus.FULL_NOT_RANSOM);
        order = orderService.getOrder(order.getId());

        assertThat(order.getSubstatus(), is(OrderSubstatus.FULL_NOT_RANSOM));
    }

    private Order processToPickupDefault(Order order) {
        return processToPickup(order, null);
    }

    private Order processToPickup(Order order, OrderSubstatus substatus) {
        orderStatusHelper.updateOrderStatus(order.getId(), DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);
        orderStatusHelper.updateOrderStatus(order.getId(), PICKUP, substatus);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), is(PICKUP));
        if (substatus == null) {
            assertThat(order.getSubstatus(), is(OrderSubstatus.PICKUP_SERVICE_RECEIVED));
        } else {
            assertThat(order.getSubstatus(), is(substatus));
        }
        return order;
    }
}
