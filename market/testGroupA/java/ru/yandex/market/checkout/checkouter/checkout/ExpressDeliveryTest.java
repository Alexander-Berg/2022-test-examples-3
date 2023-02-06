package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.BestDeliveryOptionsActualizer;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.ChangeReason;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.jackson.CheckouterDateFormats;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.rest.Page;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;
import ru.yandex.market.loyalty.api.model.discount.PriceLeftForFreeDeliveryResponseV3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.EXPRESS_DELIVERY;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.PICKUP;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.ActualDeliveryBuilder.DEFAULT_INTAKE_SHIPMENT_DAYS;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.COIN_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryReason.THRESHOLD_FREE_DELIVERY;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.ALREADY_FREE;
import static ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS;

public class ExpressDeliveryTest extends AbstractWebTestBase {

    @Test
    void shouldSetExpressFeatureForExpressDelivery() {
        Parameters blueParams = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        Order order = orderCreateHelper.createOrder(blueParams);

        assertEquals(DELIVERY, order.getDelivery().getType());
        assertThat(order.getDelivery().getFeatures(), contains(EXPRESS_DELIVERY));
    }

    @Test
    public void createExpressOrderwithWrongPaymentMethod() {
        Parameters whiteParameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        whiteParameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        whiteParameters.setCheckCartErrors(false);
        whiteParameters.setCheckOrderCreateErrors(false);

        //
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(whiteParameters);

        //
        Order orderResult = multiOrder.getOrderFailures().get(0).getOrder();
        assertThat(orderResult.getChangesReasons(), hasKey(CartChange.PAYMENT));
        assertEquals(ChangeReason.PAYMENT_METHOD_MISMATCH.name(),
                orderResult.getChangesReasons().get(CartChange.PAYMENT).get(0).getCode());
        assertThat(orderResult.getChanges(), hasItem(CartChange.PAYMENT));
    }

    @Test
    public void createExpressToPickupOrderSuccess() {
        Parameters params = BlueParametersProvider.blueNonFulfilmentOrderWithExpressPickupDelivery();
        params.setCheckOrderCreateErrors(true);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(params);
        Order order = multiOrder.getOrders().get(0);

        assertEquals(PICKUP, order.getDelivery().getType());
        assertThat(order.getDelivery().getFeatures(), hasItem(EXPRESS_DELIVERY));
    }

    @Test
    void shouldSetFreeDeliveryThresholdForCart() {
        var blueParams = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();

        var freeDeliveryThreshold = BigDecimal.valueOf(1000);
        var freeDeliveryRemaining = freeDeliveryThreshold.subtract(blueParams.getOrder().getTotal());

        var actualDelivery = blueParams.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);

        MultiCart multiCart = orderCreateHelper.cart(blueParams);

