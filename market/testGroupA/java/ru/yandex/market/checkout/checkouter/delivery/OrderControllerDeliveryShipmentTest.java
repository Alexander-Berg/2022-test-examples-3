package ru.yandex.market.checkout.checkouter.delivery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Iterables;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackId;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.common.rest.DuplicateKeyException;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderControllerTestHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.ParcelProvider;
import ru.yandex.market.checkout.test.providers.TrackCheckpointProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.common.report.model.FeedOfferId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.OrderControllerTestHelper.createOrder;

public class OrderControllerDeliveryShipmentTest extends AbstractWebTestBase {

    public static final long DELIVERY_SERVICE_ID = 123L;
    public static final long DELIVERY_SERVICE_ID_WITH_SORTING_CENTER = 200501;
    public static final String TRACK_CODE = "iddqd";
    private static final long SHOP_ID = 4545L;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrderControllerTestHelper orderControllerTestHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Сохранение нескольких новых отправлений
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5-ти штук
     * <p>
     * Действие
     * 1. Сделать запрос к POST /delivery с телом вида "multi-delivery-request-body.json"
     * <p>
     * Проверки
     * 1. В ответе на запрос содержится два отправления
     * 2. У первого отправления есть единственный трек с трек-кодом "iddqd1" и службой доставки 123
     * 3. В первое отправление добавлен товар, указанный в заказе в количестве 2 шт.
     * 4. У первого отправления есть единственный трек с трек-кодом "iddqd2" и службой доставки 321
     * 5. В первое отправление добавлен товар, указанный в заказе в количестве 3 шт.
     * 6. В поле delivery.shipment нет отправления (т.к. заказ доставляется сторонней службой доставки)
     * 7. В поле delivery.tracks содержится единственный трек с трек-кодом "iddqd1"
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Сохранение нескольких новых отправлений")
    @Test
    public void testSaveMultipleShipments() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(true);

