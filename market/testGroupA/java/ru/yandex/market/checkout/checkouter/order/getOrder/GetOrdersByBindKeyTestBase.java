package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Arrays;
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

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.json.Names.Buyer.BIND_KEY;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersByBindKeyTestBase extends AbstractWebTestBase {

    private static final String FIRST_BIND_KEY = "abcdefg";

    protected Order orderWithFirstBindKey1;
    protected Order orderWithFirstBindKey2;

    @BeforeAll
    public void init() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuyer().setBindKey(FIRST_BIND_KEY);
        orderWithFirstBindKey1 = orderCreateHelper.createOrder(parameters);
        orderWithFirstBindKey2 = orderCreateHelper.createOrder(parameters);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    public static class OrdersTest extends GetOrdersByBindKeyTestBase {

        public static Stream<Arguments> parameterizedTestData() {
            return Arrays.asList(
                    new Object[]{"GET /orders", parameterizedGetRequest("/orders?" + BIND_KEY + "={bindKey}" +
                            "&rgb=BLUE,WHITE")},
                    new Object[]{"GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID + "?" + BIND_KEY +
                                    "={bindKey}&rgb=BLUE,WHITE")},
                    new Object[]{"POST /get-orders", parameterizedPostRequest("/get-orders", "{\"rgbs\":[\"BLUE\"," +
                            "\"WHITE\"],\"bindKey\": %s}")}
            ).stream().map(Arguments::of);
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по bindKey")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void getByBindKeyTest(String caseName,
                                     GetOrdersUtils.ParameterizedRequest<String> parameterizedRequest)
                throws Exception {
            mockMvc.perform(
                    parameterizedRequest.build(FIRST_BIND_KEY)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            containsInAnyOrder(
                                    hasEntry("id", orderWithFirstBindKey1.getId().intValue()),
                                    hasEntry("id", orderWithFirstBindKey2.getId().intValue()))));
        }
    }

    public static class CountsTest extends GetOrdersByBindKeyTestBase {

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы по bindKey")
        @Test
        public void getByBindKeyTest() throws Exception {
            mockMvc.perform(
                    get("/orders/count")
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(BIND_KEY, FIRST_BIND_KEY)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(2));
        }
    }
}
