package ru.yandex.market.checkout.checkouter.order.item;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstances;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstancesPutRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.order.changerequest.parcel.ParcelCancelChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemRemovalPermission;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.OrderItemsRemovalPermissionResponse;
import ru.yandex.market.checkout.checkouter.order.itemsremoval.ReasonForNotAbleRemoveItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.common.report.model.json.common.Region;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.ITEMS_NOT_FOUND;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.postpaidBlueOrderParameters;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;

abstract class MissingItemsAbstractTest extends AbstractWebTestBase {

    static final BigDecimal MAX_REMOVABLE_PERCENT = new BigDecimal(20);
    protected static ObjectMapper mapper = new ObjectMapper();
    @Autowired
    protected OrderStatusHelper orderStatusHelper;
    @Autowired
    private CheckouterOrderHistoryEventsApi orderHistoryEventsClient;
    @Autowired
    private MissingItemsRemovalService removalService;
    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    static void checkOrderAllowed(@Nonnull OrderItemsRemovalPermissionResponse removalPermission,
                                  @Nonnull Order order,
                                  boolean isAllowedForOrder) {
        assertEquals(order.getId(), removalPermission.getOrderId());
        assertEquals(isAllowedForOrder, removalPermission.isRemovalAllowed());
        assertThat(MAX_REMOVABLE_PERCENT, Matchers.comparesEqualTo(removalPermission.getMaxTotalPercentRemovable()));
        if (!isAllowedForOrder) {
            removalPermission.getItemRemovalPermissions()
                    .forEach(ip -> assertFalse(ip.isRemovalAllowed()));
            removalPermission.getItemRemovalPermissions()
                    .forEach(ip ->
                            assertTrue(ip.getReasons().contains(ReasonForNotAbleRemoveItem.NOT_ALLOWED_BY_ORDER)));
        }
    }

