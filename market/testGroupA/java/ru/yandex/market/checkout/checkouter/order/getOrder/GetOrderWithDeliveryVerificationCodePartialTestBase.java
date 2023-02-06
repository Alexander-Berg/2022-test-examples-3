package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_POST_TERM_DELIVERY_SERVICE_ID;

public abstract class GetOrderWithDeliveryVerificationCodePartialTestBase extends AbstractWebTestBase {

    protected final String verificationCode = "12345";
    protected Order pickupOrder;
    protected String barcodeData;

    @Autowired
    protected YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    protected OrderUpdateService orderUpdateService;

    protected static Stream<Arguments> nonDeliveryCodePartialsTestData() {
        return Stream.of(
                new Object[]{null},
                new Object[]{OptionalOrderPart.DELIVERY}
        ).map(Arguments::of);
    }

    @BeforeEach
    public void init() {

        pickupOrder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withDeliveryServiceId(isPostTermOrder()
                        ? MOCK_POST_TERM_DELIVERY_SERVICE_ID
                        : MOCK_DELIVERY_SERVICE_ID)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(MOCK_DELIVERY_SERVICE_ID)
                        .addPostTerm(MOCK_POST_TERM_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        barcodeData = pickupOrder.getId() + "-" + verificationCode;

        orderUpdateService.updateDeliveryVerificationCode(pickupOrder.getId(), ClientInfo.SYSTEM, verificationCode);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldReturnVerificationPartWhenPartialPassed(boolean bySystem) throws Exception {
        var builder = addClientRole(requestBuilder(), bySystem);
        var result = mockMvc.perform(builder
                .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY_VERIFICATION_CODE.name()));

        final String orderRoot = getOrderRoot();
        commonExpects(result)
                .andExpect(jsonPath(orderRoot + ".delivery").exists())
                .andExpect(jsonPath(orderRoot + ".delivery.verificationPart").exists())
                .andExpect(jsonPath(orderRoot + ".delivery.verificationPart.verificationCode")
                        .value(verificationCode))
                .andExpect(getBarcodeDataMatcher());
    }

    @ParameterizedTest
    @MethodSource("nonDeliveryCodePartialsTestData")
    public void shouldNotReturnVerificationPartWhenPartialNotPassed(OptionalOrderPart partial) throws Exception {
        var builder = addClientRole(requestBuilder(), true);
        if (partial != null) {
            builder = builder.param(CheckouterClientParams.OPTIONAL_PARTS, partial.name());
        }
        var result = mockMvc.perform(builder);

        final String orderRoot = getOrderRoot();
        commonExpects(result)
                .andExpect(jsonPath(orderRoot + ".delivery.verificationPart").doesNotExist());
    }

    protected abstract MockHttpServletRequestBuilder requestBuilder();

    protected abstract String getOrderRoot();

    protected boolean isPostTermOrder() {
        return false;
    }

    protected ResultActions commonExpects(ResultActions result) throws Exception {
        return result.andExpect(status().isOk());
    }

    protected ResultMatcher getBarcodeDataMatcher() {
        if (isPostTermOrder()) {
            return jsonPath(getOrderRoot() + ".delivery.verificationPart.barcodeData")
                    .doesNotExist();
        }
        return jsonPath(getOrderRoot() + ".delivery.verificationPart.barcodeData")
                .value(barcodeData);
    }

    protected MockHttpServletRequestBuilder addClientRole(MockHttpServletRequestBuilder builder, boolean bySystem) {
        if (bySystem) {
            builder = builder.param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name());
        } else {
            Long userId = pickupOrder.getBuyer().getUid();
            builder = builder.param(CheckouterClientParams.CLIENT_ROLE, ClientRole.USER.name())
                    .param(CheckouterClientParams.CLIENT_ID, userId.toString());
        }
        return builder;
    }

    public static class OrdersByUidRecentTest extends GetOrderWithDeliveryVerificationCodePartialTestBase {

        @Override
        protected MockHttpServletRequestBuilder requestBuilder() {
            Long userId = pickupOrder.getBuyer().getUid();
            return get("/orders/by-uid/{uid}/recent", userId)
                    .param(CheckouterClientParams.RGB, pickupOrder.getRgb().name())
                    .param(CheckouterClientParams.STATUS, pickupOrder.getStatus().name());
        }

