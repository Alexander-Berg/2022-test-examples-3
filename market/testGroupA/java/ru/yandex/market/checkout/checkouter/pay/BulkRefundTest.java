package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.util.FormatUtils.toJson;

public class BulkRefundTest extends AbstractWebTestBase {

    private static final String PROMO_CODE = "promo";
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private OrderPayHelper payHelper;

    private Order order;

    @BeforeEach
    public void prepareOrderWithSubsidy() {
        order = createAdditionalOrder();
    }

    @Test
    public void testRefundOnlySubsidy() {
        RefundableItems refundableItemsBefore = refundService.getRefundableItems(order);

        Payment subsidy = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.SUBSIDY).get(0);

        BulkRefundResponseBody response = refundService.createBulkRefunds(List.of(
                makeRefundItems(order, subsidy.getId(), true)
        ));

        assertEquals(1, response.getCreatedRefunds().size());
        BulkOrderRefundResult firstCreatedRefunds = response.getCreatedRefunds().get(0);
        assertEquals("CREATED", firstCreatedRefunds.getStatus());
        //Должен быть только 1 рефанд, так как основной платеж мы не должны затронуть.
        assertEquals(1, firstCreatedRefunds.getRefundIds().size());

        Long refundId = firstCreatedRefunds.getRefundIds().get(0);
        Refund refund = refundService.getRefund(refundId);
        assertEquals(RefundStatus.DRAFT, refund.getStatus());
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.PROCESS_REFUND, refundId));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.PROCESS_REFUND, refundId);

        RefundableItems refundableItemsAfter = refundService.getRefundableItems(order);
        refund = refundService.getRefund(refundId);
        //Чекаем что рефанд относится к субсидии
        Payment payment = paymentService.findPayment(refund.getPaymentId(), ClientInfo.SYSTEM);
        assertEquals(PaymentGoal.SUBSIDY, payment.getType());
        assertEquals(RefundStatus.ACCEPTED, refund.getStatus());
        //Кол-во доступных айтемов для рефанда не поменялось.
        assertEquals(refundableItemsAfter.getItems().size(), refundableItemsBefore.getItems().size());
    }

    @Test
    public void testMultipleOrdersRefund() {
        Order order2 = createAdditionalOrder();
        Order order3 = createAdditionalOrder();

        Payment subsidy1 = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.SUBSIDY).get(0);
        Payment subsidy2 = paymentService.getPayments(order2.getId(), ClientInfo.SYSTEM, PaymentGoal.SUBSIDY).get(0);
        Payment subsidy3 = paymentService.getPayments(order3.getId(), ClientInfo.SYSTEM, PaymentGoal.SUBSIDY).get(0);

        BulkRefundResponseBody response = refundService.createBulkRefunds(List.of(
                makeRefundItems(order, subsidy1.getId(), true),
                makeRefundItems(order2, subsidy2.getId(), true),
                makeRefundItems(order3, subsidy3.getId(), true)
        ));
        assertEquals(3, response.getCreatedRefunds().size());
    }

    @Test
    public void testController() throws Exception {
        BulkRefundRequestBody body = new BulkRefundRequestBody();

        Payment subsidy = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM, PaymentGoal.SUBSIDY).get(0);

        body.setOrderRefundItems(List.of(makeRefundItems(order, subsidy.getId(), true)));
        MvcResult result = mockMvc.perform(
                        post("/refunds/bulkCreate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(body)))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void testControllerEmptyGoal() throws Exception {
        BulkRefundRequestBody body = new BulkRefundRequestBody();
        body.setOrderRefundItems(List.of(makeRefundItems(order, null, true)));
        MvcResult result = mockMvc.perform(
                        post("/refunds/bulkCreate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    private OrderRefundItems makeRefundItems(Order order, Long paymentId, boolean cascade) {
        OrderRefundItems orderRefund = new OrderRefundItems();
        orderRefund.setOrderId(order.getId());
        orderRefund.setItems(order.getItems().stream().map(RefundItem::of).collect(Collectors.toList()));
        orderRefund.setPaymentId(paymentId);
        orderRefund.setCascade(cascade);
        return orderRefund;
    }


    private Order createAdditionalOrder() {
        Parameters parameters = BlueParametersProvider.prepaidBlueOrderParameters();
        //чтобы были субсидии за офферы и за доставку
        parameters.setupPromo(PROMO_CODE);
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(DeliveryType.PICKUP, LoyaltyDiscount.builder()
                        .discount(BigDecimal.valueOf(50L))
                        .promoKey(PROMO_CODE)
                        .promoType(PromoType.MARKET_COUPON).build());
        Order newOrder = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(newOrder, true);
        newOrder = orderService.getOrder(newOrder.getId());
        orderStatusHelper.proceedOrderToStatus(newOrder, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        return orderService.getOrder(newOrder.getId());
    }
}
