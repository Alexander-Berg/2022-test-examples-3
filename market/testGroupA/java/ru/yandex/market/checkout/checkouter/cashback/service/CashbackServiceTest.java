package ru.yandex.market.checkout.checkouter.cashback.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.CashbackService;
import ru.yandex.market.checkout.checkouter.cashback.CashbackTestBase;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOptions;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.EnsureCashbackEmissionRequest;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.promo.DiscountInformation;
import ru.yandex.market.checkout.checkouter.promo.OrderDiscount;
import ru.yandex.market.checkout.checkouter.promo.OrderItemDiscount;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyContext;
import ru.yandex.market.checkout.helpers.PaymentGetHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.loyalty.api.model.CashbackPermision;
import ru.yandex.market.loyalty.api.model.CashbackRestrictionReason;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_EMIT_CASHBACK;


public class CashbackServiceTest extends CashbackTestBase {

    public static final OfferItemKey ITEM_KEY_1 = OfferItemKey.of("1", 123L, null);
    public static final OfferItemKey ITEM_KEY_2 = OfferItemKey.of("2", 321L, null);
    public static final String ORDER_LABEL_1 = "LABEL_1";
    public static final String ORDER_LABEL_2 = "LABEL_2";
    public static final String ORDER_LABEL_3 = "LABEL_3";
    private static final String DEFAULT_PROMO_KEY = "1";

    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private CashbackService cashbackService;
    @Autowired
    private PaymentGetHelper paymentGetHelper;

    @Test
    @DisplayName("Должны заполнить promos только для заказов с разрешенным кэшбэком и с суммой больше 0")
    void shouldFillPromosForDifferentCashbackInMultiOrder() {
        Order order1 = getOrder(ORDER_LABEL_1);
        Order order2 = getOrder(ORDER_LABEL_2);
        Order order3 = getOrder(ORDER_LABEL_3);
        MultiCart multiCart = MultiCartProvider.buildMultiCart(List.of(order1, order2, order3));
        multiCart.setSelectedCashbackOption(CashbackOption.EMIT);

        cashbackService.applyCashback(discountInformation(), multiCart);

        assertThat(order2.getPromos(), empty());
        assertThat(order3.getPromos(), empty());
        assertThat(order1.getPromos(), hasItem(
                hasProperty("promoDefinition", is(PromoDefinition.cashbackPromo(null, null, null, null)))));
        assertThat(order1.getItem(ITEM_KEY_1).getPromos(), hasItem(
                hasProperty("cashbackAccrualAmount", is(BigDecimal.valueOf(60)))));
        assertThat(order1.getItem(ITEM_KEY_2).getPromos(), empty());
    }

    @Test
    @DisplayName(
            "Должны заполнить promos только для заказов с разрешенным кэшбэком и с суммой больше 0, с другим " +
                    "количеством"
    )
    void shouldFillPromosForDifferentCashbackInMultiOrderWithDifferentQuentity() {
        Order order1 = getOrder(ORDER_LABEL_1, 2);
        Order order2 = getOrder(ORDER_LABEL_2);
        Order order3 = getOrder(ORDER_LABEL_3);
        MultiCart multiCart = MultiCartProvider.buildMultiCart(List.of(order1, order2, order3));
        multiCart.setSelectedCashbackOption(CashbackOption.EMIT);

        cashbackService.applyCashback(discountInformation(), multiCart);

        assertThat(order2.getPromos(), empty());
        assertThat(order3.getPromos(), empty());
        assertThat(order1.getPromos(), hasItem(
                hasProperty("promoDefinition", is(PromoDefinition.cashbackPromo(null, null, null, null)))));
        assertThat(order1.getItem(ITEM_KEY_1).getPromos(), hasItem(
                hasProperty("cashbackAccrualAmount", is(BigDecimal.valueOf(30)))));
        assertThat(order1.getItem(ITEM_KEY_2).getPromos(), empty());
    }

