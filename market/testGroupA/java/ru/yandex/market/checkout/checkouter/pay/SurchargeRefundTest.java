package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.refund.RefundItemService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.common.report.model.FoodtechType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.ClientInfo.SYSTEM;
import static ru.yandex.market.checkout.checkouter.pay.PaymentGoal.SURCHARGE;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.CREATE_SURCHARGE_PAYMENT;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

/**
 * @author zagidullinri
 * @date 11.05.2022
 */
public class SurchargeRefundTest extends AbstractWebTestBase {
    private static final String CARD_ID = "card-123f";
    private static final String LOGIN_ID = "login_id_123";

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ChangeOrderItemsHelper changeOrderItemsHelper;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private RefundService refundService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private ReturnService returnService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundItemService refundItemService;
    private Order order;


    @BeforeEach
    public void setUp() throws IOException {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_SURCHARGE_ORDER_ACTION, true);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_ADJUST_REFUND_ITEMS_BY_PAYMENT, true);
        trustMockConfigurer.mockWholeTrust();
    }

    @AfterEach
    public void tearDown() {
        trustMockConfigurer.resetAll();
    }

    @Test
    @DisplayName("Проверяем, что полный рефанд корректно возвращает оба платежа (ORDER_PREPAY + SURCHARGE)")
    public void testSurchargeRefundCreation() throws Exception {
        //arrange
        createOrder(2);
        updateItemQuantity(5);
        createSurchargePayment();
        clearPayments();
        //act
        createFullOrderReturn(5, true);
        //assert
        var prepayPayment = order.getPayment();
        var prepayPaymentRefund = refundService.getRefunds(prepayPayment).iterator().next();
        var surchargePayment = paymentService.getPayments(order.getId(), SYSTEM, SURCHARGE).iterator().next();
        var surchargeRefund = refundService.getRefunds(surchargePayment).iterator().next();

        assertThat(prepayPayment.getTotalAmount(), comparesEqualTo(BigDecimal.valueOf(600)));
        assertThat(prepayPaymentRefund.getAmount(), comparesEqualTo(BigDecimal.valueOf(600)));
        checkIfRefundReceiptConformToPaymentReceipt(prepayPayment, prepayPaymentRefund);
        checkIfRefundReceiptConformToRefundItems(prepayPaymentRefund);

        assertThat(surchargePayment.getTotalAmount(), comparesEqualTo(BigDecimal.valueOf(750)));
        assertThat(surchargeRefund.getAmount(), comparesEqualTo(BigDecimal.valueOf(750)));
        checkIfRefundReceiptConformToPaymentReceipt(surchargePayment, surchargeRefund);
        checkIfRefundReceiptConformToRefundItems(surchargeRefund);
    }

    @Test
    @DisplayName("Сначала рефандим часть предоплаты, затем возвращаем оставшуюся часть предоплаты и всю доплату")
    public void refundPartOfPrepayThenFullRefundShouldBeProperlyRefunded() throws Exception {
        //arrange
        createOrder(2);
        updateItemQuantity(5);
        createSurchargePayment();
        clearPayments();
        //act
        createPartialRefund(1);
        Collection<Long> partialRefundIds = refundService.getRefunds(order.getId())
                .stream()
                .map(Refund::getId)
                .collect(Collectors.toSet());
        createFullOrderReturn(4, false);
        //assert
        var prepayPayment = order.getPayment();
        var prepayPaymentRefunds = refundService.getRefunds(prepayPayment);
        Refund partialPrepayRefund = prepayPaymentRefunds.stream()
                .filter(it -> partialRefundIds.contains(it.getId()))
                .findAny()
                .orElseThrow();
        Refund fullPrepayRefund = prepayPaymentRefunds.stream()
                .filter(it -> !partialRefundIds.contains(it.getId()))
                .findAny()
                .orElseThrow();
        var surchargePayment = paymentService.getPayments(order.getId(), SYSTEM, SURCHARGE).iterator().next();
        var surchargeRefund = refundService.getRefunds(surchargePayment).iterator().next();

        //Оплатили 2 товара + доставка
        assertThat(prepayPayment.getTotalAmount(), comparesEqualTo(BigDecimal.valueOf(600)));

        //Доплатили за 3 товара
        assertThat(surchargePayment.getTotalAmount(), comparesEqualTo(BigDecimal.valueOf(750)));

        //Первый рефанд одного товара + доставка
        assertThat(partialPrepayRefund.getAmount(), comparesEqualTo(BigDecimal.valueOf(350)));
        checkIfRefundReceiptConformToRefundItems(partialPrepayRefund);

        //Второй рефанд 4 товаров (1 остался из предоплаты и 3 из доплаты)
        assertThat(fullPrepayRefund.getAmount(), comparesEqualTo(BigDecimal.valueOf(250)));
        checkIfRefundReceiptConformToRefundItems(fullPrepayRefund);
        assertThat(surchargeRefund.getAmount(), comparesEqualTo(BigDecimal.valueOf(750)));
        checkIfRefundReceiptConformToPaymentReceipt(surchargePayment, surchargeRefund);
        checkIfRefundReceiptConformToRefundItems(surchargeRefund);
    }

    @Test
    @DisplayName("Сначала рефандим всю предоплату, затем вторым рефандом возвращаем всю доплату")
    public void refundPrepayThenFullRefundShouldBeProperlyRefunded() throws Exception {
        //arrange
        createOrder(2);
        updateItemQuantity(5);
        createSurchargePayment();
        clearPayments();
        //act
        createPartialRefund(2);
        Collection<Long> partialRefundIds = refundService.getRefunds(order.getId())
                .stream()
                .map(Refund::getId)
                .collect(Collectors.toSet());
        createFullOrderReturn(3, false);
        //assert
        var prepayPayment = order.getPayment();
        var prepayPaymentRefund = refundService.getRefunds(prepayPayment).iterator().next();
        var surchargePayment = paymentService.getPayments(order.getId(), SYSTEM, SURCHARGE).iterator().next();
        var surchargeRefund = refundService.getRefunds(surchargePayment).iterator().next();

        //Оплатили 2 товара + доставка
        assertThat(prepayPayment.getTotalAmount(), comparesEqualTo(BigDecimal.valueOf(600)));

        //Доплатили за 3 товара
        assertThat(surchargePayment.getTotalAmount(), comparesEqualTo(BigDecimal.valueOf(750)));

        //Первый рефанд двух товаров + доставка
        assertThat(prepayPaymentRefund.getAmount(), comparesEqualTo(BigDecimal.valueOf(600)));
        checkIfRefundReceiptConformToPaymentReceipt(prepayPayment, prepayPaymentRefund);
        checkIfRefundReceiptConformToRefundItems(prepayPaymentRefund);

        //Второй рефанд 3 товаров (все из доплаты)
        assertThat(surchargeRefund.getAmount(), comparesEqualTo(BigDecimal.valueOf(750)));
        checkIfRefundReceiptConformToPaymentReceipt(surchargePayment, surchargeRefund);
        checkIfRefundReceiptConformToRefundItems(surchargeRefund);
    }

    @Test
    @DisplayName("Сначала рефандим всю предоплату и часть доплаты, затем вторым рефандом возвращаем остаток доплаты")
    public void refundPrepayAndPartOfSurchargeThenFullRefundShouldBeProperlyRefunded() throws Exception {
        //arrange
        createOrder(2);
        updateItemQuantity(5);
        createSurchargePayment();
        clearPayments();
        //act
        createPartialRefund(3);
        Collection<Long> partialRefundIds = refundService.getRefunds(order.getId())
                .stream()
                .map(Refund::getId)
                .collect(Collectors.toSet());
        createFullOrderReturn(2, false);
        //assert
        var prepayPayment = order.getPayment();
        var prepayPaymentRefund = refundService.getRefunds(prepayPayment).iterator().next();
        var surchargePayment = paymentService.getPayments(order.getId(), SYSTEM, SURCHARGE).iterator().next();
        var surchargeRefunds = refundService.getRefunds(surchargePayment);
        Refund partialSurchargeRefund = surchargeRefunds.stream()
                .filter(it -> partialRefundIds.contains(it.getId()))
                .findAny()
                .orElseThrow();
        Refund fullSurchargeRefund = surchargeRefunds.stream()
                .filter(it -> !partialRefundIds.contains(it.getId()))
                .findAny()
                .orElseThrow();

        //Оплатили 2 товара + доставка
        assertThat(prepayPayment.getTotalAmount(), comparesEqualTo(BigDecimal.valueOf(600)));

        //Доплатили за 3 товара
        assertThat(surchargePayment.getTotalAmount(), comparesEqualTo(BigDecimal.valueOf(750)));

        //Первый рефанд трех товаров (2 из предоплаты и 1 из доплаты) + доставка
        assertThat(prepayPaymentRefund.getAmount(), comparesEqualTo(BigDecimal.valueOf(600)));
        checkIfRefundReceiptConformToPaymentReceipt(prepayPayment, prepayPaymentRefund);
        checkIfRefundReceiptConformToRefundItems(prepayPaymentRefund);
        assertThat(partialSurchargeRefund.getAmount(), comparesEqualTo(BigDecimal.valueOf(250)));
        checkIfRefundReceiptConformToRefundItems(partialSurchargeRefund);

        //Второй рефанд 2 товаров (остаток из доплаты)
        assertThat(fullSurchargeRefund.getAmount(), comparesEqualTo(BigDecimal.valueOf(500)));
        checkIfRefundReceiptConformToRefundItems(fullSurchargeRefund);
    }

    private void checkIfRefundReceiptConformToPaymentReceipt(Payment payment, Refund refund) {
        var paymentReceipts = receiptService.findByPayment(payment);
        var refundReceipts = receiptService.findByRefund(refund);
        assertEquals(1, paymentReceipts.size());
        assertEquals(1, refundReceipts.size());
        var paymentReceipt = paymentReceipts.iterator().next();
        var refundReceipt = refundReceipts.iterator().next();
        assertEquals(ReceiptStatus.PRINTED, refundReceipt.getStatus());
        assertEquals(ReceiptType.INCOME_RETURN, refundReceipt.getType());

        assertEquals(paymentReceipt.getItems().size(), refundReceipt.getItems().size());
        for (ReceiptItem refundRI : refundReceipt.getItems()) {
            ReceiptItem paymentRI = paymentReceipt.getItems()
                    .stream()
                    .filter(refundRI::hasEqualItem)
                    .findAny()
                    .orElse(null);
            assertNotNull(paymentRI);
            assertThat(refundRI.getQuantityIfExistsOrCount(), comparesEqualTo(paymentRI.getQuantityIfExistsOrCount()));
            assertThat(refundRI.getAmount(), comparesEqualTo(paymentRI.getAmount()));
            assertThat(refundRI.getPrice(), comparesEqualTo(paymentRI.getPrice()));
        }
    }

    private void checkIfRefundReceiptConformToRefundItems(Refund refund) {
        var refundItems = refundItemService.getRefundItems(refund.getId());
        var refundReceipt = receiptService.findByRefund(refund).iterator().next();
        assertEquals(refundItems.getItems().size(), refundReceipt.getItems().size());

        for (ReceiptItem receiptItem : refundReceipt.getItems()) {
            RefundItem refundItem = refundItems.getItems()
                    .stream()
                    .filter(item -> receiptItem.hasEqualItem(item, refund.getOrderId()))
                    .findAny()
                    .orElse(null);
            assertNotNull(refundItem);
            assertThat(refundItem.getQuantityIfExistsOrCount(),
                    comparesEqualTo(receiptItem.getQuantityIfExistsOrCount()));
        }
    }

    private void createOrder(int initialCount) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getItems().iterator().next().setCount(initialCount);
        parameters.getItems().iterator().next().setValidIntQuantity(initialCount);
        parameters.setAsyncPaymentCardId(CARD_ID);
        parameters.setLoginId(LOGIN_ID);
        parameters.getReportParameters().setFoodtechType(FoodtechType.EDA_RETAIL.getId());
        order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderService.getOrder(order.getId());
    }

    private void updateItemQuantity(int newCount) throws Exception {
        OrderItem orderItem = order.getItems().iterator().next();
        orderItem.setCount(newCount);
        orderItem.setQuantity(BigDecimal.valueOf(newCount));
        ClientInfo clientInfo = ClientHelper.shopClientFor(order);
        changeOrderItemsHelper.changeOrderItems(Collections.singletonList(orderItem), clientInfo, order.getId())
                .andExpect(status().isOk());
    }


    private void createSurchargePayment() {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());
        assertTrue(queuedCallService.existsQueuedCall(CREATE_SURCHARGE_PAYMENT, order.getId()));
        queuedCallService.executeQueuedCallBatch(CREATE_SURCHARGE_PAYMENT);
    }

    protected void clearPayments() {
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        var surchargePayment = paymentService.getPayments(order.getId(), SYSTEM, SURCHARGE).iterator().next();
        orderPayHelper.notifyPaymentClear(surchargePayment);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        tmsTaskHelper.runCheckPaymentStatusTaskV2();
    }

    private void createPartialRefund(int refundableCount) throws Exception {
        RefundableItems refundableItems = refundHelper.getRefundableItemsFor(order);
        assertThat(refundableItems.getItems(), hasSize(1));
        RefundableItem refundableItem = refundableItems.getItems().iterator().next();
        refundableItem.setRefundableCount(refundableCount);
        refundableItem.setRefundableQuantity(BigDecimal.valueOf(refundableCount));
        refundableItems.getDelivery().setRefundable(true);
        refundHelper.refund(refundableItems, order, RefundReason.USER_RETURNED_ITEM, SC_OK, true);
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
//        refundHelper.proceedAsyncRefunds(order.getId());
//        for (Refund r : refundService.getRefunds(order.getId())) {
//            refundHelper.processToSuccess(r, order);
//        }
    }

    private void createFullOrderReturn(int countToReturn, boolean returnDelivery) throws Exception {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());
        var clientInfo = new ClientInfo(ClientRole.REFEREE, 123123L);
        var returnRequest = ReturnProvider.generateFullReturn(order);
        returnRequest.getItems().stream().filter(ri -> ri.getItemId() != null).forEach(ri -> {
            ri.setCount(countToReturn);
            ri.setValidIntQuantity(countToReturn);
        });
        if (!returnDelivery) {
            returnRequest.getItems().removeIf(it -> it.getItemServiceId() == null && it.getItemId() == null);
        }
        var ret = returnService.initReturn(order.getId(), clientInfo, returnRequest, Experiments.empty());
        ret = returnService.resumeReturn(order.getId(), clientInfo, ret.getId(), ret, true);
        order = orderService.getOrder(order.getId());
        returnService.createAndDoRefunds(ret, order);
        refundHelper.proceedAsyncRefunds(order.getId());
        for (Refund r : refundService.getRefunds(order.getId())) {
            refundHelper.processToSuccess(r, order);
        }
    }
}
