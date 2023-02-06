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
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;

/**
 * @author mmetlov
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersByExcludeABOTestBase extends AbstractWebTestBase {

    protected Order defaultOrder;
    protected Order modifiedOrder;

    @BeforeAll
    public void init() {
        Parameters parameters = new Parameters();
        defaultOrder = orderCreateHelper.createOrder(parameters);

        parameters.setUserGroup(UserGroup.ABO);
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

    public static class OrdersTest extends GetOrdersByExcludeABOTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", parameterizedGetRequest(
                            "/orders?-ABO={excludeABO}&rgb=BLUE,WHITE")},
                    new Object[]{"GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID +
                                    "?-ABO={excludeABO}&rgb=BLUE,WHITE")},
                    new Object[]{"POST /get-orders", parameterizedPostRequest("/get-orders",
                            "{\"rgbs\":[\"BLUE\",\"WHITE\"],\"excludeABO\":\"%s\"}")}
            ).stream().map(Arguments::of);
        }

        @DisplayName("Получить заказы без excludeABO")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void notExcludeTest(String caseName,
                                   GetOrdersUtils.ParameterizedRequest<Boolean> parameterizedRequest)
                throws Exception {
            mockMvc.perform(
                    parameterizedRequest.build(false)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            containsInAnyOrder(
                                    hasEntry("id", defaultOrder.getId().intValue()),
                                    hasEntry("id", modifiedOrder.getId().intValue()))));
        }

        @DisplayName("Получить заказы c excludeABO")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void excludeTest(String caseName, GetOrdersUtils.ParameterizedRequest<Boolean> parameterizedRequest)
                throws Exception {
            mockMvc.perform(
                    parameterizedRequest.build(true)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]", contains(hasEntry("id", defaultOrder.getId().intValue()))));
        }
    }

    public static class CountTest extends GetOrdersByExcludeABOTestBase {

        @DisplayName("Посчитать заказы без excludeABO")
        @Test
        public void notExcludeTest() throws Exception {
            mockMvc.perform(get("/orders/count/")
                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.EXCLUDE_ABO, Boolean.toString(false))
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(2));
        }

        @DisplayName("Посчитать заказы c excludeABO")
        @Test
        public void excludeTest() throws Exception {
            mockMvc.perform(get("/orders/count/")
                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.EXCLUDE_ABO, Boolean.toString(true))
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));
        }
    }
}
