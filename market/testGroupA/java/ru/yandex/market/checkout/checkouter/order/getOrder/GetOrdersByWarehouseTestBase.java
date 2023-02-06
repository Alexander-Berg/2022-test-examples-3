package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersByWarehouseTestBase extends AbstractWebTestBase {

    private static final int FIRST_WAREHOUSE_ID = 3;
    protected Order firstWarehouseOrder;


    @BeforeAll
    public void init() {
        OrderItem item1 = OrderItemProvider.buildOrderItem(FIRST_WAREHOUSE_ID);
        OrderItem item2 = OrderItemProvider.buildOrderItem(2);

        Parameters parameters1 =
                BlueParametersProvider.defaultBlueOrderParameters(OrderProvider.getBlueOrder(order ->
                        order.setItems(List.of(item1))));
        Parameters parameters2 =
                BlueParametersProvider.defaultBlueOrderParameters(OrderProvider.getBlueOrder(order ->
                        order.setItems(List.of(item2))));

        orderCreateHelper.createOrder(parameters2);
        firstWarehouseOrder = orderCreateHelper.createOrder(parameters1);
        MatcherAssert.assertThat(firstWarehouseOrder.getItems().iterator().next().getWarehouseId(),
                CoreMatchers.equalTo(FIRST_WAREHOUSE_ID));
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    public static class OrdersTest extends GetOrdersByWarehouseTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", parameterizedGetRequest("/orders?rgb=BLUE&warehouseId={warehouseId}")},
                    new Object[]{"GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID + "?" + "rgb=BLUE" +
                                    "&warehouseId" +
                                    "={warehouseId}")},
                    new Object[]{"POST /get-orders", parameterizedPostRequest("/get-orders", "{\"rgbs\":[\"BLUE\"]," +
                            "\"warehouseId\":\"%s" +
                            "\"}")}
            ).stream().map(Arguments::of);
        }

        @DisplayName("Получить все заказы со склада")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void filterWarehouseOrders(String caseName,
                                          GetOrdersUtils.ParameterizedRequest<Integer> parameterizedRequest)
                throws Exception {
            mockMvc.perform(
                    parameterizedRequest.build(FIRST_WAREHOUSE_ID)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)))
                    .andExpect(jsonPath("$.orders[*]",
                            hasItem(hasEntry("id", firstWarehouseOrder.getId().intValue()))));
        }
    }
}