    @Test
    @DisplayName("Должны заполнить restrictionReason, взяв его с уровня item")
    void shouldAlwaysFillRestrictionReason() {
        Order order1 = getOrder(ORDER_LABEL_1);
        MultiCart multiCart = MultiCartProvider.buildMultiCart(List.of(order1));

        cashbackService.fillCashbackInformation(zeroAmountDiscountInformation(), multiCart, new LoyaltyContext(
                Map.of(),
                Map.of(),
                Currency.RUR, Map.of(), Map.of(), Map.of(), null, null));

        assertThat(multiCart.getCashback(), notNullValue());
        assertThat(multiCart.getCashback(), allOf(
                hasProperty("emit", allOf(
                        hasProperty("restrictionReason", is(CashbackRestrictionReason.NOT_SUITABLE_CATEGORY)),
                        hasProperty("type", is(CashbackPermision.RESTRICTED)),
                        hasProperty("amount", is(BigDecimal.ZERO))
                )),
                hasProperty("spend", allOf(
                        hasProperty("restrictionReason", is(CashbackRestrictionReason.NOT_SUITABLE_CATEGORY)),
                        hasProperty("type", is(CashbackPermision.RESTRICTED)),
                        hasProperty("amount", is(BigDecimal.ZERO))
                ))));
    }

    @Test
    void shouldLimitSpendAmountByUserCashbackBalance() throws IOException {
        Order order1 = getOrder(ORDER_LABEL_1);
        MultiCart multiCart = MultiCartProvider.buildMultiCart(List.of(order1));
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockListPaymentMethodsWithoutCashbackAccount();

        cashbackService.fillCashbackInformation(discountInformation(), multiCart, new LoyaltyContext(
                Map.of(),
                Map.of(),
                Currency.RUR, Map.of(), Map.of(), Map.of(), null, null));

        assertThat(multiCart.getCashback(), notNullValue());
        assertThat(multiCart.getCashback(), hasProperty("spend", allOf(
                hasProperty("type", is(CashbackPermision.ALLOWED)),
                hasProperty("amount", is(BigDecimal.ZERO))
        )));
    }

