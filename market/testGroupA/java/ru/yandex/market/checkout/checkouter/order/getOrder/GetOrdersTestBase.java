package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.json.Names;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherOrdersNotFound;

/**
 * Тест на бОльшую чась параметров GET /orders/*
 * при добавлении нового параметра НЕ ДОПИСЫВАЙТЕ тест сюда, а создайте отдельный класс
 *
 * @see GetOrdersByExcludeABOTestBase
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersTestBase extends AbstractWebTestBase {

    protected static final String DATE_FORMAT_PATTERN = "dd-MM-yyyy";
    protected static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);
    protected static final DateTimeFormatter DATE_FORMAT8 = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
    protected Order defaultOrder;
    protected Order modifiedOrder;
    protected Order globalOrder;
    protected long minStatusUpdateTimestamp;
    protected long maxStatusUpdateTimestamp;
    @Autowired
    private OrderReadingDao orderReadingDao;
    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @BeforeAll
    public void init() throws Exception {
        long globalOrderId = orderServiceHelper.createGlobalOrder();
        globalOrder = orderReadingDao.getOrder(globalOrderId, ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(globalOrderId));
        jumpToFuture(2, ChronoUnit.SECONDS);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        defaultOrder = orderCreateHelper.createOrder(parameters);

        jumpToFuture(4, ChronoUnit.SECONDS);

        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setSandbox(true);
        parameters.getOrder().setNotes("per aspera ad astra");
        modifiedOrder = orderCreateHelper.createOrder(parameters);

        modifiedOrder = orderStatusHelper.updateOrderStatus(
                modifiedOrder.getId(),
                new ClientInfo(ClientRole.SYSTEM, 1L),
                OrderStatus.CANCELLED,
                OrderSubstatus.PENDING_CANCELLED
        );

        minStatusUpdateTimestamp = Stream.of(defaultOrder, modifiedOrder, globalOrder)
                .min(Comparator.comparing(Order::getStatusUpdateDate))
                .get()
                .getStatusUpdateDate()
                .getTime();

        maxStatusUpdateTimestamp = Stream.of(defaultOrder, modifiedOrder, globalOrder)
                .max(Comparator.comparing(Order::getStatusUpdateDate))
                .get()
                .getStatusUpdateDate()
                .getTime();
    }

    @Override
    @AfterEach
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }


    public static class GetTest extends GetOrdersTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", "/orders"},
                    new Object[]{"GET /orders/by-uid/{uid}", "/orders/by-uid/" + BuyerProvider.UID}
            ).stream().map(Arguments::of);
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по id")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void idTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.ID, defaultOrder.getId().toString())
                                    .param(CheckouterClientParams.ID, modifiedOrder.getId().toString())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]").value(containsInAnyOrder(
                            hasEntry("id", modifiedOrder.getId().intValue()),
                            hasEntry("id", defaultOrder.getId().intValue()))));

            long wrongId = Long.MAX_VALUE;
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.ID, Long.toString(wrongId))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: пагинация")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void pagerTest(String caseName, String urlTemplate) throws Exception {
            final String pageSize = "1";
            //PAGE_SIZE has settled, default page
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.PAGE_SIZE, pageSize)
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)));


            //PAGE_SIZE has settled, first page
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.PAGE, "1")
                                    .param(CheckouterClientParams.PAGE_SIZE, pageSize)
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)));

            //PAGE_SIZE has settled, second page
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.PAGE, "2")
                                    .param(CheckouterClientParams.PAGE_SIZE, pageSize)
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)));

            //PAGE_SIZE has settled, too big page PAGE
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.PAGE, "4")
                                    .param(CheckouterClientParams.PAGE_SIZE, "1")
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pager.page").value(3));

        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по status/substatus")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void statusAndSubstatusTest(String caseName, String urlTemplate) throws Exception {

            // find order by status
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS, defaultOrder.getStatus().name())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].status").value(defaultOrder.getStatus().name()));

            // find order by substatus
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.SUBSTATUS,
                                            OrderSubstatus.PENDING_CANCELLED.toString())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].substatus").value(OrderSubstatus.PENDING_CANCELLED.name()));

            //There is no orders with this status was created, expect error
            long wrongId = Long.MAX_VALUE;
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS, OrderStatus.DELIVERED.name())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());

        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по fake")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void fakeTest(String caseName, String urlTemplate) throws Exception {
            // search for not fake orders. Expects to find first one.
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.FAKE, String.valueOf(false))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].fake").value(false))
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)));

            //  search for fake orders. Expects to find second one.
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.FAKE, String.valueOf(true))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].fake").value(true))
                    .andExpect(jsonPath("$.orders[*]", hasSize(2)));
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по paymentId")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void paymentIdTest(String caseName, String urlTemplate) throws Exception {
            int paymentId = globalOrder.getPaymentId().intValue();

            // search order with correct payment Id. Expects to find one order.
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.PAYMENT_ID, String.valueOf(paymentId))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(globalOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].paymentId").value(paymentId))
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)));

            //Not existing payment Id, expect error
            long wrongId = Long.MAX_VALUE;
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.PAYMENT_ID, String.valueOf(wrongId))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());

        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по paymentType/paymentMethod")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void paymentTypeMethodTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.PAYMENT_TYPE, String.valueOf(PaymentType.PREPAID))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].paymentType").value(PaymentType.PREPAID.toString()));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.PAYMENT_TYPE, String.valueOf(PaymentType.POSTPAID))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по statusUpdateFromDate")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void statusUpdateFromTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM,
                                            DATE_FORMAT.format(defaultOrder.getStatusUpdateDate()))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[*]", hasSize(3)));

            LocalDateTime minusDayDate = LocalDateTime
                    .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).minusDays(1);
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM, DATE_FORMAT8.format(minusDayDate))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[*]", hasSize(3)));

            LocalDateTime tooBigDate = LocalDateTime
                    .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).plusDays(1);
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM, DATE_FORMAT8.format(tooBigDate))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по statusUpdateToDate")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void statusUpdateToTest(String caseName, String urlTemplate) throws Exception {
            //Whe set current day. There is no order would be found. Suppose this is an error and would be fixed in
            // MARKETCHECKOUT-618
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO,
                                            DATE_FORMAT.format(defaultOrder.getStatusUpdateDate()))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());

            LocalDateTime plusDayDate = LocalDateTime
                    .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).plusDays(1);
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO, DATE_FORMAT8.format(plusDayDate))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[*]", hasSize(3)));

            LocalDateTime tooSmallDate = LocalDateTime
                    .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).minusDays(1);
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO, DATE_FORMAT8.format(tooSmallDate))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по statusUpdateFromDate/statusUpdateToDate")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void statusUpdateFromToTest(String caseName, String urlTemplate) throws Exception {
            //Whe set current day. There is no order would be found. Suppose this is an error and would be fixed in
            // MARKETCHECKOUT-618
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM,
                                            DATE_FORMAT.format(defaultOrder.getStatusUpdateDate()))
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO,
                                            DATE_FORMAT.format(defaultOrder.getStatusUpdateDate()))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());

            LocalDateTime minusDayDate = LocalDateTime
                    .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).minusDays(1);
            LocalDateTime plusDayDate = LocalDateTime
                    .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).plusDays(1);

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM, DATE_FORMAT8.format(minusDayDate))
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO, DATE_FORMAT8.format(plusDayDate))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[*]", hasSize(3)));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM,
                                            DATE_FORMAT8.format(minusDayDate.minusDays(2)))
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO, DATE_FORMAT8.format(minusDayDate))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM, DATE_FORMAT8.format(plusDayDate))
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO,
                                            DATE_FORMAT8.format(plusDayDate.plusDays(2)))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());

        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по statusUpdateFromTimestamp")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void statusUpdateFromTimestampTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM_TIMESTAMP,
                                            String.valueOf(minStatusUpdateTimestamp))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[*]", hasSize(3)));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM_TIMESTAMP,
                                            String.valueOf(maxStatusUpdateTimestamp + 1000))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по statusUpdateToTimestamp")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void statusUpdateToTimestampTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO_TIMESTAMP,
                                            String.valueOf(maxStatusUpdateTimestamp + 1000))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]").value(containsInAnyOrder(
                            hasEntry("id", defaultOrder.getId().intValue()),
                            hasEntry("id", modifiedOrder.getId().intValue()),
                            hasEntry("id", globalOrder.getId().intValue()))));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO_TIMESTAMP,
                                            String.valueOf(minStatusUpdateTimestamp))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по statusUpdateFromTimestamp/statusUpdateToTimestamp")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void statusUpdateFromToTimestampTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM_TIMESTAMP,
                                            String.valueOf(minStatusUpdateTimestamp))
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO_TIMESTAMP,
                                            String.valueOf(maxStatusUpdateTimestamp + 1000))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]").value(containsInAnyOrder(
                            hasEntry("id", defaultOrder.getId().intValue()),
                            hasEntry("id", modifiedOrder.getId().intValue()),
                            hasEntry("id", globalOrder.getId().intValue()))));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM_TIMESTAMP,
                                            String.valueOf(minStatusUpdateTimestamp))
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO_TIMESTAMP,
                                            String.valueOf(maxStatusUpdateTimestamp))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]", hasSize(2)));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.STATUS_UPDATE_FROM_TIMESTAMP,
                                            String.valueOf(minStatusUpdateTimestamp))
                                    .param(CheckouterClientParams.STATUS_UPDATE_TO_TIMESTAMP,
                                            String.valueOf(minStatusUpdateTimestamp))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по shopOrderId")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void shopOrderIdTest(String caseName, String urlTemplate) throws Exception {
            final String wrongShopOrderId = "1000000";

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.SHOP_ORDER_ID, defaultOrder.getShopOrderId())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].shopOrderId").value(defaultOrder.getShopOrderId()));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.SHOP_ORDER_ID, defaultOrder.getShopOrderId())
                                    .param(CheckouterClientParams.SHOP_ORDER_ID, wrongShopOrderId)
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].shopOrderId").value(defaultOrder.getShopOrderId()));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.SHOP_ORDER_ID, wrongShopOrderId)
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по acceptMethod")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void acceptMethodTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.ACCEPT_METHOD,
                                            defaultOrder.getAcceptMethod().toString())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].acceptMethod").value(modifiedOrder.getAcceptMethod().toString()));
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по lastStatusRole")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void lastStatusRoleTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.LAST_STATUS_ROLE, ClientRole.SYSTEM.name())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.LAST_STATUS_ROLE, ClientRole.REFEREE.name())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по noAuth")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void noAuthTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.NO_AUTH, String.valueOf(false))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[1].noAuth").value(false));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.NO_AUTH, String.valueOf(true))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по userGroup")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void userGroupTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.USER_GROUP, UserGroup.DEFAULT.name())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.USER_GROUP, UserGroup.ABO.name())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по context")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void contextsTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.CONTEXT, Context.MARKET.name())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].context").value(Context.MARKET.name()));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.CONTEXT, Context.MARKET.name())
                                    .param(CheckouterClientParams.CONTEXT, Context.PINGER.name())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].context").value(Context.MARKET.name()))
                    .andExpect(jsonPath("$.orders[*]").value(not(contains(hasEntry("context",
                            not(Context.MARKET.name()))))));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.CONTEXT, Context.PINGER.name())
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по notes")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void notesTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.NOTES, "astra")
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.NOTES, "per aspera ad astra")
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(CheckouterClientParams.NOTES, "veritas")
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }

        @Tag(Tags.AUTO)
        @DisplayName("GET /orders/*: получение заказов по global")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void globalTest(String caseName, String urlTemplate) throws Exception {
            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(Names.Order.GLOBAL, String.valueOf(true))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(globalOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[*].id", hasSize(1)));

            mockMvc.perform(
                            MockMvcRequestBuilders.get(urlTemplate)
                                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                                    .param(Names.Order.GLOBAL, String.valueOf(false))
                                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[*].id", hasSize(2)));
        }
    }
}
