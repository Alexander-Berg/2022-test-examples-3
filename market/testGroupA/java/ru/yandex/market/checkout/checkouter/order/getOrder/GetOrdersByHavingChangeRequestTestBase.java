package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.net.URI;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.json.Names;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.PaymentChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.checkouter.storage.changerequest.ChangeRequestDao;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByHavingChangeRequestTestBase extends AbstractWebTestBase {

    @Autowired
    protected ChangeRequestDao changeRequestDao;
    @Autowired
    protected OrderHistoryDao orderHistoryDao;
    @Autowired
    protected OrderServiceHelper orderServiceHelper;

    protected Order orderWithoutCR;
    protected Order orderWithCancellationNewByUserCR;
    protected Order orderWithCancellationAppliedBySystemCR;
    protected Order orderWithPaymentMethodNewByUserCR;
    protected Order orderWithPaymentMethodRejectedByShopCR;

    protected ClientInfo userClientInfo = new ClientInfo(ClientRole.USER, BuyerProvider.UID);
    protected ClientInfo systemClientInfo = ClientInfo.SYSTEM;
    protected ClientInfo shopClientInfo = new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID);

    @BeforeAll
    public void init() {
        transactionTemplate.execute(tc -> {
            orderWithoutCR = orderServiceHelper.saveOrder(OrderProvider.getBlueOrder());

            orderWithCancellationNewByUserCR = orderServiceHelper.saveOrder(OrderProvider.getBlueOrder());
            long historyId = orderHistoryDao.insertOrderHistory(orderWithCancellationNewByUserCR.getId(),
                    HistoryEventType.ORDER_CHANGE_REQUEST_CREATED, ClientInfo.SYSTEM);
            var newCancellationRequestPayload = new CancellationRequestPayload(OrderSubstatus.USER_CHANGED_MIND,
                    null, null, null);
            changeRequestDao.save(orderWithCancellationNewByUserCR, newCancellationRequestPayload,
                    ChangeRequestStatus.NEW,
                    userClientInfo, historyId);

            orderWithCancellationAppliedBySystemCR = orderServiceHelper.saveOrder(OrderProvider.getBlueOrder());
            historyId = orderHistoryDao.insertOrderHistory(orderWithCancellationAppliedBySystemCR.getId(),
                    HistoryEventType.ORDER_CHANGE_REQUEST_CREATED, ClientInfo.SYSTEM);
            var appliedCancellationRequestPayload = new CancellationRequestPayload(OrderSubstatus.USER_CHANGED_MIND,
                    null, null, null);
            changeRequestDao.save(orderWithCancellationAppliedBySystemCR, appliedCancellationRequestPayload,
                    ChangeRequestStatus.APPLIED, systemClientInfo, historyId);

            orderWithPaymentMethodNewByUserCR = orderServiceHelper.saveOrder(OrderProvider.getBlueOrder());
            historyId = orderHistoryDao.insertOrderHistory(orderWithPaymentMethodNewByUserCR.getId(),
                    HistoryEventType.ORDER_CHANGE_REQUEST_CREATED, ClientInfo.SYSTEM);
            var newPaymentMethodRequestPayload = new PaymentChangeRequestPayload(PaymentMethod.APPLE_PAY, null);
            changeRequestDao.save(orderWithPaymentMethodNewByUserCR, newPaymentMethodRequestPayload,
                    ChangeRequestStatus.NEW, userClientInfo, historyId);

            orderWithPaymentMethodRejectedByShopCR = orderServiceHelper.saveOrder(OrderProvider.getBlueOrder());
            historyId = orderHistoryDao.insertOrderHistory(orderWithPaymentMethodRejectedByShopCR.getId(),
                    HistoryEventType.ORDER_CHANGE_REQUEST_CREATED, ClientInfo.SYSTEM);
            var rejectedPaymentMethodRequestPayload = new PaymentChangeRequestPayload(PaymentMethod.APPLE_PAY, null);
            changeRequestDao.save(orderWithPaymentMethodRejectedByShopCR, rejectedPaymentMethodRequestPayload,
                    ChangeRequestStatus.REJECTED, shopClientInfo, historyId);
            return null;
        });
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    public static class OrdersTest extends GetOrdersByHavingChangeRequestTestBase {

        public static Stream<Arguments> parameterizedTestData() {
            return Stream.of(
                    new Object[]{"GET /orders",
                            URI.create("/orders"),
                            HttpMethod.GET},
                    new Object[]{"GET /orders/by-uid/{userId}",
                            URI.create("/orders/by-uid/" + BuyerProvider.UID),
                            HttpMethod.GET},
                    new Object[]{"POST /get-orders",
                            URI.create("/get-orders"),
                            HttpMethod.POST}
            ).map(Arguments::of);
        }

        @DisplayName("Получить все заказы без фильтрации по статусам и типам ChangeRequest'ов")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void allOrdersTest(String testName, URI uri, HttpMethod httpMethod) throws Exception {
            var rb = request(httpMethod, uri)
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name());
            if (httpMethod == HttpMethod.POST) {
                rb.content("{\"rgbs\": [\"BLUE\"]}").contentType(MediaType.APPLICATION_JSON_UTF8);
            }
            mockMvc.perform(rb)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]", hasSize(5)));
        }

        @DisplayName("Получить заказы с CANCELLATION ChangeRequest'ами")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void filterOrdersWithCancellationChangeRequestsTest(String testName, URI uri, HttpMethod httpMethod)
                throws Exception {
            var rb = request(httpMethod, uri)
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name());
            if (httpMethod.equals(HttpMethod.POST)) {
                rb
                        .content("{\"rgbs\": [\"BLUE\"], \"" + CheckouterClientParams.HAVING_CHANGE_REQUEST_TYPES +
                                "\": [\"" + ChangeRequestType.CANCELLATION.name() + "\"]}")
                        .contentType(MediaType.APPLICATION_JSON_UTF8);
            } else {
                rb.param(CheckouterClientParams.HAVING_CHANGE_REQUEST_TYPES, ChangeRequestType.CANCELLATION.name());
            }
            mockMvc.perform(rb)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]", hasSize(2)))
                    .andExpect(jsonPath("$.orders[*]",
                            containsInAnyOrder(
                                    hasEntry("id", orderWithCancellationNewByUserCR.getId().intValue()),
                                    hasEntry("id", orderWithCancellationAppliedBySystemCR.getId().intValue()))
                    ));
        }

        @DisplayName("Получить заказы с ChangeRequest'ами в статусе NEW")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void filterOrdersWithNewChangeRequestsTest(String testName, URI uri, HttpMethod httpMethod)
                throws Exception {
            var rb = request(httpMethod, uri)
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name());
            if (httpMethod.equals(HttpMethod.POST)) {
                rb
                        .content("{\"rgbs\": [\"BLUE\"], \"" + CheckouterClientParams.HAVING_CHANGE_REQUEST_STATUSES +
                                "\": [\"" + ChangeRequestStatus.NEW.name() + "\"]}")
                        .contentType(MediaType.APPLICATION_JSON_UTF8);
            } else {
                rb.param(CheckouterClientParams.HAVING_CHANGE_REQUEST_STATUSES, ChangeRequestStatus.NEW.name());
            }
            mockMvc.perform(rb)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]", hasSize(2)))
                    .andExpect(jsonPath("$.orders[*]",
                            containsInAnyOrder(
                                    hasEntry("id", orderWithCancellationNewByUserCR.getId().intValue()),
                                    hasEntry("id", orderWithPaymentMethodNewByUserCR.getId().intValue()))
                    ));
        }

        @DisplayName("Получить заказы с PaymentMethod ChangeRequest'ами в статусе REJECTED и NEW")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void filterOrdersWithRejectedAndNewPaymentMethodChangeRequestsTest(String testName, URI uri,
                                                                                  HttpMethod httpMethod)
                throws Exception {
            var rb = request(httpMethod, uri)
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name());
            if (httpMethod.equals(HttpMethod.POST)) {
                rb
                        .content("{\"rgbs\": [\"BLUE\"], \"" + CheckouterClientParams.HAVING_CHANGE_REQUEST_TYPES +
                                "\": [\"" + ChangeRequestType.PAYMENT_METHOD.name() + "\"]," +
                                "\"" + CheckouterClientParams.HAVING_CHANGE_REQUEST_STATUSES + "\": [\"" +
                                ChangeRequestStatus.REJECTED.name() + "\",\"" + ChangeRequestStatus.NEW.name() + "\"]}")
                        .contentType(MediaType.APPLICATION_JSON_UTF8);
            } else {
                rb
                        .param(CheckouterClientParams.HAVING_CHANGE_REQUEST_TYPES,
                                ChangeRequestType.PAYMENT_METHOD.name())
                        .param(CheckouterClientParams.HAVING_CHANGE_REQUEST_STATUSES,
                                ChangeRequestStatus.REJECTED.name(),
                                ChangeRequestStatus.NEW.name());
            }
            mockMvc.perform(rb)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]", hasSize(2)))
                    .andExpect(jsonPath("$.orders[*]",
                            containsInAnyOrder(
                                    hasEntry("id", orderWithPaymentMethodNewByUserCR.getId().intValue()),
                                    hasEntry("id", orderWithPaymentMethodRejectedByShopCR.getId().intValue()))
                    ));
        }

        @DisplayName("Получить заказы с ChangeRequest'ами созданными ролью USER")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void filterOrdersWithUserAuthorChangeRequestsTest(String testName, URI uri,
                                                                 HttpMethod httpMethod) throws Exception {
            var rb = request(httpMethod, uri)
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name());
            if (httpMethod.equals(HttpMethod.POST)) {
                rb
                        .content("{\"rgbs\": [\"BLUE\"], \"" +
                                CheckouterClientParams.HAVING_CHANGE_REQUEST_AUTHOR_ROLES +
                                "\": [\"" + ClientRole.USER + "\"]}")
                        .contentType(MediaType.APPLICATION_JSON_UTF8);
            } else {
                rb
                        .param(CheckouterClientParams.HAVING_CHANGE_REQUEST_AUTHOR_ROLES,
                                ClientRole.USER.name());
            }
            mockMvc.perform(rb)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[*]", hasSize(2)))
                    .andExpect(jsonPath("$.orders[*]",
                            containsInAnyOrder(
                                    hasEntry("id", orderWithCancellationNewByUserCR.getId().intValue()),
                                    hasEntry("id", orderWithPaymentMethodNewByUserCR.getId().intValue()))
                    ));
        }
    }

    public static class CountAndExistsTest extends GetOrdersByHavingChangeRequestTestBase {

        @DisplayName("Посчитать заказы без фильтрации по статусам и типам ChangeRequest'ов")
        @Test
        public void countAllOrdersTest() throws Exception {
            mockMvc.perform(get("/orders/count")
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(resultMatcherCount(5));
        }

        @DisplayName("Посчитать заказы имеющие ChangeRequest'ы с типом PaymentMethod")
        @Test
        public void countOrdersWithPaymentMethodChangeRequestsTest() throws Exception {
            mockMvc.perform(get("/orders/count")
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                    .param(CheckouterClientParams.HAVING_CHANGE_REQUEST_TYPES,
                            ChangeRequestType.PAYMENT_METHOD.name()))
                    .andExpect(status().isOk())
                    .andExpect(resultMatcherCount(2));
        }

        @DisplayName("Посчитать заказы имеющие ChangeRequest'ы в статусе Applied и Rejected")
        @Test
        public void countOrdersWithAppliedAndRejectedChangeRequestsTest() throws Exception {
            mockMvc.perform(get("/orders/count")
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                    .param(CheckouterClientParams.HAVING_CHANGE_REQUEST_STATUSES,
                            ChangeRequestStatus.APPLIED.name(),
                            ChangeRequestStatus.REJECTED.name()))
                    .andExpect(status().isOk())
                    .andExpect(resultMatcherCount(2));
        }

        @DisplayName("Посчитать заказы имеющие CANCELLATION ChangeRequest'ы в статусе Applied")
        @Test
        public void countOrdersWithAppliedCancellationChangeRequestsTest() throws Exception {
            mockMvc.perform(get("/orders/count")
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                    .param(CheckouterClientParams.HAVING_CHANGE_REQUEST_TYPES,
                            ChangeRequestType.CANCELLATION.name())
                    .param(CheckouterClientParams.HAVING_CHANGE_REQUEST_STATUSES,
                            ChangeRequestStatus.APPLIED.name()))
                    .andExpect(status().isOk())
                    .andExpect(resultMatcherCount(1));
        }

        @DisplayName("Посчитать заказы имеющие ChangeRequest'ы созданные ролью SHOP или SYSTEM")
        @Test
        public void countOrdersWithShopAuthorChangeRequestsTest() throws Exception {
            mockMvc.perform(get("/orders/count")
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                    .param(CheckouterClientParams.HAVING_CHANGE_REQUEST_AUTHOR_ROLES,
                            ClientRole.SHOP.name(), ClientRole.SYSTEM.name()))
                    .andExpect(status().isOk())
                    .andExpect(resultMatcherCount(2));
        }

        @DisplayName("Имеются ли заказы имеющие ChangeRequest'ы в статусе Applied и Rejected")
        @Test
        public void existsOrdersWithAppliedAndRejectedChangeRequestsTest() throws Exception {
            mockMvc.perform(get("/orders/exist")
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                    .param(CheckouterClientParams.UID, Long.toString(BuyerProvider.UID))
                    .param(CheckouterClientParams.HAVING_CHANGE_REQUEST_STATUSES,
                            ChangeRequestStatus.APPLIED.name(),
                            ChangeRequestStatus.REJECTED.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("value", Matchers.equalTo(true)));
        }

        @DisplayName("Имеются ли заказы имеющие ChangeRequest'ы c типом ITEMS_REMOVAL")
        @Test
        public void existsOrdersWithItemsRemovalChangeRequestsTest() throws Exception {
            mockMvc.perform(get("/orders/exist")
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                    .param(CheckouterClientParams.UID, Long.toString(BuyerProvider.UID))
                    .param(CheckouterClientParams.HAVING_CHANGE_REQUEST_TYPES,
                            ChangeRequestType.ITEMS_REMOVAL.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("value", Matchers.equalTo(false)));
        }

        @DisplayName("Имеются ли заказы имеющие ChangeRequest'ы созданные ролью SHOP_USER")
        @Test
        public void existsOrdersWithShopUserRoleChangeRequestsTest() throws Exception {
            mockMvc.perform(get("/orders/exist")
                    .param(Names.Order.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                    .param(CheckouterClientParams.UID, Long.toString(BuyerProvider.UID))
                    .param(CheckouterClientParams.HAVING_CHANGE_REQUEST_AUTHOR_ROLES,
                            ClientRole.SHOP_USER.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("value", Matchers.equalTo(false)));
        }
    }
}
