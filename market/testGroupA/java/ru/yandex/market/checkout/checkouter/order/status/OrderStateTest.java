package ru.yandex.market.checkout.checkouter.order.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderState;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStateTest {

    @Test
    void allowedStates() {
        OrderState orderState = OrderState.builder()
                .add(OrderStatus.PROCESSING).onlyWith(OrderSubstatus.READY_TO_SHIP)
                .add(OrderStatus.DELIVERY)
                .build();

        assertFalse(orderState.missing(OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP));
        assertFalse(orderState.missing(OrderStatus.DELIVERY, OrderSubstatus.USER_RECEIVED));
        assertFalse(orderState.missing(OrderStatus.DELIVERY, null));
    }

    @Test
    void forbiddenStates() {
        OrderState orderState = OrderState.builder()
                .add(OrderStatus.PROCESSING).onlyWith(OrderSubstatus.READY_TO_SHIP)
                .add(OrderStatus.DELIVERY)
                .build();

        assertTrue(orderState.missing(OrderStatus.CANCELLED, null));
        assertTrue(orderState.missing(OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND));
        assertTrue(orderState.missing(OrderStatus.PROCESSING, OrderSubstatus.STARTED));
        assertTrue(orderState.missing(OrderStatus.PROCESSING, null));
    }

    @Test
    void exceptionWhenAddedMissingSubstatus() {
        Assertions.assertThrows(IllegalStateException.class, () -> OrderState.builder()
                .add(OrderStatus.PROCESSING).onlyWith(OrderSubstatus.USER_CHANGED_MIND)
                .build());

        Assertions.assertThrows(IllegalStateException.class, () -> OrderState.builder()
                .add(OrderStatus.PROCESSING).onlyWith(null, OrderSubstatus.USER_CHANGED_MIND)
                .build());

        Assertions.assertThrows(IllegalStateException.class, () -> OrderState.builder()
                .add(OrderStatus.PROCESSING).onlyWith(OrderSubstatus.STARTED, OrderSubstatus.USER_CHANGED_MIND)
                .build());
    }

    @Test
    void testAllMovementsWithSomeExclusionsWithoutNull() {
        OrderState orderState = OrderState.builder()
                .add(OrderStatus.CANCELLED).without(OrderSubstatus.BANK_REJECT_CREDIT_OFFER)
                .build();

        assertTrue(orderState.missing(OrderStatus.CANCELLED, OrderSubstatus.BANK_REJECT_CREDIT_OFFER));
        assertTrue(orderState.missing(OrderStatus.PROCESSING, OrderSubstatus.STARTED));

        assertFalse(orderState.missing(OrderStatus.CANCELLED, null));
        assertFalse(orderState.missing(OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND));
        assertFalse(orderState.missing(OrderStatus.CANCELLED, OrderSubstatus.USER_REFUSED_DELIVERY));
    }


    @Test
    void testAllMovementsWithSomeExclusionsWithNull() {
        OrderState orderState = OrderState.builder()
                .add(OrderStatus.CANCELLED).without(OrderSubstatus.BANK_REJECT_CREDIT_OFFER, null)
                .build();

        assertTrue(orderState.missing(OrderStatus.CANCELLED, null));
        assertTrue(orderState.missing(OrderStatus.CANCELLED, OrderSubstatus.BANK_REJECT_CREDIT_OFFER));
        assertTrue(orderState.missing(OrderStatus.PROCESSING, OrderSubstatus.STARTED));

        assertFalse(orderState.missing(OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND));
        assertFalse(orderState.missing(OrderStatus.CANCELLED, OrderSubstatus.USER_REFUSED_DELIVERY));
    }
}
