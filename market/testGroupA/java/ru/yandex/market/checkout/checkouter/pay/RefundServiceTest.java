package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.BasketStatus;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstances;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstancesPutRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.CashParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.pay.RefundTestHelper.refundableItemsFromOrder;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildCheckBasketWithConfirmedRefund;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;


public class RefundServiceTest extends AbstractWebTestBase {

    private static final String TRUST_REFUND_ID = "12345";
    private final String cis1 = "010641944023860921-DLdnD)pMAC1t";
    private final String cis2 = "010641944023860921-DLdnD)pMAC2t";
    private final String cis3 = "010641944023860921-DLdnD)pMAC3t";
    @Autowired
    private RefundService refundService;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private RefundHelper refundHelper;

    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                new Object[]{false},
                new Object[]{true}
        ).map(Arguments::of);
    }

    @BeforeEach
    public void setUp() {
        trustMockConfigurer.mockWholeTrust();
    }

    @Epic(Epics.REFUND)
    @Story(Stories.REFUND)
    @DisplayName("Проверяем, что рефанд сохраняется в статусе INIT при фейле баланса")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void checkRefundInitStatus(boolean divideBasketByItemsInBalance) throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Payment payment = payHelper.payForOrder(order);
        order = orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERY));
        RefundableItems items = refundableItemsFromOrder(order);

        trustMockConfigurer.mockCreateRefund(BasketStatus.error);

        trustMockConfigurer.mockCheckBasket(buildPostAuth(), mappingBuilder -> {
            mappingBuilder.inScenario("Check")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("First check passed");
        });
        trustMockConfigurer.mockCheckBasket(buildCheckBasketWithConfirmedRefund(TRUST_REFUND_ID,
                getTotalRefundAmount(order)), mappingBuilder -> {
            mappingBuilder.inScenario("Check")
                    .whenScenarioStateIs("First check passed");
        });
        trustMockConfigurer.mockStatusBasket(buildCheckBasketWithConfirmedRefund(TRUST_REFUND_ID,
                getTotalRefundAmount(order)), null);

        createRefund(order, payment, items);
        Collection<Refund> refunds = refundService.getRefunds(order.getId());

        assertThat(refunds, hasSize(1));
        final Refund refund = refunds.iterator().next();
        assertThat(refund.getTrustRefundId(), equalTo(TRUST_REFUND_ID));
        var isAsyncRefund = refundHelper.isAsyncRefundStrategyEnabled(refund);
        assertThat(refund.getStatus(), equalTo(isAsyncRefund ? RefundStatus.ACCEPTED : RefundStatus.RETURNED));
    }

    @Epic(Epics.REFUND)
    @Story(Stories.REFUND)
    @DisplayName("Проверяем, что рефанд BLUE заказа в статусе DELIVERED нельзя совершить вне процесса возврата")
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void checkBlueDeliveredRefundFailsWhenNotReturn(boolean divideBasketByItemsInBalance) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Parameters parameters = CashParametersProvider.createCashParameters(true);
            Order order = orderCreateHelper.createOrder(parameters);

            shopService.updateMeta(123, ShopSettingsHelper.createCustomNewPrepayMeta(123));
            shopService.updateMeta(FulfilmentProvider.FF_SHOP_ID, ShopSettingsHelper.createCustomNewPrepayMeta(
                    FulfilmentProvider.FF_SHOP_ID.intValue()));

            orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
            queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);

            //перегружаем чтобы получить нормальный платеж
            RefundableItems refundableItems = refundService.getRefundableItems(orderService.getOrder(order.getId()));
            List<RefundItem> refundItemsList = refundableItems.getItems().stream()
                    .map(ri -> new RefundItem(null, ri.getFeedId(), ri.getOfferId(), ri.getCount(),
                            ri.getQuantityIfExistsOrCount(), false, null))
                    .collect(Collectors.toList());

            RefundItems refundItems = new RefundItems(refundItemsList);
            refundService.createRefund(
                    order.getId(),
                    null,
                    "Возврат денег за уже полученный заказ",
                    new ClientInfo(ClientRole.REFEREE, 135135L),
                    RefundReason.ORDER_CANCELLED,
                    PaymentGoal.ORDER_POSTPAY,
                    false,
                    refundItems,
                    false,
                    null,
                    false
            );
        });
    }

    //https://st.yandex-team.ru/MARKETCHECKOUT-11888
    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void getRefundableItemsForOrderWithFailedReceipt(boolean divideBasketByItemsInBalance) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.NEED_DIVIDE_BASKET_BY_ITEMS_IN_BALANCE_FOR_ALL_ORDERS,
                divideBasketByItemsInBalance);
        OrderItem firstItem = OrderItemProvider.getOrderItem();
        OrderItem secondItem = OrderItemProvider.getAnotherOrderItem();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(firstItem, secondItem);

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());

        createRefundOfFirstItem(order);
        failRefundReceipt(order);

        RefundableItems result = refundService.getRefundableItems(order);
        //Так как чек пофейлился то мы его не вычитаем из списка доступных для рефанда айтемов. Потому здесь 2 а не 1.
        //Но сами айтемы возвращаются;
        assertThat(result.getItems(), hasSize(2));
    }

    @Test
    public void convertRefundWithPartiallySucceededReceipts() {
        OrderItem orderItem = OrderItemProvider.getAnotherOrderItem();
        List<OrderItemInstance> itemInstances = List.of(
                new OrderItemInstance(cis1),
                new OrderItemInstance(cis2),
                new OrderItemInstance(cis3)
        );
        orderItem.setCargoTypes(Set.of(CIS_REQUIRED_CARGOTYPE_CODE));
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(orderItem);
        Order order = orderCreateHelper.createOrder(parameters);

        orderItem = order.getItems().iterator().next();
        orderItem.setPrice(orderItem.getBuyerPrice());
        client.putOrderItemInstances(order.getId(), ClientRole.SYSTEM, 0L,
                new OrderItemInstancesPutRequest(List.of(new OrderItemInstances(orderItem.getId(), itemInstances)))
        );

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());
        Payment payment = order.getPayment();

        RefundableItems allRefundableItems = refundService.getRefundableItems(order);
        allRefundableItems.getItems().forEach(i -> {
            i.setRefundableCount(1);
            i.setRefundableQuantity(BigDecimal.ONE);
        });
        allRefundableItems.getDelivery().setRefundable(false);

        CheckBasketParams config = CheckBasketParams.buildDividedItems(order);
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        List<Refund> firstPartRefund = refundService.createRefund(
                order.getId(), orderItem.getPrice(),
                "Just Test", ClientInfo.SYSTEM, RefundReason.ORDER_CANCELLED,
                payment.getType(), false, allRefundableItems.toRefundItems(),
                false, null, false);
        refundHelper.proceedAsyncRefunds(firstPartRefund);
        succeedRefundReceipt(order);


        allRefundableItems.getItems().forEach(i -> {
            i.setRefundableCount(2);
            i.setRefundableQuantity(BigDecimal.valueOf(2));
        });
        List<Refund> secondPartRefund = refundService.createRefund(
                order.getId(), orderItem.getPrice().multiply(BigDecimal.valueOf(2)),
                "Just Test", ClientInfo.SYSTEM, RefundReason.ORDER_CANCELLED,
                payment.getType(), false, allRefundableItems.toRefundItems(),
                false, null, false);
        refundHelper.proceedAsyncRefunds(secondPartRefund);

        var events = trustMockConfigurer.servedEvents().stream()
                .filter(ev -> ev.getStubMapping().getName().equals(TrustMockConfigurer.CREATE_REFUND_STUB))
                .collect(Collectors.toList());
        String secondRefund = events.get(1).getRequest().getBodyAsString();

        Map<String, String> cisToBalance = new HashMap<>();
        order.getItems().iterator().next()
                .getInstances()
                .forEach(child -> cisToBalance.put(child.get("cis").asText(), child.get("balanceOrderId").asText()));

        assertFalse(secondRefund.contains(cisToBalance.get(cis1)));

        assertTrue(secondRefund.contains(cisToBalance.get(cis2)));
        assertTrue(secondRefund.contains(cisToBalance.get(cis3)));

    }

    private void succeedRefundReceipt(Order order) {
        Refund refund = refundService.getRefunds(order.getId()).iterator().next();
        Receipt receipt = receiptService.findByRefund(refund).iterator().next();
        receiptService.updateReceiptStatus(receipt, ReceiptStatus.PRINTED);
    }

    private void failRefundReceipt(Order order) {
        Refund refund = refundService.getRefunds(order.getId()).iterator().next();
        Receipt receipt = receiptService.findByRefund(refund).iterator().next();
        receiptService.updateReceiptStatus(receipt, ReceiptStatus.FAILED);
    }

    private void createRefundOfFirstItem(Order order) {
        final RefundableItems refundableItems = refundService.getRefundableItems(order);
        RefundableItem refundableItem = refundableItems.getItems().iterator().next();
        RefundableItems itemsToRefund = refundableItems.withItems(Collections.singletonList(refundableItem));
        createRefund(order, order.getPayment(), itemsToRefund);
    }

    private BigDecimal getTotalRefundAmount(Order order) {
        return order.getItems().stream()
                .map(OrderItem::getPrice)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO)
                .add(order.getDelivery().getBuyerPrice());
    }

    private void createRefund(Order order, Payment payment, RefundableItems items) {
        try {
            var refunds = refundService.createRefund(order.getId(), order.getBuyerTotal(), "Just Test",
                    ClientInfo.SYSTEM,
                    RefundReason.ORDER_CANCELLED, payment.getType(), false, items.toRefundItems(), false, null, false);
            refundHelper.proceedAsyncRefunds(refunds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