        Order cart = multiCart.getCartByLabel(blueParams.getOrder().getLabel())
                .orElse(null);
        assertNotNull(cart);
        assertThat(cart.getDeliveryOptions(),
                everyItem(hasProperty("freeDeliveryInfo",
                        hasProperty("freeDeliveryThreshold", equalTo(freeDeliveryThreshold)))));
        assertThat(cart.getDeliveryOptions(),
                everyItem(hasProperty("freeDeliveryInfo",
                        hasProperty("freeDeliveryRemaining", equalTo(freeDeliveryRemaining)))));
    }

    @Test
    void shouldReplaceFreeDeliveryThresholdForSingleExpress() {
        var parameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();

        var freeDeliveryThreshold = BigDecimal.valueOf(1000);
        var freeDeliveryRemaining = freeDeliveryThreshold.subtract(parameters.getOrder().getTotal());

        var actualDelivery = parameters.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);

        BigDecimal loyaltyFreeDeliveryThreshold = BigDecimal.valueOf(2000);
        parameters.getLoyaltyParameters().setFreeDeliveryThreshold(loyaltyFreeDeliveryThreshold);
        BigDecimal loyaltyFreeDeliveryRemaining =
                loyaltyFreeDeliveryThreshold.subtract(parameters.getOrder().getTotal());
        parameters.getLoyaltyParameters().setPriceLeftForFreeDelivery(loyaltyFreeDeliveryRemaining);
        PriceLeftForFreeDeliveryResponseV3 loyaltyDiscountMapValue = new PriceLeftForFreeDeliveryResponseV3(
                loyaltyFreeDeliveryRemaining,
                loyaltyFreeDeliveryThreshold,
                FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS);
        parameters.getLoyaltyParameters().getDeliveryDiscountMap()
                .put(FreeDeliveryReason.BERU_PLUS_FREE_DELIVERY, loyaltyDiscountMapValue);

        var multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getFreeDeliveryThreshold(), equalTo(freeDeliveryThreshold));
        assertThat(multiCart.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
        assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY));
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
        assertThat(multiCart.getDeliveryDiscountMap(), aMapWithSize(1));

        PriceLeftForFreeDeliveryResponseV3 thresholdInfoFromMap = multiCart.getDeliveryDiscountMap()
                .get(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY);
        assertThat(thresholdInfoFromMap.getStatus(), equalTo(FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS));
        assertThat(thresholdInfoFromMap.getThreshold(), equalTo(freeDeliveryThreshold));
        assertThat(thresholdInfoFromMap.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
    }

    @Test
    void shouldNotReplaceFreeDeliveryThresholdForSingleExpressWhenBonusUsed() {
        var parameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();

        var freeDeliveryThreshold = parameters.getOrder().getTotal().subtract(BigDecimal.TEN);
        var freeDeliveryRemaining = BigDecimal.TEN;

        var actualDelivery = parameters.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);
        parameters.getLoyaltyParameters().setFreeDeliveryStatus(ALREADY_FREE);
        parameters.getLoyaltyParameters().setFreeDeliveryReason(COIN_FREE_DELIVERY);

        var multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.COIN_FREE_DELIVERY));
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.ALREADY_FREE));
    }


    @Test
    void shouldReplaceFreeDeliveryThresholdForMultipleExpressCarts() {
        var parameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        parameters.addOrder(BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery());
        parameters.getLoyaltyParameters().setFreeDeliveryStatus(WILL_BE_FREE_WITH_MORE_ITEMS);
        parameters.getLoyaltyParameters().setFreeDeliveryReason(THRESHOLD_FREE_DELIVERY);

        var multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.EXCLUDED_ITEMS));
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.NO_FREE_DELIVERY));
        assertThat(multiCart.getDeliveryDiscountMap(), aMapWithSize(0));
    }

    @Test
    void shouldReplaceFreeDeliveryThresholdForSingleExpressWhenThresholdAchieved() {
        var parameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();

        var freeDeliveryThreshold = parameters.getOrder().getTotal();
        var freeDeliveryRemaining = BigDecimal.ZERO;

        var actualDelivery = parameters.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);

        var multiCart = orderCreateHelper.cart(parameters);

        assertThat(multiCart.getFreeDeliveryThreshold(), equalTo(freeDeliveryThreshold));
        assertThat(multiCart.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
        assertThat(multiCart.getFreeDeliveryReason(), equalTo(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY));
        assertThat(multiCart.getFreeDeliveryStatus(), equalTo(FreeDeliveryStatus.ALREADY_FREE));
        assertThat(multiCart.getDeliveryDiscountMap(), aMapWithSize(1));

        PriceLeftForFreeDeliveryResponseV3 thresholdInfoFromMap = multiCart.getDeliveryDiscountMap()
                .get(FreeDeliveryReason.THRESHOLD_FREE_DELIVERY);
        assertThat(thresholdInfoFromMap.getStatus(), equalTo(FreeDeliveryStatus.ALREADY_FREE));
        assertThat(thresholdInfoFromMap.getThreshold(), equalTo(freeDeliveryThreshold));
        assertThat(thresholdInfoFromMap.getPriceLeftForFreeDelivery(), equalTo(freeDeliveryRemaining));
    }

    @Test
    void shouldNotReplaceFreeDeliveryThresholdForSingleExpressWhenThresholdAchievedWhenSwitchedOff() {
        checkouterProperties.setExpressThresholdCalculation(false);
        var parameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();

        var freeDeliveryThreshold = parameters.getOrder().getTotal();
        var freeDeliveryRemaining = BigDecimal.ZERO;

        var actualDelivery = parameters.getReportParameters().getActualDelivery();
        actualDelivery.setFreeDeliveryThreshold(freeDeliveryThreshold);
        actualDelivery.setFreeDeliveryRemainder(freeDeliveryRemaining);

        var multiCart = orderCreateHelper.cart(parameters);

        assertNull(multiCart.getFreeDeliveryThreshold());
        assertNull(multiCart.getPriceLeftForFreeDelivery());
        assertThat(multiCart.getDeliveryDiscountMap(), aMapWithSize(0));
    }

    @Test
    void shouldFilterByDeliveryFeatures() {
        var expressParameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        Order expressOrder = orderCreateHelper.createOrder(expressParameters);

        var blueOrderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        orderCreateHelper.createOrder(blueOrderParameters);

        OrderSearchRequest orderSearchRequest = OrderSearchRequest.builder()
                .withDeliveryFeatures(EnumSet.of(EXPRESS_DELIVERY))
                .build();
        Page<BasicOrder> orders = orderService.getBasicOrders(orderSearchRequest, ClientInfo.SYSTEM);
        assertThat(orders.getItems(), hasSize(1));
        assertEquals(expressOrder.getId(), orders.getItems().iterator().next().getId());
    }

    @Test
    void shouldFilterByDeliveryFeaturesInOrdersByUid() throws Exception {
        var expressParameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        Order expressOrder = orderCreateHelper.createOrder(expressParameters);
        Long uid = expressParameters.getBuyer().getUid();

        var blueOrderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        blueOrderParameters.getBuyer().setUid(uid);
        orderCreateHelper.createOrder(blueOrderParameters);

        mockMvc.perform(MockMvcRequestBuilders.get("/orders/by-uid/{userId}", uid)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.DELIVERY_FEATURES, EXPRESS_DELIVERY.name())
        )
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].id").value(expressOrder.getId()));
    }

    @Test
    void shouldFillBestDeliveryPrices() {
        var parameters = BlueParametersProvider.blueNonFulfilmentOrderWithExpressDelivery();
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DELIVERY_SERVICE_ID, DEFAULT_INTAKE_SHIPMENT_DAYS, BigDecimal.valueOf(100))
                        .addDelivery(DELIVERY_SERVICE_ID, DEFAULT_INTAKE_SHIPMENT_DAYS, BigDecimal.valueOf(1000))
                        .addDelivery(DELIVERY_SERVICE_ID, DEFAULT_INTAKE_SHIPMENT_DAYS, BigDecimal.valueOf(10000))
                        .build()
        );
        var multiCart = orderCreateHelper.cart(parameters);
        Order cart = multiCart.getCarts().get(0);
        assertThat(cart.hasProperty(OrderPropertyType.BEST_DELIVERY_PRICES), is(true));
        List<Map<String, String>> bestDeliveryPrices = cart.getProperty(OrderPropertyType.BEST_DELIVERY_PRICES);
        assertNotNull(bestDeliveryPrices);
        assertFalse(bestDeliveryPrices.isEmpty());
        Map<String, String> bestPriceOption = bestDeliveryPrices.get(0);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(CheckouterDateFormats.DATE_FORMAT);
        assertThat(bestPriceOption.get(BestDeliveryOptionsActualizer.DATE_FIELD), is(dateFormat.format(new Date())));
    }
}
