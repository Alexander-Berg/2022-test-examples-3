package ru.yandex.market.checkout.checkouter.order.change;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.LAST_MILE_STARTED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PROCESSING_EXPIRED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.READY_FOR_LAST_MILE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_DELIVERY;

class OrderControllerStatusTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private QueuedCallService queuedCallService;

    private static void checkStatus(@Nonnull Order order,
                                    @Nullable OrderStatus expectedStatus,
                                    @Nullable OrderSubstatus expectedSubstatus) {
        if (expectedStatus != null) {
            assertEquals(expectedStatus, order.getStatus());
        }
        if (expectedSubstatus != null) {
            assertEquals(expectedSubstatus, order.getSubstatus());
        }
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("При обновлении статуса на DELIVER statusExpiryDate обнуляется")
    @Test
        // https://testpalm.yandex-team.ru/testcase/checkouter-175
    void canUpdateStatusToDeliveryFromProcessingWithErasingExpiryDate() throws Exception {
        Order order = orderStatusHelper.createOrderWithStatusTransitions(DeliveryProvider.getSelfDelivery());

        order = orderStatusHelper.requestStatusUpdate(order.getId(), OrderStatus.DELIVERY);

        checkStatus(order, OrderStatus.DELIVERY, null);
        assertNull(order.getStatusExpiryDate());
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("При обновлении статуса на PICKUP statusExpiryDate обнуляется")
    @Test
        // https://testpalm.yandex-team.ru/testcase/checkouter-177
    void canUpdateStatusToDeliveredFromPickupWithErasingExpiryDate() throws Exception {
        Order order =
                orderStatusHelper.createOrderWithStatusTransitions(DeliveryProvider.getYandexMarketPickupDelivery(),
                        OrderStatus.DELIVERY,
                        OrderStatus.PICKUP);

        order = orderStatusHelper.requestStatusUpdate(order.getId(), OrderStatus.DELIVERED);

        checkStatus(order, OrderStatus.DELIVERED, null);
        assertNull(order.getStatusExpiryDate());
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("При обновлении статуса на DELIVERED statusExpiryDate обнуляется")
    @Test
        // https://testpalm.yandex-team.ru/testcase/checkouter-178
    void canUpdateStatusToDeliveredFromDeliveryWithErasingExpiryDate() throws Exception {
        Order order = orderStatusHelper.createOrderWithStatusTransitions(DeliveryProvider.getSelfDelivery(),
                OrderStatus.DELIVERY);

        order = orderStatusHelper.requestStatusUpdate(order.getId(), OrderStatus.DELIVERED);

        checkStatus(order, OrderStatus.DELIVERED, null);
        assertNull(order.getStatusExpiryDate());
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("При смене статуса заказа с неверным shopId ошибка ORDER_NOT_FOUND")
    @Test
        // checkouter-179
    void failChangeStatusWithWrongShopId() throws Exception {
        Order order = orderStatusHelper.createOrderWithStatusTransitions(DeliveryProvider.getSelfDelivery());

        String response = orderStatusHelper.requestStatusUpdateAndReturnString(order.getId(), ClientRole.SHOP,
                "12345", OrderStatus.DELIVERY, null);

        assertThat(response, containsString("ORDER_NOT_FOUND"));
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("При смене статуса заказа с неверным userId ошибка ORDER_NOT_FOUND")
    @Test
        // checkouter-180
    void failChangeStatusWithWrongClientId() throws Exception {
        Order order = orderStatusHelper.createOrderWithStatusTransitions(DeliveryProvider.getSelfDelivery());

        String response = orderStatusHelper.requestStatusUpdateAndReturnString(order.getId(), ClientRole.USER,
                "12345", OrderStatus.DELIVERY, null);

        assertThat(response, containsString("ORDER_NOT_FOUND"));
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("Сменить статус и подстатус заказа")
    @Test
        // checkouter-181
    void changeStatusAndSubstatus() throws Exception {
        Order order = orderStatusHelper.createOrderWithStatusTransitions(DeliveryProvider.getSelfDelivery());

        order = orderStatusHelper.requestStatusUpdate(order.getId(), ClientRole.SYSTEM, "12345",
                OrderStatus.CANCELLED, OrderSubstatus.PROCESSING_EXPIRED);

        checkStatus(order, OrderStatus.CANCELLED, PROCESSING_EXPIRED);
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("Сменить статус от имени магазина")
    @Test
        // checkouter-182
    void changeStatusByShop() throws Exception {
        Order order = orderStatusHelper.createOrderWithStatusTransitions(DeliveryProvider.getSelfDelivery());

        order = orderStatusHelper.requestStatusUpdate(order.getId(), ClientRole.SYSTEM,
                String.valueOf(order.getShopId()), OrderStatus.DELIVERY);

        checkStatus(order, OrderStatus.DELIVERY, null);
    }

    @Story(Stories.PAYMENT)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("Переход в Delivered триггерит вызов Balance.UpdatePayment")
    @Test
    void testUpdatePaymentOnDelivered() {
        Order order = createAndPayPrepaidOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CALL_BALANCE_UPDATE_PAYMENT,
                order.getPayment().getId()));
    }

    @Story(Stories.PAYMENT)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("Переход в Delivered триггерит вызов печати чека на Зачёт аванса")
    @Test
    void testGenerateDeliveredReceiptOnDelivered() {
        Parameters orderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(orderParameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT,
                order.getId()));
    }

    @Test
    void testCancelFormDeliveredByReferee() throws Exception {
        Order order = orderStatusHelper.createOrderWithStatusTransitions(DeliveryProvider.getSelfDelivery(),
                OrderStatus.DELIVERY);
        order = orderStatusHelper.requestStatusUpdate(order.getId(), OrderStatus.DELIVERED);
        orderStatusHelper.requestStatusUpdate(order.getId(), ClientRole.REFEREE, "123", OrderStatus.CANCELLED,
                USER_REFUSED_DELIVERY);
    }

    @Test
    void testCancelFormDeliveredByOperator() throws Exception {
        Order order = orderStatusHelper.createOrderWithStatusTransitions(DeliveryProvider.getSelfDelivery(),
                OrderStatus.DELIVERY);
        order = orderStatusHelper.requestStatusUpdate(order.getId(), OrderStatus.DELIVERED);
        orderStatusHelper.requestStatusUpdate(order.getId(), ClientRole.CALL_CENTER_OPERATOR, "123",
                OrderStatus.CANCELLED, USER_REFUSED_DELIVERY);
    }

    @Test
    void testYandexLavkaStatuses() throws Exception {
        Parameters orderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(orderParameters);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        checkStatus(order, OrderStatus.DELIVERY, OrderSubstatus.DELIVERY_SERVICE_RECEIVED);

        // Имитируем чекпоинт или любой другой механизм уведомления, что заказ готов к вручению
        order = orderStatusHelper.requestStatusUpdate(order.getId(), ClientRole.SYSTEM, null, OrderStatus.DELIVERY,
                READY_FOR_LAST_MILE);
        checkStatus(order, OrderStatus.DELIVERY, READY_FOR_LAST_MILE);

        // Имитируем вызов курьера пользователем
        order = orderStatusHelper.requestStatusUpdate(order.getId(), ClientRole.USER,
                String.valueOf(order.getBuyer().getUid()), OrderStatus.DELIVERY, LAST_MILE_STARTED);
        checkStatus(order, OrderStatus.DELIVERY, LAST_MILE_STARTED);

        // Имитируем чекпоинт, что заказ вручен пользователю
        order = orderStatusHelper.requestStatusUpdate(order.getId(), ClientRole.SYSTEM, null, OrderStatus.DELIVERY,
                USER_RECEIVED);
        checkStatus(order, OrderStatus.DELIVERY, USER_RECEIVED);
    }

    private Order createAndPayPrepaidOrder() {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        Order order = orderCreateHelper.createOrder(parameters);
        payHelper.pay(order.getId());
        return order;
    }
}
