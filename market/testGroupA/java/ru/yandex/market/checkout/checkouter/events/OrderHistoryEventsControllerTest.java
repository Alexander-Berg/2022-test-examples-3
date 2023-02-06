package ru.yandex.market.checkout.checkouter.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackId;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.delivery.track.checkpoint.TrackCheckpointService;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.TrackCheckpointProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.common.util.collections.CollectionUtils.first;
import static ru.yandex.common.util.collections.CollectionUtils.last;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.TrackProvider.DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.TrackProvider.TRACK_CODE;
import static ru.yandex.market.checkout.test.providers.TrackProvider.TRACK_ID;
import static ru.yandex.market.checkout.test.providers.TrackProvider.createTrack;

/**
 * Тесты проходят для ручек
 * GET /orders/{orderId}/events
 * GET /orders/events?lastEventId=?
 * GET /orders/events?orderId=?
 */

public class OrderHistoryEventsControllerTest extends AbstractWebTestBase {

    @Autowired
    protected YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private TrackCheckpointService trackCheckpointService;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private TestSerializationService testSerializationService;

    private static void applyShowCheckpointEvents(boolean showCheckpointEvents, MockHttpServletRequestBuilder builder) {
        if (!showCheckpointEvents) {
            builder.param("eventTypes", "TRACK_CHECKPOINT_CHANGED")
                    .param("eventType", "TRACK_CHECKPOINT_CHANGED")
                    .param("ignoreEventTypes", "true");
        }
    }

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{
                        "GET /orders/{orderId}/events",
                        (EventGetter) (order, showCheckpointEvents, mockMvc, bodyUtils) -> {
                            MockHttpServletRequestBuilder builder = get("/orders/{orderId}/events", order.getId())
                                    .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM");

                            applyShowCheckpointEvents(showCheckpointEvents, builder);

                            MvcResult result = mockMvc.perform(builder)
                                    .andExpect(status().isOk())
                                    .andReturn();

                            String contentAsString = result.getResponse().getContentAsString();

                            PagedEvents events = bodyUtils.deserializeCheckouterObject(contentAsString,
                                    PagedEvents.class);
                            return first(events.getItems());
                        }
                },
                new Object[]{
                        "GET /orders/events?lastEventId=?",
                        (EventGetter) (order, showCheckpointEvents, mockMvc, bodyUtils) -> {
                            MockHttpServletRequestBuilder builder = get("/orders/events")
                                    .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")
                                    .param("lastEventId", "0")
                                    .param("rgb", "BLUE")
                                    .param("withWaitInterval", "false");

                            applyShowCheckpointEvents(showCheckpointEvents, builder);

                            MvcResult result = mockMvc.perform(builder)
                                    .andExpect(status().isOk())
                                    .andReturn();

                            String contentAsString = result.getResponse().getContentAsString();

                            OrderHistoryEvents response = bodyUtils.deserializeCheckouterObject(contentAsString,
                                    OrderHistoryEvents.class);
                            return last(new ArrayList<>(response.getContent()));
                        }
                },
                new Object[]{
                        "GET /orders/events/by-order-id?orderId=?",
                        (EventGetter) (order, showCheckpointEvents, mockMvc, bodyUtils) -> {
                            MockHttpServletRequestBuilder builder = get("/orders/events/by-order-id")
                                    .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")
                                    .param("orderId", order.getId().toString());

                            applyShowCheckpointEvents(showCheckpointEvents, builder);

                            MvcResult result = mockMvc.perform(builder)
                                    .andExpect(status().isOk())
                                    .andReturn();

                            String contentAsString = result.getResponse().getContentAsString();

                            OrderHistoryEvents response = bodyUtils.deserializeCheckouterObject(contentAsString,
                                    OrderHistoryEvents.class);
                            return first(new ArrayList<>(response.getContent()));
                        }
                }
        ).stream().map(Arguments::of);
    }

    /**
     * Параметр "showReturnStatuses" влияет на состав чекпоинтов в выдаче истории
     * <p>
     * Подготовка
     * 1. Создать заказ
     * 2. Добавить к заказу единственное отправление с единственным треком
     * 3. Добавить к треку чекпоинт без доставочного статуса
     * 4. Добавить к треку чекпоинт со статусом RETURN_PREPARING
     * 5. Добавить к треку чекпоинт со статусом RETURN_ARRIVED_DELIVERY
     * 6. Добавить к треку чекпоинт со статусом RETURN_TRANSMITTED_FULFILMENT
     * <p>
     * Действия и проверки
     * 1. Сделать запрос к GET /orders/{orderId}/events с параметром showReturnStatuses=0
     * 2. Проверить что в выдачу попал один чекпойнт
     * <p>
     * 2. Сделать запрос к GET /orders/{orderId}/events с параметром showReturnStatuses=1
     * 3. Проверить что в выдачу попало четыре чекпойнта
     * <p>
     * 4. Сделать запрос к GET /orders/{orderId}/events без параметра
     * 5. Проверить что в выдачу попал один чекпойнт
     */

    @Epic(Epics.GET_ORDER)
    @DisplayName("Параметр 'showReturnStatuses' влияет на состав чекпоинтов в выдаче истории")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void getOrderEventsCheckpoints(String method, EventGetter eventGetter) throws Exception {
        Long orderId = orderServiceHelper.prepareOrderWithReturnStatusesCheckpoint(
                DeliveryProvider.RUSPOSTPICKUP_DELIVERY_SERVICE_ID);

        // Действия и проверки
        mockMvc.perform(get("/orders/{orderId}/events", orderId)
                // фронты будут ходить так
                .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "0")
                .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].orderAfter.delivery.tracks[0].checkpoints").value(hasSize(1)));

        mockMvc.perform(get("/orders/{orderId}/events", orderId)
                .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "1")
                .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].orderAfter.delivery.tracks[0].checkpoints").value(hasSize(4)));

        mockMvc.perform(get("/orders/{orderId}/events", orderId)
                // по дефолту будут ходить так
                .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].orderAfter.delivery.tracks[0].checkpoints").value(hasSize(4)));
    }

    /**
     * Редактирование статуса отправления генерирует событие
     * <p>
     * Подготовка
     * 1. Создать заказ, доставляемый партнером маркета
     * 2. Добавить посылку со статусом "NEW" в заказ
     * 3. Установить статус посылки - "READY_TO_SHIP"
     * <p>
     * Действия
     * 1. Сделать запрос к ручке получения событий
     * <p>
     * Проверки
     * 1. В последнем событии у "заказа до" одно отправление
     * 2. У отправления "заказа до" установлен статус "NEW"
     * 3. У "заказа после" одно отправление
     * 4. У отправления "заказа после" установлен статус "READY_TO_SHIP"
     */

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Редактирование статуса отправления генерирует событиие")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testGetEditShipmentStatusEvent(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        Parcel shipment = new Parcel();
        shipment.setStatus(ParcelStatus.NEW);
        order.getDelivery().setParcels(Collections.singletonList(shipment));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        Parcel newShipment = new Parcel();

        long shipmentId = order.getDelivery().getParcels().get(0).getId();
        newShipment.setId(shipmentId);
        newShipment.setStatus(ParcelStatus.CREATED);

        Delivery newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(newShipment));

        editDelivery(order, newDelivery);

        // Действие
        OrderHistoryEvent event = eventGetter.get(order, true, mockMvc, testSerializationService);

        // Проверки
        Delivery deliveryBefore = event.getOrderBefore().getDelivery();
        assertThat(deliveryBefore.getParcels(), hasSize(1));
        assertEquals(shipmentId, (long) deliveryBefore.getParcels().get(0).getId());
        assertEquals(ParcelStatus.NEW, deliveryBefore.getParcels().get(0).getStatus());

        Delivery deliveryAfter = event.getOrderAfter().getDelivery();
        assertThat(deliveryAfter.getParcels(), hasSize(1));
        assertEquals(shipmentId, (long) deliveryAfter.getParcels().get(0).getId());
        assertEquals(ParcelStatus.CREATED, deliveryAfter.getParcels().get(0).getStatus());
    }

    /**
     * Добавление товара в отправление генерирует событие
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 шт. и единственным отправлением
     * 2. Добавить в отправление товар из заказа в количестве 5 шт.
     * <p>
     * Действие
     * 1. Сделать запрос к ручке получения событий
     * <p>
     * Проверки
     * 1. В отправлении "заказа до" нет ни одного товара
     * 2. В отправление "заказа после" есть товар из заказа в количестве 5 шт.
     */

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Добавление товара в отправление генерирует событие")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testGetAddParcelItemEvent(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = createOrder();
        order.getDelivery().setParcels(Collections.singletonList(new Parcel()));

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        Parcel newShipment = new Parcel();
        newShipment.setId(order.getDelivery().getParcels().get(0).getId());

        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();
        newShipment.addParcelItem(new ParcelItem(itemId, 5));

        Delivery newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(newShipment));

        editDelivery(order, newDelivery);

        // Действие
        OrderHistoryEvent event = eventGetter.get(order, true, mockMvc, testSerializationService);

        // Проверки
        Delivery deliveryBefore = event.getOrderBefore().getDelivery();
        assertThat(deliveryBefore.getParcels(), hasSize(1));
        assertNull(deliveryBefore.getParcels().get(0).getParcelItems());

        Delivery deliveryAfter = event.getOrderAfter().getDelivery();
        assertThat(deliveryAfter.getParcels(), hasSize(1));

        List<ParcelItem> items = deliveryAfter.getParcels().get(0).getParcelItems();
        assertThat(items, hasSize(1));
        assertEquals(itemId, (long) items.get(0).getItemId());
        assertEquals(5, (int) items.get(0).getCount());
    }

    /**
     * Редактирование количества товара в отправлении генерирует событие
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 шт. и единственным отправлением
     * 2. Добавить отправление в заказ
     * 4. Добавить в отправление товар из заказа в количестве 4 шт.
     * 5. Увеличить количество товара в отправлении до 5 шт.
     * <p>
     * Действие
     * 1. Сделать запрос к ручке получения событий
     * <p>
     * Проверки
     * 1. У "заказа до" одно отправление
     * 2. В отправлении "заказа до" есть товар в количестве 4 шт.
     * 3. У "заказа после" одно отправление
     * 4. В отправлении "заказа после" есть товар в количестве 5 шт.
     */

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Редактирование количества товара в отправлении генерирует событие")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testGetEditParcelItemEvent(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = createOrder();

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long itemId = first(order.getItems()).getId();

        Parcel newShipment = new Parcel();
        newShipment.addParcelItem(new ParcelItem(itemId, 4));
        order.getDelivery().setParcels(Collections.singletonList(newShipment));

        Delivery newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(newShipment));

        editDelivery(order, newDelivery);

        newShipment = new Parcel();
        newShipment.setId(order.getDelivery().getParcels().get(0).getId());
        newShipment.addParcelItem(new ParcelItem(itemId, 5));

        newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(newShipment));

        editDelivery(order, newDelivery);

        // Действия
        OrderHistoryEvent event = eventGetter.get(order, true, mockMvc, testSerializationService);

        // Проверки
        Delivery deliveryBefore = event.getOrderBefore().getDelivery();
        assertThat(deliveryBefore.getParcels(), hasSize(1));

        List<ParcelItem> itemsBefore = deliveryBefore.getParcels().get(0).getParcelItems();
        assertThat(itemsBefore, hasSize(1));
        assertEquals(itemId, (long) itemsBefore.get(0).getItemId());
        assertEquals(4, (int) itemsBefore.get(0).getCount());

        Delivery deliveryAfter = event.getOrderAfter().getDelivery();
        assertThat(deliveryAfter.getParcels(), hasSize(1));

        List<ParcelItem> itemsAfter = deliveryAfter.getParcels().get(0).getParcelItems();
        assertThat(itemsAfter, hasSize(1));
        assertEquals(itemId, (long) itemsAfter.get(0).getItemId());
        assertEquals(5, (int) itemsAfter.get(0).getCount());
    }

    /**
     * Удаление товара из отправления генерирует событие
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 шт. и единственным отправлением
     * 2. Добавить отправление в заказ
     * 3. Добавить в отправление товар из заказа в количестве 5 шт.
     * 4. Удалить товар из отправления
     * <p>
     * Действия
     * 1. Сделать запрос к ручке получения событий
     * <p>
     * Проверки
     * 1. У "заказа до" одно отправление
     * 2. В отправлении "заказа до" есть товар в количестве 5 шт.
     * 3. У "заказа после" одно отправление
     * 4. В отправлении "заказа после" нет ни одного товара
     */

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Удаление товара из отправления генерирует событие")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testGetDeleteParcelItemEvent(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = createOrder();

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));


        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        long itemId = first(order.getItems()).getId();

        Parcel newShipment = new Parcel();
        newShipment.addParcelItem(new ParcelItem(itemId, 5));
        order.getDelivery().setParcels(Collections.singletonList(newShipment));

        Delivery newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(newShipment));

        editDelivery(order, newDelivery);

        newShipment = new Parcel();
        newShipment.setId(order.getDelivery().getParcels().get(0).getId());
        newShipment.setParcelItems(Collections.emptyList());

        newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(newShipment));

        editDelivery(order, newDelivery);

        // Действия
        OrderHistoryEvent event = eventGetter.get(order, true, mockMvc, testSerializationService);

        // Проверки
        Delivery deliveryBefore = event.getOrderBefore().getDelivery();
        assertThat(deliveryBefore.getParcels(), hasSize(1));

        List<ParcelItem> itemsBefore = deliveryBefore.getParcels().get(0).getParcelItems();
        assertThat(itemsBefore, hasSize(1));
        assertEquals(itemId, (long) itemsBefore.get(0).getItemId());
        assertEquals(5, (int) itemsBefore.get(0).getCount());

        Delivery deliveryAfter = event.getOrderAfter().getDelivery();
        assertThat(deliveryAfter.getParcels(), hasSize(1));

        assertNull(deliveryAfter.getParcels().get(0).getParcelItems());
    }

    /**
     * Добавление трека генерирует событие
     * <p>
     * Подготовка
     * 1. Создать заказ
     * 2. Добавить в заказ пустое отправление
     * 3. Добавить в отправление трек с трек-кодом "iddqd" и службой доставки 123
     * <p>
     * Действие
     * 1. Сделать запрос к ручке получения событий
     * <p>
     * Проверки
     * 1. У "заказа до" одно отправление без треков
     * 2. У "заказа после" одно отправление
     * 3. В Отправлении "заказа после" есть единственный трек с трек-кодом "iddqd" и службой доставки 123
     */

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Добавление трека генерирует событие")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testGetAddTrackEvent(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = createOrder();
        order.getDelivery().setParcels(Collections.singletonList(new Parcel()));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        Parcel newShipment = new Parcel();
        newShipment.setId(order.getDelivery().getParcels().get(0).getId());
        newShipment.addTrack(createTrack());

        Delivery newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(newShipment));

        editDelivery(order, newDelivery);

        // Действие
        OrderHistoryEvent event = eventGetter.get(order, true, mockMvc, testSerializationService);

        // Проверки
        Delivery deliveryBefore = event.getOrderBefore().getDelivery();
        assertThat(deliveryBefore.getParcels(), hasSize(1));
        assertNull(deliveryBefore.getParcels().get(0).getTracks());

        Delivery deliveryAfter = event.getOrderAfter().getDelivery();
        assertThat(deliveryAfter.getParcels(), hasSize(1));

        List<Track> tracks = deliveryAfter.getParcels().get(0).getTracks();
        assertThat(tracks, hasSize(1));
        assertEquals(TRACK_CODE, tracks.get(0).getTrackCode());
        assertEquals(DELIVERY_SERVICE_ID, (long) tracks.get(0).getDeliveryServiceId());
    }

    /**
     * Редактирование трека генерирует событие
     * <p>
     * Подготовка
     * 1. Создать заказ
     * 2. Добавить отправление в заказ
     * 3. Добавить в отправление трек с трек-кодом "iddqd" и службой доставки 123
     * 4. Отредактировать трек "iddqd" установить трекерный id 111
     * <p>
     * Действие
     * 1. Сделать запрос к ручке получения событий
     * <p>
     * Проверки
     * 1. У "заказа до" одно отправление
     * 2. У отправления "заказа до" есть единственный трек с трек-кодом "iddqd" и службой доставки 123
     * 3. У трека "заказа до" отсутствует трекерный id
     * 4. У "заказа после" одно отправление
     * 5. У отправления "заказа после" есть единственный трек с трек-кодом "iddqd" и службой доставки 123
     * 6. У трека "заказа после" трекереный id равен 111
     */

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Редактирование трека генерирует событие")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testGetEditTrackEvent(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = createOrder();

        Parcel shipment = new Parcel();
        shipment.addTrack(createTrack());

        order.getDelivery().addParcel(shipment);

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        Parcel newShipment = new Parcel();
        newShipment.setId(order.getDelivery().getParcels().get(0).getId());

        Track track = createTrack();
        track.setTrackerId(111L);
        newShipment.addTrack(track);

        Delivery newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(newShipment));

        editDelivery(order, newDelivery);

        // Действие
        OrderHistoryEvent event = eventGetter.get(order, true, mockMvc, testSerializationService);

        // Проверки
        Delivery deliveryBefore = event.getOrderBefore().getDelivery();
        assertThat(deliveryBefore.getParcels(), hasSize(1));

        List<Track> tracksBefore = deliveryBefore.getParcels().get(0).getTracks();
        assertThat(tracksBefore, hasSize(1));
        assertEquals(TRACK_CODE, tracksBefore.get(0).getTrackCode());
        assertEquals(DELIVERY_SERVICE_ID, (long) tracksBefore.get(0).getDeliveryServiceId());
        assertNull(tracksBefore.get(0).getTrackerId());

        Delivery deliveryAfter = event.getOrderAfter().getDelivery();
        assertThat(deliveryAfter.getParcels(), hasSize(1));

        List<Track> tracksAfter = deliveryAfter.getParcels().get(0).getTracks();
        assertThat(tracksAfter, hasSize(1));
        assertEquals(TRACK_CODE, tracksAfter.get(0).getTrackCode());
        assertEquals(DELIVERY_SERVICE_ID, (long) tracksAfter.get(0).getDeliveryServiceId());
        assertEquals(111, (long) tracksAfter.get(0).getTrackerId());
    }

    /**
     * Удаление трека из отправления генерирует событие
     * <p>
     * Подготовка
     * 1. Создать заказ
     * 2. Добавить отправление в заказ
     * 3. Добавить в отправление два трека с трек-кодом "iddqd-1" и "iddqd-2" и службой доставки 123
     * 4. Удалить трек с трек-кодом "iddqd-2"
     * <p>
     * Действие
     * 1. Сделать запрос к ручке получения событий
     * <p>
     * Проверки
     * 1. У "заказа до" одно отправление
     * 2. У отправления "заказа до" есть два трека с трек-кодоми "iddqd-1" и "iddqd-2" и службой доставки 123
     * 3. У "заказа после" одно отправление
     * 4. У отправления "заказа после" есть единственный трек с трек-кодом "iddqd-1" и службой доставки 123
     */

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Удаление трека из отправления генерирует событие")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testGetDeleteTrackEvent(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = createOrder();

        Parcel shipment = new Parcel();
        shipment.addTrack(TrackProvider.createTrack("iddqd-1", DELIVERY_SERVICE_ID));
        shipment.addTrack(TrackProvider.createTrack("iddqd-2", DELIVERY_SERVICE_ID));

        order.getDelivery().setParcels(Collections.singletonList(shipment));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        Parcel newShipment = new Parcel();
        newShipment.setId(order.getDelivery().getParcels().get(0).getId());
        newShipment.addTrack(new Track("iddqd-1", DELIVERY_SERVICE_ID));

        Delivery newDelivery = new Delivery();
        newDelivery.addParcel(newShipment);

        editDelivery(order, newDelivery);

        // Действия
        OrderHistoryEvent event = eventGetter.get(order, true, mockMvc, testSerializationService);

        // Проверки
        Delivery deliveryBefore = event.getOrderBefore().getDelivery();
        assertThat(deliveryBefore.getParcels(), hasSize(1));

        List<Track> tracksBefore = deliveryBefore.getParcels().get(0).getTracks();
        assertThat(tracksBefore, hasSize(2));

        Delivery deliveryAfter = event.getOrderAfter().getDelivery();
        assertThat(deliveryAfter.getParcels(), hasSize(1));

        List<Track> tracksAfter = deliveryAfter.getParcels().get(0).getTracks();
        assertThat(tracksAfter, hasSize(1));
        assertEquals("iddqd-1", tracksAfter.get(0).getTrackCode());
        assertEquals(DELIVERY_SERVICE_ID, (long) tracksAfter.get(0).getDeliveryServiceId());
    }

    /**
     * События, связанные с добавлением чекпойнтов попадают в выдачу в случае если не задана фильтрация
     * <p>
     * Подготовка
     * 1. Создать заказ
     * 2. Добавить к заказу посылку с треком
     * 3. Добавить чекпойнт к треку
     * <p>
     * Действия
     * 1. Сделать запрос к ручке получения событий без фильтрации по типу события
     * <p>
     * Проверки
     * 1. Тип последнего события - TRACK_CHECKPOINT_CHANGED
     */

    @Epic(Epics.GET_ORDER)
    @DisplayName("События, связанные с добавлением чекпойнтов попадают в выдачу в случае если не задана фильтрация")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testShowAddCheckpointEvent(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = createOrder();

        Parcel shipment = new Parcel();

        Track track = createTrack();
        track.setTrackerId(100500L);
        shipment.addTrack(track);

        order.getDelivery().addParcel(shipment);

        order = orderServiceHelper.saveOrder(order);

        orderServiceHelper.insertCheckpoint(new TrackId(TRACK_CODE, DELIVERY_SERVICE_ID),
                TrackCheckpointProvider.createCheckpoint());

        // Действия
        OrderHistoryEvent event = eventGetter.get(order, true, mockMvc, testSerializationService);

        // Проверки
        assertEquals(HistoryEventType.TRACK_CHECKPOINT_CHANGED, event.getType());
    }

    /**
     * События, связанные с добавлением чекпойнтов не попадают в выдачу в случае если указан
     * задана фильтрация по типу события
     * <p>
     * Подготовка
     * 1. Создать заказ
     * 2. Добавить к заказу посылку с треком
     * 3. Добавить чекпойнт к треку
     * <p>
     * Действия
     * 1. Сделать запрос к ручке получения событий с фильтрацией, исключающей события добавления чекпойнтов
     * <p>
     * Проверки
     * 1. Тип последнего события - не TRACK_CHECKPOINT_CHANGED
     */

    @Epic(Epics.GET_ORDER)
    @DisplayName("События, связанные с добавлением чекпойнтов не попадают в выдачу в случае если задана фильтрация по" +
            " типу события")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testDoNotShowCheckpointEventIfFlagSpecified(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = createOrder();

        Parcel shipment = new Parcel();

        Track track = createTrack();
        track.setTrackerId(100500L);
        shipment.addTrack(track);

        order.getDelivery().addParcel(shipment);

        order = orderServiceHelper.saveOrder(order);

        orderServiceHelper.insertCheckpoint(TRACK_ID, TrackCheckpointProvider.createCheckpoint());

        // Действия
        OrderHistoryEvent event = eventGetter.get(order, false, mockMvc, testSerializationService);

        // Проверки
        assertNotEquals(HistoryEventType.TRACK_CHECKPOINT_CHANGED, event.getType());
    }

    /**
     * В случае редактировании одной посылки в доставке с несколькими посылками товары и
     * треки посылки которая не была отредактирована попадают в историю
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 шт.
     * 2. Добавить в заказ посылки A и B
     * 3. Добавить в посылку A товары и трек
     * 4. Добавить трек в посылку B
     * 5. Удалить трек из посылки B, не изменяя при этом посылку A
     * <p>
     * Действия
     * 1. Сделать запрос к ручке получения событий
     * <p>
     * Подготовка
     * 1. В "запросе после" последнего события две посылки
     * 2. В посылке A есть товары и трек
     * 3. В посылке B отсутствуют треки
     */

    @Epic(Epics.GET_ORDER)
    @DisplayName("В случае редактировании одной посылки в доставке с несколькими посылками товары и треки посылки " +
            "которая не была отредактирована попадают в историю")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testEditMultishipmentDeliveryEvents(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = createOrder();
        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));
        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        Parcel shipment1 = new Parcel();
        shipment1.addTrack(new Track("iddqd-1", DELIVERY_SERVICE_ID));
        shipment1.addParcelItem(new ParcelItem(itemId, 3));

        Parcel shipment2 = new Parcel();
        shipment2.addTrack(new Track("iddqd-2", DELIVERY_SERVICE_ID));

        Delivery newDelivery = new Delivery();
        newDelivery.setParcels(Arrays.asList(shipment1, shipment2));

        order = editDelivery(order, newDelivery);
        long shipment1Id = order.getDelivery().getParcels().get(1).getId();
        long shipment2Id = order.getDelivery().getParcels().get(0).getId();

        shipment1 = new Parcel();
        shipment1.setId(shipment1Id);

        shipment2 = new Parcel();
        shipment2.setId(shipment2Id);
        shipment2.setTracks(Collections.emptyList());

        newDelivery = new Delivery();
        newDelivery.setParcels(Arrays.asList(shipment1, shipment2));

        order = editDelivery(order, newDelivery);

        // Действие
        OrderHistoryEvent event = eventGetter.get(order, false, mockMvc, testSerializationService);

        // Проверки
        order = event.getOrderAfter();
        Delivery delivery = order.getDelivery();
        assertThat(delivery.getParcels(), hasSize(2));

        shipment1 = delivery.getParcels().get(1);

        assertThat(shipment1.getParcelItems(), hasSize(1));
        assertEquals(itemId, (long) shipment1.getParcelItems().get(0).getItemId());
        assertEquals(3, (int) shipment1.getParcelItems().get(0).getCount());

        assertThat(shipment1.getTracks(), hasSize(1));
        assertEquals("iddqd-1", shipment1.getTracks().get(0).getTrackCode());

        shipment2 = delivery.getParcels().get(0);
        assertNull(shipment2.getTracks());
    }

    /**
     * В случае редактирования статуса посылки в событии редактирования её товары и треки
     * отображаются коректно
     * <p>
     * Подготовка
     * 1. Создать заказ доставляемый партнером маркета
     * 2. Побавить к заказу посылку со статусом "NEW"
     * 3. Добавить в посылку товыры и треки
     * 4. Перевести посылку в статус "READY_TO_SHIP"
     * <p>
     * Действие
     * 1. Сделать запрос к ручке получения событий
     * <p>
     * Проверки
     * 1. Посылка "заказа до" находится в статусе "NEW"
     * 2. У посылки "заказа до" есть товары и треки
     * 3. Посылка "заказа после" находится в статусе "READY_TO_SHIP"
     * 4. У посылки "заказа после" есть теже товары что и у посылки "заказа до"
     */

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("В случае редактирования статуса посылки в событии редактирования её товары и треки отображаются " +
            "коректно")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testItemsAndTracksInEditShipmentStateEvent(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));
        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        Parcel shipment = new Parcel();
        shipment.setStatus(ParcelStatus.NEW);
        shipment.addTrack(createTrack());
        shipment.addParcelItem(new ParcelItem(itemId, 5));

        Delivery newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(shipment));

        order = editDelivery(order, newDelivery);

        shipment = new Parcel();
        shipment.setId(order.getDelivery().getParcels().get(0).getId());
        shipment.setStatus(ParcelStatus.CREATED);

        newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(shipment));

        order = editDelivery(order, newDelivery);

        // Действие
        OrderHistoryEvent event = eventGetter.get(order, false, mockMvc, testSerializationService);

        // Проверки
        Delivery deliveryBefore = event.getOrderBefore().getDelivery();
        assertThat(deliveryBefore.getParcels(), hasSize(1));

        Parcel shipmentBefore = deliveryBefore.getParcels().get(0);
        assertEquals(ParcelStatus.NEW, shipmentBefore.getStatus());
        assertThat(shipmentBefore.getParcelItems(), hasSize(1));
        assertThat(shipmentBefore.getTracks(), hasSize(1));

        Delivery deliveryAfter = event.getOrderAfter().getDelivery();
        assertThat(deliveryAfter.getParcels(), hasSize(1));

        Parcel shipmentAfter = deliveryAfter.getParcels().get(0);
        assertEquals(ParcelStatus.CREATED, shipmentAfter.getStatus());
        assertThat(shipmentAfter.getParcelItems(), hasSize(1));
        assertThat(shipmentAfter.getTracks(), hasSize(1));
    }

    /**
     * Проверка наличия поля buyerPriceBeforeDiscount при запросе /orders/events
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 шт.
     * <p>
     * Действия и проверки
     * 1. Сделать запрос к ручке получения событий /orders/{orderId}/events. Проверить наличие поля
     * buyerPriceBeforeDiscount
     * 2. Сделать запрос к ручке получения событий /orders/events. Проверить наличие поля buyerPriceBeforeDiscount
     * 3. Сделать запрос к ручке получения событий. Проверить наличие поля buyerPriceBeforeDiscount на
     * десериализованном объекте OrderHistoryEvent
     * <p>
     */

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Проверка наличия поля buyerPriceBeforeDiscount при запросе /orders/events")
    @ParameterizedTest(name = "method = {0}")
    @MethodSource("parameterizedTestData")
    public void testBuyerPriceBeforeDiscountNOtNull(String method, EventGetter eventGetter) throws Exception {
        // Подготовка
        Order order = createOrder();
        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));
        order = orderServiceHelper.saveOrder(order);
        Long orderId = order.getId();

        // Действия и проверки
        mockMvc.perform(get("/orders/{orderId}/events", orderId)
                .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "0")
                .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].orderAfter.items[0].buyerPriceBeforeDiscount").value(notNullValue()));

        mockMvc.perform(get("/orders/events/by-order-id", orderId)
                .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "0")
                .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")
                .param(CheckouterClientParams.ORDER_ID, String.valueOf(orderId))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].orderAfter.items[0].buyerPriceBeforeDiscount").value(notNullValue()));

        OrderHistoryEvent event = eventGetter.get(order, true, mockMvc, testSerializationService);
        for (OrderItem item : event.getOrderAfter().getItems()) {
            assertNotNull(item.getPrices().getBuyerPriceBeforeDiscount());
        }
    }

    @Epic(Epics.DELIVERY)
    @DisplayName("Проверка наличия поля delivery.verificationPart при запросах при передаче нужного partial")
    @Test
    public void testDeliveryVerificationPartReturnWithPartial() throws Exception {
        // Подготовка
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        Long orderId = order.getId();
        final String verificationCode = "12345";
        final String barcodeData = orderId + "-" + verificationCode;
        var newDelivery = new Delivery();
        newDelivery.setVerificationCode(verificationCode);
        order = editDelivery(order, newDelivery);

        // Действия и проверки
        var orderEventsJson = expectDeliveryVerificationPart(
                mockMvc.perform(get("/orders/{orderId}/events", orderId)
                        .param(CheckouterClientParams.OPTIONAL_PARTS,
                                OptionalOrderPart.DELIVERY_VERIFICATION_CODE.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")),
                "$.events[0]", verificationCode, barcodeData)
                .andReturn().getResponse().getContentAsString();
        var orderEvents = testSerializationService.deserializeCheckouterObject(orderEventsJson,
                PagedEvents.class);
        final Long eventId = first(orderEvents.getItems()).getId();

        expectDeliveryVerificationPart(
                mockMvc.perform(get("/orders/events/{eventId}", eventId)
                        .param(CheckouterClientParams.OPTIONAL_PARTS,
                                OptionalOrderPart.DELIVERY_VERIFICATION_CODE.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")),
                "$", verificationCode, barcodeData);

        expectDeliveryVerificationPart(
                mockMvc.perform(get("/orders/events/by-order-id")
                        .param(CheckouterClientParams.ORDER_ID, orderId.toString())
                        .param(CheckouterClientParams.OPTIONAL_PARTS,
                                OptionalOrderPart.DELIVERY_VERIFICATION_CODE.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")),
                "$.events[0]", verificationCode, barcodeData);
    }

    private ResultActions expectDeliveryVerificationPart(ResultActions result, String eventRoot,
                                                         String verificationCode, String barcodeData) throws Exception {
        return result.andExpect(status().isOk())
                .andExpect(jsonPath(eventRoot + ".orderAfter.delivery").exists())
                .andExpect(jsonPath(eventRoot + ".orderAfter.delivery.verificationPart").exists())
                .andExpect(jsonPath(eventRoot + ".orderAfter.delivery.verificationPart.verificationCode")
                        .value(verificationCode))
                .andExpect(jsonPath(eventRoot + ".orderAfter.delivery.verificationPart.barcodeData")
                        .value(barcodeData));
    }

    @Epic(Epics.DELIVERY)
    @DisplayName("Проверка отсутствия поля delivery.verificationPart при запросах без передачи нужного partial")
    @Test
    public void testDeliveryVerificationPartSkippedWithoutPartial() throws Exception {
        // Подготовка
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        Long orderId = order.getId();
        final String verificationCode = "12345";
        var newDelivery = new Delivery();
        newDelivery.setVerificationCode(verificationCode);
        order = editDelivery(order, newDelivery);

        // Действия и проверки
        var orderEventsJson = expectNoDeliveryVerificationPart(
                mockMvc.perform(get("/orders/{orderId}/events", orderId)
                        .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")),
                "$.events[0]")
                .andReturn().getResponse().getContentAsString();
        var orderEvents = testSerializationService.deserializeCheckouterObject(orderEventsJson,
                PagedEvents.class);
        final Long eventId = first(orderEvents.getItems()).getId();

        expectNoDeliveryVerificationPart(
                mockMvc.perform(get("/orders/events/{eventId}", eventId)
                        .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")),
                "$");

        expectNoDeliveryVerificationPart(
                mockMvc.perform(get("/orders/events/by-order-id")
                        .param(CheckouterClientParams.ORDER_ID, orderId.toString())
                        .param(CheckouterClientParams.CLIENT_ROLE, "SYSTEM")),
                "$.events[0]");
    }

    private ResultActions expectNoDeliveryVerificationPart(ResultActions result, String eventRoot) throws Exception {
        return result.andExpect(status().isOk())
                .andExpect(jsonPath(eventRoot + ".orderAfter.delivery").exists())
                .andExpect(jsonPath(eventRoot + ".orderAfter.delivery.verificationPart").doesNotExist());
    }

    private Order editDelivery(Order order, Delivery newDelivery) {
        return orderUpdateService.updateOrderDelivery(order.getId(), newDelivery, ClientInfo.SYSTEM);
    }

    private Order createOrder() {
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(true);
        order.setRgb(Color.BLUE);
        return order;
    }


    @FunctionalInterface
    protected interface EventGetter {

        OrderHistoryEvent get(Order order, boolean showCheckpointEvents, MockMvc mockMvc,
                              TestSerializationService bodyUtils) throws Exception;
    }
}
