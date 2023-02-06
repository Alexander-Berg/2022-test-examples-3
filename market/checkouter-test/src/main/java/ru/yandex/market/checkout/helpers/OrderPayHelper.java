package ru.yandex.market.checkout.helpers;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.base.Throwables;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.balance.BasketStatus;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BalancePaymentStatus;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundItem;
import ru.yandex.market.checkout.checkouter.pay.RefundItems;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplRefundStatus;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentWritingDao;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.viewmodel.CreatePaymentResponse;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.helpers.utils.PaymentParameters;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.checkout.util.sberbank.SberMockConfigurer;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.BNPL_PLAN_CONSTRUCTOR;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SANDBOX;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.STATUS;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildFailCheckBasket;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildTinkoffCessionClear;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildWaitingBankDecision;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildYaCardClear;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

@WebTestHelper
public class OrderPayHelper extends MockMvcAware {

    private final TrustMockConfigurer trustMockConfigurer;

    @Autowired
    protected CheckouterClient checkouterClient;
    @Autowired
    private SberMockConfigurer sberMockConfigurer;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private RefundService refundService;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private PaymentWritingDao paymentWritingDao;

    @Autowired
    public OrderPayHelper(WebApplicationContext webApplicationContext,
                          TestSerializationService testSerializationService,
                          TrustMockConfigurer trustMockConfigurer) {
        super(webApplicationContext, testSerializationService);
        this.trustMockConfigurer = trustMockConfigurer;
    }

    public Payment pay(long orderId) {
        return pay(orderId, PaymentParameters.DEFAULT);
    }

    public Payment payForOrder(Order order) {
        return payForOrder(order, true);
    }

    public Payment payForOrderWithoutNotification(Order order) {
        return payForOrder(order, false);
    }

    public Payment payForOrder(Order order, boolean needNotify) {
        if (OrderTypeUtils.isBusinessClient(order)) {
            payForBusinessOrder(order);
            return null;
        } else {
            return payForPersonOrder(order, needNotify);
        }
    }