        order.setItems(Collections.singleton(OrderItemProvider.buildOrderItem("qwerty", 5)));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        // Действие
        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithParcels(
                        ParcelProvider.createParcelWithTracksAndItems(
                                DELIVERY_SERVICE_ID,
                                "iddqd1",
                                new ParcelItem(itemId, 2)
                        ),
                        ParcelProvider.createParcelWithTracksAndItems(
                                321,
                                "iddqd2",
                                new ParcelItem(itemId, 3)
                        )
                )
        );

        // Проверки
        Delivery delivery = order.getDelivery();
        assertNotNull(delivery);

        List<Parcel> parcels = new ArrayList<>(delivery.getParcels());
        assertThat(delivery.getParcels(), hasSize(2));

        parcels.sort(Comparator.comparingLong(Parcel::getId));

        Parcel parcel = parcels.get(0);

        assertNotNull(parcel.getId());
        assertThat(parcel.getTracks(), hasSize(1));
        assertEquals(DELIVERY_SERVICE_ID, (long) parcel.getTracks().get(0).getDeliveryServiceId());
        assertEquals("iddqd1", parcel.getTracks().get(0).getTrackCode());
        checkParcelShipmentIdIsNull(parcel);

        List<ParcelItem> parcelItems = parcel.getParcelItems();
        assertThat(parcelItems, hasSize(1));
        assertEquals(itemId, (long) parcelItems.get(0).getItemId());
        assertEquals(2, (int) parcelItems.get(0).getCount());

        parcel = parcels.get(1);
        assertNotNull(parcel.getId());
        assertThat(parcel.getTracks(), hasSize(1));
        assertEquals(321, (long) parcel.getTracks().get(0).getDeliveryServiceId());
        assertEquals("iddqd2", parcel.getTracks().get(0).getTrackCode());
        checkParcelShipmentIdIsNull(parcel);

        parcelItems = parcel.getParcelItems();
        assertThat(parcelItems, hasSize(1));
        assertEquals(itemId, (long) parcelItems.get(0).getItemId());
        assertEquals(3, (int) parcelItems.get(0).getCount());

        assertNull(delivery.getShipment());

        assertThat(delivery.getTracks(), hasSize(1));
        // фэйлится при запуске всех тестов; НЕ фэйлится при запуске
        // gradle integrationtest --tests [..].OrderControllerTest
        // assertEquals(123, (long) delivery.getTracks().get(0).getDeliveryServiceId());
        // assertEquals("iddqd1", delivery.getTracks().get(0).getTrackCode());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Редактирование количества товара в отправлении
     * <p>
     * Подготовка
     * 1. Добавить заказ с единственным товаром в количестве пяти шт.
     * 2. К заказу добавить отправление с товаром из заказа в количестве 4-х шт.
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "edit-item-count-request-body.json"
     * <p>
     * Проверки
     * 1. В теле ответа содержится единственное отправление с тем же товаром из заказа в количестве 5 шт.
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Редактирование количества товара в отправлении")
    @Test
    public void testEditShipmentItem() throws Exception {
        // Подгтовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getPostalDelivery());
        order.setFake(true);
        order.setGlobal(false);

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));

        Parcel parcel = new Parcel();
        parcel.addParcelItem(new ParcelItem(1L, 4));
        order.getDelivery().setParcels(Collections.singletonList(parcel));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        // Действие
        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndItems(shipmentId, itemId, 5)
                )
        );

        // Проверки
        parcel = assertAndGetSingleShipment(order);
        assertEquals(shipmentId, (long) parcel.getId());

        List<ParcelItem> items = parcel.getParcelItems();
        assertThat(items, hasSize(1));
        assertEquals(itemId, (long) items.get(0).getItemId());
        assertEquals(5, (int) items.get(0).getCount());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Добавление товара в уже существующее отправление
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 шт.
     * 2. Создать для заказа пустое отправление
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "edit-item-count-request-body.json"
     * <p>
     * Проверки
     * 1. Тело ответа содержит единственное отправление
     * 2. Отправление содержит товары из заказа в количестве 5 шт.
     */

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Добавление товара в уже существующее отправление")
    @Test
    public void testAddItemToExistingShipment() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getPostalDelivery());
        order.setFake(true);
        order.setGlobal(false);

        order.setItems(Collections.singleton(OrderItemProvider.buildOrderItem("qwerty", 5)));

        order.getDelivery().setParcels(Collections.singletonList(new Parcel()));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        // Действие
        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndItems(shipmentId, itemId, 5)
                )
        );

        // Проверки
        Parcel parcel = assertAndGetSingleShipment(order);
        assertEquals(shipmentId, (long) parcel.getId());

        List<ParcelItem> items = parcel.getParcelItems();
        assertThat(items, hasSize(1));
        assertEquals(itemId, (long) items.get(0).getItemId());
        assertEquals(5, (int) items.get(0).getCount());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Удаление товара из отправления
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 шт.
     * 2. Добавить к заказу отправление, содержащее товар из заказа в количестве 4 шт.
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "del-item-request-body.json"
     * <p>
     * Проверка
     * 1. Тело ответа содержит единственное отправление без товара
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Удаление товара из отправления")
    @Test
    public void testDeleteItemFromShipment() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getPostalDelivery());
        order.setFake(true);
        order.setGlobal(false);

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));

        Parcel parcel = new Parcel();
        parcel.addParcelItem(new ParcelItem(1L, 4));
        order.getDelivery().setParcels(Collections.singletonList(parcel));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long parcelId = order.getDelivery().getParcels().get(0).getId();

        // Действие
        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndItems(parcelId, Collections.emptyList())
                )
        );

        // Проверки
        parcel = assertAndGetSingleShipment(order);
        assertEquals(parcelId, (long) parcel.getId());

        assertNull(parcel.getParcelItems());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Нельзя добавлять в отправление больше единиц товара чем есть в заказе
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 4 шт.
     * 2. Добавить отправление в заказ
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "edit-item-count-request-body.json"
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя добавлять в отправление больше единиц товара чем есть в заказе")
    @Test
    public void testErrorOnAddItemWithExceedingCount() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getPostalDelivery());
        order.setFake(true);
        order.setGlobal(false);

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 4)
        ));

        order.getDelivery().setParcels(Collections.singletonList(new Parcel()));

        order = orderServiceHelper.saveOrder(order);
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();
        long shipmentId = order.getDelivery().getParcels().get(0).getId();

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndItems(shipmentId, itemId, 5)
                )
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Нельзя отредактировать количества товаров в отправлениях так чтобы суммарное
     * количество товара превысило количество указанное в заказе
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 шт.
     * 2. Добавить в заказ отправление A
     * 3. Поместить в отправление A 2 единицы товара
     * 4. Добавить в заказа отправление B
     * 5. Поместить в отправление B 3 единицы товара
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "edit-several-item-count-requst-body.json"
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя отредактировать количества товаров в отправлениях так чтобы суммарное количество товара " +
            "превысило количество указанное в заказе")
    @Test
    public void testFailOnEditIfMaxItemCountExceeded() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(false);

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        Parcel parcel1 = new Parcel();
        parcel1.addParcelItem(new ParcelItem(itemId, 2));

        Parcel parcel2 = new Parcel();
        parcel2.addParcelItem(new ParcelItem(itemId, 3));

        Delivery delivery = new Delivery();
        delivery.setParcels(Arrays.asList(parcel1, parcel2));

        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);
        long shipment1Id = order.getDelivery().getParcels().get(0).getId();
        long shipment2Id = order.getDelivery().getParcels().get(1).getId();

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndItems(shipment1Id, itemId, 3),
                        ParcelProvider.createParcelWithIdAndItems(shipment2Id, itemId, 4)
                )
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Редактирование количества товара в нескольких отправлениях одновременно
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 7 шт.
     * 2. Добавить в заказ отправление A
     * 3. Поместить в отправление A 1 единицу товара
     * 4. Добавить в заказа отправление B
     * 5. Поместить в отправление B 6 единицы товара
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "edit-several-item-count-requst-body.json"
     * <p>
     * Проверки
     * 1. В теле ответа содержатся два отправления
     * 2. В отправлении A 3 единицы товара
     * 3. В отправлении B 4 единицы товара
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Редактирование количества товара в нескольких отправлениях одновременно")
    @Test
    public void testRebalanceItemCount() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(false);

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 7)
        ));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        Parcel parcel1 = new Parcel();
        parcel1.addParcelItem(new ParcelItem(itemId, 1));

        Parcel parcel2 = new Parcel();
        parcel2.addParcelItem(new ParcelItem(itemId, 6));

        Delivery delivery = new Delivery();
        delivery.setParcels(Arrays.asList(parcel1, parcel2));

        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);
        long shipment1Id = order.getDelivery().getParcels().get(0).getId();
        long shipment2Id = order.getDelivery().getParcels().get(1).getId();

        // Действие
        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndItems(shipment1Id, itemId, 3),
                        ParcelProvider.createParcelWithIdAndItems(shipment2Id, itemId, 4)
                )
        );

        // Проверки
        delivery = order.getDelivery();
        assertThat(delivery.getParcels(), hasSize(2));

        parcel1 = delivery.getParcels().get(0);
        assertEquals(shipment1Id, (long) parcel1.getId());
        assertThat(parcel1.getParcelItems(), hasSize(1));
        assertEquals(itemId, (long) parcel1.getParcelItems().get(0).getItemId());
        assertEquals(3, (int) parcel1.getParcelItems().get(0).getCount());

        parcel2 = delivery.getParcels().get(1);
        assertEquals(shipment2Id, (long) parcel2.getId());
        assertThat(parcel2.getParcelItems(), hasSize(1));
        assertEquals(itemId, (long) parcel2.getParcelItems().get(0).getItemId());
        assertEquals(4, (int) parcel2.getParcelItems().get(0).getCount());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Удаление отправления
     * <p>
     * Подготовка
     * 1. Создать заказ
     * 2. Добавить для заказа пустое отправление
     * <p>
     * Действия
     * 1. Выполнить запрос к POST /delivery с телом вида "del-shipment-request-body.json"
     * <p>
     * Проверки
     * 1. ответ не содержит отправлений
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Удаление отправления")
    @Test
    public void testDeleteShipment() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(false);
        order.getDelivery().setParcels(Collections.singletonList(new Parcel()));
        orderServiceHelper.saveOrder(order);
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);

        order = editOrderDelivery(order, DeliveryUpdateProvider.createDeliveryUpdate(d -> {
            d.setParcels(Collections.emptyList());
        }));

        assertThat(order.getDelivery().getParcels(), empty());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Редактирование свойств отдельной отгрузки
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 шт.
     * 2. Добавить в заказ отправление
     * 3. Добавить в отправление товар из заказа в кол-ве 4 шт.
     * 4. Добавить в отправление единственный трек
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "edit-shipment-status-request-body.json",
     * содержащий новый статус посылки. Роль - "SYSTEM"
     * <p>
     * Проверка
     * 1. В ответе содержатся одно отправление
     * 2. Status отправления "ERROR"
     * 3. Отправление B сохранило товары и трек
     * 4. Весогабариты отправления не изменились
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Редактирование свойств отдельной отгрузки")
    @Test
    public void testEditShipmentProperties() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getYandexMarketDelivery(false));
        order.setFake(true);
        order.setGlobal(true);

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));

        Parcel parcel = new Parcel();
        parcel.addTrack(TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID));
        parcel.addParcelItem(new ParcelItem(1L, 4));
        parcel.setWidth(3L);
        parcel.setHeight(5L);
        parcel.setDepth(8L);
        parcel.setWeight(13L);

        order.getDelivery().setParcels(Collections.singletonList(parcel));

        // Действие
        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();

        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndStatus(
                                shipmentId, ParcelStatus.ERROR
                        )
                )
        );

        // Проверка
        Parcel editedParcel = assertAndGetSingleShipment(order);

        assertEquals(ParcelStatus.ERROR, editedParcel.getStatus());
        assertEquals(shipmentId, (long) editedParcel.getId());

        assertThat(editedParcel.getTracks(), hasSize(1));
        assertThat(editedParcel.getParcelItems(), hasSize(1));

        assertEquals(3, (long) editedParcel.getWidth());
        assertEquals(5, (long) editedParcel.getHeight());
        assertEquals(8, (long) editedParcel.getDepth());
        assertEquals(13, (long) editedParcel.getWeight());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Если заказ доставляет партнер Маркета сохранение нескольких отправлений невозможно
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 шт. Заказ доставляется партнером Маркета.
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "multi-delivery-request-body.json",
     * содержащим несколько посылок
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Если заказ доставляет партнер Маркета сохранение нескольких отправлений невозможно")
    @Test
    public void testMultiShipmentIsNotAllowedForMarketPartner() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getPostalDelivery());
        order.setFake(true);
        order.setGlobal(false);

        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", 5)
        ));

        order = orderServiceHelper.saveOrder(order);
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithTracksAndItems(
                                DELIVERY_SERVICE_ID,
                                "iddqd1",
                                new ParcelItem(itemId, 2)
                        ),
                        ParcelProvider.createParcelWithTracksAndItems(
                                321L,
                                "iddqd2",
                                new ParcelItem(itemId, 3)
                        )
                )
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Если заказ доставляет партнер Маркета нельзя обнулять отправления
     * <p>
     * Подготовка
     * 1. Создать заказ доставляемый партнером Маркета
     * 2. Добавить отправление к заказу
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "del-shipment-request-body.json",
     * содержащим пустой массив посылок
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Если заказ доставляет партнер Маркета нельзя обнулять отправления")
    @Test
    public void testDeletingShipmentIsNotAllowedForMarketPartner() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getPostalDelivery());
        order.setFake(true);
        order.setGlobal(false);

        order.getDelivery().setParcels(Collections.singletonList(new Parcel()));

        order = orderServiceHelper.saveOrder(order);

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdate(d -> {
                    d.setParcels(Collections.emptyList());
                })
        ).andExpect(status().isBadRequest());
    }


    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Пользователю с ролью SHOP_USER запрещено редактирования статуса посылки
     * <p>
     * Подготовка
     * 1. Создать заказ, доставляемый партнером Маркета
     * 2. Перевести посылку в статус READY_TO_SHIP
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "edit-shipment-status-request-body.json",
     * содержаший статус отпрпавления ERROR
     * Роль - "SHOP_USER"
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Пользователю с ролью SHOP_USER запрещено редактирования статуса посылки")
    @Test
    public void testShopUserIsNotAllowedToEditShipmentState() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithPartnerDelivery(SHOP_ID);

        sendEditDeliveryRequestAsShopUser(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndStatus(
                                order.getDelivery().getParcels().get(0).getId(),
                                ParcelStatus.ERROR
                        )
                ),
                SHOP_ID
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Для заказа, доставляемого партнером Маркета, магазину запрещено добавление товара в посылку
     * <p>
     * Подготовка
     * 1. Создать заказ с единственным товаром в количестве 5 штук, доставляемый партнером Маркета
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "add-item-request-body.json",
     * содержищим новое наполнение посылки. Роль пользователя "SHOP_USER".
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Для заказа, доставляемого партнером Маркета, магазину запрещено добавление товара в посылку")
    @Test
    public void testShopUserIsNotAllowedToAddItemToPartnerShipment() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithPartnerDelivery(SHOP_ID);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        sendEditDeliveryRequestAsShopUser(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndItems(shipmentId, itemId, 5)
                ),
                SHOP_ID
        ).andExpect(status().isBadRequest());
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Для заказа, доставлямого партнером Маркета, магазину запрещно удалять товары из поыслки.")
    @Test
    @Deprecated
    @Disabled
    public void testShopUserIsNotAllowedToRemoveItemFromPartnerShipment() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithPartnerDelivery(SHOP_ID);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        sendEditDeliveryRequestAsSystem(order,
                DeliveryUpdateProvider.createDeliveryUpdateWithParcels(
                        ParcelProvider.createParcelWithIdAndItems(shipmentId, itemId, 5)
                ));

        //

        sendEditDeliveryRequestAsShopUser(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndItems(shipmentId, Collections.emptyList())
                ),
                SHOP_ID
        ).andExpect(status().isBadRequest());
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Для заказа, доставлямого партнером Маркета, магазину запрещно изенять товары в посылке.")
    @Test
    public void testShopUserIsNotAllowedToEditItemInPartnerShipment() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithPartnerDelivery(SHOP_ID);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        sendEditDeliveryRequestAsSystem(order,
                DeliveryUpdateProvider.createDeliveryUpdateWithParcels(
                        ParcelProvider.createParcelWithIdAndItems(
                                shipmentId,
                                Collections.singletonList(new ParcelItem(itemId, 5))
                        )
                ));

        //

        sendEditDeliveryRequestAsShopUser(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndItems(
                                shipmentId,
                                Collections.singletonList(new ParcelItem(itemId, 2))
                        )
                ),
                SHOP_ID
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Для заказа доставляемого партнером Маркета магазину разрешена установка весогабаритов
     * При этом некоторые свойства посылки сбрасываются.
     * <p>
     * Подготовка
     * 1. Создать заказ, доставляемый партнером Маркета
     * 2. Добавить к посылке трек с трек-кодом "qwerty" и службой доставки 123
     * 3. Установить для посылки значение свойства "labelUrl"
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "edit-shipment-properties-request-body.json",
     * содержищим весогабариты посылки. Роль пользователя - "USER_SHOP"
     * <p>
     * Проверки
     * 1. Высота посылки - 10
     * 2. Ширина посылки - 20
     * 3. Глубина посылки - 30
     * 4. Вес - 40
     * 5. У посылки отсутствуют треки
     * 6. У посылки не задан labelUrl
     * 7. Статус посылки - NEW
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Для заказа доставляемого партнером Маркета магазину разрешена установка весогабаритов, при этом " +
            "некоторые свойства посылки сбрасываются.")
    @Test
    @Deprecated
    @Disabled
    public void testSetShipmentWidthHeightForPartnerShipment() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithPartnerDelivery(SHOP_ID);

        Parcel parcel = order.getDelivery().getParcels().get(0);
        parcel.setStatus(ParcelStatus.READY_TO_SHIP);

        // Действие
        order = editOrderDeliveryAsShopUser(order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndProperties(
                                order.getDelivery().getParcels().get(0).getId(),
                                10, 20, 30, 40
                        )
                ), SHOP_ID);

        parcel = assertAndGetSingleShipment(order);

        assertEquals(10, (long) parcel.getWidth());
        assertEquals(20, (long) parcel.getHeight());
        assertEquals(30, (long) parcel.getDepth());
        assertEquals(40, (long) parcel.getWeight());

        assertNull(parcel.getTracks());
        assertNull(parcel.getLabelURL());
        assertEquals(ParcelStatus.NEW, parcel.getStatus());
    }


    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Для заказа доставляемого партнером Маркета магазину разрешена установка весогабаритов
     * При этом некоторые свойства посылки сбрасываются.
     * <p>
     * Подготовка
     * 1. Создать заказ, доставляемый партнером Маркета
     * 2. Добавить к посылке трек с трек-кодом "qwerty" и службой доставки 123
     * 3. Установить для посылки значение свойства "labelUrl"
     * 4. Добавить к посылке трек от сортировочного центра
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "edit-shipment-properties-request-body.json",
     * содержищим весогабариты посылки. Роль пользователя - "USER_SHOP"
     * <p>
     * Проверки
     * 1. Высота посылки - 10
     * 2. Ширина посылки - 20
     * 3. Глубина посылки - 30
     * 4. Вес - 40
     * 5. У посылки присутствует ТОЛЬКО трек от СЦ
     * 6. У посылки не задан labelUrl
     * 7. Статус посылки - NEW
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Для заказа доставляемого партнером Маркета магазину разрешена установка весогабаритов, при этом " +
            "некоторые свойства посылки сбрасываются.")
    @Test
    @Deprecated
    @Disabled
    public void testSetShipmentWidthHeightForPartnerShipmentWithSortingCenterTrack() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithPartnerDeliverySortingCenter(SHOP_ID);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        // Действие
        order = editOrderDeliveryAsShopUser(order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndProperties(
                                order.getDelivery().getParcels().get(0).getId(),
                                10, 20, 30, 40
                        )
                ), SHOP_ID);

        Parcel parcel = assertAndGetSingleShipment(order);

        assertEquals(10, (long) parcel.getWidth());
        assertEquals(20, (long) parcel.getHeight());
        assertEquals(30, (long) parcel.getDepth());
        assertEquals(40, (long) parcel.getWeight());

        assertThat(parcel.getTracks(), hasSize(1));
        assertNull(parcel.getLabelURL());
        assertEquals(ParcelStatus.NEW, parcel.getStatus());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Для заказа доставляемого сторонними службами нельзя установить весогабариты
     * <p>
     * Подготовка
     * 1. Создать заказ, доставляемый сторонними службами
     * 2. Добавить отправление к заказу
     * <p>
     * Действия
     * 1. Выполнить запрос к POST /delivery с телом вида "edit-shipment-properties-request-body.json",
     * содержищим весогабариты посылки. Роль пользователя - "SYSTEM"
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Для заказа доставляемого сторонними службами нельзя установить весогабариты")
    @Test
    public void testImpossibleToSetWidthForShopOrderShipment() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(false);

        order.getDelivery().setParcels(Collections.singletonList(new Parcel()));

        order = orderServiceHelper.saveOrder(order);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndProperties(
                                shipmentId,
                                10, 20, 30, 40
                        )
                )
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Магазину нельзя добавлять посылки для неглобального заказа
     * <p>
     * Подготовка
     * 1. Создать неглобальный заказ
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом: {"shipments": [{}]}.
     * Роль пользователя - "SHOP_USER"
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Магазину нельзя добавлять посылки для неглобального заказа")
    @Test
    public void testShopIsNotAllowedToAddShipmentForNotGlobalOrder() throws Exception {
        // Подготовка
        Order order = createOrder(false);

        order = orderServiceHelper.saveOrder(order);

        sendEditDeliveryRequestAsShopUser(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithTracks(DELIVERY_SERVICE_ID, "asdasd")
                ),
                SHOP_ID
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Магазин может добавлять посылки к глобальному заказу
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом: {"shipments": [{}]}.
     * Роль пользователя - "SHOP"
     * <p>
     * Проверки
     * 1. Доставка содержит одну посылку
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Магазин может добавлять посылки к глобальному заказу")
    @Test
    public void testShopCanAddShipmentsToGlobalOrder() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithSelfDelivery(true);

        order = editOrderDeliveryAsShop(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithTracks(DELIVERY_SERVICE_ID, "asdasd")
                )
        );

        assertAndGetSingleShipment(order);
    }

    @DisplayName("Нельзя добавить ещё один заказ с такими же треками")
    @Test
    public void cannotCreateOrderWithTheSameTracks() {
        // Добавляем первый заказ
        orderControllerTestHelper.createOrderWithSelfDelivery(true);

        try {
            // Пытаемся создать ещё один заказ с такими же треками
            orderControllerTestHelper.createOrderWithSelfDelivery(true);
            fail("InvalidRequestException on duplicated tracks wasn't thrown");
        } catch (DuplicateKeyException ex) {
            assertNotNull(ex.getMessage());
            assertTrue(ex.getMessage().contains("with the same trackCode and deliveryServiceId"), ex.getMessage());
        }
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Нельзя установить отрицательную ширину посылки
     * <p>
     * Подготовка
     * 1. Создать заказ, доставляемый партнером Маркета
     * 2. Добавить посылку в заказ
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "shipment-with-negative-width.json",
     * содержащем посылку с отрицательной шириной
     * <p>
     * Проверка
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Нельзя установить отрицательную ширину посылки")
    @Test
    public void testErrorOnSavingOrderShipmentWithNegativeId() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithPartnerDelivery(SHOP_ID);

        // Действие
        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithProperties(
                                -10L, null, null, null
                        )
                )
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Нельзя сохранить две посылки с одним и тем же идентифиактором
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ
     * 2. Добавить посылку к заказу
     * <p>
     * Действе
     * 1. Выполнить запрос к POST /delivery с телом вида "save-shipments-with-same-id.json",
     * содержащим две посылки с идентификатором существующей посылки в заказе
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя сохранить две посылки с одним и тем же идентифиактором")
    @Test
    public void testErrorOnSaveTwoShipmentsWithSameIds() throws Exception {
        // Подготовка
        Order order = createOrder(true);
        order.getDelivery().setParcels(Collections.singletonList(new Parcel()));
        order = orderServiceHelper.saveOrder(order);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithId(shipmentId),
                        ParcelProvider.createParcelWithId(shipmentId)
                )
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Нельзя сохранить содержимое посылки с дублирующимися товарами
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ с единственным товаром в кол-ве 5 шт.
     * <p>
     * Действе
     * 1. Выполнить запрос к POST /delivery с телом вида "add-same-shipment-item-twice.json",
     * содержащим посылку в который товар из заказа входит дважды
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя сохранить содержимое посылки с дублирующимися товарами")
    @Test
    public void testErrorOnSaveTwoSameShipmentItems() throws Exception {
        // Подготовка
        Order order = createOrder(true);
        order = orderServiceHelper.saveOrder(order);
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithItems(
                                new ParcelItem(itemId, 1),
                                new ParcelItem(itemId, 2)
                        )
                )
        ).andExpect(status().isBadRequest());
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Должна быть возможность повторно добавлять итем в посылку")
    @Test
    public void shouldAllowToReaddItemToShipment() throws Exception {
        Order order = createOrder(true);

        order.setItems(Arrays.asList(
                OrderItemProvider.buildOrderItem("qwerty-1", 5),
                OrderItemProvider.buildOrderItem("qwerty-2", 5)
        ));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        Long orderItemId1 = order.getItem(new FeedOfferId("qwerty-1", OrderItemProvider.FEED_ID)).getId();
        Long orderItemId2 = order.getItem(new FeedOfferId("qwerty-2", OrderItemProvider.FEED_ID)).getId();

        Long parcelId = Iterables.getOnlyElement(editOrderDelivery(order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithItems(
                                new ParcelItem(orderItemId1, 1),
                                new ParcelItem(orderItemId2, 1)
                        )
                )).getDelivery().getParcels()).getId();

        editOrderDelivery(order, DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                ParcelProvider.createParcelWithIdAndItems(
                        parcelId,
                        Collections.singletonList(new ParcelItem(orderItemId2, 1))
                )
        ));

        editOrderDelivery(order, DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                ParcelProvider.createParcelWithIdAndItems(
                        parcelId,
                        Arrays.asList(
                                new ParcelItem(orderItemId1, 1),
                                new ParcelItem(orderItemId2, 1)
                        )
                )
        ));
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("")
    @Test
    public void shouldNotCreateDuplicateEventsIfSameTrackPushedTwice() throws Exception {
        Order order = createOrder(true);
        order.getDelivery()
                .setParcels(
                        Collections.singletonList(
                                ParcelProvider.createParcel(new ParcelItem(Iterables.getOnlyElement(order.getItems())))
                        )
                );
        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        Delivery orderDelivery = order.getDelivery();
        Delivery delivery = DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            Parcel parcel = new Parcel();
            parcel.setId(orderDelivery.getParcels().get(0).getId());
            parcel.setTracks(Collections.singletonList(TrackProvider.createTrack("124156fag14", 774L)));
            d.setParcels(Collections.singletonList(parcel));
        });

        orderDeliveryHelper.updateOrderDelivery(order.getId(), ClientInfo.SYSTEM, delivery);
        orderDeliveryHelper.updateOrderDelivery(order.getId(), ClientInfo.SYSTEM, delivery);

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());

        long countDeliveryUpdated = events.getItems().stream()
                .filter(ohe -> ohe.getType() == HistoryEventType.ORDER_DELIVERY_UPDATED)
                .count();

        Assertions.assertEquals(1, countDeliveryUpdated);
    }

    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Чекаутер должен тримить trackNumber")
    @Test
    public void shouldTrimTrackNumber() throws Exception {
        Order order = createOrder(true);
        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        String trackNumber = "track with space on end   ";
        Delivery delivery = DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                ParcelProvider.createParcelWithTracks(
                        TrackProvider.createTrack(trackNumber, DELIVERY_SERVICE_ID)
                )
        );

        Order updated = orderDeliveryHelper.updateOrderDelivery(order, ClientInfo.SYSTEM, delivery);

        Assertions.assertEquals(trackNumber.trim(),
                updated.getDelivery().getParcels().get(0).getTracks().get(0).getTrackCode());
    }

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Чекпоинты не должны сохраняться")
    @Test
    public void testCheckpointsIgnored() throws Exception {
        Order order = createOrder(true);
        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        Delivery delivery = DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            Parcel parcel = new Parcel();

            parcel.setTracks(Collections.singletonList(
                    TrackProvider.createTrack("124156fag14", 774L)));
            parcel.getTracks().get(0).setCheckpoints(
                    Collections.singletonList(TrackCheckpointProvider.createCheckpoint()));

            d.setParcels(Collections.singletonList(parcel));
        });

        Order result = orderDeliveryHelper.updateOrderDelivery(order.getId(), ClientInfo.SYSTEM, delivery);
        assertThat(result.getDelivery().getTracks(),
                contains(hasProperty("checkpoints", anyOf(empty(), nullValue()))));
        assertThat(result.getDelivery().getParcels().get(0).getTracks(), contains(hasProperty("checkpoints",
                anyOf(empty(), nullValue()))));
    }

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Не должно ругаться на изменение айтемов если айтемы не меняются")
    @Test
    public void testItemsDontFailWhenSame() throws Exception {
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getSelfDelivery());
        order.setFake(true);
        order.setGlobal(true);

        order.setItems(Collections.singleton(OrderItemProvider.buildOrderItem("qwerty", 5)));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long itemId = order.getItem(new FeedOfferId("qwerty", 1L)).getId();

        Delivery deliveryUpdateRequest = DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                ParcelProvider.createParcelWithTracksAndItems(
                        DELIVERY_SERVICE_ID,
                        TRACK_CODE,
                        new ParcelItem(itemId, 2)
                ),
                ParcelProvider.createParcelWithTracksAndItems(
                        321,
                        "iddqd2",
                        new ParcelItem(itemId, 3)
                )
        );

        editOrderDelivery(order, deliveryUpdateRequest);

        orderServiceHelper.insertCheckpoint(new TrackId(TRACK_CODE, DELIVERY_SERVICE_ID),
                TrackCheckpointProvider.createCheckpoint());

        deliveryUpdateRequest = orderService.getOrder(order.getId()).getDelivery();

        editOrderDelivery(order, deliveryUpdateRequest);
    }

    private Parcel assertAndGetSingleShipment(Order order) {
        assertThat(order.getDelivery().getParcels(), hasSize(1));
        return order.getDelivery().getParcels().get(0);
    }

    public Order editOrderDelivery(Order order, Delivery delivery) throws Exception {
        return orderDeliveryHelper.updateOrderDelivery(order.getId(), ClientInfo.SYSTEM, delivery);
    }

    public Order editOrderDeliveryAsShop(Order order, Delivery delivery) throws Exception {
        return orderDeliveryHelper.updateOrderDelivery(order.getId(), new ClientInfo(ClientRole.SHOP, SHOP_ID,
                SHOP_ID), delivery);
    }

    public Order editOrderDeliveryAsShopUser(Order order, Delivery delivery, long shopId) throws Exception {
        return orderDeliveryHelper.updateOrderDelivery(order.getId(), new ClientInfo(ClientRole.SHOP_USER,
                BuyerProvider.UID, shopId), delivery);
    }

    private ResultActions sendEditDeliveryRequestAsShopUser(Order order, Delivery delivery, long shopId)
            throws Exception {
        return orderDeliveryHelper.updateOrderDeliveryForActions(
                order.getId(), new ClientInfo(ClientRole.SHOP_USER, BuyerProvider.UID, shopId), delivery
        );
    }

    private ResultActions sendEditDeliveryRequestAsSystem(Order order, Delivery delivery) throws Exception {
        return orderDeliveryHelper.updateOrderDeliveryForActions(
                order.getId(), ClientInfo.SYSTEM, delivery
        );
    }

    private void checkParcelShipmentIdIsNull(Parcel parcel) {
        assertEquals(0L, parcel.getShipmentId().longValue());
        final Boolean isNullInDB = masterJdbcTemplate.queryForObject(
                "select exists(select 1 from parcel where id = ? and shop_shipment_id is null)",
                (rs, rn) -> rs.getBoolean(1), parcel.getId());
        assertEquals(true, isNullInDB);
    }
}
