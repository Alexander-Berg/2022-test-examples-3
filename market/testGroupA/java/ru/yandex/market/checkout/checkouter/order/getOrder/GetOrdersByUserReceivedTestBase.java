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
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.json.Names.Delivery.USER_RECEIVED;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.UID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersByUserReceivedTestBase extends AbstractWebTestBase {

    protected Order receivedOrder;
    protected Order notReceivedOrder;

    @BeforeAll
    public void init() {
        Parameters parameters = new Parameters();
        receivedOrder = orderCreateHelper.createOrder(parameters);
        receivedOrder = orderStatusHelper.proceedOrderToStatus(receivedOrder, OrderStatus.DELIVERED);
        orderUpdateService.updateDeliveryReceived(receivedOrder.getId(), new ClientInfo(ClientRole.USER, UID), true);

        notReceivedOrder = orderCreateHelper.createOrder(parameters);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }


    public static class OrdersTest extends GetOrdersByUserReceivedTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", parameterizedGetRequest("/orders" +
                            "?" + USER_RECEIVED + "={userReceived}&rgb=BLUE,WHITE")},
                    new Object[]{"GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID +
                                    "?" + USER_RECEIVED + "={userReceived}&rgb=BLUE,WHITE")},
                    new Object[]{"POST /get-orders", parameterizedPostRequest("/get-orders",
                            "{\"rgbs\":[\"BLUE\",\"WHITE\"],\"userReceived\": %s}")}
            ).stream().map(Arguments::of);
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по userReceived")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void getByUserReceivedTest(String caseName,
                                          GetOrdersUtils.ParameterizedRequest<Boolean> parameterizedRequest)
                throws Exception {
            mockMvc.perform(
                    parameterizedRequest.build(Boolean.TRUE)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            contains(hasEntry("id", receivedOrder.getId().intValue()))));

            mockMvc.perform(
                    parameterizedRequest.build(Boolean.FALSE)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            contains(hasEntry("id", notReceivedOrder.getId().intValue()))));
        }
    }

    public static class CountsTest extends GetOrdersByUserReceivedTestBase {

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы по userReceived")
        @Test
        public void getByUserReceivedTest() throws Exception {
            mockMvc.perform(
                    get("/orders/count")
                            .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                            .param(USER_RECEIVED, Boolean.TRUE.toString())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));

            mockMvc.perform(
                    get("/orders/count")
                            .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                            .param(USER_RECEIVED, Boolean.FALSE.toString())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));
        }
    }
}
