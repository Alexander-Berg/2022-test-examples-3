package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.builders.AbstractPaymentBuilder;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateProductParams;
import ru.yandex.market.checkout.util.loyalty.AbstractLoyaltyBundleResponseTransformer;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_REFUND;
import static ru.yandex.market.checkout.providers.CashbackTestProvider.singleItemCashbackResponse;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkBatchServiceOrderCreationCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkLoadPartnerCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkOptionalCreateServiceProductCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkPayBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsParamsProvider.createBasketParamsForShopDeliveryOrder;

public class LiftOptionsPaymentTest extends AbstractPaymentTestBase {

    /**
     * Число из ELEVATOR_LIFT_PRICE
     */
    public static final BigDecimal LIFT_PRICE = BigDecimal.valueOf(150);
    public static final BigDecimal DELIVERY_PRICE = BigDecimal.valueOf(10);
    public static final BigDecimal TOTAL_DELIVERY_PRICE = BigDecimal.valueOf(150 + 10);

    /**
     * Это дефолтное значение из
     * OrderItemProvider.applyDefaults
     */
    public static final BigDecimal ITEM_PRICE = BigDecimal.valueOf(250);
    public static final BigDecimal TOTAL_PRICE = BigDecimal.valueOf(250 + 10 + 150); // айтемы + доставка + подъем
    public static final BigDecimal CASHBACK_ITEM_PRICE = BigDecimal.valueOf(150.01);
    public static final BigDecimal CASHBACK_TOTAL_PRICE_WITH_CASHBACK = BigDecimal.valueOf(150.01 + 10 + 150);
    public static final BigDecimal CASHBACK_TOTAL_PRICE_WITHOUT_CASHBACK = BigDecimal.valueOf(150.01 + 10 + 150 - 30);
    /**
     * Это число зависит от дефолтного ответа лоялти, который конфигурится в
     * {@link AbstractLoyaltyBundleResponseTransformer}
     */
    private static final BigDecimal CASHBACK_SPEND_ITEM = new BigDecimal(30);
    @Autowired
    public RefundHelper refundHelper;
    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private QueuedCallService queuedCallService;
    private Order order;
    private Parameters parameters;

    @BeforeEach
    public void init() throws Exception {
        super.init();
        OrderItem orderItem = OrderItemProvider.getOrderItem();
        parameters =
                WhiteParametersProvider.shopDeliveryOrder(OrderProvider.orderBuilder()
                        .item(orderItem)
                        .build()
                );
        enableLiftOptions(parameters);
        parameters.getOrder().getDelivery().setPrice(DELIVERY_PRICE);
        parameters.getOrder().getDelivery().setBuyerPrice(DELIVERY_PRICE);
        parameters.getOrder().getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        // самый простой способ задать стоимость подъема
        parameters.getOrder().getDelivery().setLiftType(LiftType.CARGO_ELEVATOR);
        parameters.getReportParameters().setLargeSize(true);
        parameters.setMockLoyalty(true);
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
    }

