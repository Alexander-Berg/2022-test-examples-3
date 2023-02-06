package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
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
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.UNKNOWN;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByDigitalEnabledTest extends AbstractWebTestBase {

    private static final String RGB = "WHITE,BLUE";

    private Order digitalOrder;
    private Order usualOrder;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                new Object[]{"GET /orders",
                        parameterizedGetRequest("/orders?rgb=" + RGB + "&digitalEnabled={digitalEnabled}")},
                new Object[]{"GET /orders/by-uid/{uid}",
                        parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID + "?" +
                                "rgb=" + RGB + "&digitalEnabled={digitalEnabled}")},
                new Object[]{"POST /get-orders", parameterizedPostRequest("/get-orders",
                        "{\"rgbs\": [\"BLUE\",\"WHITE\"], \"digitalEnabled\": %s}")}
        ).map(Arguments::of);
    }

    @BeforeAll
    public void init() {
        OrderItem digitalItem = OrderItemProvider.buildOrderItemDigital("1");
        Parameters digitalParameters = WhiteParametersProvider.digitalOrderPrameters();
        digitalParameters.getOrder().setItems(Collections.singleton(digitalItem));
        digitalOrder = orderCreateHelper.createOrder(digitalParameters);

        Parameters usualParameters = BlueParametersProvider.defaultBlueOrderParameters();
        usualOrder = orderCreateHelper.createOrder(usualParameters);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    @DisplayName("Получить все заказы, включая цифровые")
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void shouldReturnAllOrdersWhenDigitalEnabledIsTrue(
            String caseName,
            GetOrdersUtils.ParameterizedRequest<Boolean> parameterizedRequest) throws Exception {
        mockMvc.perform(
                parameterizedRequest.build(Boolean.TRUE)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(2)))
                .andExpect(jsonPath("$.orders[*]", containsInAnyOrder(
                        hasEntry("id", usualOrder.getId().intValue()),
                        hasEntry("id", digitalOrder.getId().intValue()))));
    }

    @DisplayName("Получить последние заказы, включая цифровые")
    @Test
    public void shouldReturnNoDigitalOrderWhenGetRecentAndDigitalEnabledIsTrue() throws Exception {
        mockMvc.perform(
                get("/orders/by-uid/" + BuyerProvider.UID + "/recent")
                        .param(CheckouterClientParams.RGB, RGB)
                        .param(CheckouterClientParams.STATUS, Arrays.stream(OrderStatus.values())
                                .filter(status -> !UNKNOWN.equals(status))
                                .map(OrderStatus::name)
                                .collect(Collectors.joining(",")))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$", containsInAnyOrder(
                        hasEntry("id", usualOrder.getId().intValue()),
                        hasEntry("id", digitalOrder.getId().intValue()))));
    }

    @DisplayName("Посчитать заказы, включая цифровые")
    @Test
    public void shouldReturnAllOrdersCountWhenDigitalEnabledIsTrue() throws Exception {
        mockMvc.perform(
                get("/orders/count")
                        .param(CheckouterClientParams.RGB, RGB)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(2));
    }
}
