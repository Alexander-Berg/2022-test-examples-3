package ru.yandex.market.checkout.pushapi.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartParameters;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderAcceptHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderParameters;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderStatusHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderStatusParameters;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.shopapi.ShopApiConfigurer;
import ru.yandex.market.request.trace.RequestContextHolder;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.PICKUP;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_EMAIL;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_NAME;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_PHONE;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.DELIVERY_ADDRESS;


public class OrderVisibilityTest extends AbstractOrderVisibilityTestBase {

    @Autowired
    private PushApiCartHelper pushApiCartHelper;
    @Autowired
    private PushApiOrderAcceptHelper orderAcceptHelper;
    @Autowired
    private PushApiOrderStatusHelper orderStatusHelper;
    @Autowired
    private ShopApiConfigurer shopApiConfigurer;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{null, DELIVERY, false},
                new Object[]{null, DELIVERY, true},
                new Object[]{emptyMap(), DELIVERY, false},
                new Object[]{singletonMap(BUYER, true), DELIVERY, false},
                new Object[]{singletonMap(BUYER, false), DELIVERY, false},
                new Object[]{singletonMap(BUYER_EMAIL, true), DELIVERY, false},
                new Object[]{singletonMap(BUYER_EMAIL, false), DELIVERY, false},
                new Object[]{singletonMap(BUYER_NAME, true), DELIVERY, false},
                new Object[]{singletonMap(BUYER_NAME, false), DELIVERY, false},
                new Object[]{singletonMap(BUYER_PHONE, true), DELIVERY, false},
                new Object[]{singletonMap(BUYER_PHONE, false), DELIVERY, false},
                new Object[]{singletonMap(DELIVERY_ADDRESS, true), DELIVERY, false},
                new Object[]{singletonMap(DELIVERY_ADDRESS, false), DELIVERY, false},
                new Object[]{singletonMap(DELIVERY_ADDRESS, false), PICKUP, false},
// закомменчено, пока фукнциональность видимости DELIVERY_DATES и DELIVERY_PRICE выключена
//                new Object[]{singletonMap(DELIVERY_DATES, Boolean.TRUE)},
//                new Object[]{singletonMap(DELIVERY_DATES, Boolean.FALSE)},
//                new Object[]{singletonMap(DELIVERY_PRICE, Boolean.TRUE)},
//                new Object[]{singletonMap(DELIVERY_PRICE, Boolean.FALSE)},
                new Object[]{ImmutableMap.of(BUYER_EMAIL, false, BUYER_PHONE, false, DELIVERY_ADDRESS, true),
                        DELIVERY, false}
        ).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testOrderAccept(Map<OrderVisibility, Boolean> orderVisibilityMap, DeliveryType deliveryType,
                                boolean dropship) throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters();
        prepareDelivery(parameters.getOrder().getDelivery(), deliveryType);
        prepareDropshipOrder(parameters.getOrder(), dropship);
        RequestContextHolder.createNewContext();
        shopApiConfigurer.mockOrderResponse(parameters);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        checkOrderVisibility(orderVisibilityMap, deliveryType, dropship, isShowBuyer(dropship, orderVisibilityMap));
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testOrderStatus(Map<OrderVisibility, Boolean> orderVisibilityMap, DeliveryType deliveryType,
                                boolean dropship) throws Exception {
        PushApiOrderStatusParameters parameters = new PushApiOrderStatusParameters();
        prepareDelivery(parameters.getOrderChange().getDelivery(), deliveryType);
        prepareDropshipOrder(parameters.getOrderChange(), dropship);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);
        mockSettingsForDifferentParameters(parameters);
        orderStatusHelper.orderStatusForActions(parameters)
                .andExpect(status().isOk());

        checkOrderVisibility(orderVisibilityMap, deliveryType, dropship, true);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testCart(Map<OrderVisibility, Boolean> orderVisibilityMap, DeliveryType deliveryType,
                         boolean dropship) throws Exception {
        PushApiCartParameters parameters = new PushApiCartParameters();
        prepareDelivery(parameters.getRequest().getDelivery(), deliveryType);
        prepareDropshipCart(parameters.getRequest(), dropship);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);
        mockSettingsForDifferentParameters(parameters);
        pushApiCartHelper.cart(parameters);

        checkCartVisibility(orderVisibilityMap, deliveryType, dropship);
    }

    @Test
    public void testOrderAcceptWithNullDeliveryPrice() throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters();
        prepareDelivery(parameters.getOrder().getDelivery(), DELIVERY);
        prepareDropshipOrder(parameters.getOrder(), true);
        RequestContextHolder.createNewContext();
        shopApiConfigurer.mockOrderResponse(parameters);
        mockSettingsForDifferentParameters(parameters);

        parameters.getOrder().getDelivery().setPrice(null);
        orderAcceptHelper.orderAcceptForActions(parameters);
    }

    @Test
    public void testOrderAcceptWithNegativeDeliveryPrice() throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters();
        prepareDelivery(parameters.getOrder().getDelivery(), DELIVERY);
        prepareDropshipOrder(parameters.getOrder(), true);
        RequestContextHolder.createNewContext();
        shopApiConfigurer.mockOrderResponse(parameters);
        mockSettingsForDifferentParameters(parameters);

        parameters.getOrder().getDelivery().setPrice(BigDecimal.valueOf(-1));
        orderAcceptHelper.orderAcceptForActions(parameters, 400, true);
    }

    @Test
    public void testCartWithNullDeliveryPrice() throws Exception {
        PushApiCartParameters parameters = new PushApiCartParameters();
        prepareDelivery(parameters.getRequest().getDelivery(), DELIVERY);
        prepareDropshipCart(parameters.getRequest(), true);
        mockSettingsForDifferentParameters(parameters);

        List<DeliveryResponse> pushApiDeliveryResponses = new ArrayList<>();
        pushApiDeliveryResponses.add(DeliveryProvider.shopSelfDelivery().buildResponse(DeliveryResponse::new));

        pushApiDeliveryResponses.get(0).setRegionId(null);
        pushApiDeliveryResponses.get(0).setVat(null);
        pushApiDeliveryResponses.get(0).setPrice(null);
        parameters.setShopCartResponse(new CartResponse(Collections.emptyList(), pushApiDeliveryResponses,
                Collections.emptyList()));

        pushApiCartHelper.cart(parameters);
    }

    @Test
    public void testCartWithNegativeDeliveryPrice() throws Exception {
        PushApiCartParameters parameters = new PushApiCartParameters();
        prepareDelivery(parameters.getRequest().getDelivery(), DELIVERY);
        prepareDropshipCart(parameters.getRequest(), true);
        mockSettingsForDifferentParameters(parameters);

        List<DeliveryResponse> pushApiDeliveryResponses = new ArrayList<>();
        pushApiDeliveryResponses.add(DeliveryProvider.shopSelfDelivery().buildResponse(DeliveryResponse::new));

        pushApiDeliveryResponses.get(0).setRegionId(null);
        pushApiDeliveryResponses.get(0).setVat(null);
        pushApiDeliveryResponses.get(0).setPrice(BigDecimal.valueOf(-1));
        parameters.setShopCartResponse(new CartResponse(Collections.emptyList(), pushApiDeliveryResponses,
                Collections.emptyList()));

        pushApiCartHelper.cartException(parameters, "delivery price is negative");
    }

    private void checkOrderVisibility(Map<OrderVisibility, Boolean> orderVisibilityMap, DeliveryType deliveryType,
                                      boolean dropship, boolean showBuyer) throws IOException {
        checkOrderVisibility(dropship, showBuyer, deliveryType, orderVisibilityMap);
    }

    private void checkCartVisibility(Map<OrderVisibility, Boolean> orderVisibilityMap, DeliveryType deliveryType,
                                     boolean dropship) throws IOException {
        checkCartVisibility(dropship, false, deliveryType, orderVisibilityMap);
    }
}
