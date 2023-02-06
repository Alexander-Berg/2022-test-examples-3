package ru.yandex.market.delivery.mdbapp.integration.service;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import steps.orderSteps.OrderEventSteps;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.cancel.result.CancellationResultEnqueueService;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.util.OrderEventUtils;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CancelOrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Обработчик чекаутерных событий перевода в статус CANCELLED с отменой заказа в LOM")
public class LomCancelOrderHandlerTest extends MockContextualTest {
    @Autowired
    private LomCancelOrderHandler lomCancelOrderHandler;

    @SpyBean
    private CancellationResultEnqueueService cancellationResultEnqueueService;

    @Autowired
    private FeatureProperties featureProperties;

    @Autowired
    private LomClient lomClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
            cancellationResultEnqueueService,
            lomClient
        );
    }

    @Test
    @DisplayName("Успешная отмена заказа в LOM")
    public void success() {
        Order order = getOrder(OrderStatus.CANCELLED);
        OrderSearchFilter filter = OrderSearchFilter.builder()
            .externalIds(Set.of(String.valueOf(order.getId())))
            .senderIds(Set.of(order.getShopId()))
            .build();
        when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(true)))
            .thenReturn(PageResult.of(List.of(getLomOrder()), 1, 1, 1));

        lomCancelOrderHandler.handle(getOrderHistoryEvent(OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND));

        verify(lomClient).cancelOrder(
            eq(123L),
            eq(CancelOrderDto.builder().reason(CancellationOrderReason.USER_CHANGED_MIND).build()),
            eq(true)
        );
        verify(cancellationResultEnqueueService, never()).enqueue(
            anyLong(),
            any(CancellationOrderStatus.class),
            any(CancellationOrderReason.class),
            eq(null)
        );
    }

    @Test
    @DisplayName("Успешная отмена стрельбового заказа в LOM")
    public void successTankEvent() {
        Order order = getOrder(OrderStatus.CANCELLED);
        OrderSearchFilter filter = OrderSearchFilter.builder()
            .externalIds(Set.of(String.valueOf(order.getId())))
            .senderIds(Set.of(order.getShopId()))
            .build();
        when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(true)))
            .thenReturn(PageResult.of(List.of(getLomOrder()), 1, 1, 1));
        var event = getOrderHistoryTankEvent(OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND);

        doNothing().when(cancellationResultEnqueueService).enqueue(
            eq(event.getOrderAfter().getId()),
            eq(CancellationOrderStatus.SUCCESS),
            eq(CancellationOrderReason.USER_CHANGED_MIND),
            eq(null)
        );

        lomCancelOrderHandler.handle(event);

        verify(lomClient).cancelOrder(
            eq(123L),
            eq(CancelOrderDto.builder().reason(CancellationOrderReason.USER_CHANGED_MIND).build()),
            eq(true)
        );
        verify(cancellationResultEnqueueService).enqueue(
            eq(123L),
            eq(CancellationOrderStatus.SUCCESS),
            eq(CancellationOrderReason.USER_CHANGED_MIND),
            eq(null)
        );
    }

    @Test
    @DisplayName("Заказ не найден в LOM")
    public void notFoundInLom() {
        Order order = getOrder(OrderStatus.CANCELLED);
        OrderSearchFilter filter = OrderSearchFilter.builder()
            .externalIds(Set.of(String.valueOf(order.getId())))
            .senderIds(Set.of(order.getShopId()))
            .build();
        when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(true)))
            .thenReturn(PageResult.of(List.of(), 0, 1, 1));

        lomCancelOrderHandler.handle(getOrderHistoryEvent(OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND));

        verify(lomClient, never()).cancelOrder(anyLong(), any(CancelOrderDto.class), anyBoolean());
    }

    @Test
    @DisplayName("Заказ не является отмененным в чекаутере")
    public void notCancelled() {
        lomCancelOrderHandler.handle(getOrderHistoryEvent(OrderStatus.PROCESSING, null));

        verify(lomClient, never()).searchOrders(any(OrderSearchFilter.class), anySet(), any(Pageable.class));
        verify(lomClient, never()).cancelOrder(anyLong(), any(CancelOrderDto.class), anyBoolean());
    }

    @Test
    @DisplayName("Фейковый заказ не отменяется в LOM")
    public void acceptFakeOrderEventsDisabled() {
        OrderHistoryEvent orderHistoryEvent = getOrderHistoryEvent(
            OrderStatus.CANCELLED,
            OrderSubstatus.USER_CHANGED_MIND
        );
        orderHistoryEvent.getOrderAfter().setFake(true);
        featureProperties.setAcceptFakeOrderEventsEnabled(false);
        lomCancelOrderHandler.handle(orderHistoryEvent);
        featureProperties.setAcceptFakeOrderEventsEnabled(true);
        verify(lomClient, never()).searchOrders(any(OrderSearchFilter.class), anySet(), any(Pageable.class));
        verify(lomClient, never()).cancelOrder(anyLong(), any(CancelOrderDto.class), anyBoolean());
    }

    @Test
    @DisplayName("Доставленный в LOM заказ не может быть отменен")
    public void notAbleToCancel() {
        Order order = getOrder(OrderStatus.CANCELLED);
        OrderSearchFilter filter = OrderSearchFilter.builder()
            .externalIds(Set.of(String.valueOf(order.getId())))
            .senderIds(Set.of(order.getShopId()))
            .build();
        when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(true)))
            .thenReturn(PageResult.of(List.of(getLomOrderDelivered()), 1, 1, 1));

        lomCancelOrderHandler.handle(getOrderHistoryEvent(OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND));

        verify(lomClient, never()).cancelOrder(anyLong(), any(CancelOrderDto.class), anyBoolean());
    }

    @Test
    @DisplayName("Запрос на отмену в LOM вернул 422 код ошибки")
    public void notAccepted() {
        Order order = getOrder(OrderStatus.CANCELLED);
        OrderSearchFilter filter = OrderSearchFilter.builder()
            .externalIds(Set.of(String.valueOf(order.getId())))
            .senderIds(Set.of(order.getShopId()))
            .build();
        when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(true)))
            .thenReturn(PageResult.of(List.of(getLomOrder()), 1, 1, 1));
        when(lomClient.cancelOrder(anyLong(), any(CancelOrderDto.class), anyBoolean()))
            .thenThrow(new HttpTemplateException(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Not accepted order cancel status: DELIVERED"
            ));

        lomCancelOrderHandler.handle(getOrderHistoryEvent(OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND));
    }

    @Nonnull
    private OrderHistoryEvent getOrderHistoryEvent(OrderStatus newOrderStatus, OrderSubstatus newOrderSubstatus) {
        Order orderBefore = getOrder(OrderStatus.PROCESSING);
        Order orderAfter = getOrder(newOrderStatus, newOrderSubstatus);
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setOrderBefore(orderBefore);
        event.setOrderAfter(orderAfter);
        return event;
    }

    @Nonnull
    private OrderHistoryEvent getOrderHistoryTankEvent(OrderStatus newOrderStatus, OrderSubstatus newOrderSubstatus) {
        var event = getOrderHistoryEvent(newOrderStatus, newOrderSubstatus);
        var buy = new Buyer(OrderEventUtils.TANK_UID);
        event.getOrderAfter().setBuyer(buy);
        return event;
    }

    @Nonnull
    private Order getOrder(OrderStatus orderStatus) {
        Order order = OrderEventSteps.buildBeruDropshipOrderWithCancelRequest(1L, 1L, 1L, 1L);
        order.setStatus(orderStatus);
        return order;
    }

    @Nonnull
    private Order getOrder(OrderStatus orderStatus, OrderSubstatus newOrderSubstatus) {
        Order order = getOrder(orderStatus);
        order.setSubstatus(newOrderSubstatus);
        return order;
    }

    @Nonnull
    private OrderDto getLomOrderDelivered() {
        return new OrderDto()
            .setId(123L)
            .setStatus(ru.yandex.market.logistics.lom.model.enums.OrderStatus.DELIVERED);
    }

    @Nonnull
    private OrderDto getLomOrder() {
        return new OrderDto()
            .setId(123L)
            .setStatus(ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING);
    }

}
