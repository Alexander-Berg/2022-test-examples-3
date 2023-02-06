package ru.yandex.market.checkout.checkouter.order.item;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.CannotRemoveItemException;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.storage.changerequest.ChangeRequestDao;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.ITEMS_NOT_FOUND;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.USER_REQUESTED_REMOVE;

class MissingItemsNotificationTest extends MissingItemsAbstractTest {

    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;

    @Autowired
    private ChangeRequestDao changeRequestDao;

    static Stream<Arguments> getEditRequestsForValidation() {
        return Stream.of(
                Arguments.of("Valid request", true, (Function<Order, OrderEditRequest>)
                        MissingItemsNotificationTest::getEditRequestWithManyMissingUnits),
                Arguments.of("Valid request with reason", true, (Function<Order, OrderEditRequest>)
                        MissingItemsAbstractTest::getEditRequestWithValidReason),
                Arguments.of("Invalid request: Exceeding item count", false, (Function<Order, OrderEditRequest>)
                        MissingItemsNotificationTest::getEditRequestWithExceedingCount),
                Arguments.of("Invalid request: Negative item count", false, (Function<Order, OrderEditRequest>)
                        MissingItemsNotificationTest::getEditRequestWithNegativeItemCount),
                Arguments.of("Invalid request: Non existing item", false, (Function<Order, OrderEditRequest>)
                        MissingItemsNotificationTest::getEditRequestWithNonExistingItem),
                Arguments.of("Invalid request: Ambiguous item info", false, (Function<Order, OrderEditRequest>)
                        MissingItemsNotificationTest::getEditRequestWithAmbiguousItemInfo),
                Arguments.of("Invalid request: Invalid reason", false, (Function<Order, OrderEditRequest>)
                        MissingItemsNotificationTest::getEditRequestWithWrongReason)
        );
    }

