package ru.yandex.market.checkout.checkouter.delivery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
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
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackId;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.common.rest.DuplicateKeyException;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderControllerTestHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.test.builders.ParcelBuilder;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.OrderControllerTestHelper.SELF_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.helpers.OrderControllerTestHelper.createOrder;

public class OrderControllerDeliveryTrackTest extends AbstractWebTestBase {

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
     * Добавление трека запросом с телом устаревшего формата
     * <p>
     * Подготовка
     * 1. Создать заказ
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "deprecated-delivery-request-body.json" (устаревший формат)
     * <p>
     * Проверки
     * 1. Ответ на запрос содержит единственное отправление с заполненным полем "id"
     * 2. Отправление содержит единственный трек с трек-кодом "iddqd" и службой доставки 123
     * 3. То же отправление содержится в поле delivery.shipment (без треков)
     * 4. В поле delivery.tracks содержится трек с трек-кодом "iddqd"
     */

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Добавление трека запросом с телом устаревшего формата")
    @Test
    public void testSaveShipmentWithDeprecatedBody() throws Exception {
        // Подготовка
        Order order = orderServiceHelper.createPostOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        // Действие
        order = editOrderDelivery(order,
                DeliveryUpdateProvider.createDeliveryUpdate(d -> {
                    Parcel parcel = new Parcel();

                    parcel.setTracks(Lists.newArrayList(TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID)));

                    d.setParcels(Collections.singletonList(parcel));
                })
        );

        // Проверки
        Delivery delivery = order.getDelivery();
        assertNotNull(delivery);

        Parcel parcel = assertAndGetSingleShipment(order);
        assertNotNull(parcel.getId());

        assertAndGetSingleTrack(parcel, TRACK_CODE, DELIVERY_SERVICE_ID);

        // Проверяем обратную совместимость
        assertNotNull(delivery.getTracks());

        assertThat(delivery.getTracks(), hasSize(1));
        assertEquals(TRACK_CODE, delivery.getTracks().get(0).getTrackCode());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Добавление трека к существующему отправлению
     * <p>
     * Подготовка
     * 1. Добавить трек с единтсвенным товаром в количестве 5 шт.
     * 2. Добавить отправление к заказу
     * 3. Добавить в отправление одну единицу товара из заказа
     * <p>
     * Действия
     * 1. Выполнить запрос к POST /delivery с телом вида "add-track-request-body.json",
     * содержащим новый стек с трек-кодом "iddqd" и службо доставки "123"
     * <p>
     * Проверки
     * 1. Ответ содержит единственное отправление
     * 2. Отправление содержит одну единицу товара из заказа
     * 3. Отправление содержит единственный трек с трек-кодом "iddqd" и службой доставки 123
     * 4. Статус трека - "NEW"
     * 5. Дата создения трека - сегодняшнее число
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Добавление трека к существующему отправлению")
    @Test
    public void testAddTrackToShipment() throws Exception {
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

        Parcel parcel = new Parcel();
        parcel.addParcelItem(new ParcelItem(itemId, 1));

        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(parcel));

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();

