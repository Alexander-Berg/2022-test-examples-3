package ru.yandex.market.checkout.checkouter.order.item;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.backbone.converter.MissingItemsNotificationConverter;
import ru.yandex.market.checkout.backbone.validation.order.item.instances.CisItemInstancesValidator;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.ItemCount;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEventReasonDetails;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderItemsException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.CannotRemoveItemException;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.promo.PromoConfigurer;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.BundleOrderHelper;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderItemsException.ITEM_NOT_FOUND_CODE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.ANAPLAN_ID;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.FIRST_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SHOP_PROMO_KEY;

class MissingItemsRemovalTest extends MissingItemsAbstractTest {

    private final String cis1 = "010641944023860921-DLdnD)pMAC1t";
    private final String cis2 = "010641944023860921-DLdnD)pMAC2t";
    private final String cis3 = "010641944023860921-DLdnD)pMAC3t";
    private final String cis4 = "010641944023860921-DLdnD)pMAC4t";
    private final String cisWithCryptotail = "0104601662000016215RNef*\u001d93B0Ik";
    private final String cisWithoutCryptoTail = "0104601662000016215RNef*";
    private final String uit1 = "123456789";
    private final String uit2 = "223456789";
    private final String uit3 = "323456789";
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private BundleOrderHelper bundleOrderHelper;
    @Autowired
    private PromoConfigurer promoConfigurer;

    private static Stream<Arguments> checkRemoveAvailableSource() {
        return Stream.of(
                Arguments.of(DeliveryType.DELIVERY, DELIVERY, ClientRole.DELIVERY_SERVICE),
//                Arguments.of(DeliveryType.DELIVERY, PICKUP, ClientRole.DELIVERY_SERVICE), Cannot proceed to status
//               PICKUP from status DELIVERED
                Arguments.of(DeliveryType.DELIVERY, DELIVERY, ClientRole.PICKUP_SERVICE),
//                Arguments.of(DeliveryType.DELIVERY, PICKUP, ClientRole.PICKUP_SERVICE), Cannot proceed to status
//               PICKUP from status DELIVERED

                Arguments.of(DeliveryType.PICKUP, DELIVERY, ClientRole.DELIVERY_SERVICE),
                Arguments.of(DeliveryType.PICKUP, PICKUP, ClientRole.DELIVERY_SERVICE),
                Arguments.of(DeliveryType.PICKUP, DELIVERY, ClientRole.PICKUP_SERVICE),
                Arguments.of(DeliveryType.PICKUP, PICKUP, ClientRole.PICKUP_SERVICE)
        );
    }

    private static void checkNoZeroItems(@Nonnull Order order) {
        order.getItems().stream().map(OrderItem::getCount).forEach(count -> assertTrue(count > 0));
    }

    private static void checkItemsWereNotIncreased(@Nonnull Map<Long, OrderItem> itemsBefore,
                                                   @Nonnull Map<Long, OrderItem> itemsAfter) {
        // Проверка, что нет новых айтемов
        assertTrue(itemsAfter.keySet().stream().allMatch(itemsBefore::containsKey));

        // Проверка что ни один айтем не увеличился в количестве
        assertTrue(itemsAfter.entrySet().stream()
                .noneMatch(entry -> itemsBefore.get(entry.getKey()).getCount() < entry.getValue().getCount()));
    }