        @Override
        protected String getOrderRoot() {
            return "$.[0]";
        }

        @Override
        protected ResultActions commonExpects(ResultActions result) throws Exception {
            return super.commonExpects(result)
                    .andExpect(jsonPath("$.[*]", hasSize(1)));
        }
    }

    public static class PostTermOrdersByUidRecentTest extends OrdersByUidRecentTest {

        @Override
        protected boolean isPostTermOrder() {
            return true;
        }
    }

    public static class ByOrderIdTest extends GetOrderWithDeliveryVerificationCodePartialTestBase {

        @Override
        protected MockHttpServletRequestBuilder requestBuilder() {
            return get("/orders/{orderId}", pickupOrder.getId());
        }

        @Override
        protected String getOrderRoot() {
            return "$";
        }
    }

    public static class PostTermByOrderIdTest extends ByOrderIdTest {

        @Override
        protected boolean isPostTermOrder() {
            return true;
        }
    }

    public static class OrderByUidTest extends GetOrderWithDeliveryVerificationCodePartialTestBase {

        @Override
        protected MockHttpServletRequestBuilder requestBuilder() {
            Long userId = pickupOrder.getBuyer().getUid();
            return get("/orders/by-uid/{uid}", userId)
                    .param(CheckouterClientParams.RGB, pickupOrder.getRgb().name())
                    .param(CheckouterClientParams.STATUS, pickupOrder.getStatus().name());
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

    public static class PostTermOrderByUidTest extends OrderByUidTest {

        @Override
        protected boolean isPostTermOrder() {
            return true;
        }
    }

    public static class OrdersTest extends GetOrderWithDeliveryVerificationCodePartialTestBase {

        @Override
        protected MockHttpServletRequestBuilder requestBuilder() {
            Long userId = pickupOrder.getBuyer().getUid();
            return get("/orders")
                    .param(CheckouterClientParams.UID, userId.toString())
                    .param(CheckouterClientParams.ID, pickupOrder.getId().toString())
                    .param(CheckouterClientParams.RGB, pickupOrder.getRgb().name())
                    .param(CheckouterClientParams.STATUS, pickupOrder.getStatus().name());
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

    public static class PostTermOrdersTest extends OrdersTest {

        @Override
        protected boolean isPostTermOrder() {
            return true;
        }
    }

    public static class GetOrdersTest extends GetOrderWithDeliveryVerificationCodePartialTestBase {

        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        @Override
        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        public void shouldReturnVerificationPartWhenPartialPassed(boolean bySystem) throws Exception {
            var builder = addClientRole(
                    requestBuilder(OptionalOrderPart.DELIVERY_VERIFICATION_CODE),
                    bySystem);

            final String orderRoot = getOrderRoot();
            commonExpects(mockMvc.perform(builder))
                    .andExpect(jsonPath(orderRoot + ".delivery").exists())
                    .andExpect(jsonPath(orderRoot + ".delivery.verificationPart").exists())
                    .andExpect(jsonPath(orderRoot + ".delivery.verificationPart.verificationCode")
                            .value(verificationCode))
                    .andExpect(getBarcodeDataMatcher());
        }

        @Override
        @ParameterizedTest
        @MethodSource("nonDeliveryCodePartialsTestData")
        public void shouldNotReturnVerificationPartWhenPartialNotPassed(OptionalOrderPart partial) throws Exception {
            var builder = addClientRole(requestBuilder(partial), true);
            var result = mockMvc.perform(builder);

            final String orderRoot = getOrderRoot();
            commonExpects(result)
                    .andExpect(jsonPath(orderRoot + ".delivery.verificationPart").doesNotExist());
        }

        @Override
        protected MockHttpServletRequestBuilder requestBuilder() {
            return requestBuilder(null);
        }

        private MockHttpServletRequestBuilder requestBuilder(OptionalOrderPart partial) {
            var ordersRequest = new OrderSearchRequest();
            ordersRequest.userId = pickupOrder.getBuyer().getUid();
            ordersRequest.orderIds = List.of(pickupOrder.getId());
            ordersRequest.rgbs = Set.of(pickupOrder.getRgb());
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

    public static class PostTermGetOrdersTest extends GetOrdersTest {

        @Override
        protected boolean isPostTermOrder() {
            return true;
        }
    }
}
