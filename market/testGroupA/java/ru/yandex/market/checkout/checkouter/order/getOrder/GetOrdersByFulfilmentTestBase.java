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
import ru.yandex.market.checkout.checkouter.json.Names;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;

/**
 * https://testpalm.yandex-team.ru/testcase/checkouter-173
 *
 * @author asafev
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersByFulfilmentTestBase extends AbstractWebTestBase {

    protected Order fulfilmentOrder;
    protected Order simpleOrder1;
    protected Order simpleOrder2;

    @BeforeAll
    public void init() {
        fulfilmentOrder = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        simpleOrder1 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        simpleOrder2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    public static class OrdersTest extends GetOrdersByFulfilmentTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", parameterizedGetRequest("/orders?fulfilment={fulfilment}")},
                    new Object[]{"GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID + "?fulfilment={fulfilment" +
                                    "}")},
                    new Object[]{"POST /get-orders", parameterizedPostRequest("/get-orders", "{\"rgbs\": [\"BLUE\"], " +
                            "\"fulfillment\":\"%s\"}")}
            ).stream().map(Arguments::of);
        }

        @DisplayName("Получить все не fulfilment-заказы (fulfilment = false)")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void filterNonFulfilmentOrdersTest(String caseName,
                                                  GetOrdersUtils.ParameterizedRequest<Boolean> parameterizedRequest)
                throws Exception {
            mockMvc.perform(
                    parameterizedRequest.build(false)
                            .param(Names.Order.RGB, Color.BLUE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]", hasSize(2)))
                    .andExpect(jsonPath("$.orders[*]",
                            containsInAnyOrder(
                                    hasEntry("id", simpleOrder1.getId().intValue()),
                                    hasEntry("id", simpleOrder2.getId().intValue())
                            )
                    ));
        }

        @DisplayName("Получить только fulfilment-заказы (fulfilment = true)")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void filterFulfilmentOrderTest(String caseName,
                                              GetOrdersUtils.ParameterizedRequest<Boolean> parameterizedRequest)
                throws Exception {
            mockMvc.perform(
                    parameterizedRequest.build(true)
                            .param(Names.Order.RGB, Color.BLUE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)))
                    .andExpect(jsonPath("$.orders[*]", contains(hasEntry("id", fulfilmentOrder.getId().intValue()))));
        }
    }

    public static class CountTest extends GetOrdersByFulfilmentTestBase {

        @DisplayName("Посчитать не-fulfilment-заказы")
        @Test
        public void nonFulfilmentCountTest() throws Exception {
            mockMvc.perform(get("/orders/count/")
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(Names.Order.IS_FULFILMENT, Boolean.toString(false))
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(2));
        }

        @DisplayName("Посчитать fulfilment-заказы")
        @Test
        public void fulfilmentCountTest() throws Exception {
            mockMvc.perform(get("/orders/count/")
                    .param(Names.Order.IS_FULFILMENT, Boolean.toString(true))
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));
        }
    }
}
