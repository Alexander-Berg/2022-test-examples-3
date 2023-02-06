package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT;

public class PaySubsidyTest extends AbstractPaymentTestBase {

    @Autowired
    private QueuedCallService queuedCallService;

    @BeforeEach
    void setUp() {
        checkouterProperties.setEnableUpdatePaymentMode(true);
    }

    @DisplayName("При поступлении заказа в постомат, иметь возможноcть заплатить субсидию")
    @Test
    public void shouldPaySubsidyWhenMardoOrderInPickup() {
        // задание на создание субсидийного платежа создается в момент -> DELIVERY
        // плюс сразу переводим, что заказ в постамате
        order.set(
                orderStatusHelper.proceedOrderToStatus(
                        orderServiceTestHelper.createUnpaidBlueOrderWithPickupMardoDelivery(),
                        OrderStatus.PICKUP));

        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order().getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_SUBSIDY_PAYMENT, order().getId());
        // после выполенения заказ должен пропасть из очереди
        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order().getId()));
    }

    @DisplayName("При получении заказа с магазинной доставкой выплачивается субсидия")
    @Test
    public void shouldPaySubsidyWhenShopDeliveryOrderInDelivered() {
        order.set(
                orderStatusHelper.proceedOrderToStatus(
                        orderServiceTestHelper.createUnpaidBlueOrderWithPickupDelivery(),
                        OrderStatus.PICKUP));
        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order().getId()));

        // задание на создание субсидийного платежа создается в момент -> DELIVERED
        order.set(
                orderStatusHelper.proceedOrderToStatus(
                        order.get(),
                        OrderStatus.DELIVERED));

        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order().getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_SUBSIDY_PAYMENT, order().getId());
        // после выполенения заказ должен пропасть из очереди
        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order().getId()));
    }
}
