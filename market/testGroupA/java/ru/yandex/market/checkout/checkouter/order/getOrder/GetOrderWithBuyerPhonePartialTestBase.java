package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_BUYER_PHONE_PARTIAL;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_PHONE_ID;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_PHONE;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.PHONE;

/**
 * Тесты обратной совместимости, использующие открытый номера телефона покупателя.
 * Удалить в MARKETCHECKOUT-27094
 */
public abstract class GetOrderWithBuyerPhonePartialTestBase extends AbstractWebTestBase {

    protected Order order;

    @Autowired
    protected YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    protected OrderUpdateService orderUpdateService;

    protected static Stream<Arguments> nonBuyerPhonePartialsTestData() {
        return Stream.of(
                new Object[]{null},
                new Object[]{OptionalOrderPart.BUYER},
                new Object[]{OptionalOrderPart.DELIVERY}
        ).map(Arguments::of);
    }

    protected static Stream<Arguments> partialsTestData() {
        return Stream.of(
                new Object[]{null},
                new Object[]{OptionalOrderPart.BUYER},
                new Object[]{OptionalOrderPart.BUYER_PHONE},
                new Object[]{OptionalOrderPart.DELIVERY}
        ).map(Arguments::of);
    }

    @BeforeEach
    public void init() {
        checkouterFeatureWriter.writeValue(ENABLE_BUYER_PHONE_PARTIAL, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, false);
    }

