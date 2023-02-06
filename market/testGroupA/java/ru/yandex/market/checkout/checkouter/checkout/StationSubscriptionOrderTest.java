package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cart.station.Price;
import ru.yandex.market.checkout.checkouter.cart.station.StationSubscriptionInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubmethod;
import ru.yandex.market.checkout.checkouter.validation.PaymentMethodNotApplicableError;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.matching.CheckoutErrorMatchers;
import ru.yandex.market.checkout.util.mediabilling.MediabillingMockConfigurer;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX;

/**
 * @author ugoryntsev
 */
public class StationSubscriptionOrderTest extends AbstractWebTestBase {

    public static final long BANNED_UID = 42L;
    private static final long MUID = 1L << 60 | 1;
    private static final BigDecimal SUBSCRIPTION_PRICE = BigDecimal.valueOf(369);

    @Autowired
    private MediabillingMockConfigurer mediabillingMockConfigurer;

    private static OrderItem createStationItem() {
        return OrderItemProvider.defaultOrderItem();
    }

    private static OrderItem createAnotherStationItem() {
        return OrderItemProvider.getAnotherOrderItem();
    }

    private static Parameters createParameters() {
        var parameters = BlueParametersProvider
                .defaultBlueOrderParametersWithItems(createStationItem());
        parameters.getReportParameters().setYaSubscriptionOffer(true);
        return parameters;
    }

    @BeforeEach
    public void setUp() {
        mediabillingMockConfigurer.mockWholeMediabilling();
    }

    @Test
    @DisplayName("given_StationSubscriptionEnabled_when_OrderFromTargetShop_then_HasPlusSubscriptionInfo")
    public void testHasPlusSubscriptionInfo() {
        //given
        checkouterProperties.setEnableStationSubscription(true);
        Parameters parameters = createParameters();

        //when
        MultiCart cart = orderCreateHelper.cart(parameters);

        //then
        assertThat(cart.getPlusSubscription(), not(nullValue()));
        StationSubscriptionInfo plusSubscription = cart.getPlusSubscription();
        assertThat(plusSubscription.getSubscriptionPrice(), equalTo(Price.ZERO_RUB_PRICE));
        assertThat(plusSubscription.getStationPrice().getValue(), equalTo(SUBSCRIPTION_PRICE));
        assertThat(plusSubscription.getPayDurationCount(), equalTo(1L));
        assertThat(plusSubscription.getPayDurationType(), equalTo("Month"));
    }

    @Test
    @DisplayName(
            "given_StationSubscriptionDisabled_when_DefaultOrder_then_HasNoPaymentOptionsAndPlusSubscriptionObject")
    public void testDisableStationSubscription() {
        //given
        checkouterProperties.setEnableStationSubscription(false);
        Parameters parameters = createParameters();

        //when
        MultiCart cart = orderCreateHelper.cart(parameters);

        //then
        assertThat(cart.getPaymentOptions(), empty());
        assertNull(cart.getPlusSubscription());
    }

    @Test
    @DisplayName("given_StationSubscriptionOrder_when_CreateOrder_then_HasRealPriceProperty")
    public void testHasRealPriceProperty() throws Exception {
        //given
        checkouterProperties.setEnableStationSubscription(true);
        Parameters parameters = createParameters();

        MultiCart cart = orderCreateHelper.cart(parameters);

        parameters.setUseErrorMatcher(false);

        //when
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        Order order = checkout.getCarts().get(0);

        //then
        Order savedOrder = orderService.getOrder(order.getId());
        assertThat(savedOrder.getProperty(OrderPropertyType.STATION_REAL_PRICE).doubleValue(), is(4990.0));
        assertThat(savedOrder.getPaymentSubmethod(), is(PaymentSubmethod.STATION_SUBSCRIPTION));
    }

    @Test
    @DisplayName("when_CheckoutStationSubscriptionOrder_then_GetOrder")
    public void testOrderCreated() throws Exception {
        //given
        checkouterProperties.setEnableStationSubscription(true);
        checkouterProperties.setEnableInstallments(true);
        Parameters parameters = createParameters();

        MultiCart cart = orderCreateHelper.cart(parameters);

        //when
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        Order order = checkout.getCarts().get(0);

        //then
        OrderItem item = order.getItems().stream().findFirst().get();
        assertThat(item.getBuyerPrice(), is(SUBSCRIPTION_PRICE));
        assertThat(item.getPrices().getBuyerPriceBeforeDiscount(), is(SUBSCRIPTION_PRICE));
        assertThat(order.getPaymentSubmethod(), is(PaymentSubmethod.STATION_SUBSCRIPTION));

        Order savedOrder = orderService.getOrder(order.getId());
        assertThat(savedOrder.getPaymentMethod(), is(YANDEX));
        assertThat(savedOrder.getPaymentSubmethod(), is(PaymentSubmethod.STATION_SUBSCRIPTION));
        assertThat(savedOrder.getItems().stream().findFirst().get().getPrice().doubleValue(),
                is(SUBSCRIPTION_PRICE.doubleValue()));
        assertThat(savedOrder.getItemsTotal().doubleValue(), is(SUBSCRIPTION_PRICE.doubleValue()));
        assertThat(savedOrder.getBuyerItemsTotal().doubleValue(), is(SUBSCRIPTION_PRICE.doubleValue()));
    }