    // Склад не стал удалять недостающие товары и прекратил сборку заказа
    @Nonnull
    private static OrderEditRequest getEditRequestWhenWarehouseStoppedProcessing(@Nonnull Order order) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(false, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                toItemInfo(maxCountItem, maxCountItem.getCount() - 1)
        ), ITEMS_NOT_FOUND));
        return editRequest;
    }

    // Нехватка товаров > 20% заказа, не подходит под условия удаления
    @Nonnull
    private static OrderEditRequest getEditRequestWithManyMissingUnits(@Nonnull Order order) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                toItemInfo(maxCountItem, maxCountItem.getCount() / 2)
        ), ITEMS_NOT_FOUND));
        return editRequest;
    }

    // Нехватка товаров > 20% заказа, не подходит под условия удаления, но отменять нельзя.
    @Nonnull
    private static OrderEditRequest getEditRequestWithManyMissingUnitsWithCancelDisabled(@Nonnull Order order) {
        return getEditRequestWithManyMissingUnitsWithCancelDisabled(order, ITEMS_NOT_FOUND);
    }

    // Нехватка товаров > 20% заказа, не подходит под условия удаления, но отменять нельзя.
    @Nonnull
    private static OrderEditRequest getEditRequestWithManyMissingUnitsWithCancelDisabled(
            @Nonnull Order order,
            HistoryEventReason reason) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                toItemInfo(maxCountItem, maxCountItem.getCount() / 2)
        ), reason, true));
        return editRequest;
    }

    // Товаров осталось больше чем было в заказе изначально. Запрос невалидный
    @Nonnull
    private static OrderEditRequest getEditRequestWithExceedingCount(@Nonnull Order order) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                toItemInfo(maxCountItem, maxCountItem.getCount() + 1)
        ), ITEMS_NOT_FOUND));
        return editRequest;
    }

    // Отрицательное количество товаров в запросе. Запрос невалидный
    @Nonnull
    private static OrderEditRequest getEditRequestWithNegativeItemCount(@Nonnull Order order) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                buildItemInfoWithNegativeCount(maxCountItem)
        ), ITEMS_NOT_FOUND));
        return editRequest;
    }

    private static ItemInfo buildItemInfoWithNegativeCount(OrderItem maxCountItem) {
        ItemInfo infoWithNegativeCount = toItemInfo(maxCountItem, 0);
        Field countField;
        try {
            countField = ItemInfo.class.getDeclaredField("count");
            countField.setAccessible(true);
            ReflectionUtils.setField(countField, infoWithNegativeCount, -1);
            countField.setAccessible(false);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return infoWithNegativeCount;
    }

    // В запросе несколько элементов для одного айтема. Запрос невалидный
    //todo нужно пофиксить тест кейсы, при нескольких корректных ItemInfo - запрос не падает
    @Nonnull
    private static OrderEditRequest getEditRequestWithAmbiguousItemInfo(@Nonnull Order order) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                buildItemInfoWithNegativeCount(maxCountItem),
                buildItemInfoWithNegativeCount(maxCountItem)
        ), ITEMS_NOT_FOUND));
        return editRequest;
    }

    // В запросе не валидная причина удаления
    @Nonnull
    private static OrderEditRequest getEditRequestWithWrongReason(@Nonnull Order order) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                toItemInfo(maxCountItem, maxCountItem.getCount() - 1)
        ), HistoryEventReason.USER_MOVED_DELIVERY_DATES));
        return editRequest;
    }

    // Лишний айтем в запросе. Запрос невалидный
    @Nonnull
    private static OrderEditRequest getEditRequestWithNonExistingItem(@Nonnull Order order) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, minCountItem.getCount()),
                toItemInfo(maxCountItem, maxCountItem.getCount()),
                new ItemInfo(111, "111", 1)
        ), ITEMS_NOT_FOUND));
        return editRequest;
    }

    // Одной позиции нет полностью, подходит под условия удаления
    @Nonnull
    private static OrderEditRequest getEditRequestWhereOneItemIsTotallyMissed(@Nonnull Order order,
                                                                              boolean cancelDisabled) {
        OrderItem maxCountItem = getItemWithMaxCount(order);
        OrderItem minCountItem = getItemWithMinCount(order);
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(new MissingItemsNotification(true, List.of(
                toItemInfo(minCountItem, 0),
                toItemInfo(maxCountItem, maxCountItem.getCount())
        ), ITEMS_NOT_FOUND, cancelDisabled));
        return editRequest;
    }

    @Test
    void noChangeRequestCreatedIfNothingChanged() {
        Order order = createOrderWithTwoItems();

        OrderEditRequest editRequest = getEditRequestWhereNothingChanged(order, true);
        client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest);

        assertNoChangeRequests(order.getId());
    }

    @Test
    void itemsRemovalRequestCreatedIfRemovalAllowed() {
        Order order = createOrderWithTwoItems();

        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(order);
        client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest);

        assertOrderHasItemsRemovalRequest(order.getId());
    }

    @Test
    void itemsRemovalRequestCreatedIfOneItemIsTotallyMissed() {
        Order order = createOrderWithTwoItems();

        OrderEditRequest editRequest = getEditRequestWhereOneItemIsTotallyMissed(order, false);
        client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest);

        assertOrderHasItemsRemovalRequest(order.getId());
    }

    @Test
    void cancellationRequestCreatedIfTooManyMissing() {
        Order order = createOrderWithTwoItems();

        OrderEditRequest editRequest = getEditRequestWithManyMissingUnits(order);
        client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest);

        assertOrderHasCancellationRequest(order.getId());
    }

    @Test
    void exceptionIfTooManyMissingAndCancelDisabled() {
        Order order = createOrderWithTwoItems();
        OrderEditRequest editRequest = getEditRequestWithManyMissingUnitsWithCancelDisabled(order);

        ErrorCodeException exception = Assertions.assertThrows(ErrorCodeException.class,
                () -> client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest));
        assertEquals(exception.getCode(), CannotRemoveItemException.DELETION_AMOUNT_EXCEEDS_THRESHOLD_CODE);
    }

    @Test
    void exceptionIfOrderInForbiddenStateToRemove() {
        Order order = createOrderWithTwoItems();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        OrderEditRequest editRequest =
                getEditRequestWithManyMissingUnitsWithCancelDisabled(order, USER_REQUESTED_REMOVE);

        ErrorCodeException exception = Assertions.assertThrows(ErrorCodeException.class,
                () -> client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest));
        assertEquals(exception.getCode(), OrderStatusNotAllowedException.NOT_ALLOWED_CODE);
    }

    @Test
    void exceptionIfOrderCancellationRequestedAndCancelDisabled() throws Exception {
        Order order = createOrderWithTwoItems();
        OrderEditRequest editRequest = getEditRequestWhereOneItemIsTotallyMissed(order, true);
        Order request = cancellationRequestHelper.createCancellationRequest(order.getId(),
                new CancellationRequest(OrderSubstatus.USER_CHANGED_MIND, ""),
                new ClientInfo(ClientRole.USER, BuyerProvider.UID));

        List<ChangeRequest> requestDaoByOrder = changeRequestDao.findByOrder(request);
        ChangeRequest cancelChangeRequest =
                requestDaoByOrder.stream().filter(it -> it.getType() == ChangeRequestType.CANCELLATION)
                .findFirst().get();

        boolean success = client.updateChangeRequestStatus(
                order.getId(),
                cancelChangeRequest.getId(),
                ClientRole.SYSTEM,
                null,
                new ChangeRequestPatchRequest(ChangeRequestStatus.APPLIED, null, null)
        );

        assertTrue(success);

        ErrorCodeException exception = Assertions.assertThrows(ErrorCodeException.class,
                () -> client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest));
        assertEquals(exception.getCode(), CannotRemoveItemException.NOT_ALLOWED_REASON);
    }

    @Test
    void cancellationRequestCreatedIfWarehouseStoppedProcessing() {
        Order order = createOrderWithTwoItems();

        OrderEditRequest editRequest = getEditRequestWhenWarehouseStoppedProcessing(order);
        client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest);

        assertOrderHasCancellationRequest(order.getId());
    }

    @Test
    void cancellationRequestCreatedIfNoItemsChangeButWarehouseStoppedProcessing() {
        Order order = createOrderWithTwoItems();

        OrderEditRequest editRequest = getEditRequestWhereNothingChanged(order, false);
        client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest);
        assertOrderHasCancellationRequest(order.getId());
    }

    @Test
    void cancellationRequestCreatedIfRemovalIsDisabled() {
        Order order = createOrderWithTwoItems();

        checkouterProperties.setItemsRemoveAllow(false);

        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(order);
        client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest);

        assertOrderHasCancellationRequest(order.getId());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getEditRequestsForValidation")
    void orderEditRequestValidationTest(String caseName,
                                        boolean isRequestValid,
                                        Function<Order, OrderEditRequest> editRequestProvider) {
        Order order = createOrderWithTwoItems();

        OrderEditRequest editRequest = editRequestProvider.apply(order);

        if (isRequestValid) {
            client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest);
        } else {
            assertEquals(400, assertThrows(ErrorCodeException.class,
                    () -> client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest))
                    .getStatusCode()
            );
            assertNoChangeRequests(order.getId());
        }
    }

    @Test
    void clientErrorWhenOrderHasWrongStatus() {
        Order order = createOrderWithTwoItems();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(order);
        assertEquals(400, assertThrows(ErrorCodeException.class,
                () -> client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest))
                .getStatusCode()
        );

        assertNoChangeRequests(order.getId());
    }

    private void assertNoChangeRequests(long orderId) {
        Order order = getLastEvent(orderId).getOrderAfter();
        assertNull(order.getCancellationRequest());
        assertThat(order.getChangeRequests(), Matchers.empty());
    }

    private void assertOrderHasItemsRemovalRequest(long orderId) {
        Order order = getLastEvent(orderId).getOrderAfter();
        assertNull(order.getCancellationRequest());
        assertTrue(order.getChangeRequests().stream().map(ChangeRequest::getType).noneMatch(type ->
                type == ChangeRequestType.PARCEL_CANCELLATION || type == ChangeRequestType.CANCELLATION));
        assertEquals(1, order.getChangeRequests().stream()
                .filter(cr -> cr.getType() == ChangeRequestType.ITEMS_REMOVAL)
                .count());
    }
}
