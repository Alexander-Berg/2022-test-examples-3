package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.ControllerUtils;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherOrdersNotFound;

/**
 * @author mmetlov
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersClientRoleTestBase extends AbstractWebTestBase {

    private static final long SHOP_CLIENT_ID = 1234L;
    private static final long REFEREE_CLIENT_ID = 5678L;

    protected Order defaultOrder;
    protected Order modifiedOrder;

    private static MockHttpServletRequestBuilder withClientParams(MockHttpServletRequestBuilder builder,
                                                                  ClientRole role, Long clientId, Long shopId) {
        if (role != null) {
            builder.param(CheckouterClientParams.CLIENT_ROLE, role.name());
        }

        if (clientId != null) {
            builder.param(CheckouterClientParams.CLIENT_ID, clientId.toString());
        }

        if (shopId != null) {
            builder.param(CheckouterClientParams.SHOP_ID, shopId.toString());
        }

        builder.param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name());

        return builder;
    }

    @BeforeAll
    public void init() throws Exception {
        Parameters parameters = new Parameters();

        defaultOrder = orderCreateHelper.createOrder(parameters);

        parameters.getBuyer().setUid(555L);
        parameters.setShopId(775L);
        modifiedOrder = orderCreateHelper.createOrder(parameters);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    public static class OrdersTest extends GetOrdersClientRoleTestBase {

        private Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier;

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", (Supplier<MockHttpServletRequestBuilder>) () ->
                            get("/orders")
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                    },
                    new Object[]{"POST /get-orders", (Supplier<MockHttpServletRequestBuilder>) () ->
                            MockMvcRequestBuilders.post("/get-orders")
                                    .content("{\"rgbs\":[\"BLUE\",\"WHITE\"]}")
                                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    }
            ).stream().map(Arguments::of);
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы без роли")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void testNoRole(String caseName, Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier)
                throws Exception {
            this.requestBuilderSupplier = requestBuilderSupplier;
            mockMvc.perform(builderWithClientRoleParams(null, null, null))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы с ролью UNKNOWN")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void testClientRoleUnknown(String caseName,
                                          Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier)
                throws Exception {
            this.requestBuilderSupplier = requestBuilderSupplier;
            mockMvc.perform(builderWithClientRoleParams(ClientRole.UNKNOWN, null, null))
                    .andExpect(status().is4xxClientError());
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы с ролью SYSTEM")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void testClientRoleSystem(String caseName,
                                         Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier)
                throws Exception {
            this.requestBuilderSupplier = requestBuilderSupplier;
            mockMvc.perform(builderWithClientRoleParams(ClientRole.SYSTEM, null, null))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            containsInAnyOrder(
                                    hasEntry("id", defaultOrder.getId().intValue()),
                                    hasEntry("id", modifiedOrder.getId().intValue())
                            ))
                    );
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы с ролью SHOP")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void testClientRoleShop(String caseName,
                                       Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier)
                throws Exception {
            this.requestBuilderSupplier = requestBuilderSupplier;
            // для каждого магазина свои заказы
            mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP, OrderProvider.SHOP_ID, null))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            contains(hasEntry("id", defaultOrder.getId().intValue()))));

            mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP, modifiedOrder.getShopId(), null))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            contains(hasEntry("id", modifiedOrder.getId().intValue()))));

            mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP, Long.MAX_VALUE, null))
                    .andExpect(resultMatcherOrdersNotFound());

            // без shopId 400
            mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP, null, null))
                    .andExpect(status().is4xxClientError());
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы с ролью SHOP_USER")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void testClientRoleShopUser(String caseName,
                                           Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier)
                throws Exception {
            this.requestBuilderSupplier = requestBuilderSupplier;
            // для каждого магазина свои заказы
            mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP_USER, SHOP_CLIENT_ID, OrderProvider.SHOP_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            contains(hasEntry("id", defaultOrder.getId().intValue()))));

            mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP_USER, SHOP_CLIENT_ID,
                    modifiedOrder.getShopId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            contains(hasEntry("id", modifiedOrder.getId().intValue()))));

            mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP_USER, SHOP_CLIENT_ID, Long.MAX_VALUE))
                    .andExpect(resultMatcherOrdersNotFound());

            // без clientId 400
            mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP_USER, null, OrderProvider.SHOP_ID))
                    .andExpect(status().is4xxClientError());

            // без shopId 400
            mockMvc.perform(builderWithClientRoleParams(ClientRole.SHOP_USER, SHOP_CLIENT_ID, null))
                    .andExpect(status().is4xxClientError());
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы с ролью USER")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void testClientRoleUser(String caseName,
                                       Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier)
                throws Exception {
            this.requestBuilderSupplier = requestBuilderSupplier;
            // для каждого пользователя свои заказы
            mockMvc.perform(builderWithClientRoleParams(ClientRole.USER, BuyerProvider.UID, null))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            contains(hasEntry("id", defaultOrder.getId().intValue()))));

            mockMvc.perform(builderWithClientRoleParams(ClientRole.USER, modifiedOrder.getBuyer().getUid(), null))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            contains(hasEntry("id", modifiedOrder.getId().intValue()))));

            mockMvc.perform(builderWithClientRoleParams(ClientRole.USER, Long.MAX_VALUE, null))
                    .andExpect(resultMatcherOrdersNotFound());

            // без clientId 400
            mockMvc.perform(builderWithClientRoleParams(ClientRole.USER, null, null))
                    .andExpect(status().is4xxClientError());
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы с ролью REFEREE")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void testClientRoleReferee(String caseName,
                                          Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier)
                throws Exception {
            this.requestBuilderSupplier = requestBuilderSupplier;
            mockMvc.perform(builderWithClientRoleParams(ClientRole.REFEREE, REFEREE_CLIENT_ID, null))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            containsInAnyOrder(
                                    hasEntry("id", defaultOrder.getId().intValue()),
                                    hasEntry("id", modifiedOrder.getId().intValue()))));

            // без clientId 400
            mockMvc.perform(builderWithClientRoleParams(ClientRole.REFEREE, null, null))
                    .andExpect(status().is4xxClientError());
        }

        private MockHttpServletRequestBuilder builderWithClientRoleParams(ClientRole role, Long clientId, Long shopId) {
            return withClientParams(requestBuilderSupplier.get(), role, clientId, shopId);
        }
    }

    public static class ServiceGetOrdersTest extends GetOrdersClientRoleTestBase {

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы с ролью REFEREE")
        @Test
        public void testClientRoleReferee() {
            PagedOrders orders = orderService.getOrders(
                    new OrderSearchRequest(),
                    ControllerUtils.buildClientInfo(ClientRole.REFEREE, REFEREE_CLIENT_ID)
            );
            assertThat(orders.getItems(), hasSize(2));
            assertThat(
                    orders.getItems().stream().map(Order::getId).collect(Collectors.toList()),
                    containsInAnyOrder(defaultOrder.getId(), modifiedOrder.getId())
            );
        }
    }

    public static class CountsTest extends GetOrdersClientRoleTestBase {

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы без роли")
        @Test
        public void testNoRole() throws Exception {
            mockMvc.perform(withClientParams(get("/orders/count"), null, null, null))
                    .andExpect(resultMatcherCount(0));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы с ролью UNKNOWN")
        @Test
        public void testClientRoleUnknown() throws Exception {
            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.UNKNOWN, null, null))
                    .andExpect(status().is4xxClientError());
        }

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы с ролью SYSTEM")
        @Test
        public void testClientRoleSystem() throws Exception {
            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.SYSTEM, null, null))
                    .andExpect(resultMatcherCount(2));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы с ролью SHOP")
        @Test
        public void testClientRoleShop() throws Exception {
            // для каждого магазина свои заказы
            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.SHOP, OrderProvider.SHOP_ID, null))
                    .andExpect(resultMatcherCount(1));

            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.SHOP, modifiedOrder.getShopId(), null))
                    .andExpect(resultMatcherCount(1));

            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.SHOP, Long.MAX_VALUE, null))
                    .andExpect(resultMatcherCount(0));

            // без shopId 400
            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.SHOP, null, null))
                    .andExpect(status().is4xxClientError());
        }

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы с ролью SHOP_USER")
        @Test
        public void testClientRoleShopUser() throws Exception {
            // для каждого магазина свои заказы
            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.SHOP_USER, 12345L, OrderProvider.SHOP_ID))
                    .andExpect(resultMatcherCount(1));

            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.SHOP_USER, 12345L,
                    modifiedOrder.getShopId()))
                    .andExpect(resultMatcherCount(1));

            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.SHOP_USER, 12345L, Long.MAX_VALUE))
                    .andExpect(resultMatcherCount(0));

            // без clientId 400
            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.SHOP_USER, null, OrderProvider.SHOP_ID))
                    .andExpect(status().is4xxClientError());

            // без shopId 400
            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.SHOP_USER, 12345L, null))
                    .andExpect(status().is4xxClientError());
        }

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы с ролью USER")
        @Test
        public void testClientRoleUser() throws Exception {
            // для каждого пользователя свои заказы
            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.USER, BuyerProvider.UID, null))
                    .andExpect(resultMatcherCount(1));

            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.USER,
                    modifiedOrder.getBuyer().getUid(), null))
                    .andExpect(resultMatcherCount(1));

            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.USER, Long.MAX_VALUE, null))
                    .andExpect(resultMatcherCount(0));

            // без clientId 400
            mockMvc.perform(withClientParams(get("/orders/count"), ClientRole.USER, null, null))
                    .andExpect(status().is4xxClientError());
        }

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы с ролью REFEREE")
        @Test
        public void testClientRoleReferee() throws Exception {
            mockMvc.perform(withClientParams(get("/orders/count"),
                    ClientRole.REFEREE, 12345L, null))
                    .andExpect(resultMatcherCount(2));

            // без clientId 400
            mockMvc.perform(withClientParams(get("/orders/count"),
                    ClientRole.REFEREE, null, null))
                    .andExpect(status().is4xxClientError());
        }

    }
}
