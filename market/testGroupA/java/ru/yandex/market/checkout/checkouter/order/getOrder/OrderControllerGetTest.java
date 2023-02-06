package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.json.IntHolder;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.tvm.TvmAuthorization;
import ru.yandex.market.checkout.checkouter.tvm.TvmAuthorizationType;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderItemViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderViewModel;
import ru.yandex.market.checkout.common.trace.TvmAuthorizationContextHolder;
import ru.yandex.market.checkout.helpers.OrderControllerTestHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.OrdersGetHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.b2b.B2bCustomersMockConfigurer;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.resale.ResaleSpecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.common.util.collections.CollectionUtils.first;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.BUSINESS_BALANCE_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.RGB;
import static ru.yandex.market.checkout.checkouter.json.Names.Buyer.ASSESSOR;
import static ru.yandex.market.checkout.helpers.OrderControllerTestHelper.createOrder;
import static ru.yandex.market.checkout.test.providers.ResaleSpecsProvider.getResaleSpecs;

public class OrderControllerGetTest extends AbstractWebTestBase {

    private static final long SHOP_ID = 4545L;
    private static final ClientInfo CLIENT_INFO = new ClientInfo(ClientRole.SHOP_USER, BuyerProvider.UID, SHOP_ID);
    @Autowired
    private OrderGetHelper orderGetHelper;
    @Autowired
    private OrdersGetHelper ordersGetHelper;
    @Autowired
    private TestSerializationService testSerializationService;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrderControllerTestHelper orderControllerTestHelper;
    @Autowired
    private B2bCustomersMockConfigurer b2bCustomersMockConfigurer;

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS)
    @DisplayName("Проверяем работу shopReturnStatuses")
    @Test
    public void getOrders() throws Exception {
        orderServiceHelper.prepareOrderWithReturnStatusesCheckpoint(
                DeliveryProvider.RUSPOSTPICKUP_DELIVERY_SERVICE_ID
        );

        mockMvc.perform(get("/orders")
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].delivery.tracks[0].checkpoints").value(hasSize(0)));

        mockMvc.perform(get("/orders")
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].delivery.tracks[0].checkpoints").value(hasSize(0)));

        mockMvc.perform(get("/orders")
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].delivery.tracks[0].checkpoints").value(hasSize(0)));
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS)
    @DisplayName("Проверяем работу getOrders с твм")
    @Test
    public void getOrdersWithTvm() throws Exception {
        try {


            orderServiceHelper.prepareOrderWithReturnStatusesCheckpoint(
                    DeliveryProvider.RUSPOSTPICKUP_DELIVERY_SERVICE_ID
            );

            mockMvc.perform(get("/orders")
                    .param(CLIENT_ROLE, "USER")
                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                    .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                    .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].delivery.tracks").exists())
                    .andExpect(jsonPath("$.orders[0].delivery.parcels[0].tracks").exists())
                    .andExpect(jsonPath("$.orders[0].delivery.parcels[0].shipmentId").exists())
                    .andExpect(jsonPath("$.orders[0].delivery.parcels[0].shopShipmentId").exists());

            //tvm  white list
            TvmAuthorization serviceTvmAuthorization = new TvmAuthorization(TvmAuthorizationType.SERVICE,
                    2011504L);
            TvmAuthorizationContextHolder.setTvmAuthorization(serviceTvmAuthorization);

            mockMvc.perform(get("/orders")
                    .param(CLIENT_ROLE, "USER")
                    .param(CheckouterClientParams.RGB, Color.BLUE.name())
                    .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                    .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                    .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].delivery.tracks").exists())
                    .andExpect(jsonPath("$.orders[0].delivery.parcels[0].tracks").exists())
                    .andExpect(jsonPath("$.orders[0].delivery.parcels[0].shipmentId").exists())
                    .andExpect(jsonPath("$.orders[0].delivery.parcels[0].shopShipmentId").exists());

            serviceTvmAuthorization = new TvmAuthorization(TvmAuthorizationType.SERVICE, 2L);
            TvmAuthorizationContextHolder.setTvmAuthorization(serviceTvmAuthorization);

            //tvm no  white list
            mockMvc.perform(get("/orders")
                    .param(CLIENT_ROLE, "USER")
                    .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                    .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                    .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orders[0].delivery.tracks").doesNotExist())
                    .andExpect(jsonPath("$.orders[0].delivery.id").doesNotExist())
                    .andExpect(jsonPath("$.orders[0].delivery.shipment").doesNotExist())
                    .andExpect(jsonPath("$.orders[0].delivery.shipments").doesNotExist())
                    .andExpect(jsonPath("$.orders[0].delivery.shipments").doesNotExist())
                    .andExpect(jsonPath("$.orders[0].delivery.parcels[0].tracks").doesNotExist())
                    .andExpect(jsonPath("$.orders[0].delivery.parcels[0].shipmentId").doesNotExist())
                    .andExpect(jsonPath("$.orders[0].delivery.parcels[0].shopShipmentId").doesNotExist());
        } finally {
            TvmAuthorizationContextHolder.setTvmAuthorization(TvmAuthorization.notAuthorized());
        }
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @DisplayName("Получение заказа по дате доставки")
    @Test
    public void getOrdersByDeliveryDate() {
        Date date1 = createDate(1);
        Date date2 = createDate(2);
        Date date3 = createDate(3);
        Date date4 = createDate(4);
        Date date5 = createDate(5);

        Order order12 = OrderProvider.getPrepaidOrder();
        patchAndPersist(order12, date1, date2);

        Order order13 = OrderProvider.getPrepaidOrder();
        patchAndPersist(order13, date1, date3);

        Order order15 = OrderProvider.getPrepaidOrder();
        patchAndPersist(order15, date1, date5);

        Order order35 = OrderProvider.getPrepaidOrder();
        patchAndPersist(order35, date3, date5);

        Order order45 = OrderProvider.getPrepaidOrder();
        patchAndPersist(order45, date4, date5);

        Order order22 = OrderProvider.getPrepaidOrder();
        patchAndPersist(order22, date2, date2);

        Order order33 = OrderProvider.getPrepaidOrder();
        patchAndPersist(order33, date3, date3);

        Order order44 = OrderProvider.getPrepaidOrder();
        patchAndPersist(order44, date4, date4);

        checkDeliveryDatesFilter(date3, null, order15.getId(), order35.getId(), order45.getId(), order44.getId());
        checkDeliveryDatesFilter(null, date3, order12.getId(), order22.getId());
        checkDeliveryDatesFilter(date2, date5, order13.getId(), order33.getId(), order44.getId());
    }


    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @DisplayName("showReturnStatuses должен фильтровать чекпоинты в ручке /order/{orderId}")
    @Test
    public void testShowReturnStatusesInGetOrder() throws Exception {
        long postOrder = orderServiceHelper.prepareOrderWithReturnStatusesCheckpoint(
                DeliveryProvider.RUSPOSTPICKUP_DELIVERY_SERVICE_ID
        );

        mockMvc.perform(get("/orders/{orderId}", postOrder)
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.tracks[0].checkpoints").value(hasSize(0)));

        mockMvc.perform(get("/orders/{orderId}", postOrder)
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.tracks[0].checkpoints").value(hasSize(0)));

        mockMvc.perform(get("/orders/{orderId}", postOrder)
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.tracks[0].checkpoints").value(hasSize(0)));

        mockMvc.perform(get("/orders/{orderId}", postOrder)
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].partnerPriceMarkup.coefficients").value(hasSize(2)));

        mockMvc.perform(get("/orders/{orderId}", postOrder)
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].partnerPrice").value(100));
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @DisplayName("showReturnStatuses должен фильтровать чекпоинты в ручке /order/{orderId}")
    @Test
    public void testRgbParamInGetOrder() throws Exception {
        long postOrder = orderServiceHelper.prepareOrder().getId();

        mockMvc.perform(get("/orders/{orderId}", postOrder)
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.RGB, String.valueOf(Color.BLUE)))
                .andExpect(status().isOk());
        Order order = client.getOrder(postOrder, ClientRole.USER, BuyerProvider.UID, false,
                List.of(Color.BLUE, Color.WHITE));
        assertEquals((long) order.getId(), postOrder);
        assertEquals(order.getRgb(), Color.BLUE);

        try {
            client.getOrder(postOrder, ClientRole.USER, BuyerProvider.UID, false,
                    Collections.singletonList(Color.RED));
            Assertions.fail("Should throw OrderNotFoundException");
        } catch (OrderNotFoundException ignored) {
        } catch (Throwable th) {
            Assertions.fail("Should throw OrderNotFoundException, thrown: " + th);
        }

        mockMvc.perform(get("/orders/{orderId}", postOrder)
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID)))
                .andExpect(status().isOk());
        order = client.getOrder(postOrder, ClientRole.USER, BuyerProvider.UID);
        assertEquals((long) order.getId(), postOrder);
        assertEquals(order.getRgb(), Color.BLUE);
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @DisplayName("Должен фильтровать заказы по businessBalanceId в ручке /orders/by-uid/{userId}")
    @Test
    public void shouldFilterOrdersByBusinessBalanceId() throws Exception {
        b2bCustomersMockConfigurer.mockReservationDate(LocalDate.now().plusDays(5));
        orderServiceHelper
                .createPostOrder(order -> order.getBuyer().setBusinessBalanceId(123L))
                .getId();

        mockMvc.perform(get("/orders/by-uid/{userId}", BuyerProvider.UID)
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.RGB, String.valueOf(Color.BLUE))
                .param(BUSINESS_BALANCE_ID, "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].buyer.businessBalanceId").value(123))
                .andExpect(jsonPath("$.orders[0].buyer.type").value("BUSINESS"));
    }

    /**
     * GET /orders
     * <p>
     * Товары в отправлении сортируются по id товара и посылки сортируются то id первого товара в отправлении
     * <p>
     * Подготовка
     * 1. Создать заказ с товарами A и B в количестве 5 шт. каждый (id товара A < id товара B)
     * 2. Добавить в заказ посылку shipment1
     * 3. Добавить в посылку shipment1 товар B в количестве 3 шт.
     * 4. Добавить в посылку shipment1 трек с трек-кодом "iddqd-1"
     * 5. Добавить в заказ посылку shipment2
     * 6. Добавить в посылку shipment2 товар B в количестве 2 шт.
     * 7. Добавить в посылку shipment2 товар А в количестве 5 шт.
     * 8. Добавить в посылку shipment2 трек с трек-кодом "iddqd-2"
     * <p>
     * Действия
     * 1. Сделать запрос к GET /orders. Роль пользователя - "SHOP_USER"
     * <p>
     * Проверки
     * 1. У заказа две посылки
     * 2. Посылка shipment2 первая в списке
     * 3. В посылке shipment2 есть товары A (5 шт.) и B (2 шт.). При этом A - первый в списке.
     * 4. У посылки shipment2 есть единственный трек с трек-номером "iddqd-2"
     * 5. В посылке shipment1 есть единственный товар B в количестве 3 шт.
     * 6. У посылки shipment1 есть единственный трек с трек-номером "iddqd-1"
     * 7. В поле доставки shipment пусто (т.к. заказ доставляется сторонней службой доставки)
     * 8. В поле доставки tracks попал трек посылки shipment2
     */

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS)
    @DisplayName("Товары в отправлении сортируются по id товара и посылки сортируются то id первого товара в " +
            "отправлении")
    @Test
    public void testGetMultiShipmentOrder() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithSelfDelivery(true);
        long itemId1 = order.getItem(new FeedOfferId("qwerty-1", 1L)).getId();
        long itemId2 = order.getItem(new FeedOfferId("qwerty-2", 1L)).getId();

        // Действие
        PagedOrders orders = ordersGetHelper
                .getOrders(new ClientInfo(ClientRole.SHOP_USER, BuyerProvider.UID, SHOP_ID));

        // Проверки
        order = Iterables.getFirst(orders.getItems(), null);
        assertNotNull(order);

        Delivery delivery = order.getDelivery();
        assertThat(delivery.getParcels(), hasSize(2));

        Parcel shipment2 = delivery.getParcels().get(0);
        List<ParcelItem> items = shipment2.getParcelItems();
        assertThat(items, hasSize(2));

        assertEquals(itemId1, (long) items.get(0).getItemId());
        assertEquals(5, (int) items.get(0).getCount());

        assertEquals(itemId2, (long) items.get(1).getItemId());
        assertEquals(2, (int) items.get(1).getCount());

        Parcel shipment1 = delivery.getParcels().get(1);
        assertThat(shipment1.getParcelItems(), hasSize(1));
        assertEquals(itemId2, (long) shipment1.getParcelItems().get(0).getItemId());
        assertEquals(3, (int) shipment1.getParcelItems().get(0).getCount());

        assertNull(delivery.getShipment());

        assertThat(delivery.getTracks(), hasSize(1));
        assertEquals("iddqd-2", delivery.getTracks().get(0).getTrackCode());
    }


    /**
     * GET /orders
     * <p>
     * В случае если указан параметр "trackCode" выдачу попадает только заказ у которого есть
     * посылка с треком с совпадающим трек-кодом
     * <p>
     * Подготовка
     * 1. Создать первый заказ
     * 2. Добавить к заказу две посылки
     * 3. В одну из посылок добавить трек с трек-кодом "iddqd-2"
     * 4. Создать вротой заказ
     * 5. Добавить ко второму заказу посылку с треком у которого трек-код равен "iddqd-4"
     * <p>
     * Действие
     * 1. Сделать запрос к GET /orders с параметром trackCode=iddqd-2.
     * <p>
     * Проверки
     * 1. В выдачу попал только первый заказ
     */

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS)
    @DisplayName("В случае если указан параметр 'trackCode' выдачу попадает только заказ у которого есть осылка с " +
            "треком с совпадающим трек-кодом")
    @Test
    public void testSearchMultiShipmentOrderByTrackCode() throws Exception {
        // Подготовка
        Order order1 = orderControllerTestHelper.createOrderWithSelfDelivery(true);

        Order order2 = createOrder(true);
        Parcel shipment = new Parcel();
        shipment.addTrack(TrackProvider.createTrack("iddqd-4", 123L));
        order2.getDelivery().setParcels(Collections.singletonList(shipment));
        orderServiceHelper.saveOrder(order2);

        // Действия
        OrderSearchRequest orderSearchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withTrackCodes(new String[]{"iddqd-2"})
                .build();

        PagedOrders orders = ordersGetHelper.getOrders(
                orderSearchRequest,
                new ClientInfo(ClientRole.SHOP_USER, BuyerProvider.UID, SHOP_ID)
        );

        // Проверки
        assertEquals(1, orders.getItems().size());
        assertEquals(order1.getId(), first(orders.getItems()).getId());
    }

    /**
     * GET /orders
     * <p>
     * В случае если указан параметр hasTrackCode=true в выдачу попадают только заказы
     * с треками
     * <p>
     * Подготовка
     * 1. Создать первый заказ
     * 2. Добавить к заказу две посылки
     * 3. В одну из посылок добавить трек с трек-кодом "iddqd-2"
     * 4. Создать второй заказ (без треков)
     * <p>
     * Действие
     * 1. Сделать запрос к GET /orders с параметром hasTrackCode=true.
     * <p>
     * Проверки
     * 1. В выдачу попал только первый заказ
     */

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS)
    @DisplayName("В случае если указан параметр hasTrackCode=true в выдачу попадают только заказы с треками")
    @Test
    public void testSearchMultiShipmentOrderByHasTrackCode() throws Exception {
        Order order1 = orderControllerTestHelper.createOrderWithSelfDelivery(true);

        Order order2 = createOrder(true);
        orderServiceHelper.saveOrder(order2);

        // Действия
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .build();
        request.hasTrackCode = true;

        PagedOrders orders = ordersGetHelper.getOrders(request, CLIENT_INFO);

        // Проверки
        assertEquals(1, orders.getItems().size());
        assertEquals(order1.getId(), first(orders.getItems()).getId());
    }


    /**
     * GET /orders
     * <p>
     * В выдачу попадают идентификаторы товаров в заказе
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 шт.
     * <p>
     * Действия
     * 1. Запросить заказ через ручку GET /orders
     * <p>
     * Проверки
     * 1. Товар в теле ответа содержит уникальный идентификатор
     */

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @DisplayName("В выдачу попадают идентификаторы товаров в заказе")
    @Test
    public void testOrderItemHasId() throws Exception {
        // Подготовка
        Order order = createOrder(true);
        order.setItems(Collections.singletonList(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));
        order = orderServiceHelper.saveOrder(order);


        Order returnedOrder = orderGetHelper.getOrder(order.getId(), ClientInfo.SYSTEM);

        // Проверки
        assertThat(returnedOrder.getItems(), hasSize(1));
        assertEquals(
                first(order.getItems()).getId(),
                first(returnedOrder.getItems()).getId()
        );
    }

    /**
     * GET /orders
     * <p>
     * Поле shipment не заполняется для заказов, доставляемых сторонними службами
     * <p>
     * Подготовка
     * 1. Создать заказ, доставляемый сторонней службой
     * 2. Добавить посылку к заказу
     * <p>
     * Действие
     * 1. Запросить заказ через GET /orders/{orderId}
     * <p>
     * Проверки
     * 1. У заказа отна посылки
     * 2. Поле shipment не заполнено
     */

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @DisplayName("Поле shipment не заполняется для заказов, доставляемых сторонними службами")
    @Test
    public void testDoNotShowShipmentToSelfDeliveredOrder() throws Exception {
        // Подготовка
        Order order = createOrder(true);
        order.getDelivery().addParcel(new Parcel());
        order = orderServiceHelper.saveOrder(order);

        // Действие
        order = orderGetHelper.getOrder(order.getId(), ClientInfo.SYSTEM);

        Delivery delivery = order.getDelivery();

        // Проверки
        assertThat(delivery.getParcels(), hasSize(1));
        assertNull(delivery.getShipment());
    }

    @Test
    public void shouldCountByAssessor() throws Exception {
        int assessorOrderCount = 2;
        int notAssessorCount = 3;

        for (int i = 0; i < assessorOrderCount; i++) {
            Order order = OrderProvider.getBlueOrder(o -> {
                o.setBuyer(BuyerProvider.getBuyerAssessor());
            });

            orderServiceHelper.saveOrder(order);
        }

        for (int i = 0; i < notAssessorCount; i++) {
            orderServiceHelper.saveOrder(OrderProvider.getBlueOrder());
        }

        IntHolder countByAssessor = testSerializationService.deserializeCheckouterObject(mockMvc.perform(
                get("/orders/count")
                        .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(RGB, Color.BLUE.name())
                        .param(ASSESSOR, "true")
        ).andReturn().getResponse().getContentAsString(), IntHolder.class);

        Assertions.assertEquals(assessorOrderCount, countByAssessor.getValue());

        IntHolder countByNotAssessor = testSerializationService.deserializeCheckouterObject(mockMvc.perform(
                get("/orders/count")
                        .param(ASSESSOR, "false")
                        .param(RGB, Color.BLUE.name())
                        .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
        ).andReturn().getResponse().getContentAsString(), IntHolder.class);

        Assertions.assertEquals(notAssessorCount, countByNotAssessor.getValue());
    }


    @Test
    public void checkEstimatedDeliveryFindTest() throws Exception {
        orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());

        Order estimatedOrder = OrderProvider.getPrepaidOrder();
        estimatedOrder.getDelivery().setEstimated(true);
        orderCreateService.createOrder(estimatedOrder, ClientInfo.SYSTEM);

        mockMvc.perform(get("/orders")
                        .param(CLIENT_ROLE, "SYSTEM")
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.HAS_ESTIMATED_DELIVERY, String.valueOf(Boolean.TRUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").value(hasSize(1)))
                .andExpect(jsonPath("$.orders[0].delivery.estimated").value(true));

        mockMvc.perform(get("/orders")
                        .param(CLIENT_ROLE, "SYSTEM")
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.HAS_ESTIMATED_DELIVERY, String.valueOf(Boolean.FALSE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").value(hasSize(1)))
                .andExpect(jsonPath("$.orders[0].delivery.estimated").doesNotExist());

        mockMvc.perform(get("/orders")
                        .param(CLIENT_ROLE, "SYSTEM")
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").value(hasSize(2)));
    }

    @Test
    public void checkEstimatedDeliveryCountTest() throws Exception {
        orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());

        Order estimatedOrder = OrderProvider.getPrepaidOrder();
        estimatedOrder.getDelivery().setEstimated(true);
        orderCreateService.createOrder(estimatedOrder, ClientInfo.SYSTEM);

        mockMvc.perform(get("/orders/count")
                        .param(CLIENT_ROLE, "SYSTEM")
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.HAS_ESTIMATED_DELIVERY, String.valueOf(Boolean.TRUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(1));

        mockMvc.perform(get("/orders/count")
                        .param(CLIENT_ROLE, "SYSTEM")
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.HAS_ESTIMATED_DELIVERY, String.valueOf(Boolean.FALSE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(1));

        mockMvc.perform(get("/orders/count")
                        .param(CLIENT_ROLE, "SYSTEM")
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(2));
    }

    @Test
    public void shouldLoadResaleSpecksWithfResaleOrderPartial() throws Exception {
        ResaleSpecs resaleSpecs = getResaleSpecs(null);
        Order postOrder = orderServiceHelper.createPostOrder(order -> {
            order.getItems().forEach(i -> i.setResaleSpecs(resaleSpecs));
        });

        String jsonResult = mockMvc.perform(get("/orders/{orderId}", postOrder.getId())
                        .param(CLIENT_ROLE, "USER")
                        .param(RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "0")
                        .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.RESALE_SPECS.name()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        OrderViewModel orderViewModel = testSerializationService.deserializeCheckouterObject(jsonResult,
                OrderViewModel.class);
        OrderItemViewModel itemViewModel = orderViewModel.getItems().iterator().next();
        assertEquals(true, itemViewModel.isResale());
        assertEquals("rv", itemViewModel.getResaleSpecs().getReasonValue());
        assertEquals("rt", itemViewModel.getResaleSpecs().getReasonText());
        assertEquals("cv", itemViewModel.getResaleSpecs().getConditionValue());
        assertEquals("ct", itemViewModel.getResaleSpecs().getConditionText());
    }

    @Test
    public void shouldNotLoadResaleSpecksWithoutResaleOrderPartial() throws Exception {
        ResaleSpecs resaleSpecs = getResaleSpecs(null);
        Order postOrder = orderServiceHelper.createPostOrder(order -> {
            order.getItems().forEach(i -> i.setResaleSpecs(resaleSpecs));
        });

        String jsonResult = mockMvc.perform(get("/orders/{orderId}", postOrder.getId())
                        .param(CLIENT_ROLE, "USER")
                        .param(RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                        .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "0"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        OrderViewModel orderViewModel = testSerializationService.deserializeCheckouterObject(jsonResult,
                OrderViewModel.class);
        OrderItemViewModel itemViewModel = orderViewModel.getItems().iterator().next();
        assertFalse(itemViewModel.isResale());
        assertNull(itemViewModel.getResaleSpecs());
    }

    private void checkDeliveryDatesFilter(Date dateGreaterThan, Date dateLessThan, Long... expectedOrderIdArr) {
        OrderSearchRequest req = new OrderSearchRequest();
        req.deliveryToDateGreaterThan = dateGreaterThan;
        req.deliveryToDateLessThan = dateLessThan;
        PagedOrders orders = orderService.getOrders(req, ClientInfo.SYSTEM);
        List<Long> actualOrderIds = orders.getItems().stream().map(BasicOrder::getId).collect(Collectors.toList());
        List<Long> expectedOrderIds = Arrays.asList(expectedOrderIdArr);
        Collections.sort(expectedOrderIds);
        Collections.sort(actualOrderIds);
        Assertions.assertEquals(expectedOrderIds, actualOrderIds, "wrong filter logic");
    }

    private void patchAndPersist(Order order, Date from, Date to) {
        order.getDelivery().getDeliveryDates().setFromDate(from);
        order.getDelivery().getDeliveryDates().setToDate(to);
        orderCreateService.createOrder(order, ClientInfo.SYSTEM);
    }

    private Date createDate(int dayOfMonth) {
        Calendar toDate;
        toDate = Calendar.getInstance();
        toDate.set(2014, Calendar.APRIL, dayOfMonth, 0, 0, 0);
        toDate.clear(Calendar.MILLISECOND);
        return toDate.getTime();
    }
}