    @ParameterizedTest(name = "Тип оплаты: {0}. Проверка платежа, чека и запроса в траст " +
            "при создании и оплате заказа с платным подъемом, без субсидий")
    @EnumSource(value = PaymentMethod.class, names = {
            "YANDEX",
            "APPLE_PAY",
            "GOOGLE_PAY",
    })
    public void testPaymentForPrepayOrderWithLifting(PaymentMethod paymentMethod) {
        parameters.setPaymentMethod(paymentMethod);
        order = orderCreateHelper.createOrder(parameters);
        assertEquals(TOTAL_PRICE.compareTo(order.getBuyerTotal()), 0, order.getBuyerTotal() + " vs " + TOTAL_PRICE);

        // проверяем плетеж
        Payment payment = orderPayHelper.pay(order.getId());
        // обновляем заказ, чтобы появились необходимые данные от платежа
        order = orderService.getOrder(order.getId());
        assertEquals(TOTAL_PRICE.compareTo(order.getBuyerTotal()), 0, order.getBuyerTotal() + " vs " + TOTAL_PRICE);
        checkPayment(payment, order, TOTAL_PRICE);

        // проверяем чек
        Receipt receipt = receiptService.findByPayment(payment).iterator().next();
        assertEquals(receipt.getItems().stream().map(ReceiptItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(order.getTotal()), 0);
        assertEquals(receipt.getItems().stream().map(ReceiptItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(TOTAL_PRICE), 0);
        assertNull(payment.getPartitions());

        // проверяем запросы в траст
        OneElementBackIterator<ServeEvent> callIterator = trustMockConfigurer.eventsIterator();
        ShopMetaData shop = shopService.getMeta(order.getShopId());
        checkLoadPartnerCall(callIterator, shop.getClientId());
        checkOptionalCreateServiceProductCall(callIterator, CreateProductParams.product(
                shop.getClientId(),
                "" + shop.getClientId() + "-" + avoidNull(shop.getYaMoneyId(), shop.getCampaignId()),
                avoidNull(shop.getYaMoneyId(), shop.getCampaignId() + "_" + shop.getClientId())
        ));
        checkLoadPartnerCall(callIterator, shop.getClientId());
        checkBatchServiceOrderCreationCall(callIterator, payment.getUid());
        CreateBasketParams basketParams = createBasketParamsForShopDeliveryOrder(order, payment.getId());
        checkCreateBasketCall(callIterator, basketParams);
        checkPayBasketCall(callIterator, payment.getUid(), payment.getBasketKey());
        assertFalse(callIterator.hasNext());
    }

    @ParameterizedTest(name = "Тип оплаты: {0}. Проверка платежа, чека и запроса в траст " +
            "при создании и оплате заказа с платным подъемом, без субсидий")
    @EnumSource(value = PaymentMethod.class, names = {
            "CASH_ON_DELIVERY",
            "CARD_ON_DELIVERY",
    })
    public void testPaymentForPostpaidOrderWithLifting(PaymentMethod paymentMethod) {
        parameters.setPaymentMethod(paymentMethod);
//        ShopMetaData shopMetaData = createCustomNewPrepayMeta((int) WhiteParametersProvider.WHITE_SHOP_ID);
//        shopService.updateMeta(WhiteParametersProvider.WHITE_SHOP_ID, shopMetaData);

        order = orderCreateHelper.createOrder(parameters);
        assertEquals(TOTAL_PRICE.compareTo(order.getBuyerTotal()), 0,
                order.getBuyerTotal() + " vs " + TOTAL_PRICE);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        // дсбс + DELIVERED статус заказа => в очередь кладет только одно событие на субсидийный платеж
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId());
        // после выполенения заказ должен пропасть из очереди
        assertFalse(queuedCallService.existsQueuedCall(ORDER_CREATE_SUBSIDY_PAYMENT, order.getId()));
        // чека и походов в траст нету, так как вся фин часть на стороне магазина
    }

    @DisplayName("Проверка размазывания скидок(кэшбек, спасибо) в заказе со платным подъемом")
    @Test
    public void testPaymentPartitionForOrderWithLiftingDelivery() throws Exception {
        parameters.setCheckCartErrors(false);
        parameters.setupPromo("PROMO");
        parameters.getLoyaltyParameters()
                .setExpectedCashbackOptionsResponse(singleItemCashbackResponse(CASHBACK_SPEND_ITEM));
        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        MultiCart cart = orderCreateHelper.cart(parameters);
        //Заказ с кэшбэком
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        order = orderService.getOrder(checkout.getOrders().get(0).getId());

        // проверяем платеж
        Payment payment = orderPayHelper.pay(order.getId());
        assertEquals(CASHBACK_TOTAL_PRICE_WITH_CASHBACK.compareTo(order.getBuyerTotal()), 0,
                CASHBACK_TOTAL_PRICE_WITH_CASHBACK + " vs " + order.getBuyerTotal());
        assertEquals(CASHBACK_TOTAL_PRICE_WITH_CASHBACK.compareTo(payment.getTotalAmount()), 0,
                CASHBACK_TOTAL_PRICE_WITH_CASHBACK + " vs " + payment.getTotalAmount());
        assertEquals(2, payment.getPartitions().size());
        assertEquals(CASHBACK_SPEND_ITEM.compareTo(payment.amountByAgent(PaymentAgent.YANDEX_CASHBACK)), 0);
        assertEquals(CASHBACK_TOTAL_PRICE_WITHOUT_CASHBACK.compareTo(payment.amountByAgent(PaymentAgent.DEFAULT)), 0);

        // обновляем заказ, чтобы появились необходимые данные от платежа
        order = orderService.getOrder(order.getId());

        // проверяем чек
        Receipt receipt = receiptService.findByPayment(payment).iterator().next();
        assertEquals(receipt.getItems().stream().map(ReceiptItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(order.getTotal()), 0);
        assertEquals(receipt.getItems().stream().map(ReceiptItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(order.getTotal()), 0);

        // проверяем запросы в траст
        OneElementBackIterator<ServeEvent> callIterator = trustMockConfigurer.eventsIterator();
        ShopMetaData shop = shopService.getMeta(order.getShopId());
        checkLoadPartnerCall(callIterator, shop.getClientId());
        checkOptionalCreateServiceProductCall(callIterator, CreateProductParams.product(
                shop.getClientId(),
                "" + shop.getClientId() + "-" + avoidNull(shop.getYaMoneyId(), shop.getCampaignId()),
                avoidNull(shop.getYaMoneyId(), shop.getCampaignId() + "_" + shop.getClientId())
        ));
        checkLoadPartnerCall(callIterator, shop.getClientId());
        checkBatchServiceOrderCreationCall(callIterator, payment.getUid());
        CreateBasketParams basketParams = createBasketParamsForShopDeliveryOrder(order, payment.getId());
        checkCreateBasketCall(callIterator, basketParams);
        checkPayBasketCall(callIterator, payment.getUid(), payment.getBasketKey());
        assertFalse(callIterator.hasNext());
    }

    @SuppressWarnings("checkstyle:HiddenField")
    @DisplayName("Проверка того, что при бесплатной доставке плата за подъем нулевая")
    @Test
    public void freeLiftingTest() {
        Order order = orderServiceTestHelper.createUnpaidBlueOrder(o -> {
            o.getDelivery().setLiftPrice(BigDecimal.ZERO);
            o.getDelivery().setPrice(BigDecimal.ZERO);
            o.getDelivery().setBuyerPrice(BigDecimal.ZERO);
            o.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
            // самый простой способ задать стоимость подъема
            o.getDelivery().setLiftType(LiftType.CARGO_ELEVATOR);
            o.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        });

        // проверяем платеж
        Payment payment = orderPayHelper.pay(order.getId());
        assertNotEquals(0, BigDecimal.ZERO.compareTo(order.getBuyerTotal()));
        assertNotEquals(0, BigDecimal.ZERO.compareTo(payment.getTotalAmount()));

        // проверяем чек
        Receipt receipt = receiptService.findByPayment(payment).iterator().next();
        List<ReceiptItem> receiptDeliveryCollection = receipt.getItems().stream()
                .filter(it -> AbstractPaymentBuilder.DELIVERY_TITLE.equals(it.getItemTitle()))
                .collect(Collectors.toList());

        assertEquals(0, receiptDeliveryCollection.size());
    }

    @SuppressWarnings("checkstyle:HiddenField")
    @DisplayName("Проверка того, что при бесплатной доставке плата за подъем  ненулевая")
    @Test
    public void notFreeLiftingTest() {
        Order order = orderServiceTestHelper.createUnpaidBlueOrder(o -> {
            o.getDelivery().setLiftPrice(BigDecimal.ONE);
            o.getDelivery().setPrice(BigDecimal.ZERO);
            o.getDelivery().setBuyerPrice(BigDecimal.ZERO);
            o.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
            // самый простой способ задать стоимость подъема
            o.getDelivery().setLiftType(LiftType.CARGO_ELEVATOR);
            o.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        });

        // проверяем платеж
        Payment payment = orderPayHelper.pay(order.getId());
        assertNotEquals(0, BigDecimal.ZERO.compareTo(order.getBuyerTotal()));
        assertNotEquals(0, BigDecimal.ZERO.compareTo(payment.getTotalAmount()));

        // проверяем чек
        Receipt receipt = receiptService.findByPayment(payment).iterator().next();
        List<ReceiptItem> receiptDeliveryCollection = receipt.getItems().stream()
                .filter(it -> AbstractPaymentBuilder.DELIVERY_TITLE.equals(it.getItemTitle()))
                .collect(Collectors.toList());

        assertEquals(1, receiptDeliveryCollection.size());

        ReceiptItem deliveryReceipt = receiptDeliveryCollection.get(0);
        assertEquals(0, BigDecimal.ONE.compareTo(deliveryReceipt.getAmount()));
    }

    @ParameterizedTest(name = "Тип оплаты: {0}. Проверка возврата")
    @EnumSource(value = PaymentMethod.class, names = {
            "YANDEX",
            "APPLE_PAY",
            "GOOGLE_PAY",
    })
    public void testRefundPrepayOrderWithLiftingOptions(PaymentMethod paymentMethod) {
        parameters.setPaymentMethod(paymentMethod);
        order = orderCreateHelper.createOrder(parameters);
        Payment payment = orderPayHelper.pay(order.getId());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());
        order = orderStatusHelper.proceedOrderToStatus(order, CANCELLED);
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);
        Refund refund = refundHelper.anyRefundFor(order, PaymentGoal.ORDER_PREPAY);
        refund = refundHelper.proceedAsyncRefund(refund);
        assertEquals(RefundStatus.ACCEPTED, refund.getStatus());
        assertEquals(payment.getTotalAmount().compareTo(refund.getAmount()), 0,
                payment.getTotalAmount() + " vs " + refund.getAmount());
        assertEquals(TOTAL_PRICE.compareTo(refund.getAmount()), 0,
                TOTAL_PRICE + " vs " + refund.getAmount());
        Receipt receipt = receiptService.findByRefund(refund).iterator().next();
        Optional<ReceiptItem> deliverInRefundReceipt = receipt.getItems().stream()
                .filter(ReceiptItem::isDelivery).findFirst();
        assertTrue(deliverInRefundReceipt.isPresent());
        assertEquals(TOTAL_DELIVERY_PRICE.compareTo(deliverInRefundReceipt.get().getAmount()), 0,
                TOTAL_DELIVERY_PRICE + " vs " + deliverInRefundReceipt.get().getAmount());
        assertEquals(ReceiptStatus.NEW, receipt.getStatus());
    }

    @Test
    public void testPayWithEnableLiftingRefundWithDisabledLifting() {
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        order = orderCreateHelper.createOrder(parameters);
        Payment payment = orderPayHelper.pay(order.getId());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());

        checkouterProperties.setEnableLiftOptions(false);
        order = orderStatusHelper.proceedOrderToStatus(order, CANCELLED);
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);
        Refund refund = refundHelper.anyRefundFor(order, PaymentGoal.ORDER_PREPAY);
        refund = refundHelper.proceedAsyncRefund(refund);
        assertEquals(RefundStatus.ACCEPTED, refund.getStatus());
        assertEquals(payment.getTotalAmount().compareTo(refund.getAmount()), 0,
                payment.getTotalAmount() + " vs " + refund.getAmount());
        assertEquals(TOTAL_PRICE.compareTo(refund.getAmount()), 0,
                TOTAL_PRICE + " vs " + refund.getAmount());
        Receipt receipt = receiptService.findByRefund(refund).iterator().next();
        Optional<ReceiptItem> deliverInRefundReceipt = receipt.getItems().stream()
                .filter(ReceiptItem::isDelivery).findFirst();
        assertTrue(deliverInRefundReceipt.isPresent());
        assertEquals(TOTAL_DELIVERY_PRICE.compareTo(deliverInRefundReceipt.get().getAmount()), 0,
                TOTAL_DELIVERY_PRICE + " vs " + deliverInRefundReceipt.get().getAmount());
        assertEquals(ReceiptStatus.NEW, receipt.getStatus());
    }

    private void checkPayment(Payment payment, Order order, BigDecimal total) {
        assertEquals(total.compareTo(order.getBuyerTotal()), 0,
                total + " vs " + order.getBuyerTotal());
        assertEquals(total.compareTo(payment.getTotalAmount()), 0,
                total + " vs " + payment.getTotalAmount());
    }

    private void enableLiftOptions(Parameters parameters) {
        checkouterProperties.setEnableLiftOptions(true);
        parameters.setExperiments(MARKET_UNIFIED_TARIFFS + "=" + MARKET_UNIFIED_TARIFFS_VALUE);
    }
}