    @Test
    @DisplayName("when_StationSubscriptionOrder_then_OnlyYandexPaymentOption")
    public void testPaymentOptions() {
        //given
        checkouterProperties.setEnableStationSubscription(true);
        Parameters parameters = createParameters();

        //when
        MultiCart cart = orderCreateHelper.cart(parameters);

        //then
        assertThat(cart.getPaymentOptions(), hasSize(1));
        assertThat(cart.getPaymentOptions(), hasItem(PaymentMethod.YANDEX));

        cart.getCarts().get(0).getDeliveryOptions().forEach(delivery -> {
                    assertThat(delivery.getPaymentOptions(), hasSize(1));
                    assertThat(delivery.getPaymentOptions(), hasItem(PaymentMethod.YANDEX));
                }
        );

        assertThat(cart.getCarts().get(0).getPaymentOptions(), hasSize(1));
        assertThat(cart.getCarts().get(0).getPaymentOptions(), hasItem(PaymentMethod.YANDEX));
        assertThat(cart.getCarts().get(0).getPaymentSubmethod(), is(PaymentSubmethod.STATION_SUBSCRIPTION));
    }

    @Test
    @DisplayName("given_NotApplicablePaymentMethod_when_StationSubscriptionOrder_then_Error")
    public void testNotApplicablePaymentMethod() throws Exception {
        //given
        checkouterProperties.setEnableStationSubscription(true);
        Parameters parameters = createParameters();

        //when
        MultiCart cart = orderCreateHelper.cart(parameters);
        parameters.setPaymentMethod(PaymentMethod.APPLE_PAY);
        //then
        parameters.setErrorMatcher(jsonPath("$.validationErrors[0].code")
                .value(PaymentMethodNotApplicableError.CODE));
        orderCreateHelper.checkout(cart, parameters);
    }

    @Test
    @DisplayName("given_StationSubscriptionEnabled_when_TwoOrderFromTargetShop_then_Error")
    public void testItemsCountLimit() {
        //given
        checkouterProperties.setEnableStationSubscription(true);
        Parameters parameters = BlueParametersProvider
                .defaultBlueOrderParametersWithItems(createStationItem(), createAnotherStationItem());
        parameters.getReportParameters().setYaSubscriptionOffer(true);

        //when
        parameters.setErrorMatcher(jsonPath("$.validationErrors[0].code").value("MULTIPLE_ITEMS_NOT_ALLOWED"));
        MultiCart cart = orderCreateHelper.cart(parameters);

        //then
        assertThat(cart.hasErrors(), equalTo(true));
    }

    @Test
    @DisplayName("given_BuyerNotAuthorized_when_OrderFromTargetShop_Then_Error")
    public void testNoAuth() {
        //given
        checkouterProperties.setEnableStationSubscription(true);
        Order order = OrderProvider.getBlueOrder();
        order.setNoAuth(true);
        order.getBuyer().setUid(MUID);
        order.getBuyer().setMuid(MUID);
        order.setItems(List.of(createStationItem()));

        Parameters parameters = BlueParametersProvider
                .defaultBlueOrderParameters(order, order.getBuyer());
        parameters.getReportParameters().setYaSubscriptionOffer(true);

        //then
        parameters.setErrorMatcher(CheckoutErrorMatchers.NO_AUTH);
        parameters.setExpectedCartReturnCode(403);

        //when
        orderCreateHelper.cart(parameters);
    }

    @Test
    @DisplayName("given_BuyerBannedByScoreInMB_when_OrderFromTargetShop_Then_Error")
    public void testBannedUser() {
        //given
        checkouterProperties.setEnableStationSubscription(true);
        Order order = OrderProvider.getBlueOrder();
        order.getBuyer().setUid(BANNED_UID);
        order.getBuyer().setMuid(BANNED_UID);
        order.setItems(List.of(createStationItem()));

        Parameters parameters = BlueParametersProvider
                .defaultBlueOrderParameters(order, order.getBuyer());
        parameters.getReportParameters().setYaSubscriptionOffer(true);

        //then
        parameters.setErrorMatcher(jsonPath("$.validationErrors[0].code").value("UNAPPROVED_SCORE"));
        parameters.setExpectedCartReturnCode(200);

        //when
        orderCreateHelper.cart(parameters);
    }

    @Test
    @DisplayName("given_StationSubscriptionOrder_when_GetOrderEditOptions_then_PaymentOptionsNotChanged")
    public void testNoChangeOrderEditOptions() throws Exception {
        //given
        checkouterProperties.setEnableStationSubscription(true);
        checkouterProperties.setEnableInstallments(true);
        Parameters parameters = createParameters();

        MultiCart cart = orderCreateHelper.cart(parameters);

        parameters.setUseErrorMatcher(false);
        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        Order order = checkout.getCarts().get(0);

        //when
        assertThat(order.getPaymentSubmethod(), is(PaymentSubmethod.STATION_SUBSCRIPTION));

        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        //then
        OrderEditOptions editOptions = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                BuyerProvider.UID, singletonList(BLUE), request);
        assertThat(editOptions.getPaymentOptions(), containsInAnyOrder(YANDEX));
    }

}
