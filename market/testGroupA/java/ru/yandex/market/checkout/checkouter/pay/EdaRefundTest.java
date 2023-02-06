package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.eda.EdaOrderChangePriceRequest;
import ru.yandex.market.checkout.checkouter.order.eda.StorageEdaOrderService;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author : poluektov
 * date: 2021-02-20.
 */
public class EdaRefundTest extends AbstractWebTestBase {

    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private StorageEdaOrderService storageEdaOrderService;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private ReturnService returnService;

    private Order edaOrder;

    @BeforeEach
    public void init() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters().setIsEda(true);
        edaOrder = orderCreateHelper.createOrder(parameters);
        assertEquals(edaOrder.getShopId(), WhiteParametersProvider.WHITE_SHOP_ID);
    }

    //TODO: кейс не работает, так как требует создать ретерн. Думать
//    @Test
    public void testFullRefund() {
        Order edaPaidOrder = orderStatusHelper.proceedOrderToStatus(edaOrder, OrderStatus.PROCESSING);
        orderPayHelper.notifyPaymentClear(edaPaidOrder.getPayment());
        edaPaidOrder = orderService.getOrder(edaPaidOrder.getId());
        orderStatusHelper.proceedOrderToStatus(edaPaidOrder, OrderStatus.DELIVERED);
        Refund refund = orderPayHelper.refundByAmount(edaPaidOrder, edaPaidOrder.getBuyerTotal(), true);
    }

    @Test
    public void testOrderCancel() {
        Order edaPaidOrder = orderStatusHelper.proceedOrderToStatus(edaOrder, OrderStatus.PROCESSING);
        orderPayHelper.notifyPaymentClear(edaPaidOrder.getPayment());
        edaPaidOrder = orderService.getOrder(edaPaidOrder.getId());
        orderStatusHelper.proceedOrderToStatus(edaPaidOrder, OrderStatus.DELIVERY);
        orderStatusHelper.proceedOrderToStatus(edaPaidOrder, OrderStatus.CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, edaOrder.getId()));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, edaOrder.getId());
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, edaOrder.getId()));

        Collection<Refund> refunds = refundService.getRefunds(edaPaidOrder.getId());
        assertEquals(1, refunds.size());
        Refund refund = refunds.iterator().next();
        assertEquals(edaPaidOrder.getBuyerTotal(), refund.getAmount());
    }

    @Test
    public void testOrderCancelAfterPriceReduction() {
        Order edaPaidOrder = orderStatusHelper.proceedOrderToStatus(edaOrder, OrderStatus.PROCESSING);

        client.eda().changeEdaOrderPrice(
                new RequestClientInfo(ClientRole.SHOP_USER, 0L, WhiteParametersProvider.WHITE_SHOP_ID),
                edaPaidOrder.getId(),
                new EdaOrderChangePriceRequest(edaPaidOrder.getItemsTotal().subtract(BigDecimal.TEN)));
        BigDecimal delta = storageEdaOrderService.getPriceDelta(edaPaidOrder.getId());
        assertEquals(BigDecimal.TEN.negate(), delta);

        orderPayHelper.notifyPaymentClear(edaPaidOrder.getPayment());
        edaPaidOrder = orderService.getOrder(edaPaidOrder.getId());
        orderStatusHelper.proceedOrderToStatus(edaPaidOrder, OrderStatus.DELIVERY);

        orderStatusHelper.proceedOrderToStatus(edaPaidOrder, OrderStatus.CANCELLED);
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, edaOrder.getId()));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_REFUND, edaOrder.getId());
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, edaOrder.getId()));

        Collection<Refund> refunds = refundService.getRefunds(edaPaidOrder.getId());
        assertEquals(1, refunds.size());
        Refund refund = refunds.iterator().next();
        assertEquals(edaPaidOrder.getBuyerTotal().add(delta), refund.getAmount());
    }

    @Test
    public void testOrderReturn() {
        Order edaPaidOrder = orderStatusHelper.proceedOrderToStatus(edaOrder, OrderStatus.PROCESSING);
        orderPayHelper.notifyPaymentClear(edaPaidOrder.getPayment());
        edaPaidOrder = orderService.getOrder(edaPaidOrder.getId());
        orderStatusHelper.proceedOrderToStatus(edaPaidOrder, OrderStatus.DELIVERED);

        Return ret = returnHelper.createReturn(edaPaidOrder.getId(),
                ReturnProvider.generateReturnWithDelivery(edaPaidOrder,
                        edaPaidOrder.getDelivery().getDeliveryServiceId()));
        assertEquals(ReturnStatus.REFUND_IN_PROGRESS, ret.getStatus());
        returnService.processReturnPayments(edaPaidOrder.getId(), ret.getId(), ClientInfo.SYSTEM);

        Collection<Refund> refunds = refundService.getRefunds(edaPaidOrder.getId());
        assertEquals(1, refunds.size());
        Refund refund = refunds.iterator().next();
        assertEquals(edaPaidOrder.getBuyerTotal(), refund.getAmount());
    }

    @Test
    public void testItemsOnlyReturnAfterReduction() {
        Order edaPaidOrder = orderStatusHelper.proceedOrderToStatus(edaOrder, OrderStatus.PROCESSING);

        client.eda().changeEdaOrderPrice(
                new RequestClientInfo(ClientRole.SHOP_USER, 0L, WhiteParametersProvider.WHITE_SHOP_ID),
                edaPaidOrder.getId(),
                new EdaOrderChangePriceRequest(edaPaidOrder.getItemsTotal().subtract(BigDecimal.TEN)));
        BigDecimal delta = storageEdaOrderService.getPriceDelta(edaPaidOrder.getId());
        assertEquals(BigDecimal.TEN.negate(), delta);

        orderPayHelper.notifyPaymentClear(edaPaidOrder.getPayment());
        edaPaidOrder = orderService.getOrder(edaPaidOrder.getId());
        orderStatusHelper.proceedOrderToStatus(edaPaidOrder, OrderStatus.DELIVERED);

        Return ret = returnHelper.createReturn(edaPaidOrder.getId(),
                ReturnProvider.generateReturnWithDelivery(edaPaidOrder,
                        edaPaidOrder.getDelivery().getDeliveryServiceId(), false));
        assertEquals(ReturnStatus.REFUND_IN_PROGRESS, ret.getStatus());
        returnService.processReturnPayments(edaPaidOrder.getId(), ret.getId(), ClientInfo.SYSTEM);

        Collection<Refund> refunds = refundService.getRefunds(edaPaidOrder.getId());
        assertEquals(1, refunds.size());
        Refund refund = refunds.iterator().next();
        assertEquals(edaPaidOrder.getBuyerItemsTotal().add(delta), refund.getAmount());
    }
}