    public void createOrder(boolean phoneVisibleForShop) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuyer().setPersonalPhoneId(null);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        Long shopId = parameters.getOrder().getShopId();
        ShopMetaData shopMetaData = parameters.getShopMetaData().get(shopId);
        ShopMetaData tunedShopMetaData = ShopMetaDataBuilder.createCopy(shopMetaData)
                .withOrderVisibilityMap(ImmutableMap.of(BUYER, true, BUYER_PHONE, phoneVisibleForShop))
                .build();
        parameters.addShopMetaData(shopId, tunedShopMetaData);
        order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
    }

    @Test
    public void whenBuyerPhoneIsNotVisibleInMetaDataWithBuyerPhonePartialShouldReturnPhone() throws Exception {
        // Assign
        createOrder(false);
        var builder = addClientRole(requestBuilder());
        // Act
        var result = mockMvc.perform(builder
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.BUYER_PHONE.name()));
        // Assert
        commonExpects(result)
                .andExpect(getBuyerPhoneMatcher());
    }

    @ParameterizedTest
    @MethodSource("nonBuyerPhonePartialsTestData")
    public void whenBuyerPhoneIsNotVisibleInMetaDataWithoutBuyerPhonePartialShouldNotReturnPhone(
            OptionalOrderPart partial) throws Exception {
        // Assign
        createOrder(false);
        var builder = addClientRole(requestBuilder());
        if (partial != null) {
            builder = builder.param(CheckouterClientParams.OPTIONAL_PARTS, partial.name());
        }
        // Act
        var result = mockMvc.perform(builder);
        // Assert
        commonExpects(result)
                .andExpect(getNoBuyerPhoneMatcher());
    }

    @ParameterizedTest
    @MethodSource("partialsTestData")
    public void whenBuyerPhoneIsVisibleInMetaDataShouldReturnPhoneWithAnyPartial(OptionalOrderPart partial)
            throws Exception {
        // Assign
        createOrder(true);
        var builder = addClientRole(requestBuilder());
        if (partial != null) {
            builder = builder.param(CheckouterClientParams.OPTIONAL_PARTS, partial.name());
        }
        // Act
        var result = mockMvc.perform(builder);
        // Assert
        commonExpects(result)
                .andExpect(getBuyerPhoneMatcher());
    }

    @Test
    public void whenBuyerPhoneIsNotVisibleInMetaDataWithBuyerPhonePartialWithoutToggleShouldNotReturnPhone()
            throws Exception {
        // Assign
        checkouterProperties.setEnableBuyerPhonePartial(false);
        createOrder(false);
        var builder = addClientRole(requestBuilder());
        // Act
        var result = mockMvc.perform(builder
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.BUYER_PHONE.name()));
        // Assert
        commonExpects(result)
                .andExpect(getNoBuyerPhoneMatcher());
    }

    @ParameterizedTest
    @MethodSource("nonBuyerPhonePartialsTestData")
    public void whenBuyerPhoneIsNotVisibleInMetaDataWithoutBuyerPhonePartialWithoutToggleShouldNotReturnPhone(
            OptionalOrderPart partial) throws Exception {
        checkouterProperties.setEnableBuyerPhonePartial(false);
        whenBuyerPhoneIsNotVisibleInMetaDataWithoutBuyerPhonePartialShouldNotReturnPhone(partial);
    }

    @ParameterizedTest
    @MethodSource("partialsTestData")
    public void whenBuyerPhoneIsVisibleInMetaDataWithoutToggleShouldReturnPhoneWithAnyPartial(
            OptionalOrderPart partial)
            throws Exception {
        checkouterProperties.setEnableBuyerPhonePartial(false);
        whenBuyerPhoneIsVisibleInMetaDataShouldReturnPhoneWithAnyPartial(partial);
    }

    protected abstract MockHttpServletRequestBuilder requestBuilder() throws Exception;

    protected abstract String getOrderRoot();

    protected ResultActions commonExpects(ResultActions result) throws Exception {
        return result.andExpect(status().isOk());
    }

    protected ResultMatcher getBuyerPhoneMatcher() {
        return jsonPath(getOrderRoot() + ".buyer.phone")
                .value(PHONE);
    }

    protected ResultMatcher getNoBuyerPhoneMatcher() {
        return jsonPath(getOrderRoot() + ".buyer.phone")
                .doesNotExist();
    }

    protected MockHttpServletRequestBuilder addClientRole(MockHttpServletRequestBuilder builder) {
        return builder.param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SHOP.name())
                .param(CheckouterClientParams.CLIENT_ID, order.getShopId().toString());
    }

    public static class ByOrderIdTest extends GetOrderWithBuyerPhonePartialTestBase {

        @Override
        protected MockHttpServletRequestBuilder requestBuilder() {
            return get("/orders/{orderId}", order.getId());
        }

        @Override
        protected String getOrderRoot() {
            return "$";
        }
    }

    public static class OrdersTest extends GetOrderWithBuyerPhonePartialTestBase {

        @Override
        protected MockHttpServletRequestBuilder requestBuilder() {
            Long userId = order.getBuyer().getUid();
            return get("/orders")
                    .param(CheckouterClientParams.UID, userId.toString())
                    .param(CheckouterClientParams.ID, order.getId().toString())
                    .param(CheckouterClientParams.RGB, order.getRgb().name())
                    .param(CheckouterClientParams.STATUS, order.getStatus().name());
        }

        @Override
        protected String getOrderRoot() {
            return "$.orders[0]";
        }

        @Override
        protected ResultActions commonExpects(ResultActions result) throws Exception {
            return super.commonExpects(result)
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)));
        }
    }

    public static class GetOrdersTest extends GetOrderWithBuyerPhonePartialTestBase {

        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        @Override
        @Test
        public void whenBuyerPhoneIsNotVisibleInMetaDataWithBuyerPhonePartialShouldReturnPhone() throws Exception {
            // Assign
            createOrder(false);
            var builder = addClientRole(requestBuilder(OptionalOrderPart.BUYER_PHONE));
            // Act
            var result = mockMvc.perform(builder);
            // Assert
            commonExpects(result)
                    .andExpect(getBuyerPhoneMatcher());
        }

        @Override
        @ParameterizedTest
        @MethodSource("nonBuyerPhonePartialsTestData")
        public void whenBuyerPhoneIsNotVisibleInMetaDataWithoutBuyerPhonePartialShouldNotReturnPhone(
                OptionalOrderPart partial) throws Exception {
            // Assign
            createOrder(false);
            var builder = addClientRole(requestBuilder(partial));
            // Act
            var result = mockMvc.perform(builder);
            // Assert
            commonExpects(result)
                    .andExpect(getNoBuyerPhoneMatcher());
        }

        @Override
        @ParameterizedTest
        @MethodSource("partialsTestData")
        public void whenBuyerPhoneIsVisibleInMetaDataShouldReturnPhoneWithAnyPartial(OptionalOrderPart partial)
                throws Exception {
            // Assign
            createOrder(true);
            var builder = addClientRole(requestBuilder(partial));
            // Act
            var result = mockMvc.perform(builder);
            // Assert
            commonExpects(result)
                    .andExpect(getBuyerPhoneMatcher());
        }

        @Override
        protected MockHttpServletRequestBuilder requestBuilder() {
            return requestBuilder(null);
        }

        private MockHttpServletRequestBuilder requestBuilder(OptionalOrderPart partial) {
            var ordersRequest = new OrderSearchRequest();
            ordersRequest.userId = order.getBuyer().getUid();
            ordersRequest.orderIds = List.of(order.getId());
            ordersRequest.rgbs = Set.of(order.getRgb());
            ordersRequest.partials = partial == null ? null : EnumSet.of(partial);
            var content = GSON.toJson(ordersRequest);
            return post("/get-orders")
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_UTF8);
        }

        @Override
        protected String getOrderRoot() {
            return "$.orders[0]";
        }

        @Override
        protected ResultActions commonExpects(ResultActions result) throws Exception {
            return super.commonExpects(result)
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)));
        }
    }
}