    private static void checkParcelItemsMatchOrderItems(@Nonnull Order order) {
        List<Parcel> parcels = order.getDelivery().getParcels();
        assertEquals(1, parcels.size());
        Parcel parcel = parcels.get(0);

        Map<Long, OrderItem> items = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, i -> i));
        Map<Long, ParcelItem> parcelItems = parcel.getParcelItems().stream()
                .collect(Collectors.toMap(ParcelItem::getItemId, i -> i));

        assertEquals(items.keySet(), parcelItems.keySet());
        items.keySet().forEach(id -> assertEquals(items.get(id).getCount(), parcelItems.get(id).getCount()));
    }

    @Test
    @DisplayName("Успешное удаление одной единицы товара из одной позиции для fulfilment заказа")
    void itemReducedIfRemovalAllowed() throws Exception {
        Order order = createOrderWithTwoItems();
        // этот order item был удален, его id будем проверять ниже
        OrderItem notFoundOrderItem = getItemWithMaxCount(order);
        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(order);
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);

        // MDB успешно подтверждает ChangeRequest
        updateChangeRequest(order, changeRequest.getId(), true);

        validateItemsRemoval(order, orderService.getOrder(order.getId()), editRequest);

        OrderHistoryEvent event = getOrdersItemUpdatedEvent(order.getId());
        assertEquals(HistoryEventType.ITEMS_UPDATED, event.getType());
        assertEquals(HistoryEventReason.ITEMS_NOT_FOUND, event.getReason());
        OrderHistoryEventReasonDetails expectedDetails =
                new OrderHistoryEventReasonDetails(List.of(new ItemCount(notFoundOrderItem.getId(), 1)));
        assertEquals(expectedDetails, event.getReasonDetails());
    }

    @Test
    @DisplayName("Успешное удаление одной единицы товара из одной позиции для fulfilment заказа на Pickup " +
            "для Тинькоф заказа")
    void itemReducedForFbyOnPickupWithTinkof() throws Exception {
        Order order = createOrderWithTwoItems(PaymentMethod.TINKOFF_CREDIT, DeliveryType.PICKUP);
        order = orderStatusHelper.proceedOrderToStatus(order, PICKUP);
        // этот order item был удален, его id будем проверять ниже
        OrderItem notFoundOrderItem = getItemWithMaxCount(order);
        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(order,
                HistoryEventReason.USER_REQUESTED_REMOVE);
        notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest, ClientRole.PICKUP_SERVICE);

        validateItemsRemoval(order, orderService.getOrder(order.getId()), editRequest, false);

        OrderHistoryEvent event = getOrdersItemUpdatedEvent(order.getId());
        assertEquals(HistoryEventType.ITEMS_UPDATED, event.getType());
        assertEquals(HistoryEventReason.USER_REQUESTED_REMOVE, event.getReason());
        OrderHistoryEventReasonDetails expectedDetails =
                new OrderHistoryEventReasonDetails(List.of(new ItemCount(notFoundOrderItem.getId(), 1)));
        assertEquals(expectedDetails, event.getReasonDetails());
    }

    @Test
    @DisplayName("Успешное удаление одной единицы товара из одной позиции для fulfilment заказа на Pickup")
    void itemReducedForFbyOnPickup() throws Exception {
        Order order = createOrderWithTwoItems(DeliveryType.PICKUP, OrderStatus.PICKUP);
        // этот order item был удален, его id будем проверять ниже
        OrderItem notFoundOrderItem = getItemWithMaxCount(order);
        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(order,
                HistoryEventReason.USER_REQUESTED_REMOVE);
        notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest, ClientRole.PICKUP_SERVICE);

        validateItemsRemoval(order, orderService.getOrder(order.getId()), editRequest, false);

        OrderHistoryEvent event = getOrdersItemUpdatedEvent(order.getId());
        assertEquals(HistoryEventType.ITEMS_UPDATED, event.getType());
        assertEquals(HistoryEventReason.USER_REQUESTED_REMOVE, event.getReason());
        OrderHistoryEventReasonDetails expectedDetails =
                new OrderHistoryEventReasonDetails(List.of(new ItemCount(notFoundOrderItem.getId(), 1)));
        assertEquals(expectedDetails, event.getReasonDetails());
    }

    @Test
    @DisplayName("Успешное удаление одной единицы товара из fbs заказа с указанием пустых кизов")
        // кизы пустые для имитации случая, когда в доставку попадают fbs заказы требующие наличие кизов, но без них
        // MARKETCHECKOUT-27863. Должно исправится в OPSPROJECT-4553
        // После него установка в тесте пустого киза не принципиальна
    void itemReducedForFbyOnPickup2() {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPostpaidParameters();
        parameters.getItems().forEach(item -> {
            item.setCargoTypes(CisItemInstancesValidator.CIS_REQUIRED_CARGOTYPE_CODES);
            item.setCount(10);
        });
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        OrderItem notFoundOrderItem = getItemWithMaxCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfoWithInstances(notFoundOrderItem, 9, Set.of(new OrderItemInstance("")))
        ), HistoryEventReason.USER_REQUESTED_REMOVE, true));

        notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest, ClientRole.DELIVERY_SERVICE);

        validateItemsRemoval(order, orderService.getOrder(order.getId()), editRequest, false);
    }

    @Test
    @DisplayName("Удаление запрещено из за невалидной причины удаления")
    void removeForbiddenByInvalidReason() {
        Order order = createOrderWithTwoItems(DeliveryType.DELIVERY, OrderStatus.DELIVERY);
        // этот order item был удален, его id будем проверять ниже
        OrderItem notFoundOrderItem = getItemWithMaxCount(order);
        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(order, HistoryEventReason.ITEMS_NOT_FOUND);

        ErrorCodeException exception =
                assertThrows(ErrorCodeException.class, () -> client.editOrder(order.getId(),
                        ClientRole.DELIVERY_SERVICE, null, List.of(Color.BLUE), editRequest));

        assertEquals(exception.getCode(), CannotRemoveItemException.NOT_ALLOWED_REASON);
    }

    @Test
    @DisplayName("Удаление товара с промо cheapest_as_gift на сборке")
    void removeItemWithCheapestAsGiftPromo() {
        Order order = bundleOrderHelper.createTypicalOrderWithCheapestAsGift(parameters -> {
            promoConfigurer.importFrom(parameters);
            OrderItem item1 = itemOf(parameters.getOrder(), FIRST_OFFER);
            promoConfigurer.applyDirectDiscount(
                    item1,
                    PROMO_KEY,
                    ANAPLAN_ID,
                    SHOP_PROMO_KEY,
                    BigDecimal.valueOf(1999), null, true, true);
            promoConfigurer.applyTo(parameters);
        });
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(order);
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);

        updateChangeRequest(order, changeRequest.getId(), false);

        validateItemsRemoval(order, orderService.getOrder(order.getId()), editRequest);
    }

    @Nonnull
    private OrderItem itemOf(@Nonnull Order order, @Nonnull String offerId) {
        return order.getItems().stream()
                .filter(item -> item.getOfferId().equals(offerId))
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Успешное удаление одной единицы товара из одной позиции для fulfilment заказа на Delivery")
    @Disabled
    void itemReducedForFbyOnDelivery() throws Exception {
        Order order = createOrderWithTwoItems(DeliveryType.DELIVERY, OrderStatus.DELIVERY);
        // этот order item был удален, его id будем проверять ниже
        OrderItem notFoundOrderItem = getItemWithMaxCount(order);
        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(order,
                HistoryEventReason.USER_REQUESTED_REMOVE);
        notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest, ClientRole.DELIVERY_SERVICE);

        validateItemsRemoval(order, orderService.getOrder(order.getId()), editRequest);

        OrderHistoryEvent event = getOrdersItemUpdatedEvent(order.getId());
        assertEquals(HistoryEventType.ITEMS_UPDATED, event.getType());
        assertEquals(HistoryEventReason.USER_REQUESTED_REMOVE, event.getReason());
        OrderHistoryEventReasonDetails expectedDetails =
                new OrderHistoryEventReasonDetails(List.of(new ItemCount(notFoundOrderItem.getId(), 1)));
        assertEquals(expectedDetails, event.getReasonDetails());
    }

    @Test
    @DisplayName("Успешное удаление одной единицы товара из одной позиции для fulfilment заказа. Запрос через itemId")
    void itemReducedIfRemovalAllowedRequestedByItemIds() throws Exception {
        Order order = createOrderWithTwoItems();
        // этот order item был удален, его id будем проверять ниже
        OrderItem notFoundOrderItem = getItemWithMaxCount(order);
        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithOneMissingUnitByItemId(order);
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);

        // MDB успешно подтверждает ChangeRequest
        updateChangeRequest(order, changeRequest.getId(), true);

        validateItemsRemoval(order, orderService.getOrder(order.getId()), editRequest);

        OrderHistoryEvent event = getOrdersItemUpdatedEvent(order.getId());
        assertEquals(HistoryEventType.ITEMS_UPDATED, event.getType());
        assertEquals(HistoryEventReason.ITEMS_NOT_FOUND, event.getReason());
        OrderHistoryEventReasonDetails expectedDetails =
                new OrderHistoryEventReasonDetails(List.of(new ItemCount(notFoundOrderItem.getId(), 1)));
        assertEquals(expectedDetails, event.getReasonDetails());
    }

    @Test
    @DisplayName("Успешное удаление одной единицы товара из одной позиции с указанием причины")
    void itemReducedIfRemovalAllowedWithReason() throws Exception {
        Order order = createOrderWithTwoItems();

        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithValidReason(order);
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);

        // MDB успешно подтверждает ChangeRequest
        updateChangeRequest(order, changeRequest.getId(), true);

        validateItemsRemoval(order, orderService.getOrder(order.getId()), editRequest);

        OrderHistoryEvent event = getOrdersItemUpdatedEvent(order.getId());
        assertEquals(HistoryEventType.ITEMS_UPDATED, event.getType());
        assertEquals(HistoryEventReason.ITEMS_NOT_FOUND, event.getReason());
    }

    @Test
    @DisplayName("Успешное удаление одной позиции заказа целиком")
    void itemTotallyRemovedIfRemovalAllowed() throws Exception {
        Order order = createOrderWithTwoItems();
        // этот order item был удален, его id будем проверять ниже
        OrderItem notFoundOrderItem = getItemWithMinCount(order);
        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithOneTotallyMissingItem(order);
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);

        // MDB успешно подтверждает ChangeRequest
        updateChangeRequest(order, changeRequest.getId(), true);

        validateItemsRemoval(order, orderService.getOrder(order.getId()), editRequest);
        OrderHistoryEvent event = getOrdersItemUpdatedEvent(order.getId());
        OrderHistoryEventReasonDetails expectedDetails =
                new OrderHistoryEventReasonDetails(List.of(new ItemCount(notFoundOrderItem.getId(), 1)));
        assertEquals(expectedDetails, event.getReasonDetails());
    }

    @Test
    @DisplayName("Успешное удаление одной позиции заказа целиком через указаниe itemId")
    void itemTotallyRemovedIfRemovalAllowedRequestedItemId() throws Exception {
        Order order = createOrderWithTwoItems();
        // этот order item был удален, его id будем проверять ниже
        OrderItem notFoundOrderItem = getItemWithMinCount(order);
        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithOneTotallyMissingItemByItemId(order);
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);

        // MDB успешно подтверждает ChangeRequest
        updateChangeRequest(order, changeRequest.getId(), true);

        validateItemsRemoval(order, orderService.getOrder(order.getId()), editRequest);
        OrderHistoryEvent event = getOrdersItemUpdatedEvent(order.getId());
        OrderHistoryEventReasonDetails expectedDetails =
                new OrderHistoryEventReasonDetails(List.of(new ItemCount(notFoundOrderItem.getId(), 1)));
        assertEquals(expectedDetails, event.getReasonDetails());
    }

    @Test
    @DisplayName("Удаление одной штучки товара из заказа с редактированием кизов")
    void removeOneInstanceOfItemWithEditCises() {
        //Создание заказа с двумя товарами. Один с кизами и требованием кизов. Другой обычный
        Order order = createOrderWithInstances(true,
                new OrderItemInstance(cis1),
                new OrderItemInstance(cis2),
                new OrderItemInstance(cis3));
        //запрос на удаление одной штучки из товара и замену кизов в нем
        OrderEditRequest editRequest = getEditRequestWithInstances(order,
                new OrderItemInstance(cis2),
                new OrderItemInstance(cis4));
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);
        updateChangeRequest(order, changeRequest.getId(), true);

        Order actualOrder = orderService.getOrder(order.getId());
        OrderItem itemWithCis = filterItemsBy(actualOrder.getItems(), item -> item.getInstances() != null);
        OrderItem itemWithoutCis = filterItemsBy(actualOrder.getItems(), item -> item.getInstances() == null);

        assertTrue(itemWithCis.getInstances().toString().contains("\"cis\":\"" + cis2 + "\""));
        assertTrue(itemWithCis.getInstances().toString().contains("\"cis\":\"" + cis4 + "\""));
        assertTrue(itemWithCis.getInstances().toString().contains("\"balanceOrderId\":"));
        assertNull(itemWithoutCis.getInstances());
    }

    @Test
    @DisplayName("Удаление одной штучки товара из заказа с редактированием кизов - обычный и с криптохвостом")
    void removeOneInstanceOfItemWithEditCisesFull() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CIS_FULL_VALIDATION, true);
        //Создание заказа с двумя товарами. Один с кизами и требованием кизов. Другой обычный
        Order order = createOrderWithInstances(true,
                new OrderItemInstance(cis1),
                new OrderItemInstance(cis2),
                new OrderItemInstance(cis3));
        //запрос на удаление одной штучки из товара и замену кизов в нем
        OrderEditRequest editRequest = getEditRequestWithInstances(order,
                new OrderItemInstance(cisWithCryptotail),
                new OrderItemInstance(cis4));
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);
        updateChangeRequest(order, changeRequest.getId(), true);

        Order actualOrder = orderService.getOrder(order.getId());
        OrderItem itemWithCis = filterItemsBy(actualOrder.getItems(), item -> item.getInstances() != null);
        OrderItem itemWithoutCis = filterItemsBy(actualOrder.getItems(), item -> item.getInstances() == null);

        assertTrue(itemWithCis.getInstances().toString().contains("\"cis\":\"" + cisWithoutCryptoTail + "\""));
        assertTrue(itemWithCis.getInstances().toString().contains(
                "\"cisFull\":\"" + "0104601662000016215RNef*\\u001D93B0Ik" + "\""
        ));
        assertTrue(itemWithCis.getInstances().toString().contains("\"cis\":\"" + cis4 + "\""));
        assertTrue(itemWithCis.getInstances().toString().contains("\"balanceOrderId\":"));
        assertNull(itemWithoutCis.getInstances());
    }

    @Test
    @DisplayName("Редактирование кизов без удаления чего-либо")
    void editCisesWithoutRemoveAnything() {
        //Создание заказа с двумя товарами. Один с кизами и требованием кизов. Другой обычный
        Order order = createOrderWithInstances(true,
                new OrderItemInstance(cis1),
                new OrderItemInstance(cis2),
                new OrderItemInstance(cis3));
        //запрос на удаление без удаления чего-либо, но с редактирование кизов
        OrderEditRequest editRequest =
                getEditRequestWithInstancesEditOnly(order,
                        new OrderItemInstance(cis1),
                        new OrderItemInstance(cis2),
                        new OrderItemInstance(cis4));
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);
        updateChangeRequest(order, changeRequest.getId(), false);

        Order actualOrder = orderService.getOrder(order.getId());
        OrderItem itemWithCis = filterItemsBy(actualOrder.getItems(), item -> item.getInstances() != null);
        OrderItem itemWithoutCis = filterItemsBy(actualOrder.getItems(), item -> item.getInstances() == null);

        assertTrue(itemWithCis.getInstances().toString().contains("\"cis\":\"" + cis1 + "\""));
        assertTrue(itemWithCis.getInstances().toString().contains("\"cis\":\"" + cis2 + "\""));
        assertTrue(itemWithCis.getInstances().toString().contains("\"cis\":\"" + cis4 + "\""));
        assertFalse(itemWithCis.getInstances().toString().contains("\"balanceOrderId\":"));
        assertNull(itemWithoutCis.getInstances());
    }

    @Test
    @DisplayName("Удаление одной штучки товара из заказа с редактированием кизов и uit")
    void removeOneInstanceOfItemWithEditCisesAndUit() {
        //Создание заказа с двумя товарами. Один с товарными идентификаторами и требованием кизов. Другой обычный
        Order order = createOrderWithInstances(true,
                new OrderItemInstance(cis1, uit1, null),
                new OrderItemInstance(cis2, uit2, null),
                new OrderItemInstance(cis3, uit3, null));
        //запрос на удаление одной штучки из товара и замену товарных идентификаторов в нем
        OrderEditRequest editRequest = getEditRequestWithInstances(order,
                new OrderItemInstance(cis2, uit3, null),
                new OrderItemInstance(cis3, uit2, null));
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);
        updateChangeRequest(order, changeRequest.getId(), true);

        Order actualOrder = orderService.getOrder(order.getId());
        OrderItem itemWithCis = filterItemsBy(actualOrder.getItems(), item -> item.getInstances() != null);
        OrderItem itemWithoutCis = filterItemsBy(actualOrder.getItems(), item -> item.getInstances() == null);

        assertTrue(itemWithCis.getInstances().toString().contains("\"cis\":\"" + cis2 + "\""));
        assertTrue(itemWithCis.getInstances().toString().contains("\"cis\":\"" + cis3 + "\""));
        assertTrue(itemWithCis.getInstances().toString().contains("\"UIT\":\"" + uit3 + "\""));
        assertTrue(itemWithCis.getInstances().toString().contains("\"UIT\":\"" + uit2 + "\""));
        assertTrue(itemWithCis.getInstances().toString().contains("\"balanceOrderId\":"));
        assertNull(itemWithoutCis.getInstances());
    }

    @Test
    @DisplayName("Удаление одной штучки товара из заказа с редактированиемuit")
    void removeOneInstanceOfItemWithEditUit() {
        //Создание заказа с двумя товарами. Один с товарными идентификаторами и требованием кизов. Другой обычный
        Order order = createOrderWithInstances(false,
                new OrderItemInstance(null, uit1, null),
                new OrderItemInstance(null, uit2, null),
                new OrderItemInstance(null, uit3, null));
        //запрос на удаление одной штучки из товара и замену товарных идентификаторов в нем
        OrderEditRequest editRequest = getEditRequestWithInstances(order,
                new OrderItemInstance(null, uit3, null),
                new OrderItemInstance(null, uit2, null));
        ChangeRequest changeRequest = notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest);
        updateChangeRequest(order, changeRequest.getId(), true);

        Order actualOrder = orderService.getOrder(order.getId());
        OrderItem itemWithCis = filterItemsBy(actualOrder.getItems(), item -> item.getInstances() != null);
        OrderItem itemWithoutCis = filterItemsBy(actualOrder.getItems(), item -> item.getInstances() == null);

        assertTrue(itemWithCis.getInstances().toString().contains("\"UIT\":\"" + uit3 + "\""));
        assertTrue(itemWithCis.getInstances().toString().contains("\"UIT\":\"" + uit2 + "\""));
        assertFalse(itemWithCis.getInstances().toString().contains("\"balanceOrderId\":"));
        assertNull(itemWithoutCis.getInstances());
    }

    @DisplayName("Удаление item в order")
    @ParameterizedTest
    @MethodSource("checkRemoveAvailableSource")
    void removeItemForFBSInDeliveryStatusTest(DeliveryType deliveryType, OrderStatus orderStatus,
                                              ClientRole clientRole) {
        Order order = createOrderWithTwoItems(deliveryType, orderStatus, p ->
                p.getOrder().getItems()
                        .forEach(oi -> p.getReportParameters()
                                .overrideItemInfo(oi.getFeedOfferId()).getFulfilment().fulfilment = false));
        // этот order item был удален, его id будем проверять ниже
        OrderItem notFoundOrderItem = getItemWithMaxCount(order);
        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(order,
                HistoryEventReason.USER_REQUESTED_REMOVE);

        List<ChangeRequest> d = assertDoesNotThrow(() -> client.editOrder(order.getId(),
                clientRole, null, List.of(Color.BLUE), editRequest));

        Order rereadedOrder = orderService.getOrder(order.getId());

        OrderItem updatedItem = rereadedOrder.getItems().stream()
                .filter(it -> notFoundOrderItem.getId().equals(it.getId()))
                .findFirst().get();

        assertEquals(10, notFoundOrderItem.getCount());
        assertEquals(9, updatedItem.getCount());
    }

    private OrderHistoryEvent getOrdersItemUpdatedEvent(long orderId) throws Exception {
        return eventsGetHelper.getOrderHistoryEvents(orderId)
                .getItems().stream()
                .filter(event -> event.getType() == HistoryEventType.ITEMS_UPDATED)
                .findAny()
                .orElseThrow(NullPointerException::new);
    }

    private void validateItemsRemoval(@Nonnull Order orderBefore,
                                      @Nonnull Order orderAfter,
                                      @Nonnull OrderEditRequest editRequest) {
        validateItemsRemoval(orderBefore, orderAfter, editRequest, true);
    }

    private void validateItemsRemoval(@Nonnull Order orderBefore,
                                      @Nonnull Order orderAfter,
                                      @Nonnull OrderEditRequest editRequest,
                                      boolean needFreezedCheck) {
        checkParcelItemsMatchOrderItems(orderAfter);

        checkNoZeroItems(orderAfter);

        Map<Long, OrderItem> itemsBefore = orderBefore.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, i -> i));
        Map<Long, OrderItem> itemsAfter = orderAfter.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, i -> i));
        assertNotNull(editRequest.getMissingItemsNotification());
        Collection<ItemInfo> itemsNotification = editRequest.getMissingItemsNotification().getRemainedItems();

        checkItemsWereNotIncreased(itemsBefore, itemsAfter);

        // Проверка соответствия полностью удаленных айтемов
        Set<ItemInfo> expectedInfoWithTotallyItemRemoved = itemsNotification.stream()
                .filter(itemInfo -> itemInfo.getCount() == 0)
                .collect(Collectors.toSet());
        for (ItemInfo requestedInfo : expectedInfoWithTotallyItemRemoved) {
            OrderItemsException exception = Assertions.assertThrows(OrderItemsException.class,
                    () -> MissingItemsNotificationConverter.defineItemByInfoWithCheck(orderAfter, requestedInfo));
            assertEquals(exception.getCode(), ITEM_NOT_FOUND_CODE);
        }

        // Проверка соответствия частично удаленных айтемов
        Set<ItemInfo> expectedInfoWithPartiallyItemRemoved = itemsNotification.stream()
                .filter(itemInfo -> itemInfo.getCount() != 0)
                .collect(Collectors.toSet());
        for (ItemInfo requestedInfo : expectedInfoWithPartiallyItemRemoved) {
            OrderItem itemAfter = MissingItemsNotificationConverter.defineItemByInfoWithCheck(orderAfter,
                    requestedInfo);
            assertEquals(itemAfter.getCount(), requestedInfo.getCount());
        }

        // Проверяем консистентность данных о фризах
        if (needFreezedCheck && !orderAfter.isIgnoreStocks()) {
            itemsAfter.values().forEach(item -> assertEquals(item.getCount(), item.getFitFreezed()));
        }
    }
}