    public void payForBusinessOrder(Order order) {
        MockHttpServletRequestBuilder basic = post("/orders/bill-paid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderIds\": [" + order.getId() + "]}");
        try {
            performApiRequest(basic);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Payment payForPersonOrder(Order order, boolean needNotify) {
        PaymentParameters paymentParameters = new PaymentParameters();
        paymentParameters.setUid(order.getBuyer().getUid());
        paymentParameters.setSandbox(order.isFake());
        if (order.getPaymentMethod() == PaymentMethod.CREDIT) {
            sberMockConfigurer.mockWholeSber();
        }

        Payment payment = pay(order.getId(), paymentParameters);
        if (order.getPaymentMethod() == PaymentMethod.CREDIT) {
            sberMockConfigurer.mockGetOrderStatusCompleted();
        }
        if (needNotify) {
            if (order.isBnpl()) {
                notifyBnplPayment(payment);
            } else {
                notifyPayment(payment);
            }
        }
        return payment;
    }

    public Payment payForOrders(List<Order> orders) {
        return payForOrders(orders, true);
    }

    public Payment payForOrdersWithoutNotification(List<Order> orders) {
        return payForOrders(orders, false);
    }

    private Payment payForOrders(List<Order> orders, boolean needNotify) {
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        Order order = orders.get(0);
        PaymentParameters paymentParameters = new PaymentParameters();
        paymentParameters.setUid(order.getBuyer().getUid());
        paymentParameters.setSandbox(order.isFake());

        Payment payment = pay(orderIds, paymentParameters);
        if (needNotify) {
            if (order.isBnpl()) {
                notifyBnplPayment(payment);
            } else {
                notifyPayment(payment);
            }
        }
        return payment;
    }

    public void refundAllOrderItems(Order order) {
        Refund refund = refund(order, null, false);
        notifyRefund(refund);
    }

    public Payment pay(List<Long> orderIds, PaymentParameters paymentParameters) {
        if (!paymentParameters.isSandbox()) {
            // prepare balance
            trustMockConfigurer.mockWholeTrust();
        }
        MockHttpServletRequestBuilder basic = post("/orders/payment/")
                .contentType(MediaType.APPLICATION_JSON)
                .content((new JSONArray(orderIds)).toString())
                .param(CheckouterClientParams.UID, String.valueOf(paymentParameters.getUid()))
                .param(SANDBOX, paymentParameters.isSandbox() ? "1" : "0")
                .param(BNPL_PLAN_CONSTRUCTOR, "split_4_month");
        if (paymentParameters.getReturnPath() != null) {
            basic.param("returnPath", paymentParameters.getReturnPath());
        }

        try {
            return performApiRequest(basic, Payment.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Payment pay(long orderId, PaymentParameters paymentParameters) {
        try {
            if (!paymentParameters.isSandbox()) {
                // prepare balance
                trustMockConfigurer.mockWholeTrust();
            }

            MockHttpServletRequestBuilder basic = post("/orders/{orderId}/payment", orderId)
                    .param(CheckouterClientParams.UID, String.valueOf(paymentParameters.getUid()))
                    .param("returnPath", paymentParameters.getReturnPath())
                    .param(SANDBOX, paymentParameters.isSandbox() ? "1" : "0")
                    .param(BNPL_PLAN_CONSTRUCTOR, "split_4_month");

            // maybe missing params
            if (paymentParameters.getPaymentFormType() != null) {
                basic.param(CheckouterClientParams.PAYMENT_FORM_TYPE, paymentParameters.getPaymentFormType().name());
            }

            return performApiRequest(basic, Payment.class);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public CreatePaymentResponse payWithRealResponse(Order order) {
        try {
            MockHttpServletRequestBuilder basic = post("/orders/{orderId}/payment", order.getId())
                    .param(CheckouterClientParams.UID, String.valueOf(order.getBuyer().getUid()))
                    .param("returnPath", new PaymentParameters().getReturnPath())
                    .param(BNPL_PLAN_CONSTRUCTOR, "split_4_month");
            return performApiRequest(basic, CreatePaymentResponse.class);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public CreatePaymentResponse payWithNoAuthResponse(Order order) {
        try {
            MockHttpServletRequestBuilder basic = post("/orders/{orderId}/payment", order.getId())
                    .param("returnPath", new PaymentParameters().getReturnPath())
                    .param(BNPL_PLAN_CONSTRUCTOR, "split_4_month");
            return performApiRequest(basic, CreatePaymentResponse.class);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void notifyBnplPayment(Payment payment) {
        try {
            MockHttpServletRequestBuilder request = post(
                    "/payments/{paymentId}/notify-basket",
                    payment.getId()
            );
            mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andDo(log());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void notifyPayment(Payment payment) {
        notifyPaymentInner(payment, BalancePaymentStatus.authorized.name());
    }

    public void notifyPaymentClear(Payment payment) {
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        notifyPaymentInner(payment, BalancePaymentStatus.cleared.name());
    }

    public void notifyWaitingBankDecision(Payment payment) {
        trustMockConfigurer.mockCheckBasket(buildWaitingBankDecision());
        trustMockConfigurer.mockStatusBasket(buildWaitingBankDecision(), null);
        notifyPaymentInner(payment, BalancePaymentStatus.wait_for_processing.name());
    }

    public void notifyTinkoffCessionClear(Payment payment) {
        trustMockConfigurer.mockCheckBasket(buildTinkoffCessionClear());
        trustMockConfigurer.mockStatusBasket(buildTinkoffCessionClear(), null);
        notifyPaymentInner(payment, BalancePaymentStatus.cleared.name());
    }

    public void notifyPaymentCancel(Payment payment) {
        trustMockConfigurer.mockCheckBasket(buildFailCheckBasket());
        trustMockConfigurer.mockStatusBasket(buildFailCheckBasket(), null);
        notifyPaymentInner(payment, BalancePaymentStatus.cleared.name());
    }

    public void notifyYaCardCrear(Payment payment) {
        trustMockConfigurer.mockCheckBasket(buildYaCardClear());
        trustMockConfigurer.mockStatusBasket(buildYaCardClear(), null);
        notifyPaymentInner(payment, BalancePaymentStatus.cleared.name());
    }

    public void notifyBnplFinished(Payment payment) {
        notifyBnplFinished(payment, null);
    }

    public void notifyBnplFinished(Payment payment, Long puid) {
        notifyPaymentInner(payment, "bnpl_finished", puid);
    }

    public Payment createUnNotifiedSupplierPayment(Collection<Order> orders) {
        //в таске создаем дополнительный саплаерный платеж
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PAYMENT_CREATE_SUPPLIER_PAYMENT);

        //Проверяем, что в корзине сапплаерного платежа прикопались важные для Траста данные
        List<ServeEvent> trustEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(TrustMockConfigurer.CREATE_BASKET_STUB))
                .collect(Collectors.toList());
        trustEvents.stream().map(e -> e.getRequest().getBodyAsString())
                .forEach(b -> {
                    assertTrue(b.contains("sberbank_credit")); //paymethod_id правильный
                    assertTrue(b.contains("origin_payment_id")); //прикопан исходный платеж для сверок
                    assertTrue(b.contains("fiscal_force")); //да пребудет с тобой фискальная сила!
                });

        //ищем саплаерный платеж. он один на все эти заказы (проверяем это)
        return validateSupplierPayment(orders);
    }

    @Nonnull
    public Payment validateSupplierPayment(Collection<Order> orders) {
        Order someOrder = orders.iterator().next();
        List<Payment> payments = paymentService.getPayments(
                someOrder.getId(),
                ClientInfo.SYSTEM,
                PaymentGoal.SUPPLIER_PAYMENT
        );
        assertFalse(payments.isEmpty());
        Payment supplierPayment = payments.get(0);
        Collection<Long> actualOrderIdsByPayment = orderService.getOrdersByPayment(
                supplierPayment.getId(),
                ClientInfo.SYSTEM
        ).stream().map(Order::getId).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        Collection<Long> expectedOrderIds =
                orders.stream().map(Order::getId).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        assertEquals(expectedOrderIds, actualOrderIdsByPayment);

        //проверяем сумму саплаерного платежа
        BigDecimal ordersTotal = orders.stream().map(Order::getBuyerTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(supplierPayment.getTotalAmount(), numberEqualsTo(ordersTotal));
        return supplierPayment;
    }

    /**
     * создает и проверяет саплаерный платеж. для этого выполняет отложенный вызов, шлет нотификацию, и прочая,прочая
     */
    public Payment doStuffForSupplierPayment(Collection<Order> orders) {
        Payment supplierPayment = createUnNotifiedSupplierPayment(orders);

        //нотифаим этот платеж как успешный
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        notifyPaymentClear(supplierPayment);
        return supplierPayment;
    }

    private void notifyPaymentInner(Payment payment, String status) {
        notifyPaymentInner(payment, status, null);
    }

    private void notifyPaymentInner(Payment payment, String status, Long puid) {
        try {
            MockHttpServletRequestBuilder request = post(
                    "/payments/{paymentId}/notify-basket?status={status}" +
                            "&trust_payment_id={basketId}&purchase_token" +
                            "={purchaseToken}&puid={puid}",
                    payment.getId(),
                    status,
                    payment.getBasketKey().getBasketId(),
                    payment.getBasketKey().getPurchaseToken(),
                    puid
            );
            if (payment.isFake()) {
                //Новая ручка нотифая не умеет в sandbox.
                request = post("/payments/{paymentId}/notify", payment.getId())
                        .param("trust_payment_id", payment.getBasketId())
                        .param(STATUS, BasketStatus.success.name())
                        .param(SANDBOX, Boolean.toString(payment.isFake()));
                performApiRequest(request, Payment.class);
            } else {
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andDo(log());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void notifyPlusPayment(Long paymentId, String status, String trustPaymentId, String purchaseToken)
            throws Exception {
        mockMvc.perform(
                post(
                        "/payments/{paymentId}/notify-basket?status={status}" +
                                "&trust_payment_id={basketId" +
                                "}&purchase_token={purchaseToken}",
                        paymentId,
                        status,
                        trustPaymentId,
                        purchaseToken
                )
        ).andExpect(status().isOk());
    }

    public void oldNotifyPayment(Payment payment, BalancePaymentStatus status) {
        try {
            MockHttpServletRequestBuilder request = post(
                    "/payments/{paymentId}/notify?status={status}" +
                            "&trust_payment_id={basketId}&purchase_token" +
                            "={purchaseToken}",
                    payment.getId(),
                    status.name(),
                    payment.getBasketKey().getBasketId(),
                    payment.getBasketKey().getPurchaseToken()
            );
            if (payment.isFake()) {
                performApiRequest(request, Payment.class);
            } else {
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andDo(log());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public Refund refund(Order order) {
        return refund(order, null, false);
    }

    public Refund refund(Order order, RefundItems refundItems) {
        return refund(order, refundItems, false);
    }

    public Refund refund(Order order, RefundItems refundItems, boolean refundDelivery) {
        if (refundItems == null) {
            refundItems = new RefundItems(order.getItems().stream()
                    .map(RefundItem::of)
                    .collect(toList()));
            if (refundDelivery) {
                refundItems.getItems().add(RefundItem.forDelivery());
            }
        }

        var refund = checkouterClient.refunds().postRefund(order.getId(), ClientRole.REFEREE, 0L,
                order.getShopId(),
                RefundReason.ORDER_CANCELLED, "", false,
                PaymentGoal.ORDER_PREPAY, refundItems, null
        );

        return refundHelper.proceedAsyncRefund(refund);
    }

    public Refund refundByAmount(Order order, BigDecimal amount, boolean refundDelivery) {
        RefundItems refundItems = new RefundItems();
        if (refundDelivery) {
            refundItems.setItems(List.of(RefundItem.forDelivery()));
        }
        return checkouterClient.refunds().postRefund(order.getId(), ClientRole.REFEREE, 0L, order.getShopId(),
                RefundReason.ORDER_CANCELLED, "", false,
                PaymentGoal.ORDER_PREPAY, refundItems, amount.doubleValue()
        );
    }

    public void notifyRefund(Refund refund) {
        Optional.of(refund)
                .map(Refund::getPayment)
                .map(Payment::getType)
                .ifPresentOrElse(paymentGoal -> {
                            if (paymentGoal == PaymentGoal.BNPL) {
                                notifyBnplRefund(refund);
                            } else {
                                notifyRefund(refund, BasketStatus.success);
                            }
                        },
                        () -> notifyRefund(refund, BasketStatus.success)
                );
    }

    public void notifyRefund(Refund refund, BasketStatus status) {
        if (status != BasketStatus.error) {
            trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildReversalWithRefundConfig(
                    refund.getTrustRefundId()));
            trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildReversalWithRefundConfig(
                    refund.getTrustRefundId()), null);
        }
        try {
            MockHttpServletRequestBuilder request = post("/payments/{paymentId}/notify-basket",
                    refund.getId())
                    .param("trust_refund_id", refund.getTrustRefundId())
                    .param(STATUS, status.name())
                    .param(SANDBOX, Boolean.toString(refund.isFake()));

            if (refund.isFake()) {
                performApiRequest(request, Refund.class);
            } else {
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andDo(log());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public void notifyBnplRefund(Refund refund) {
        notifyBnplRefund(refund, BnplRefundStatus.APPROVED);
    }

    public void notifyBnplRefund(Refund refund, BnplRefundStatus refundStatus) {
        try {
            bnplMockConfigurer.mockRefundInfo(refundStatus);
            MockHttpServletRequestBuilder request = post(
                    "/refunds/notify")
                    .param("external_refund_id", refund.getTrustRefundId())
                    .param(STATUS, refundStatus.getValue())
                    .param(SANDBOX, Boolean.toString(refund.isFake()));
            if (refund.isFake()) {
                performApiRequest(request, Refund.class);
            } else {
                mockMvc.perform(request)
                        .andExpect(status().isOk())
                        .andDo(log());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public Collection<Refund> proceedAsyncRefunds(Collection<Refund> refunds) {
        return refunds
                .stream()
                .map(refundHelper::proceedAsyncRefund)
                .collect(toList());
    }

    public void updatePaymentStatus(Long paymentId, PaymentStatus paymentStatus) {
        Payment payment = paymentService.findPayment(paymentId, ClientInfo.SYSTEM);

        transactionTemplate.execute(st -> {
            payment.setStatus(paymentStatus);
            paymentWritingDao.updateStatus(payment, ClientInfo.SYSTEM);
            return true;
        });
    }
}
