package ru.yandex.market.checkout.checkouter.tasks.balance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.helpers.utils.PaymentParameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateDeliveryReceiptCall;

public class OffsetAdvanceReceiptCreatorTaskTest extends AbstractPaymentTestBase {

    private List<Order> orders = new ArrayList<>();


    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptService receiptService;

    @BeforeEach
    public void prepareOrders() {
        orders.add(orderServiceTestHelper.createUnpaidBlueOrder(null));
        orders.add(orderServiceTestHelper.createUnpaidBlueOrder(null));
        trustMockConfigurer.resetRequests();
    }

    @Test
    public void shouldCreateNewReceipt() {
        // Arrange
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT, order.getId()));

        // Act
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        // Assert
        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT, order.getId()));
        final List<Receipt> receipts = receiptService.findByOrder(order.getId(), ReceiptStatus.NEW);
        assertNotNull(receipts);
        assertEquals(1, receipts.size());
        assertEquals(ReceiptType.OFFSET_ADVANCE_ON_DELIVERED, receipts.get(0).getType());
    }


    @Test
    public void checkTrustCall() {
        // Arrange
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT, order.getId()));

        trustMockConfigurer.resetRequests();
        // Act
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        Payment payment = orderService.getOrder(order.getId()).getPayment();

        ServeEvent event = trustMockConfigurer.eventsIterator().next();
        checkCreateDeliveryReceiptCall(event, payment, body -> {
            JsonArray ordersJson = body.getAsJsonArray("orders");

            int linesSize = order.getItems().size();
            Delivery delivery = order.getDelivery();
            if (delivery != null && !delivery.isFree()) {
                linesSize++;
            }
            assertEquals(linesSize, ordersJson.size());
        });
    }

    @Test
    public void shouldCreateReceiptForAllOrdersInFinalStatus() throws Exception {
        ordersPay((new PaymentParameters()).getReturnPath());
        notifyPayment(asIds(), orders.get(0).getPayment());

        //клирим платеж путем перевода заказов в delivery
        paymentTestHelper.tryClearMultipayment(orders, Collections.emptyList());

        orders = getOrdersFromDB();

        Order order = orders.iterator().next();

        //Переводим первый заказ в DELIVERED
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT, order.getId()));

        //Запускаем таску
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);
        List<Receipt> receipts = receiptService.findByOrder(order.getId(), ReceiptStatus.NEW);
        //Доставочный чек есть, причем он на заказ, который в DELIVERY перевели
        assertEquals(1, receipts.size());
        assertEquals(order.getId(), receipts.iterator().next().getItems().iterator().next().getOrderId());

        //Переводим второй заказ в DELIVERED
        Order anotherOrder = orders.get(1);
        orderStatusHelper.proceedOrderToStatus(anotherOrder, OrderStatus.DELIVERED);

        //Запускаем таску еще раз
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        receipts = receiptService.findByOrder(anotherOrder.getId(), ReceiptStatus.NEW);

        //и у другого заказа чек есть, а всего их 2
        assertEquals(2, receipts.size());
        //айтемы второго заказа есть в чеках
        Set<Long> anotherOrderItemsIds = anotherOrder.getItems()
                .stream().map(OrderItem::getId).collect(Collectors.toSet());

        assertTrue(receipts.stream()
                .anyMatch(
                        receipt -> receipt.getItems().stream()
                                .filter(ReceiptItem::isOrderItem)
                                .allMatch(ri -> anotherOrderItemsIds.contains(ri.getItemId()))));


        assertEquals(ReceiptType.OFFSET_ADVANCE_ON_DELIVERED, receipts.iterator().next().getType());
    }

    private List<Long> asIds() {
        return orders.stream().map(Order::getId).collect(Collectors.toList());
    }

    private ResultActions ordersPay(String returnPath) throws Exception {
        ResultActions resultActions = ordersPay(asIds(), returnPath);
        orders = getOrdersFromDB();
        return resultActions;
    }

    private List<Order> getOrdersFromDB() {
        return new ArrayList<>(
                orderService.getOrders(
                        asIds()
                ).values()
        );
    }
}
