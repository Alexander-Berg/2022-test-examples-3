package ru.yandex.market.checkout.checkouter.service.business;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.Cashback;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.loyalty.api.model.CashbackOptions;
import ru.yandex.market.loyalty.api.model.CashbackResponse;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;
import ru.yandex.market.loyalty.api.model.discount.PriceLeftForFreeDeliveryResponseV3;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentBooleanFeatureType.STORE_TOTAL_ADDITIONAL_MULTICART_CASHBACK;
import static ru.yandex.market.checkout.checkouter.order.OrderPropertyType.TOTAL_MISSED_ORDER_CASHBACK;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.ALICE_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.COIN_THRESHOLD_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.ALREADY_FREE;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS;

public class LoyaltyServiceImplTest extends AbstractWebTestBase {

    private static final String TOTAL_ADDITIONAL_MULTIORDER_CASHBACK_PROPERTY = "totalAdditionalMultiorderCashback";
    private static final String SELECTED_CASHBACK_OPTION = "selectedCashbackOption";
    @Autowired
    private OrderHistoryEventsTestHelper eventsTestHelper;

    @Test
    public void testDeliveryDiscountMapForwarding() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(COIN_THRESHOLD_FREE_DELIVERY,
                        PriceLeftForFreeDeliveryResponseV3
                                .builder()
                                .setPriceLeftForFreeDelivery(BigDecimal.TEN)
                                .setStatus(WILL_BE_FREE_WITH_MORE_ITEMS)
                                .setThreshold(BigDecimal.ONE)
                                .build());
        parameters.getLoyaltyParameters()
                .addDeliveryDiscount(FreeDeliveryReason.ALICE_FREE_DELIVERY,
                        PriceLeftForFreeDeliveryResponseV3
                                .builder()
                                .setPriceLeftForFreeDelivery(BigDecimal.ZERO)
                                .setStatus(FreeDeliveryStatus.ALREADY_FREE)
                                .setThreshold(BigDecimal.TEN)
                                .build());

        var cart = orderCreateHelper.cart(parameters);

        var deliveryDiscountMap = cart.getDeliveryDiscountMap();
        assertThat(deliveryDiscountMap, is(notNullValue()));
        assertThat(deliveryDiscountMap.size(), is(2));

        var first = deliveryDiscountMap.get(COIN_THRESHOLD_FREE_DELIVERY);
        assertThat(first.getStatus(), is(WILL_BE_FREE_WITH_MORE_ITEMS));
        assertThat(first.getPriceLeftForFreeDelivery(), is(BigDecimal.TEN));
        assertThat(first.getThreshold(), is(BigDecimal.ONE));

