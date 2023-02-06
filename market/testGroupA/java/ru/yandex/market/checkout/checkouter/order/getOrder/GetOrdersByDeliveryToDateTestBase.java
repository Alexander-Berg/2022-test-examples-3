package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.DELIVERY_TO_DATE_GREATER_THAN;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.DELIVERY_TO_DATE_LESS_THAN;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersByDeliveryToDateTestBase extends AbstractWebTestBase {

    private static final DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
    protected Order orderWithSmallerDeliveryToDate;
    protected Order orderWithLargerDeliveryToDate;

    @BeforeAll
    public void init() {
        DeliveryDates smallerDeliveryDates = DeliveryDates.deliveryDates(getClock(), 4, 7);
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.getPushApiDeliveryResponses().forEach(deliveryResponse ->
                deliveryResponse.setDeliveryDates(smallerDeliveryDates));
        parameters.getBuiltMultiCart().getCarts().forEach(order -> order.getDelivery()
                .setDeliveryDates(smallerDeliveryDates));
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        orderWithSmallerDeliveryToDate = orderCreateHelper.createOrder(parameters);

        DeliveryDates greaterDeliveryDates = DeliveryDates.deliveryDates(getClock(), 10, 15);
        parameters.getPushApiDeliveryResponses().forEach(deliveryResponse ->
                deliveryResponse.setDeliveryDates(greaterDeliveryDates));
        parameters.getBuiltMultiCart().getCarts().forEach(order -> order.getDelivery()
                .setDeliveryDates(greaterDeliveryDates));
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        orderWithLargerDeliveryToDate = orderCreateHelper.createOrder(parameters);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    public static class OrdersGreaterThanTest extends GetOrdersByDeliveryToDateTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", parameterizedGetRequest("/orders" +
                            "?" + DELIVERY_TO_DATE_GREATER_THAN + "={deliveryToDateGreaterThan}&rgb=BLUE,WHITE")},
                    new Object[]{"GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID +
                                    "?" + DELIVERY_TO_DATE_GREATER_THAN + "={deliveryToDateGreaterThan}&rgb=BLUE,WHITE"
                            )},
                    new Object[]{"POST /get-orders", parameterizedPostRequest("/get-orders",
                            "{\"rgbs\":[\"BLUE\",\"WHITE\"],\"deliveryToDateGreaterThan\": %s}")}
            ).stream().map(Arguments::of);
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по deliveryToDateGreaterThan")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void getByDeliveryToDateGreaterThanTest(
                String caseName,
                GetOrdersUtils.ParameterizedRequest<String> parameterizedRequest) throws Exception {
            mockMvc.perform(
                    parameterizedRequest.build(DF.format(DateUtil.addDay(DateUtil.now(), 8)))
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            contains(hasEntry("id", orderWithLargerDeliveryToDate.getId().intValue()))));

            mockMvc.perform(
                    parameterizedRequest.build(DF.format(DateUtil.now()))
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            containsInAnyOrder(
                                    hasEntry("id", orderWithSmallerDeliveryToDate.getId().intValue()),
                                    hasEntry("id", orderWithLargerDeliveryToDate.getId().intValue()))));
        }
    }


    public static class OrdersLessThanTest extends GetOrdersByDeliveryToDateTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", parameterizedGetRequest("/orders" +
                            "?" + DELIVERY_TO_DATE_LESS_THAN + "={deliveryToDateLessThan}&rgb=BLUE,WHITE")},
                    new Object[]{"GET /orders/by-uid/{uid}",
                            parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID +
                                    "?" + DELIVERY_TO_DATE_LESS_THAN + "={deliveryToDateLessThan}&rgb=BLUE,WHITE")},
                    new Object[]{"POST /get-orders", parameterizedPostRequest("/get-orders",
                            "{\"rgbs\":[\"BLUE\",\"WHITE\"],\"deliveryToDateLessThan\": %s}")}
            ).stream().map(Arguments::of);
        }


        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по deliveryToDateLessThan")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void getByDeliveryToDateLessThanTest(String caseName,
                                                    GetOrdersUtils.ParameterizedRequest<String> parameterizedRequest)
                throws Exception {
            mockMvc.perform(
                    parameterizedRequest.build(DF.format(DateUtil.addDay(DateUtil.now(), 8)))
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            contains(hasEntry("id", orderWithSmallerDeliveryToDate.getId().intValue()))));

            mockMvc.perform(
                    parameterizedRequest.build(DF.format(DateUtil.addDay(DateUtil.now(), 16)))
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]",
                            containsInAnyOrder(
                                    hasEntry("id", orderWithSmallerDeliveryToDate.getId().intValue()),
                                    hasEntry("id", orderWithLargerDeliveryToDate.getId().intValue()))));
        }
    }

    public static class CountsTest extends GetOrdersByDeliveryToDateTestBase {

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы по deliveryToDateGreaterThan")
        @Test
        public void getByDeliveryToDateGreaterThanTest() throws Exception {
            mockMvc.perform(
                    get("/orders/count")
                            .param(DELIVERY_TO_DATE_GREATER_THAN, DF.format(DateUtil.addDay(DateUtil.now(), 8)))
                            .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));

            mockMvc.perform(
                    get("/orders/count")
                            .param(DELIVERY_TO_DATE_GREATER_THAN, DF.format(DateUtil.now()))
                            .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(2));
        }


        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы по deliveryToDateLessThan")
        @Test
        public void getByDeliveryToDateLessThanTest() throws Exception {
            mockMvc.perform(
                    get("/orders/count")
                            .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                            .param(DELIVERY_TO_DATE_LESS_THAN, DF.format(DateUtil.addDay(DateUtil.now(), 8)))
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));

            mockMvc.perform(
                    get("/orders/count")
                            .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                            .param(DELIVERY_TO_DATE_LESS_THAN, DF.format(DateUtil.addDay(DateUtil.now(), 16)))
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(2));
        }
    }
}
