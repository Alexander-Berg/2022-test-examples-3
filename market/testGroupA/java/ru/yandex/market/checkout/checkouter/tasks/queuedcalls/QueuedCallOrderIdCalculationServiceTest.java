package ru.yandex.market.checkout.checkouter.tasks.queuedcalls;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCallOrderIdCalculatorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.STARTED_BY_USER;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class QueuedCallOrderIdCalculationServiceTest extends AbstractPaymentTestBase {

    @Autowired
    protected ReturnHelper returnHelper;
    @Autowired
    protected RefundService refundService;
    @Autowired
    private QueuedCallOrderIdCalculatorService calculatorService;
    @Autowired
    private ReceiptService receiptService;

    @Test
    @DisplayName("POSITIVE: проверка, что для всех типов объектов queued call есть функция вычисления order_id")
    void queuedCallObjectTypeTest() {
        for (CheckouterQCObjectType type : CheckouterQCObjectType.values()) {
            if (type == CheckouterQCObjectType.ORDER) {
                assertEquals(1L, calculatorService.calculateOrderId(type, 1L));
            } else {
                assertNull(calculatorService.calculateOrderId(type, 1L));
            }
        }
    }

    @Test
    @DisplayName("POSITIVE: вычисление order_id для QueuedCallObjectType = ORDER")
    void orderQCObjectTypeTest() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        assertEquals(order.getId(), calculatorService.calculateOrderId(CheckouterQCObjectType.ORDER, order.getId()));
    }

    @Test
    @DisplayName("POSITIVE: вычисление order_id для QueuedCallObjectType = PAYMENT")
    void paymentQCObjectTypeTest() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Payment payment = paymentHelper.payForOrder(order);
        assertEquals(order.getId(),
                calculatorService.calculateOrderId(CheckouterQCObjectType.PAYMENT, payment.getId()));
    }

    @Test
    @DisplayName("POSITIVE: вычисление order_id для QueuedCallObjectType = RECEIPT(for payment)")
    void receiptPaymentQCObjectTypeTest() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        paymentHelper.payForOrder(order);
        List<Receipt> receipts = receiptService.findByOrder(order.getId());
        receipts.forEach(receipt -> assertEquals(
                order.getId(),
                calculatorService.calculateOrderId(CheckouterQCObjectType.RECEIPT, receipt.getId())
        ));
    }

    @Test
    @DisplayName("POSITIVE: вычисление order_id для QueuedCallObjectType = RECEIPT(for refund)")
    void receiptRefundQCObjectTypeTest() {
        Order order = createRefundedOrder();

        List<Receipt> receipts = receiptService.findByOrder(order.getId());
        receipts.forEach(receipt -> assertEquals(
                order.getId(),
                calculatorService.calculateOrderId(CheckouterQCObjectType.RECEIPT, receipt.getId())
        ));
    }

    @Test
    @DisplayName("POSITIVE: вычисление order_id для QueuedCallObjectType = REFUND")
    void returnQCObjectTypeTest() {
        Order order = createRefundedOrder();
        Collection<Refund> refunds = refundService.getRefunds(order.getId());

        refunds.forEach(refund -> assertEquals(
                order.getId(),
                calculatorService.calculateOrderId(CheckouterQCObjectType.REFUND, refund.getId())
        ));
    }

    @Test
    @DisplayName("POSITIVE: вычисление order_id для QueuedCallObjectType = TRACK")
    void trackQCObjectTypeTest() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());

        Delivery delivery = order.getDelivery();
        Parcel shipment = new Parcel();
        shipment.addTrack(new Track("iddqd-1", 123L));
        delivery.setParcels(List.of(shipment));

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        order.getDelivery().getTracksJson().forEach(track -> assertEquals(
                order.getId(),
                calculatorService.calculateOrderId(CheckouterQCObjectType.TRACK, track.getId())
        ));
    }

    @Test
    @DisplayName("POSITIVE: вычислить order_id для QueuedCallObjectType = ITEM_SERVICE")
    void itemServiceQCObjectTypeTest() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        OrderItem orderItem = order.getItems().iterator().next();
        ItemService itemService = orderItem.getServices().iterator().next();
        assertNotNull(itemService);
        assertEquals(order.getId(),
                calculatorService.calculateOrderId(CheckouterQCObjectType.ITEM_SERVICE, itemService.getId()));
    }

    @Test
    @DisplayName("POSITIVE: вычислить order_id для QueuedCallObjectType = RETURN_DELIVERY")
    void returnDeliveryQCObjectTypeTest() {
        Pair<Order, Return> initialPair = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(),
                (ret, ord) -> {
                    ret.setStatus(STARTED_BY_USER);
                    return ret;
                });

        Order order = initialPair.getFirst();
        Return orderReturn =
                returnHelper.addReturnDelivery(order, initialPair.getSecond(), returnHelper.getDefaultReturnDelivery());
        ReturnDelivery returnDelivery = orderReturn.getDelivery();
        assertNotNull(returnDelivery);
        assertEquals(order.getId(),
                calculatorService.calculateOrderId(CheckouterQCObjectType.RETURN_DELIVERY, returnDelivery.getId()));
    }

    @Nonnull
    private Order createRefundedOrder() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());

        Return ret = ReturnProvider
                .generateReturnWithDelivery(order, order.getDelivery().getDeliveryServiceId());
        returnHelper.createReturn(order.getId(), ret);
        returnHelper.processReturnPayments(order, ret);

        return order;
    }
}