        var second = deliveryDiscountMap.get(ALICE_FREE_DELIVERY);
        assertThat(second.getStatus(), is(ALREADY_FREE));
        assertThat(second.getPriceLeftForFreeDelivery(), is(BigDecimal.ZERO));
        assertThat(second.getThreshold(), is(BigDecimal.TEN));
    }

    @Test
    public void shouldFillTotalMultiorderCashbackWhenPropertyIsOnAndSelectedCashbackOptionIsEmit() throws Exception {
        checkouterFeatureWriter.writeValue(STORE_TOTAL_ADDITIONAL_MULTICART_CASHBACK, true);
        var parameters = defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getCarts(), hasSize(1));
        var cart = multiOrder.getCarts().get(0);
        Order savedOrder = orderService.getOrder(cart.getId());
        assertThat(cart.getProperty(TOTAL_ADDITIONAL_MULTIORDER_CASHBACK_PROPERTY), is("100"));
        assertThat(savedOrder.getProperty(TOTAL_ADDITIONAL_MULTIORDER_CASHBACK_PROPERTY), is("100"));
    }

    @Test
    public void shouldNotFillTotalMultiorderCashbackWhenSelectedCashbackOptionIsSpend() throws Exception {
        checkouterFeatureWriter.writeValue(STORE_TOTAL_ADDITIONAL_MULTICART_CASHBACK, true);
        var parameters = defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.SPEND);
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getCarts(), hasSize(1));
        var cart = multiOrder.getCarts().get(0);
        Order savedOrder = orderService.getOrder(cart.getId());
        assertThat(cart.getProperty(TOTAL_ADDITIONAL_MULTIORDER_CASHBACK_PROPERTY), is(nullValue()));
        assertThat(savedOrder.getProperty(TOTAL_ADDITIONAL_MULTIORDER_CASHBACK_PROPERTY), is(nullValue()));
    }

    @Test
    public void shouldNotFillTotalMultiorderCashbackWhenPropertyIsOff() throws Exception {
        checkouterFeatureWriter.writeValue(STORE_TOTAL_ADDITIONAL_MULTICART_CASHBACK, false);
        var parameters = defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var multiCart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.checkout(multiCart, parameters);
        assertThat(multiOrder.getCarts(), hasSize(1));
        var cart = multiOrder.getCarts().get(0);
        assertThat(cart.getProperty(TOTAL_ADDITIONAL_MULTIORDER_CASHBACK_PROPERTY), is(nullValue()));
    }

    @Test
    public void shouldFillSelectedCashbackOptionProperty() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.checkout(cart, parameters);
        Order savedOrder = multiOrder.getCarts().get(0);
        assertThat(savedOrder.getProperty(SELECTED_CASHBACK_OPTION), is(CashbackOption.EMIT.name()));
        OrderHistoryEvent orderHistoryEvent = eventsTestHelper.getAllEvents(savedOrder.getId()).iterator().next();
        assertThat(orderHistoryEvent.getOrderAfter().getProperty(SELECTED_CASHBACK_OPTION),
                is(CashbackOption.EMIT.name()));
    }

    @Test
    public void shouldNotFillSelectedCashbackOptionProperty() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setSelectedCashbackOption(null);
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.checkout(cart, parameters);
        Order savedOrder = multiOrder.getCarts().get(0);
        assertThat(savedOrder.getProperty(SELECTED_CASHBACK_OPTION), is(nullValue()));
    }

    @Test
    public void shouldDisableSpendOptionWhenCashbackBalanceNegative() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(
                new CashbackResponse(
                        CashbackOptions.allowed(BigDecimal.ONE),
                        CashbackOptions.allowed(BigDecimal.TEN),
                        CashbackType.EMIT
                )
        );
        trustMockConfigurer.mockNegativeListWalletBalanceResponse();
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertThat(cart.getCashback().getSpend().getAmount(), equalTo(BigDecimal.ZERO));
    }

    @Test
    public void shouldRoundCashbackAmount() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        BigDecimal cashbackEmitAmount = new BigDecimal("2.7");
        BigDecimal cashbackSpendAmount = new BigDecimal("1.2");
        BigDecimal expectedCashbackEmitAmount = BigDecimal.valueOf(2);
        BigDecimal expectedCashbackSpendAmount = BigDecimal.valueOf(1);
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(
                new CashbackResponse(
                        CashbackOptions.allowed(cashbackEmitAmount),
                        CashbackOptions.allowed(cashbackSpendAmount),
                        CashbackType.EMIT
                )
        );
//        мокаем баланс баллов
        trustMockConfigurer.mockWholeTrust();
        MultiCart cart = orderCreateHelper.cart(parameters);
        Cashback cartCashback = cart.getCashback();
        assertThat(cartCashback.getEmit().getAmount(), equalTo(expectedCashbackEmitAmount));
        assertThat(cartCashback.getSpend().getAmount(), equalTo(expectedCashbackSpendAmount));
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        Cashback checkoutCashback = checkout.getCashback();
        assertThat(checkoutCashback.getEmit().getAmount(), equalTo(expectedCashbackEmitAmount));
        assertThat(checkoutCashback.getSpend().getAmount(), equalTo(expectedCashbackSpendAmount));
    }

    @Test
    public void shouldFillTotalMissedOrderCashbackProperty() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        BigDecimal expectedOrderEmitAmount = BigDecimal.valueOf(10000);
        parameters.getLoyaltyParameters().setCalcsExpectedCashbackResponse(
                new CashbackResponse(
                        CashbackOptions.allowed(expectedOrderEmitAmount),
                        CashbackOptions.allowed(BigDecimal.TEN),
                        null
                )
        );
        MultiCart cart = orderCreateHelper.cart(parameters);
        cart.setSelectedCashbackOption(CashbackOption.EMIT);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        assertThat(checkout.getCarts().get(0).getProperty(TOTAL_MISSED_ORDER_CASHBACK), is(expectedOrderEmitAmount));
    }

}
