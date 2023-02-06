package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.CashbackTestProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.loyalty.response.OrderItemResponseBuilder;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_EMIT_CASHBACK;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class CashbackEmitInfoTest extends AbstractWebTestBase {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private QueuedCallService queuedCallService;

    @Test
    public void shouldFillCashbackEmitInfoWithItemsCashbackWhenPropertyIsOnAndSelectedCashbackOptionIsEmit()
            throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_BOOST_PVZ, Boolean.TRUE);
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getCarts(), hasSize(1));
        var cart = multiOrder.getCarts().get(0);
        Order savedOrder = orderService.getOrder(cart.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO));
        assertThat(savedOrder.getCashbackEmitInfo(), notNullValue());
        assertThat(savedOrder.getCashbackEmitInfo().getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));
        createAndClearCashbackPayment(savedOrder);

        var cashbackPayments = paymentService.getPayments(savedOrder.getId(), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT);
        assertThat(cashbackPayments, hasSize(1));
        assertThat(cashbackPayments.get(0).getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));

        savedOrder = orderService.getOrder(cart.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO));
        assertThat(savedOrder.getCashbackEmitInfo(), notNullValue());
        assertThat(savedOrder.getCashbackEmitInfo().getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));
    }

    @Test
    public void shouldFillCashbackEmitInfoWithItemsCashbackWhenPropertyIsOffAndSelectedCashbackOptionIsEmit()
            throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_BOOST_PVZ, Boolean.FALSE);
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getCarts(), hasSize(1));
        var cart = multiOrder.getCarts().get(0);
        Order savedOrder = orderService.getOrder(cart.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO));
        assertThat(savedOrder.getCashbackEmitInfo(), notNullValue());
        assertThat(savedOrder.getCashbackEmitInfo().getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));
        createAndClearCashbackPayment(savedOrder);

        var cashbackPayments = paymentService.getPayments(savedOrder.getId(), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT);
        assertThat(cashbackPayments, hasSize(1));
        assertThat(cashbackPayments.get(0).getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));

        savedOrder = orderService.getOrder(cart.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO));
        assertThat(savedOrder.getCashbackEmitInfo(), notNullValue());
        assertThat(savedOrder.getCashbackEmitInfo().getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));
    }

    @Test
    public void shouldFillCashbackEmitInfoWithOnlyBoostPvzCashbackWhenPropertyIsOnAndSelectedCashbackOptionIsEmit()
            throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_BOOST_PVZ, Boolean.TRUE);
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        parameters.getLoyaltyParameters().setExpectedCashbackOptionsResponse(
                CashbackTestProvider.onlyOrderCashbackOptionsEmitResponseWithUiPromoFlags(new BigDecimal("100"),
                        List.of("special-pickup-promo")));
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(
                CashbackTestProvider.сashbackEmitResponseWithUiPromoFlags(new BigDecimal("100"),
                        List.of("special-pickup-promo")));
        parameters.getLoyaltyParameters().expectResponseItem(
                OrderItemResponseBuilder.createFrom(parameters.getItems().iterator().next()));
        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getCarts(), hasSize(1));
        var cart = multiOrder.getCarts().get(0);
        Order savedOrder = orderService.getOrder(cart.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO));
        assertThat(savedOrder.getCashbackEmitInfo(), notNullValue());
        assertThat(savedOrder.getCashbackEmitInfo().getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));

        orderStatusHelper.proceedOrderToStatus(savedOrder, OrderStatus.DELIVERED);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, savedOrder.getId()));

        savedOrder = orderService.getOrder(cart.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO));
        assertThat(savedOrder.getPromos(), hasSize(1));
        assertThat(savedOrder.getCashbackEmitInfo(), notNullValue());
        assertThat(savedOrder.getCashbackEmitInfo().getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));
    }

    @Test
    public void shouldNotFillCashbackEmitInfoWithOnlyBoostPvzCashbackWhenPropertyIsOffAndSelectedCashbackOptionIsEmit()
            throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_BOOST_PVZ, Boolean.FALSE);
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        parameters.getLoyaltyParameters().setExpectedCashbackOptionsResponse(
                CashbackTestProvider.onlyOrderCashbackOptionsEmitResponseWithUiPromoFlags(
                        new BigDecimal("100"), List.of("special-pickup-promo")));
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(
                CashbackTestProvider.сashbackEmitResponseWithUiPromoFlags(new BigDecimal("100"),
                        List.of("special-pickup-promo")));
        parameters.getLoyaltyParameters().expectResponseItem(
                OrderItemResponseBuilder.createFrom(parameters.getItems().iterator().next()));
        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getCarts(), hasSize(1));
        var cart = multiOrder.getCarts().get(0);
        Order savedOrder = orderService.getOrder(cart.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO));
        assertThat(savedOrder.getCashbackEmitInfo(), nullValue());

        orderStatusHelper.proceedOrderToStatus(savedOrder, OrderStatus.DELIVERED);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, savedOrder.getId()));
    }

    @Test
    public void shouldFillCashbackEmitInfoWithItemsCashbackAndBoostPvzCashbackAndSelectedCashbackOptionIsEmit()
            throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_BOOST_PVZ, Boolean.TRUE);
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(
                CashbackTestProvider.сashbackEmitResponseWithUiPromoFlags(new BigDecimal("100"),
                        List.of("special-pickup-promo")));
        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getCarts(), hasSize(1));
        var cart = multiOrder.getCarts().get(0);
        Order savedOrder = orderService.getOrder(cart.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO));
        assertThat(savedOrder.getCashbackEmitInfo(), notNullValue());
        assertThat(savedOrder.getCashbackEmitInfo().getTotalAmount(), comparesEqualTo(new BigDecimal("200.00")));

        createAndClearCashbackPayment(savedOrder);
        var cashbackPayments = paymentService.getPayments(savedOrder.getId(), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT);
        assertThat(cashbackPayments, hasSize(1));
        assertThat(cashbackPayments.get(0).getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));

        savedOrder = orderService.getOrder(cart.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO));
        assertThat(savedOrder.getCashbackEmitInfo(), notNullValue());
        assertThat(savedOrder.getCashbackEmitInfo().getTotalAmount(), comparesEqualTo(new BigDecimal("200.00")));
    }

    @Test
    public void shouldFillCashbackEmitInfoWithItemsCashbackAndBoostPvzCashbackWhenSelectedCashbackOptionIsEmit()
            throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLED_BOOST_PVZ, Boolean.FALSE);
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(
                CashbackTestProvider.сashbackEmitResponseWithUiPromoFlags(new BigDecimal("100"),
                        List.of("special-pickup-promo")));
        parameters.setMockLoyalty(true);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getCarts(), hasSize(1));
        var cart = multiOrder.getCarts().get(0);
        Order savedOrder = orderService.getOrder(cart.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO));
        assertThat(savedOrder.getCashbackEmitInfo(), notNullValue());
        assertThat(savedOrder.getCashbackEmitInfo().getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));

        createAndClearCashbackPayment(savedOrder);
        var cashbackPayments = paymentService.getPayments(savedOrder.getId(), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT);
        assertThat(cashbackPayments, hasSize(1));
        assertThat(cashbackPayments.get(0).getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));

        savedOrder = orderService.getOrder(cart.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO));
        assertThat(savedOrder.getCashbackEmitInfo(), notNullValue());
        assertThat(savedOrder.getCashbackEmitInfo().getTotalAmount(), comparesEqualTo(new BigDecimal("100.00")));
    }

    @Test
    void shouldFillEmitSelectedCashbackOptionFromLoyalty() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        BigDecimal expectedEmitCashback = BigDecimal.TEN;
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                CashbackOptions.allowed(expectedEmitCashback), CashbackOptions.allowed(BigDecimal.ONE),
                CashbackType.EMIT
        ));
        MultiCart cart = orderCreateHelper.cart(parameters);
        cart.setSelectedCashbackOption(null);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        assertThat(checkout.getSelectedCashbackOption(), is(CashbackOption.EMIT));
        Order order = orderService.getOrder(checkout.getOrders().get(0).getId());
        createAndClearCashbackPayment(order);
        List<Payment> payments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT);
        assertThat(payments, hasSize(1));
        assertThat(payments.get(0).getTotalAmount(), comparesEqualTo(expectedEmitCashback));
    }

    @Test
    void shouldFillSpendSelectedCashbackOptionFromLoyalty() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                CashbackOptions.allowed(BigDecimal.TEN), CashbackOptions.allowed(BigDecimal.ONE),
                CashbackType.SPEND
        ));
        MultiCart cart = orderCreateHelper.cart(parameters);
        cart.setSelectedCashbackOption(null);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        assertThat(checkout.getSelectedCashbackOption(), is(CashbackOption.SPEND));
    }

    @Test
    void shouldNotFillSpendSelectedCashbackOptionFromLoyalty() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                CashbackOptions.allowed(BigDecimal.TEN), CashbackOptions.allowed(BigDecimal.ZERO),
                CashbackType.SPEND
        ));
        MultiCart cart = orderCreateHelper.cart(parameters);
        cart.setSelectedCashbackOption(null);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        assertThat(checkout.getSelectedCashbackOption(), nullValue());
    }

    @Test
    void shouldFillNullSelectedCashbackOptionFromLoyalty() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(new CashbackResponse(
                CashbackOptions.allowed(BigDecimal.TEN), CashbackOptions.allowed(BigDecimal.ONE),
                null
        ));
        MultiCart cart = orderCreateHelper.cart(parameters);
        cart.setSelectedCashbackOption(CashbackOption.EMIT);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        assertThat(checkout.getSelectedCashbackOption(), nullValue());
    }

    private Payment createAndClearCashbackPayment(Order order) {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        var cashbackEmitPayment = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT).iterator().next();
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);
        orderPayHelper.notifyPaymentClear(cashbackEmitPayment);
        return cashbackEmitPayment;
    }

}
