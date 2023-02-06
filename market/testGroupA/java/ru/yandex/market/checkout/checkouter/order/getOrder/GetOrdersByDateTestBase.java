package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.ParameterizedRequest;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.FROM_DATE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.TO_DATE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.UID;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherBoolean;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherOrders;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherOrdersNotFound;

/**
 * @author mmetlov
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersByDateTestBase extends AbstractWebTestBase {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    protected Order order;
    protected Order earlierOrder;

    @BeforeAll
    public void init() {
        super.setUpBase();

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        order = orderCreateHelper.createOrder(parameters);

        jumpToPast(1, ChronoUnit.DAYS);
        try {
            parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder()
                    .addDelivery(DeliveryProvider.yandexDelivery()
                            .courier(false)
                            .buildActualDeliveryOption(getClock()))
                    .build());
            earlierOrder = orderCreateHelper.createOrder(parameters);
        } finally {
            clearFixed();
        }
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    public static class OrdersFromTest extends GetOrdersByDateTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", (ParameterizedRequest<Date>) param ->
                            get("/orders")
                                    .param(FROM_DATE, DATE_FORMAT.format(param))
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                    },
                    new Object[]{"GET /orders/by-uid/{uid}", (ParameterizedRequest<Date>) param ->
                            get("/orders/by-uid/" + BuyerProvider.UID)
                                    .param(FROM_DATE, DATE_FORMAT.format(param))
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                    },
                    new Object[]{"POST /get-orders", (ParameterizedRequest<Date>) param ->
                            MockMvcRequestBuilders.post("/get-orders")
                                    .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"fromDate\":\"%s\"}",
                                            param.getTime() / 1000))
                                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    }).stream().map(Arguments::of);
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по fromDate первого заказа")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void fromEarliest(String caseName, ParameterizedRequest<Date> parameterizedRequest) throws Exception {
            mockMvc.perform(
                            parameterizedRequest.build(earlierOrder.getCreationDate())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrders(order, earlierOrder));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по fromDate второго заказа")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void fromLater(String caseName, ParameterizedRequest<Date> parameterizedRequest) throws Exception {
            mockMvc.perform(
                            parameterizedRequest.build(order.getCreationDate())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrders(order));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по слишком поздней fromDate")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void fromTooLate(String caseName, ParameterizedRequest<Date> parameterizedRequest) throws Exception {
            mockMvc.perform(
                            parameterizedRequest.build(DateUtil.addDay(order.getCreationDate(), 1))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }
    }


    /////////////////////////////

    public static class CountsFromTest extends GetOrdersByDateTestBase {

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы по fromDate первого заказа")
        @Test
        public void fromEarliest() throws Exception {
            mockMvc.perform(
                            get("/orders/count")
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                                    .param(FROM_DATE, DATE_FORMAT.format(earlierOrder.getCreationDate()))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(2));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы по fromDate второго заказа")
        @Test
        public void fromLater() throws Exception {
            mockMvc.perform(
                            get("/orders/count")
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                                    .param(FROM_DATE, DATE_FORMAT.format(order.getCreationDate()))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы по слишком поздней fromDate")
        @Test
        public void fromTooLate() throws Exception {
            mockMvc.perform(
                            get("/orders/count")
                                    .param(FROM_DATE, DATE_FORMAT.format(DateUtil.addDay(order.getCreationDate(), 1)))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(0));
        }

    }

    public static class ExistFromTest extends GetOrdersByDateTestBase {

        @Tag(Tags.AUTO)
        @DisplayName("Проверить наличие заказов по fromDate первого заказа")
        @Test
        public void fromEarliest() throws Exception {
            mockMvc.perform(
                            get("/orders/exist")
                                    .param(UID, order.getUserClientInfo().getUid().toString())
                                    .param(FROM_DATE, DATE_FORMAT.format(earlierOrder.getCreationDate()))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherBoolean(true));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Проверить наличие заказов по fromDate второго заказа")
        @Test
        public void fromLater() throws Exception {
            mockMvc.perform(
                            get("/orders/exist")
                                    .param(UID, order.getUserClientInfo().getUid().toString())
                                    .param(FROM_DATE, DATE_FORMAT.format(order.getCreationDate()))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherBoolean(true));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Проверить наличие заказов по слишком поздней fromDate")
        @Test
        public void fromTooLate() throws Exception {
            mockMvc.perform(
                            get("/orders/exist")
                                    .param(UID, order.getUserClientInfo().getUid().toString())
                                    .param(FROM_DATE, DATE_FORMAT.format(DateUtil.addDay(order.getCreationDate(), 1)))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherBoolean(false));
        }
    }


    public static class OrdersToTest extends GetOrdersByDateTestBase {


        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", (ParameterizedRequest<Date>) param ->
                            get("/orders")
                                    .param(TO_DATE, DATE_FORMAT.format(param))
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                    },
                    new Object[]{"GET /orders/by-uid/{uid}", (ParameterizedRequest<Date>) param ->
                            get("/orders/by-uid/" + BuyerProvider.UID)
                                    .param(TO_DATE, DATE_FORMAT.format(param))
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                    },
                    new Object[]{"POST /get-orders", (ParameterizedRequest<Date>) param ->
                            MockMvcRequestBuilders.post("/get-orders")
                                    .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"toDate\":\"%s\"}",
                                            (param.getTime() / 1000 + 1)))
                                    .contentType(MediaType.APPLICATION_JSON_UTF8)}).stream().map(Arguments::of);
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по слишком ранней toDate")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void toTooEarly(String caseName, ParameterizedRequest<Date> parameterizedRequest) throws Exception {
            mockMvc.perform(
                            parameterizedRequest.build(DateUtil.addDay(earlierOrder.getCreationDate(), -1))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по toDate первого заказа")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void toEarly(String caseName, ParameterizedRequest<Date> parameterizedRequest) throws Exception {
            mockMvc.perform(
                            parameterizedRequest.build(earlierOrder.getCreationDate())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrders(earlierOrder));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Получить заказы по toDate второго заказа")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void toLater(String caseName, ParameterizedRequest<Date> parameterizedRequest) throws Exception {
            mockMvc.perform(
                            parameterizedRequest.build(order.getCreationDate())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrders(order, earlierOrder));
        }
    }

    public static class CountsToTest extends GetOrdersByDateTestBase {

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы по слишком ранней toDate")
        @Test
        public void toTooEarly() throws Exception {
            mockMvc.perform(
                            get("/orders/count")
                                    .param(TO_DATE, DATE_FORMAT.format(
                                            DateUtil.addDay(earlierOrder.getCreationDate(), -1)))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(0));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы по toDate первого заказа")
        @Test
        public void toLater() throws Exception {
            mockMvc.perform(
                            get("/orders/count")
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                                    .param(TO_DATE, DATE_FORMAT.format(earlierOrder.getCreationDate()))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Посчитать заказы по toDate второго заказа")
        @Test
        public void toTooLate() throws Exception {
            mockMvc.perform(
                            get("/orders/count")
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                                    .param(TO_DATE, DATE_FORMAT.format(order.getCreationDate()))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(2));
        }

    }


    public static class ExistToTest extends GetOrdersByDateTestBase {

        @Tag(Tags.AUTO)
        @DisplayName("Проверить наличие заказов по слишком ранней toDate")
        @Test
        public void toTooEarly() throws Exception {
            mockMvc.perform(
                            get("/orders/exist")
                                    .param(UID, order.getUserClientInfo().getUid().toString())
                                    .param(TO_DATE, DATE_FORMAT.format(
                                            DateUtil.addDay(earlierOrder.getCreationDate(), -1)))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherBoolean(false));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Проверить наличие заказов по toDate первого заказа")
        @Test
        public void toLater() throws Exception {
            mockMvc.perform(
                            get("/orders/exist")
                                    .param(UID, order.getUserClientInfo().getUid().toString())
                                    .param(TO_DATE, DATE_FORMAT.format(earlierOrder.getCreationDate()))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherBoolean(true));
        }

        @Tag(Tags.AUTO)
        @DisplayName("Проверить наличие заказов по toDate второго заказа")
        @Test
        public void toTooLate() throws Exception {
            mockMvc.perform(
                            get("/orders/exist")
                                    .param(UID, order.getUserClientInfo().getUid().toString())
                                    .param(TO_DATE, DATE_FORMAT.format(order.getCreationDate()))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherBoolean(true));
        }

    }
}