    // Ничего не поменялось в заказе
    @Nonnull
    static OrderEditRequest getEditRequestWhereNothingChanged(@Nonnull Order order, boolean alreadyRemovedByWarehouse) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(alreadyRemovedByWarehouse, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                toItemInfo(maxCountItem, maxCountItem.getCount())
        ), ITEMS_NOT_FOUND));
        return editRequest;
    }

    // Не изменилось количество товаров, но есть изменение кизов
    @Nonnull
    static OrderEditRequest getEditRequestWithInstancesEditOnly(
            @Nonnull Order order,
            OrderItemInstance... itemInstances
    ) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                toItemInfoWithInstances(maxCountItem, maxCountItem.getCount(), Set.of(itemInstances))
        ), HistoryEventReason.ITEMS_NOT_FOUND));
        return editRequest;
    }

    // Нехватка товаров < 20% заказа, подходит под условия удаления
    @Nonnull
    static OrderEditRequest getEditRequestWithOneMissingUnit(@Nonnull Order order, HistoryEventReason reason) {
        return getEditRequestWithOneMissingUnit(order, false, reason);
    }

    // Нехватка товаров < 20% заказа, подходит под условия удаления
    @Nonnull
    static OrderEditRequest getEditRequestWithOneMissingUnit(@Nonnull Order order) {
        return getEditRequestWithOneMissingUnit(order, false, ITEMS_NOT_FOUND);
    }

    // Нехватка товаров < 20% заказа, подходит под условия удаления
    @Nonnull
    static OrderEditRequest getEditRequestWithOneMissingUnitByItemId(@Nonnull Order order) {
        return getEditRequestWithOneMissingUnit(order, true, ITEMS_NOT_FOUND);
    }

    // Нехватка товаров < 20% заказа, подходит под условия удаления
    @Nonnull
    private static OrderEditRequest getEditRequestWithOneMissingUnit(@Nonnull Order order,
                                                                     boolean byItemId,
                                                                     HistoryEventReason reason) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        List<ItemInfo> itemInfos = new ArrayList<>();
        itemInfos.add(toItemInfo(maxCountItem, maxCountItem.getCount() - 1, byItemId));
        if (maxCountItem != minCountItem) {
            itemInfos.add(toItemInfo(minCountItem, minCountItem.getCount(), byItemId));
        }
        boolean cancelDisabled = !(Boolean.TRUE.equals(order.isFulfilment())
                && order.getStatus().equals(OrderStatus.PROCESSING));
        editRequest.setMissingItemsNotification(
                new MissingItemsNotification(true, itemInfos, reason, cancelDisabled));
        return editRequest;
    }

    // Одна позиция полностью отсутствует. Нехватка товаров < 20% заказа, подходит под условия удаления
    @Nonnull
    static OrderEditRequest getEditRequestWithOneTotallyMissingItem(@Nonnull Order order) {
        return getEditRequestWithOneTotallyMissingItem(order, false);
    }

    // Одна позиция полностью отсутствует. Нехватка товаров < 20% заказа, подходит под условия удаления
    @Nonnull
    static OrderEditRequest getEditRequestWithOneTotallyMissingItemByItemId(@Nonnull Order order) {
        return getEditRequestWithOneTotallyMissingItem(order, true);
    }

    // Одна позиция полностью отсутствует. Нехватка товаров < 20% заказа, подходит под условия удаления
    @Nonnull
    private static OrderEditRequest getEditRequestWithOneTotallyMissingItem(@Nonnull Order order, boolean byItemId) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, 0, byItemId),
                toItemInfo(maxCountItem, maxCountItem.getCount(), byItemId)
        ), ITEMS_NOT_FOUND));
        return editRequest;
    }

    // С валидной причиной
    @Nonnull
    static OrderEditRequest getEditRequestWithValidReason(@Nonnull Order order) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                toItemInfo(maxCountItem, maxCountItem.getCount() - 1)
        ), HistoryEventReason.ITEMS_NOT_FOUND));
        return editRequest;
    }

    @Nonnull
    static OrderEditRequest getEditRequestWithInstances(@Nonnull Order order, OrderItemInstance... itemInstances) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                toItemInfoWithInstances(maxCountItem, maxCountItem.getCount() - 1, Set.of(itemInstances))
        ), HistoryEventReason.ITEMS_NOT_FOUND));
        return editRequest;
    }

    @Nonnull
    static ItemInfo toItemInfo(@Nonnull OrderItem orderItem, int remainedCount) {
        return toItemInfo(orderItem, remainedCount, false);
    }

    @Nonnull
    static ItemInfo toItemInfo(@Nonnull OrderItem orderItem, int remainedCount, boolean byItemId) {
        if (byItemId) {
            return new ItemInfo(orderItem.getId(), remainedCount, new HashSet<>());
        }
        return new ItemInfo(orderItem.getSupplierId(), orderItem.getShopSku(), remainedCount);
    }

    @Nonnull
    static ItemInfo toItemInfoWithInstances(@Nonnull OrderItem orderItem,
                                            int remainedCount,
                                            Set<OrderItemInstance> itemInstances) {
        return new ItemInfo(orderItem.getId(), orderItem.getSupplierId(),
                orderItem.getShopSku(), remainedCount, itemInstances);
    }

    @Nonnull
    static OrderItem getItemWithMaxCount(@Nonnull Order order) {
        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException();
        }
        return order.getItems().stream().max(Comparator.comparingInt(OfferItem::getCount)).get();
    }

    @Nonnull
    static OrderItem getItemWithMinCount(@Nonnull Order order) {
        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException();
        }
        return order.getItems().stream().min(Comparator.comparingInt(OfferItem::getCount)).get();
    }

    @BeforeEach
    final void setup() {
        checkouterProperties.setItemsRemoveAllow(true);
        checkouterProperties.setEnableInstallments(true);
        stockStorageConfigurer.mockOkForRefreeze();
    }

    @AfterEach
    final void cleanMocks() {
        stockStorageConfigurer.resetMappings();
        stockStorageConfigurer.resetRequests();
    }

    void updateChangeRequest(Order order, long changeRequestId, boolean checkStockRefreeze) {
        reportMock.resetRequests();

        if (checkStockRefreeze) {
            stockStorageConfigurer.resetRequests();
        }

        boolean success = client.updateChangeRequestStatus(
                order.getId(), changeRequestId, ClientRole.SYSTEM, null, new ChangeRequestPatchRequest(
                        ChangeRequestStatus.APPLIED, null, null
                )
        );
        assertTrue(success);

        // Проверяем, что при редактировании парселов не производилась актуализация в репорте
        reportMock.verify(0, anyRequestedFor(anyUrl()));

        if (checkStockRefreeze) {
            // Не знаю зачем, но зачем-то нам нужен рефриз
            queuedCallsHelper.runItemsRefreezeQCProcessor(order.getId(), order.getItems());
        }
    }

    void checkAllItemsAllowed(@Nonnull Order order, boolean isAllowedForOrder) {
        OrderItemsRemovalPermissionResponse removalPermission = getItemsRemovalPermission(order.getId());
        checkOrderAllowed(removalPermission, order, isAllowedForOrder);
        Set<Long> gotItemIds = removalPermission.getItemRemovalPermissions().stream()
                .peek(ip -> assertTrue(ip.isRemovalAllowed()))
                .map(OrderItemRemovalPermission::getItemId)
                .collect(Collectors.toSet());
        assertEquals(
                order.getItems().stream().map(OrderItem::getId).collect(Collectors.toSet()),
                gotItemIds
        );
    }

    ChangeRequest notifyMissingItemsAndExpectItemsRemoval(long orderId, @Nonnull OrderEditRequest editRequest) {
        return notifyMissingItemsAndExpectItemsRemoval(orderId, editRequest, ClientRole.SYSTEM);
    }

    ChangeRequest notifyMissingItemsAndExpectItemsRemoval(
            long orderId, @Nonnull OrderEditRequest editRequest, ClientRole role) {
        List<ChangeRequest> changeRequests = client
                .editOrder(orderId, role, null, List.of(Color.BLUE), editRequest);

        assertEquals(1, changeRequests.size());
        ChangeRequest changeRequest = changeRequests.get(0);
        assertEquals(ChangeRequestType.ITEMS_REMOVAL, changeRequest.getType());
        return changeRequest;
    }

    @Nonnull
    ChangeRequest notifyMissingItemsAndExpectOrderCancellation(long orderId, @Nonnull OrderEditRequest editRequest) {
        List<ChangeRequest> changeRequests = client
                .editOrder(orderId, ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest);

        MatcherAssert.assertThat(
                changeRequests.stream().map(ChangeRequest::getType).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(ChangeRequestType.CANCELLATION, ChangeRequestType.PARCEL_CANCELLATION)
        );

        return changeRequests.stream()
                .filter(cr -> cr.getType() == ChangeRequestType.PARCEL_CANCELLATION)
                .findAny()
                .orElseThrow();
    }

    @Nonnull
    OrderItemsRemovalPermissionResponse getItemsRemovalPermission(long orderId) {
        OrderItemsRemovalPermissionResponse fromService = removalService.getItemsRemovalPermission(orderId);
        OrderItemsRemovalPermissionResponse fromClient = client.getOrderItemsRemovalPermissions(orderId);
        assertEquals(fromClient, fromService);
        return fromClient;
    }

    @Nonnull
    Order createOrderWithTwoItems() {
        return createOrderWithTwoItems(PaymentMethod.CARD_ON_DELIVERY, DeliveryType.DELIVERY);
    }

    @Nonnull
    Order createOrderWithTwoItems(DeliveryType deliveryType, OrderStatus status) {
        return createOrderWithTwoItems(deliveryType, status, p -> {
        });
    }

    @Nonnull
    Order createOrderWithTwoItems(DeliveryType deliveryType, OrderStatus status, Consumer<Parameters> consumer) {
        Order order = createOrderWithTwoItems(PaymentMethod.CARD_ON_DELIVERY, deliveryType, consumer);
        if (!order.getStatus().equals(status)) {
            return orderStatusHelper.proceedOrderToStatus(order, status);
        }
        return order;
    }

    @Nonnull
    Order createOrderWithTwoItems(PaymentMethod paymentMethod, DeliveryType type, Consumer<Parameters> consumer) {
        Parameters params = paymentMethod.getPaymentType() == PaymentType.POSTPAID ?
                postpaidBlueOrderParameters() :
                prepaidBlueOrderParameters();
        params.setPaymentMethod(paymentMethod);
        params.setShowCredits(true);
        params.setShowInstallments(true);
        params.addOtherItem(10);
        params.getItems().forEach(item -> item.setManufacturerCountries(List.of(new Region())));
        if (type == DeliveryType.PICKUP) {
            params.setDeliveryType(type);
        }

        consumer.accept(params);
        return orderCreateHelper.createOrder(params);
    }

    @Nonnull
    Order createOrderWithTwoItems(PaymentMethod paymentMethod, DeliveryType type) {
        return createOrderWithTwoItems(paymentMethod, type, p -> {
        });
    }

    @Nonnull
    // Создает заказ с двумя товарами. В один из товаров добавляем товарные идентификаторы
    protected Order createOrderWithInstances(boolean isCisesRequired, OrderItemInstance... itemInstances) {
        Parameters parameters = postpaidBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        parameters.addOtherItem(3);
        OrderItem otherItem = filterItemsBy(parameters.getItems(), item -> item.getShopSku().equals("sku-3"));
        if (isCisesRequired) {
            otherItem.setCargoTypes(Set.of(CIS_REQUIRED_CARGOTYPE_CODE));
        }
        otherItem.setPrice(new BigDecimal("10"));
        otherItem.setBuyerPrice(new BigDecimal("10"));
        Order insertedOrder = orderCreateHelper.createOrder(parameters);
        otherItem = filterItemsBy(insertedOrder.getItems(), item -> item.getShopSku().equals("sku-3"));
        client.putOrderItemInstances(insertedOrder.getId(), ClientRole.SYSTEM, 0L,
                new OrderItemInstancesPutRequest(List.of(new OrderItemInstances(otherItem.getId(),
                        List.of(itemInstances)))));
        return orderService.getOrder(insertedOrder.getId());
    }

    protected OrderItem filterItemsBy(Collection<OrderItem> items, Predicate<OrderItem> predicate) {
        List<OrderItem> foundedItems = items.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        assertEquals(foundedItems.size(), 1);
        return foundedItems.get(0);
    }

    void assertOrderHasCancellationRequest(long orderId) {
        Order order = getLastEvent(orderId).getOrderAfter();
        assertNotNull(order.getCancellationRequest());
        assertEquals(OrderSubstatus.MISSING_ITEM, order.getCancellationRequest().getSubstatus());
        assertEquals(1, order.getChangeRequests().stream()
                .filter(cr -> cr.getType() == ChangeRequestType.CANCELLATION)
                .map(cr -> (CancellationRequestPayload) cr.getPayload())
                .filter(payload -> payload.getSubstatus() == OrderSubstatus.MISSING_ITEM)
                .count());
        assertEquals(1, order.getChangeRequests().stream()
                .filter(cr -> cr.getType() == ChangeRequestType.PARCEL_CANCELLATION)
                .map(cr -> (ParcelCancelChangeRequestPayload) cr.getPayload())
                .filter(payload -> payload.getSubstatus() == OrderSubstatus.MISSING_ITEM)
                .count());
    }

    @Nullable
    OrderHistoryEvent getLastEvent(long orderId) {
        OrderFilter orderFilter = new OrderFilter();
        orderFilter.setRgb(new Color[]{Color.BLUE});
        return orderHistoryEventsClient.getOrderHistoryEvents(
                0, 100, null, false, null,
                orderFilter, ClientRole.SYSTEM, null, null, Set.of(OptionalOrderPart.CHANGE_REQUEST)
        ).getContent().stream()
                .filter(event -> event.getOrderAfter().getId() == orderId)
                .max(Comparator.comparingLong(OrderHistoryEvent::getId))
                .orElse(null);
    }
}
