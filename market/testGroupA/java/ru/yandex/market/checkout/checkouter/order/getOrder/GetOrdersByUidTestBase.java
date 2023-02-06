package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.stream.Stream;

import io.qameta.allure.Epic;
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

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.edit.OrderEditService;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.ParameterizedRequest;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherBoolean;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherOrdersNotFound;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

/**
 * @author mmetlov
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class GetOrdersByUidTestBase extends AbstractWebTestBase {

    protected Order defaultOrder;
    protected Order modifiedOrder;
    protected Order orderWithOutletDelivery;

    @Autowired
    protected YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    protected OrderEditService orderEditService;

    @BeforeAll
    public void init() {
        setupFeatureDefaults();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getBuyer().setUid(555L);
        defaultOrder = orderCreateHelper.createOrder(parameters);

        parameters.getBuyer().setUid(666L);
        modifiedOrder = orderCreateHelper.createOrder(parameters);

        orderWithOutletDelivery = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();
    }

    @AfterEach
    @Override
    public void tearDownBase() {
        setupFeatureDefaults();
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }


    public static class OrdersTest extends GetOrdersByUidTestBase {

        public static Stream<Arguments> parameterizedTestData() {

            return Arrays.asList(
                    new Object[]{"GET /orders", parameterizedGetRequest("/orders?uid={uid}")},
                    new Object[]{"GET /orders/by-uid/{uid}", parameterizedGetRequest("/orders/by-uid/{uid}")},
                    new Object[]{"POST /get-orders", parameterizedPostRequest("/get-orders",
                            "{\"rgbs\":[\"BLUE\",\"WHITE\"],\"userId\":%s}")}
            ).stream().map(Arguments::of);
        }

        @Epic(Epics.GET_ORDER)
        @DisplayName("Получить заказ по uid")
        @ParameterizedTest(name = "{0}")
        @MethodSource("parameterizedTestData")
        public void uidTest(String caseName, ParameterizedRequest<Long> parameterizedRequest) throws Exception {
            long uid = defaultOrder.getBuyer().getUid();
            long uid2 = modifiedOrder.getBuyer().getUid();

            // First user Id, expects one order in response with same user id
            mockMvc.perform(
                    parameterizedRequest.build(uid)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].buyer.uid").value(defaultOrder.getBuyer().getUid().intValue()))
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)));

            // Second user Id, expects one order in response with same user id
            mockMvc.perform(
                    parameterizedRequest.build(uid2)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                    .andExpect(jsonPath("$.orders[0].buyer.uid").value(modifiedOrder.getBuyer().getUid().intValue()))
                    .andExpect(jsonPath("$.orders[*]", hasSize(1)));

            //Not existing shop Id, expect error
            long wrongId = Long.MAX_VALUE;
            mockMvc.perform(
                    parameterizedRequest.build(wrongId)
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name()))
                    .andExpect(resultMatcherOrdersNotFound());
        }
    }

    public static class CountsTest extends GetOrdersByUidTestBase {

        @DisplayName("Посчитать заказ по uid")
        @Test
        public void uidTest() throws Exception {
            Long uid = defaultOrder.getBuyer().getUid();
            Long uid2 = modifiedOrder.getBuyer().getUid();

            // First user Id, expects one order in response with same user id
            mockMvc.perform(
                    get("/orders/count")
                            .param("uid", uid.toString())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));

            // Second user Id, expects one order in response with same user id
            mockMvc.perform(
                    get("/orders/count")
                            .param("uid", uid2.toString())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(1));

            //Not existing shop Id, expect error
            Long wrongId = Long.MAX_VALUE;
            mockMvc.perform(
                    get("/orders/count")
                            .param("uid", wrongId.toString())
                            .param(CheckouterClientParams.RGB, Color.BLUE.name())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherCount(0));

        }
    }

    public static class ExistTest extends GetOrdersByUidTestBase {

        @DisplayName("Проверить наличие заказа по uid")
        @Test
        public void uidTest() throws Exception {
            Long uid = defaultOrder.getBuyer().getUid();
            Long uid2 = modifiedOrder.getBuyer().getUid();


            // First user Id, expects one order in response with same user id
            mockMvc.perform(
                    get("/orders/exist")
                            .param("uid", uid.toString())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherBoolean(true));

            // Second user Id, expects one order in response with same user id
            mockMvc.perform(
                    get("/orders/exist")
                            .param("uid", uid2.toString())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherBoolean(true));

            //Not existing shop Id, expect error
            Long wrongId = Long.MAX_VALUE;
            mockMvc.perform(
                    get("/orders/exist")
                            .param("uid", wrongId.toString())
                            .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                    .andExpect(resultMatcherBoolean(false));

        }
    }


    public static class RecentOrdersTest extends GetOrdersByUidTestBase {

        @DisplayName("Проверяем, что заказ с DeliveryType=PICKUP возвращается с заполненным outlet")
        @Test
        public void pickupDeliveryHasOutletAddressTest() throws Exception {
            Long uid = orderWithOutletDelivery.getBuyer().getUid();

            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.RGB, orderWithOutletDelivery.getRgb().name())
                            .param(CheckouterClientParams.STATUS, orderWithOutletDelivery.getStatus().name())
                            .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[*]", hasSize(1)))
                    .andExpect(jsonPath("$.[0].delivery.outlet").isMap())
                    .andExpect(jsonPath("$.[0].delivery.outletId").exists());
        }

        @DisplayName("Проверяем, что заказ с DeliveryType=PICKUP возвращается с заполненными сроками хранения")
        @Test
        public void pickupDeliveryHasOutletPeriods() throws Exception {
            Long uid = orderWithOutletDelivery.getBuyer().getUid();

            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.RGB, orderWithOutletDelivery.getRgb().name())
                            .param(CheckouterClientParams.STATUS, orderWithOutletDelivery.getStatus().name())
                            .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[*]", hasSize(1)))
                    .andExpect(jsonPath("$.[0].delivery.outletStoragePeriod").exists());
        }

        @DisplayName("Ошибка 400 если не верно указанны параметры")
        @Test
        public void requiredParametersTest() throws Exception {
            Long uid = defaultOrder.getBuyer().getUid();

            //No RGB required field
            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.STATUS, defaultOrder.getStatus().name()))
                    .andExpect(status().is4xxClientError());

            //No STATUS required field
            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.RGB, defaultOrder.getRgb().name()))
                    .andExpect(status().is4xxClientError());

            //No PAGE_SIZE greater then maximum value
            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.STATUS, defaultOrder.getStatus().name())
                            .param(CheckouterClientParams.RGB, defaultOrder.getRgb().name())
                            .param(CheckouterClientParams.PAGE_SIZE, "21")) //see OrderController#MAX_RECENT_ORDERS
                    .andExpect(status().is4xxClientError());
        }
    }

    public static class GetOrdersByUidFiltersTest extends GetOrdersByUidTestBase {

        @DisplayName("Заказа возвращаются с учетом параметров")
        @Test
        public void filtersTest() throws Exception {
            Long uid = defaultOrder.getBuyer().getUid();
            Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
            parameters.getBuyer().setUid(uid);
            //create second order for user
            final Long lastOrderId = orderCreateHelper.createOrder(parameters).getId();
            assertNotEquals(lastOrderId, defaultOrder.getId(), "Orders must be different");
            //No orders in UNPAID status
            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.RGB, defaultOrder.getRgb().name())
                            .param(CheckouterClientParams.STATUS, OrderStatus.UNPAID.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[*]", hasSize(2)));
            //Return orders in descent order by creation date (last order goes first)
            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.RGB, defaultOrder.getRgb().name())
                            .param(CheckouterClientParams.STATUS, defaultOrder.getStatus().name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[*]", hasSize(2)))
                    .andExpect(jsonPath("$.[0].id").value(lastOrderId))
                    .andExpect(jsonPath("$.[1].id").value(defaultOrder.getId()));
            //Limit response by page_size parameter
            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.RGB, defaultOrder.getRgb().name())
                            .param(CheckouterClientParams.STATUS, defaultOrder.getStatus().name())
                            .param(CheckouterClientParams.PAGE_SIZE, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[*]", hasSize(1)))
                    .andExpect(jsonPath("$.[0].id").value(lastOrderId));
        }

        @DisplayName("Заказы возвращаются с учетом параметра active")
        @Test
        public void shouldReturnOnlyActiveWhenOnlyActiveFilterIsActive() throws Exception {
            Long uid = defaultOrder.getBuyer().getUid();
            Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
            parameters.getBuyer().setUid(uid);
            orderCreateHelper.createOrder(parameters);

            setFixedTime(getClock().instant().plus(15, ChronoUnit.DAYS));
            Long id = orderCreateHelper.createOrder(parameters).getId();

            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.RGB, defaultOrder.getRgb().name())
                            .param(CheckouterClientParams.STATUS, defaultOrder.getStatus().name())
                            .param(CheckouterClientParams.PAGE_SIZE, "1")
                            .param(CheckouterClientParams.ACTIVE, Boolean.TRUE.toString())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[*]", hasSize(1)))
                    .andExpect(jsonPath("$.[0].id").value(id));
        }
    }

    public static class GetOrdersByUidPartialsTest extends GetOrdersByUidTestBase {

        @Autowired
        private OrderInsertHelper orderInsertHelper;

        @DisplayName("Вернуть заказ с частично заполненными полями")
        @Test
        public void partialsTest() throws Exception {
            Long uid = defaultOrder.getBuyer().getUid();

            // Fill fields that exists only in orders table
            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.RGB, defaultOrder.getRgb().name())
                            .param(CheckouterClientParams.STATUS, defaultOrder.getStatus().name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[*]", hasSize(1)))
                    .andExpect(jsonPath("$.[0].id").value(defaultOrder.getId()))
                    .andExpect(jsonPath("$.[0].status").value(defaultOrder.getStatus().name()))
                    .andExpect(jsonPath("$.[0].rgb").value(defaultOrder.getRgb().name()))
                    .andExpect(jsonPath("$.[0].items").doesNotExist())
                    .andExpect(jsonPath("$.[0].delivery").doesNotExist())
                    .andExpect(jsonPath("$.[0].buyer").doesNotExist());

            // Fill delivery field
            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.RGB, defaultOrder.getRgb().name())
                            .param(CheckouterClientParams.STATUS, defaultOrder.getStatus().name())
                            .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[*]", hasSize(1)))
                    .andExpect(jsonPath("$.[0].id").value(defaultOrder.getId()))
                    .andExpect(jsonPath("$.[0].status").value(defaultOrder.getStatus().name()))
                    .andExpect(jsonPath("$.[0].rgb").value(defaultOrder.getRgb().name()))
                    .andExpect(jsonPath("$.[0].items").doesNotExist())
                    .andExpect(jsonPath("$.[0].buyer").doesNotExist())
                    .andExpect(jsonPath("$.[0].delivery").exists())
                    .andExpect(jsonPath("$.[0].delivery.type").value(defaultOrder.getDelivery().getType().name()))
                    .andExpect(jsonPath("$.[0].delivery.dates").exists())
                    .andExpect(jsonPath("$.[0].delivery.buyerAddress").exists())
                    .andExpect(jsonPath("$.[0].delivery.verificationPart").doesNotExist());

            // Fill items field
            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", uid)
                            .param(CheckouterClientParams.RGB, defaultOrder.getRgb().name())
                            .param(CheckouterClientParams.STATUS, defaultOrder.getStatus().name())
                            .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.ITEMS.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[*]", hasSize(1)))
                    .andExpect(jsonPath("$.[0].id").value(defaultOrder.getId()))
                    .andExpect(jsonPath("$.[0].status").value(defaultOrder.getStatus().name()))
                    .andExpect(jsonPath("$.[0].rgb").value(defaultOrder.getRgb().name()))
                    .andExpect(jsonPath("$.[0].delivery").doesNotExist())
                    .andExpect(jsonPath("$.[0].buyer").doesNotExist())
                    .andExpect(jsonPath("$.[0].items").exists())
                    .andExpect(jsonPath("$.[0].items[*]", hasSize(defaultOrder.getItems().size())));

            // Create order without 'buyerAddress' in Delivery and with 'muid' and 'bindKey' in Buyer
            final Order customOrder = OrderProvider.getBlueOrder();
            customOrder.getDelivery().setBuyerAddress(null);
            customOrder.getBuyer().setMuid(9523L);
            customOrder.getBuyer().setBindKey("bk7896");
            orderInsertHelper.insertOrder(customOrder);
            // Do not return 'buyerAddress' in Delivery if not set
            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", customOrder.getBuyer().getUid())
                            .param(CheckouterClientParams.RGB, customOrder.getRgb().name())
                            .param(CheckouterClientParams.STATUS, customOrder.getStatus().name())
                            .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[*]", hasSize(1)))
                    .andExpect(jsonPath("$.[0].id").value(customOrder.getId()))
                    .andExpect(jsonPath("$.[0].status").value(customOrder.getStatus().name()))
                    .andExpect(jsonPath("$.[0].rgb").value(customOrder.getRgb().name()))
                    .andExpect(jsonPath("$.[0].items").doesNotExist())
                    .andExpect(jsonPath("$.[0].buyer").doesNotExist())
                    .andExpect(jsonPath("$.[0].delivery").exists())
                    .andExpect(jsonPath("$.[0].delivery.type").value(customOrder.getDelivery().getType().name()))
                    .andExpect(jsonPath("$.[0].delivery.dates").exists())
                    .andExpect(jsonPath("$.[0].delivery.buyerAddress").doesNotExist())
                    .andExpect(jsonPath("$.[0].delivery.verificationPart").doesNotExist());

            // Fill buyer field
            mockMvc.perform(
                    get("/orders/by-uid/{uid}/recent", customOrder.getBuyer().getUid())
                            .param(CheckouterClientParams.RGB, customOrder.getRgb().name())
                            .param(CheckouterClientParams.STATUS, customOrder.getStatus().name())
                            .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.BUYER.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[*]", hasSize(1)))
                    .andExpect(jsonPath("$.[0].id").value(customOrder.getId()))
                    .andExpect(jsonPath("$.[0].status").value(customOrder.getStatus().name()))
                    .andExpect(jsonPath("$.[0].rgb").value(customOrder.getRgb().name()))
                    .andExpect(jsonPath("$.[0].items").doesNotExist())
                    .andExpect(jsonPath("$.[0].delivery").doesNotExist())
                    .andExpect(jsonPath("$.[0].buyer").exists())
                    .andExpect(jsonPath("$.[0].buyer.uid").value(customOrder.getBuyer().getUid()))
                    .andExpect(jsonPath("$.[0].buyer.muid").value(customOrder.getBuyer().getMuid()))
                    .andExpect(jsonPath("$.[0].buyer.bindKey").value(customOrder.getBuyer().getBindKey()));
        }
    }
}