    /**
     * Создаем платеж с начислением кешбэка, но платеж на начисление кешбэка не был проведен.
     * Проверяем, что в итоге будет создан QC на проведение начисления кешбэка.
     *
     * @throws Exception если вдруг что-то плохо
     */
    @Test
    void shouldEnsureCashbackEmissionForDeliveredOrderWithoutCashbackEmitPayment() throws Exception {
        var today = LocalDate.now();
        // создаем платеж c кешбэком и двигаем в DELIVERED
        singleItemWithCashbackParams.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var cart = orderCreateHelper.cart(singleItemWithCashbackParams);
        var checkout = orderCreateHelper.checkout(cart, singleItemWithCashbackParams);
        var order = Iterables.getOnlyElement(checkout.getCarts());
        var orderId = order.getId();
        orderStatusHelper.proceedOrderToStatus(order, DELIVERED);
        var noCashbackParams = BlueParametersProvider.defaultBlueOrderParameters();
        noCashbackParams.setCheckCartErrors(false);
        noCashbackParams.setupPromo("NOT_A_CASHBACK_PROMO");
        var noCashbackCart = orderCreateHelper.cart(noCashbackParams);
        var noCashbackCheckout = orderCreateHelper.checkout(noCashbackCart, noCashbackParams);
        var noCashbackOrder = Iterables.getOnlyElement(noCashbackCheckout.getCarts());
        var noCashbackOrderId = noCashbackOrder.getId();

        var orderPayments = paymentGetHelper.getOrderPayments(orderId, ClientInfo.SYSTEM);
        var cashbackEmitPayments = orderPayments.getItems().stream()
                .filter(p -> p.getType() == PaymentGoal.CASHBACK_EMIT)
                .collect(Collectors.toList());
        assertThat(cashbackEmitPayments, is(emptyIterable()));
        // проверяем начисления кешбэка
        cashbackService.ensureCashbackEmission(new EnsureCashbackEmissionRequest(today, today, DELIVERED, null));
        var queuedCallCreated = queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, orderId);
        var queuedCallCreatedForNoCashbackOrder = queuedCallService
                .existsQueuedCall(ORDER_EMIT_CASHBACK, noCashbackOrderId);
        assertThat(queuedCallCreated, is((true)));
        assertThat(queuedCallCreatedForNoCashbackOrder, is((false)));
    }

    @Test
    void shouldThrowExceptionWhenRequestIsInvalid() {
        var today = LocalDate.now();
        var invalidStatuses = new EnsureCashbackEmissionRequest(today, today, null, null);
        var invalidDates = new EnsureCashbackEmissionRequest(today, today.minusDays(1), DELIVERED, null);
        var invalidDates2 = new EnsureCashbackEmissionRequest(today, today.plusWeeks(1), DELIVERED, null);
        var ex1 = assertThrows(IllegalArgumentException.class,
                () -> cashbackService.ensureCashbackEmission(invalidStatuses));
        var ex2 = assertThrows(IllegalArgumentException.class,
                () -> cashbackService.ensureCashbackEmission(invalidDates));
        var ex3 = assertThrows(IllegalArgumentException.class,
                () -> cashbackService.ensureCashbackEmission(invalidDates2));
        assertThat(ex1.getMessage(), is("Please provide either order status or substatus"));
        assertThat(ex2.getMessage(), is("fromDate should be before toDate"));
        assertThat(ex3.getMessage(), is("Please provide a period of time less than or equal to 2 days"));
    }

    private Order getOrder(String label) {
        return getOrder(label, 1);
    }

    private Order getOrder(String label, int count) {
        return OrderProvider.orderBuilder()
                .label(label)
                .item(OrderItemProvider.orderItemBuilder()
                        .count(count)
                        .offer(ITEM_KEY_1.getFeedId(), ITEM_KEY_1.getOfferId())
                        .build())
                .item(OrderItemProvider.orderItemBuilder()
                        .count(1)
                        .offer(ITEM_KEY_2.getFeedId(), ITEM_KEY_2.getOfferId())
                        .build())
                .build();
    }

    private DiscountInformation discountInformation() {
        return new DiscountInformation(null, null, Collections.emptyList(),
                List.of(
                        new OrderDiscount(ORDER_LABEL_1, itemsDiscounts(), null, null, null,
                                allowed(60)),
                        new OrderDiscount(ORDER_LABEL_2, itemsDiscounts(), null, null, null,
                                restricted()),
                        new OrderDiscount(ORDER_LABEL_3, itemsDiscounts(), null, null, null,
                                allowed(0))), null, allowed(60),
                new HashMap<>(), null);
    }

    private DiscountInformation zeroAmountDiscountInformation() {
        return new DiscountInformation(null, null, Collections.emptyList(),
                List.of(
                        new OrderDiscount(ORDER_LABEL_1, List.of(
                                new OrderItemDiscount(ITEM_KEY_1, null, BigDecimal.valueOf(600),
                                        BigDecimal.ONE,
                                        Collections.emptyList(),
                                        allowed(0)),
                                new OrderItemDiscount(ITEM_KEY_2, null, BigDecimal.valueOf(600),
                                        BigDecimal.ONE,
                                        Collections.emptyList(),
                                        restricted())
                        ), null, null, null,
                                allowed(0))), null,
                allowed(0),
                new HashMap<>(), null);
    }

    private List<OrderItemDiscount> itemsDiscounts() {
        return List.of(
                new OrderItemDiscount(ITEM_KEY_1, null, BigDecimal.valueOf(600), BigDecimal.ONE,
                        Collections.emptyList(),
                        allowed(60)),
                new OrderItemDiscount(ITEM_KEY_2, null, BigDecimal.valueOf(600), BigDecimal.ONE,
                        Collections.emptyList(),
                        restricted())
        );
    }

    private Cashback allowed(long amount) {
        return new Cashback(
                CashbackOptions.allowed(BigDecimal.valueOf(amount), DEFAULT_PROMO_KEY),
                CashbackOptions.allowed(BigDecimal.valueOf(amount), DEFAULT_PROMO_KEY)
        );
    }

    private Cashback restricted() {
        return new Cashback(
                CashbackOptions.restricted(CashbackRestrictionReason.NOT_SUITABLE_CATEGORY),
                CashbackOptions.restricted(CashbackRestrictionReason.NOT_SUITABLE_CATEGORY)
        );
    }
}
