package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.ParameterizedRequest;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherOrdersNotFound;

/**
 * @author mmetlov
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersByShopIdTestBase extends AbstractWebTestBase {

    protected Order defaultOrder;
    protected Order modifiedOrder;

    @BeforeAll
    public void init() {
        Parameters parameters = new Parameters();
        defaultOrder = orderCreateHelper.createOrder(parameters);

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

    public static class OrdersTest extends GetOrdersByShopIdTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", parameterizedGetRequest("/orders?shopId={shop-id}")},
                    new Object[]{"GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID + "?shopId={shop-id}")},
                    new Object[]{"POST /get-orders", parameterizedPostRequest("/get-orders", "{\"rgbs\":[\"WHITE\"," +
                            "\"BLUE\"],\"shopId\":%s}")}
            ).stream().map(Arguments::of);
        }

        @DisplayName("Получить заказы по shopId")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void shopIdTest(String caseName, ParameterizedRequest<Long> parameterizedRequest) throws Exception {
            // First shop Id, expects one order in response with same shop id
            mockMvc.perform(
                    parameterizedRequest.build(defaultOrder.getShopId())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].shopId").value(defaultOrder.getShopId().intValue()))
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)));

            // Second shop Id, expects one order in response with same shop id
            mockMvc.perform(
                    parameterizedRequest.build(modifiedOrder.getShopId())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].shopId").value(modifiedOrder.getShopId().intValue()));

            //Not existing shop Id, expect error
            long wrongId = Long.MAX_VALUE;
            mockMvc.perform(
                    parameterizedRequest.build(wrongId)
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());

        }
    }

    public static class CountsTest extends GetOrdersByShopIdTestBase {

        @DisplayName("Посчитать заказы по shopId")
        @Test
        public void shopIdTest() throws Exception {
            // First shop Id, expects one order in response with same shop id
            mockMvc.perform(
                    get("/orders/count")
                            .param("shopId", defaultOrder.getShopId().toString())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));

            // Second shop Id, expects one order in response with same shop id
            mockMvc.perform(
                    get("/orders/count")
                            .param("shopId", modifiedOrder.getShopId().toString())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));

            //Not existing shop Id, expect error
            Long wrongId = Long.MAX_VALUE;
            mockMvc.perform(
                    get("/orders/count")
                            .param("shopId", wrongId.toString())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(0));
        }
    }
}