        // Действия
        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndTracks(shipmentId, Lists.newArrayList(
                                TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID)
                        ))
                )
        );

        // Проверки
        parcel = assertAndGetSingleShipment(order);
        Track track = assertAndGetSingleTrack(parcel, TRACK_CODE, DELIVERY_SERVICE_ID);
        assertCreationDate(track);
        assertThat(parcel.getParcelItems(), hasSize(1));
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Добавление трека к существующему отправлению заказа, доставляемого сторонней службой доставки
     * <p>
     * Подготовка
     * 1. Создать заказ, доставляемый сторонней службой доставки
     * 2. Добавить посылку к заказу
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "add-track-request-body.json",
     * содержащим новый стек с трек-кодом "iddqd" и службо доставки "123"
     * <p>
     * Проверки
     * 1. В теле ответа одна посылка
     * 2. У посылки один трек
     * 3. Трек-код трека - "iddqd"
     * 4. Служба доставки трека - "123"
     * 5. Статус трека - "NEW"
     * 6. Дата создения трека - сегодняшнее число
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Добавление трека к существующему отправлению заказа, доставляемого сторонней службой доставки")
    @Test
    public void testAddTrackToShipmentOfSelfDeliveryOrder() throws Exception {
        // Подготоска
        Order order = createOrder(true);
        order.getDelivery().setParcels(Collections.singletonList(new Parcel()));
        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();

        // Действие
        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndTracks(shipmentId, Lists.newArrayList(
                                TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID)
                        ))
                )
        );

        // Проверки
        Parcel parcel = assertAndGetSingleShipment(order);
        Track track = assertAndGetSingleTrack(parcel, TRACK_CODE, DELIVERY_SERVICE_ID);
        assertCreationDate(track);
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * При добавлении посылки с треком, для свойств трека устанавливаются корректные значения по-умолчанию
     * <p>
     * Подготовка
     * 1. Создать заказ, доставляемый сторонней службой доставки
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "add-shipment-with-track.json",
     * содержащим новый стек с трек-кодом "iddqd" и службо доставки "123"
     * <p>
     * Проверки
     * 1. В теле ответа одна посылка
     * 2. У посылки один трек
     * 3. Трек-код трека - "iddqd"
     * 4. Служба доставки трека - "123"
     * 5. Статус трека - "NEW"
     * 6. Дата создения трека - сегодняшнее число
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("При добавлении посылки с треком, для свойств трека устанавливаются корректные значения по-умолчанию")
    @Test
    public void testAddTrackWithShipmentToSelfDeliveryOrder() throws Exception {
        // Подготовка
        Order order = createOrder(true);
        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        // Действия
        order = editOrderDelivery(order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithTracks(DELIVERY_SERVICE_ID, TRACK_CODE)
                ));

        // Проверки
        Parcel parcel = assertAndGetSingleShipment(order);
        Track track = assertAndGetSingleTrack(parcel, TRACK_CODE, DELIVERY_SERVICE_ID);
        assertCreationDate(track);
        assertEquals(DeliveryServiceType.CARRIER, track.getDeliveryServiceType());
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("При добавлении посылки с треком, для трека устанавливается тип службы доставки SORTING_CENTER, " +
            "из настроек, если DeliveryPartnerType = YANDEX_MARKET и есть соответствующая настройка")
    @Test
    public void testSetDeliveryServiceTypeForNewTrackWithoutTypeForSortingCenterParcel() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithPartnerDelivery(10773L);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        // Действия
        Parcel parcelWithTracks = ParcelProvider.createParcelWithTracks(
                DELIVERY_SERVICE_ID_WITH_SORTING_CENTER,
                TRACK_CODE
        );
        parcelWithTracks.getTracks().forEach(t -> t.setDeliveryServiceType(null));
        Delivery delivery = DeliveryUpdateProvider.createDeliveryUpdateWithShipments(parcelWithTracks);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        order = editOrderDelivery(order, delivery);

        // Проверки
        Parcel parcel = assertAndGetSingleShipment(order);
        Track track = assertAndGetSingleTrack(parcel, TRACK_CODE, DELIVERY_SERVICE_ID_WITH_SORTING_CENTER);
        assertCreationDate(track);
        assertEquals(DeliveryServiceType.CARRIER, track.getDeliveryServiceType());
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("При добавлении посылки с треком, для трека устанавливается тип службы доставки CARRIER, " +
            "из запроса, если DeliveryPartnerType = YANDEX_MARKET")
    @Test
    public void testSetDeliveryServiceTypeForNewTrackWithTypeForSortingCenterParcel() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithPartnerDelivery(10773L);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        // Действия
        Delivery delivery = DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                ParcelProvider.createParcelWithTracks(DELIVERY_SERVICE_ID_WITH_SORTING_CENTER, TRACK_CODE)
        );
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        order = editOrderDelivery(order, delivery);

        // Проверки
        Parcel parcel = assertAndGetSingleShipment(order);
        Track track = assertAndGetSingleTrack(parcel, TRACK_CODE, DELIVERY_SERVICE_ID_WITH_SORTING_CENTER);
        assertCreationDate(track);
        assertEquals(DeliveryServiceType.CARRIER, track.getDeliveryServiceType());
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("При добавлении посылки с треком, для трека устанавливается тип службы доставки CARRIER, " +
            "если DeliveryPartnerType = YANDEX_MARKET и нет соответствующей настройки")
    @Test
    public void testSetDeliveryServiceTypeForNewTrackForShopWithoutSettings() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithPartnerDelivery(10773L);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        // Действия
        Delivery delivery = DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                ParcelProvider.createParcelWithTracks(456, TRACK_CODE)
        );
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        order = editOrderDelivery(order, delivery);

        // Проверки
        Parcel parcel = assertAndGetSingleShipment(order);
        Track track = assertAndGetSingleTrack(parcel, TRACK_CODE, 456);
        assertCreationDate(track);
        assertEquals(DeliveryServiceType.CARRIER, track.getDeliveryServiceType());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Редактирование трека
     * <p>
     * Подготовка
     * 1. Создать заказ
     * 2. Добавить в заказ отправление с единстенным треком с трек-кодом "iddqd" и без трекерного id
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "edit-track-request-body.json"
     * <p>
     * Проверки
     * 1. Тело запроса содержит единственное отправление
     * 2. Отправление содержит единственый трек с трек-кодом "iddqd" и трекерным id 111
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Редактирование трека")
    @Test
    public void testEditTrack() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getPostalDelivery());
        order.setFake(true);
        order.setGlobal(false);

        Parcel parcel = new Parcel();
        parcel.addTrack(TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID));
        order.getDelivery().setParcels(Collections.singletonList(parcel));

        order = orderServiceHelper.saveOrder(order);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();

        // Действие
        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndTracks(shipmentId, Lists.newArrayList(
                                TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID, 111L)
                        ))
                )
        );

        // проверки
        parcel = assertAndGetSingleShipment(order);
        assertEquals(shipmentId, (long) parcel.getId());
        assertAndGetSingleTrack(parcel, TRACK_CODE, DELIVERY_SERVICE_ID);
    }


    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Удаление трека
     * <p>
     * Подготовка
     * 1. Создать запрос
     * 2. Добавить в запрос отправление
     * 3. Добавить в отправление два трека с трек-кодами "iddqd-1" и "iddqd-2" (оба без трекерного id)
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "del-track-request-body.json"
     * <p>
     * Проверки
     * 1. Тело ответа содержит единственное отправление
     * 2. В отправлении содержится единственный трек с трек-кодом "iddqd-1" и трекерным id 111
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Удаление трека")
    @Test
    public void testDeleteTrack() throws Exception {
        // Подготовка
        Order order = OrderProvider.getPostPaidOrder();
        order.setDelivery(DeliveryProvider.getPostalDelivery());
        order.setFake(true);
        order.setGlobal(false);

        Parcel parcel = new Parcel();
        parcel.addTrack(TrackProvider.createTrack("iddqd-1", DELIVERY_SERVICE_ID));
        parcel.addTrack(TrackProvider.createTrack("iddqd-2", DELIVERY_SERVICE_ID));

        order.getDelivery().setParcels(Collections.singletonList(parcel));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();

        // Действие
        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndTracks(shipmentId, Lists.newArrayList(
                                TrackProvider.createTrack("iddqd-1", DELIVERY_SERVICE_ID, 111L)
                        ))
                )
        );

        // Проверка
        parcel = assertAndGetSingleShipment(order);
        assertEquals(shipmentId, (long) parcel.getId());
        assertAndGetSingleTrack(parcel, "iddqd-1", DELIVERY_SERVICE_ID);
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * При удалении трека из заказа с несколькими посылками удаленный трек перестает попадать в выдачу
     * а у других посылок сохраняются их треки и товары
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ даоставляемый сторонней службой доставки
     * 2. Добавить в заказ посылки A и B
     * 3. Добавить в полылку А товары и трек с чекпоинтом
     * 4. Добавить в посылку B товары и трек
     * <p>
     * Действия
     * 1. Выполнить запрос к POST /delivery с телом вида "del-track-from-multishipment-order.json",
     * с пустым массивом треков у заказа B
     * <p>
     * Проверки
     * 1. У заказа две посылки
     * 2. У посылки A сохранились все треки и товары
     * 3. Товары в посылке B не изменились
     * 4. У посылки B не осталось треков
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("При удалении трека из заказа с несколькими посылками удаленный трек перестает попадать в выдачу а у" +
            " других посылок сохраняются их треки и товары")
    @Test
    public void testDeleteTrackFromOrderWithMultipleShipments() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithSelfDelivery(true);
        orderServiceHelper.insertCheckpoint(
                new TrackId("iddqd-2", DELIVERY_SERVICE_ID)
        );

        long itemId1 = order.getItem(new FeedOfferId("qwerty-1", 1L)).getId();
        long itemId2 = order.getItem(new FeedOfferId("qwerty-2", 1L)).getId();

        Delivery delivery = order.getDelivery();
        long shipment1Id = delivery.getParcels().get(0).getId();
        long shipment2Id = delivery.getParcels().get(1).getId();

        // Действия
        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithId(shipment1Id),
                        ParcelProvider.createParcelWithIdAndTracks(shipment2Id, Collections.emptyList())
                )
        );

        // Проверки
        order = orderService.getOrder(order.getId());
        delivery = order.getDelivery();

        assertThat(delivery.getParcels(), hasSize(2));

        Parcel parcel1 = delivery.getParcels().get(0);
        assertAndGetSingleTrack(parcel1, "iddqd-2", DELIVERY_SERVICE_ID);
        assertThat(parcel1.getParcelItems(), hasSize(2));
        assertEquals(itemId1, (long) parcel1.getParcelItems().get(0).getItemId());
        assertEquals(5, (int) parcel1.getParcelItems().get(0).getCount());
        assertEquals(itemId2, (long) parcel1.getParcelItems().get(1).getItemId());
        assertEquals(2, (int) parcel1.getParcelItems().get(1).getCount());

        Parcel parcel2 = delivery.getParcels().get(1);
        assertThat(parcel2.getParcelItems(), hasSize(1));
        assertEquals(itemId2, (long) parcel2.getParcelItems().get(0).getItemId());
        assertEquals(3, (int) parcel2.getParcelItems().get(0).getCount());
        assertNull(parcel2.getTracks());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Пользователю с ролью "SHOP_USER" запрещено добавление треков в посылку заказа,
     * доставляемого партнером Маркета
     * <p>
     * Подготовка
     * 1. Создать заказ, доставляемый партнером Маркета
     * 2. Добавить к посылке трек с трек-кодом "qwerty" и службой доставки 123
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "add-track-request-body.json",
     * содержищим новый трек. Роль пользователя "SHOP_USER"
     * <p>
     * Проверка
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Пользователю с ролью 'SHOP_USER' запрещено добавление треков в посылку заказа, доставляемого " +
            "партнером Маркета")
    @Test
    public void testShopUserIsNotAllowedToAddTracksToPartnerShipment() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithPartnerDelivery(SHOP_ID);
        Parcel parcel = order.getDelivery().getParcels().get(0);

        sendEditDeliveryRequestAsShopUser(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndTracks(
                                parcel.getId(), TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID)
                        )
                ),
                SHOP_ID
        ).andExpect(status().isBadRequest());
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

    @DisplayName("Можно добавить такой же трек к существующему заказу")
    @Test
    public void canAddTheSameTracksToTheOrder() throws Exception {
        // Добавляем заказ
        Order order = orderControllerTestHelper.createOrderWithSelfDelivery(true);
        final String trackCode = order.getDelivery().getParcels().get(0).getTracks().get(0).getTrackCode();

        // Пытаемся изменить доставку и добавить отправление с таким же треком
        Parcel parcel = ParcelBuilder.instance()
                .withTracks(Collections.singletonList(
                        TrackProvider.createTrack(trackCode, SELF_DELIVERY_SERVICE_ID)))
                .build();
        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(parcel));
        order = editOrderDelivery(order, delivery);

        List<String> tracks = order.getDelivery().getParcels()
                .stream()
                .flatMap(p -> p.getTracks().stream())
                .map(Track::getTrackCode)
                .collect(Collectors.toList());
        assertEquals(1, tracks.size(), tracks.toString());
        assertThat(tracks, containsInAnyOrder(trackCode));
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Нельзя удалять посылку имеющую хотя бы один трек со значащим чекпоинтом
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ, доставляемый сторонней службой
     * 2. Добавить посылку в заказ
     * 3. Добавить трек к посылке
     * 4. Добавить к треку чекпойнт с доставочным статусом 123
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "del-shipment-request-body.json",
     * одержащим пустой массив посылок. Роль пользователя - "SYSTEM".
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя удалять посылку имеющую хотя бы один трек со значащим чекпоинтом")
    @Test
    public void testImpossibleToDeleteShipmentWithCheckpoints() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithSelfDelivery(true);

        Parcel parcel = new Parcel();
        parcel.addTrack(TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID));

        order.getDelivery().setParcels(Collections.singletonList(parcel));

        order = orderServiceHelper.saveOrder(order);
        orderServiceHelper.insertCheckpoint(
                new TrackId(TRACK_CODE, DELIVERY_SERVICE_ID)
        );


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
     * Нельзя удалить трек если у него есть значимые чекпойнты
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ, доставляемый сторонней службой
     * 2. Добавить посылку к заказу
     * 3. Добавить трек к посылке
     * 4. Добавить к треку чекпоинт с доставочным статусом 123
     * <p>
     * Действие
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "drop-tracks-request-body.json",
     * одержащим пустой массив треков в посылке. Роль пользователя - "SYSTEM".
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя удалить трек если у него есть значимые чекпойнты")
    @Test
    public void testImpossibleToDeleteTrackWithCheckpoints() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithSelfDelivery(true);

        Parcel parcel = new Parcel();
        parcel.addTrack(TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID));

        order.getDelivery().setParcels(Collections.singletonList(parcel));

        order = orderServiceHelper.saveOrder(order);
        orderServiceHelper.insertCheckpoint(
                new TrackId(TRACK_CODE, DELIVERY_SERVICE_ID)
        );
        long shipmentId = order.getDelivery().getParcels().get(0).getId();

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndTracks(
                                shipmentId, Collections.emptyList()
                        )
                )
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Разрешено удаление посылки содержащей треки, содержащие чекпоинты с доставочными статусами 0 и 1
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ, доставляемый сторонней службой
     * 2. Добавить посылку в заказ
     * 3. Добавить трек к посылке
     * 4. Добавить к треку чекпойнт с доставочным статусом 0
     * 5. Добавить к треку чекпойнт с доставочным статусом 1
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "del-shipment-request-body.json",
     * одержащим пустой массив посылок. Роль пользователя - "SYSTEM".
     * <p>
     * Проверка
     * 1. У заказа отстуствуют отправления
     */

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Разрешено удаление посылки содержащей треки, содержащие чекпоинты с доставочными статусами 0 и 1")
    @Test
    public void testDeleteShipmentWithExceptionalCheckpoints() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithSelfDelivery(true);

        Parcel parcel = new Parcel();
        parcel.addTrack(TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID));

        order.getDelivery().setParcels(Collections.singletonList(parcel));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        orderServiceHelper.insertCheckpoint(new TrackId(TRACK_CODE, DELIVERY_SERVICE_ID),
                TrackCheckpointProvider.createCheckpoint(0));
        orderServiceHelper.insertCheckpoint(new TrackId(TRACK_CODE, DELIVERY_SERVICE_ID),
                TrackCheckpointProvider.createCheckpoint(1));

        // Действия
        order = editOrderDelivery(order,
                DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
                    d.setParcels(Collections.emptyList());
                })
        );

        // Проверки
        assertThat(order.getDelivery().getParcels(), empty());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Нельзя изменить наполнение посылки в случае если у нее есть хотя бы один трек
     * со значимыми чекпойнтами
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ, доставляемый сторонней службой
     * 2. Добавить посылку в заказ
     * 3. Добавить трек к посылке
     * 4. Добавить товар к посылке
     * 5. Добавить к треку чекпойнт с доставочным статусом 123
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "del-item-request-body.json",
     * содержащем пустой массив товаров в посылке. Роль пользователя - "SYSTEM".
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */

    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Нельзя изменить наполнение посылки в случае если у нее есть хотя бы один трек со значимыми " +
            "чекпойнтами")
    @Test
    public void testImpossibleToDeleteItemFromShipmentWithCheckpoints() throws Exception {
        // Подготовка
        Order order = orderControllerTestHelper.createOrderWithSelfDelivery(true);

        Parcel parcel = new Parcel();
        parcel.addTrack(TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID));

        order.getDelivery().setParcels(Collections.singletonList(parcel));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();
        long itemId = order.getItem(new FeedOfferId("qwerty-1", 1L)).getId();

        Parcel newParcel = new Parcel();
        newParcel.setId(shipmentId);
        newParcel.addParcelItem(new ParcelItem(itemId, 5));
        Delivery newDelivery = new Delivery();
        newDelivery.setParcels(Collections.singletonList(newParcel));
        orderUpdateService.updateOrderDelivery(order.getId(), newDelivery, ClientInfo.SYSTEM);

        orderServiceHelper.insertCheckpoint(new TrackId(TRACK_CODE, DELIVERY_SERVICE_ID),
                TrackCheckpointProvider.createCheckpoint());
        shipmentId = order.getDelivery().getParcels().get(0).getId();

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndItems(
                                shipmentId,
                                Collections.emptyList()
                        )
                )
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Нельзя добавить трек с отсутствующим трек-кодом
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ, доставляемый сторонней службой доставки
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "track-without-code.json",
     * содержащем новую посылку с треком без трек-номера
     * <p>
     * Проверка
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя добавить трек с отсутствующим трек-кодом")
    @Test
    public void testErrorOnSaveTrackWithoutTrackCode() throws Exception {
        // Подготовка
        Order order = createOrder(true);
        order = orderServiceHelper.saveOrder(order);

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithTracks(
                                new Track(null, DELIVERY_SERVICE_ID)
                        )
                )
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Невозможно добавить треки к посылке если их итоговое количество превышает 5 шт.
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ, доставляемый сторонней службой доставки
     * 2. Добавить посылку в заказ
     * 3. Добавить к посылке два трека
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "too-many-tracks.json",
     * содержащем посылку с шестью треками (2 существующих + 4 новых)
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Невозможно добавить треки к посылке если их итоговое количество превышает 5 шт.")
    @Test
    public void testErrorIfMaxTrackCountExceeded() throws Exception {
        // Подготовка
        Order order = createOrder(true);

        Parcel parcel = new Parcel();
        parcel.addTrack(TrackProvider.createTrack("iddqd-1", DELIVERY_SERVICE_ID));
        parcel.addTrack(TrackProvider.createTrack("iddqd-2", DELIVERY_SERVICE_ID));

        order.getDelivery().setParcels(Collections.singletonList(parcel));

        order = orderServiceHelper.saveOrder(order);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndTracks(
                                shipmentId,
                                IntStream.rangeClosed(1, 6)
                                        .mapToObj(i -> new Track("iddqd-" + i, DELIVERY_SERVICE_ID))
                                        .collect(Collectors.toList())
                        )
                )
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Треки добавляются корректно, в случае если их количество в одной посылке не превышает 5 шт.
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ, доставляемый сторонней службой доставки
     * 2. Добавить посылку в заказ
     * 3. Добавить к посылке два трека
     * <p>
     * Действие
     * 1. Выполнить запрос к POST /delivery с телом вида "too-many-tracks.json",
     * содержащем посылку с пятью треками (2 существующих + 3 новых)
     * <p>
     * Проверки
     * 1. Код ответа - 200
     * 2. У посылки в ответе 5 треков
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Треки добавляются корректно, в случае если их количество в одной посылке не превышает 5 шт.")
    @Test
    public void testAddMaxPossibleTracksToShipment() throws Exception {
        // Подготовка
        Order order = createOrder(true);

        Parcel parcel = new Parcel();
        parcel.addTrack(TrackProvider.createTrack("iddqd-1", DELIVERY_SERVICE_ID));
        parcel.addTrack(TrackProvider.createTrack("iddqd-2", DELIVERY_SERVICE_ID));

        order.getDelivery().setParcels(Collections.singletonList(parcel));

        order = orderServiceHelper.saveOrder(order);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        long shipmentId = order.getDelivery().getParcels().get(0).getId();

        order = editOrderDelivery(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithIdAndTracks(
                                shipmentId,
                                IntStream.rangeClosed(1, 5)
                                        .mapToObj(i -> new Track("iddqd-" + i, DELIVERY_SERVICE_ID))
                                        .collect(Collectors.toList())
                        )
                )
        );

        // Проверки
        assertThat(assertAndGetSingleShipment(order).getTracks(), hasSize(5));
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Нельзя добавить посылку в случае если количество треков в ней превышает 5 шт.
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ, доставляемый сторонней службой доставки
     * <p>
     * Действе
     * 1. Выполнить запрос к POST /delivery с телом вида "new-shipment-with-too-many-tracks.json",
     * содержащем новую посылку с шестью треками
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @DisplayName("Нельзя добавить посылку в случае если количество треков в ней превышает 5 шт.")
    @Test
    public void testErrorOnAddShipmentWithTooManyTracks() throws Exception {
        // Подготовка
        Order order = createOrder(true);
        order = orderServiceHelper.saveOrder(order);

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithTracks(
                                IntStream.rangeClosed(1, 6)
                                        .mapToObj(i -> TrackProvider.createTrack("iddqd-" + i, DELIVERY_SERVICE_ID))
                                        .toArray(Track[]::new)
                        )
                )
        ).andExpect(status().isBadRequest());
    }

    /**
     * POST /orders/{orderId}/delivery
     * <p>
     * Нельзя сохранить треки с совпадающими трек-кодами и службой доставки
     * <p>
     * Подготовка
     * 1. Создать глобальный заказ
     * <p>
     * Действе
     * 1. Выполнить запрос к POST /delivery с телом вида "add-same-track-twice.json",
     * содержащим один и тот же трек в двух разный посылках
     * <p>
     * Проверки
     * 1. Код ответа - 400
     */
    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_DELIVERY)
    @DisplayName("Нельзя сохранить треки с совпадающими трек-кодами и службой доставки")
    @Test
    public void testErrorOnPushTwoTracksWithSameTrackCodeAndDeliveryService() throws Exception {
        // Подготовка
        Order order = createOrder(true);
        order.setFake(false);
        order = orderServiceHelper.saveOrder(order);

        sendEditDeliveryRequestAsSystem(
                order,
                DeliveryUpdateProvider.createDeliveryUpdateWithShipments(
                        ParcelProvider.createParcelWithTracks(
                                TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID),
                                TrackProvider.createTrack(TRACK_CODE, DELIVERY_SERVICE_ID)
                        )
                )
        ).andExpect(status().isBadRequest());
    }

    private Parcel assertAndGetSingleShipment(Order order) {
        assertThat(order.getDelivery().getParcels(), hasSize(1));
        return order.getDelivery().getParcels().get(0);
    }

    private Track assertAndGetSingleTrack(Parcel parcel, String trackCode, long deliveryServiceId) {
        assertThat(parcel.getTracks(), hasSize(1));
        Track track = parcel.getTracks().get(0);
        assertEquals(trackCode, track.getTrackCode());
        assertEquals(deliveryServiceId, (long) track.getDeliveryServiceId());
        assertEquals(TrackStatus.NEW, track.getStatus());
        return track;
    }

    public Order editOrderDelivery(Order order, Delivery delivery) throws Exception {
        return orderDeliveryHelper.updateOrderDelivery(order.getId(), ClientInfo.SYSTEM, delivery);
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

    private void assertCreationDate(Track track) {
        assertNotNull(track.getCreationDate());
        LocalDate creationDate = LocalDateTime.ofInstant(
                track.getCreationDate().toInstant(),
                ZoneOffset.systemDefault()
        ).toLocalDate();
        assertEquals(LocalDate.now(), creationDate);
    }
}
