package ru.yandex.market.delivery.mdbapp.components.queue.parcel.cancel;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import steps.orderSteps.OrderEventSteps;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.exception.ChangeRequestException;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CancelOrderDto;
import ru.yandex.market.logistics.lom.model.dto.CancellationOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Отмены заказа в LOM")
public class CancelLomOrderTest extends AllMockContextualTest {

    private static final Long ORDER_ID = 1L;
    private static final Long PARCEL_ID = 100L;
    private static final Long SHOP_ID = 1000L;
    private static final long LOM_ORDER_ID = 42L;
    public static final long UPDATE_REQUEST_ID = 155L;

    @Autowired
    private ChangeRequestCancelParcelConsumer consumer;
    @Autowired
    private LomClient lomClient;

    @MockBean
    private CheckouterOrderService checkouterOrderService;

    @Test
    @DisplayName("Успешный сценарий отмены заказа в LOM")
    void cancelOrder() {
        mockSearchOrders(List.of(new OrderDto().setId(LOM_ORDER_ID)));

        TaskExecutionResult result = execCancel();

        assertEquals(TaskExecutionResult.finish(), result);
        verify(checkouterOrderService).processChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);
        verify(lomClient).cancelOrder(
            LOM_ORDER_ID,
            CancelOrderDto.builder().reason(CancellationOrderReason.CUSTOM).build(),
            true
        );
    }

    @Test
    @DisplayName("Успешный сценарий отмены заказа в LOM. Сабстатус DELIVERY_SERIVCE_UNDELIVERED")
    void cancelOrderSerivce() {
        mockSearchOrders(List.of(new OrderDto().setId(LOM_ORDER_ID)), OrderSubstatus.DELIVERY_SERIVCE_UNDELIVERED);

        TaskExecutionResult result = execCancel();

        assertEquals(TaskExecutionResult.finish(), result);
        verify(checkouterOrderService).processChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);
        verify(lomClient).cancelOrder(
            LOM_ORDER_ID,
            CancelOrderDto.builder().reason(CancellationOrderReason.DELIVERY_SERVICE_UNDELIVERED).build(),
            true
        );
    }

    @Test
    @DisplayName("Успешный сценарий отмены заказа в LOM при наличии дублей")
    void cancelOrderWithDuplicates() {
        mockSearchOrders(List.of(
            new OrderDto().setId(LOM_ORDER_ID),
            new OrderDto().setId(LOM_ORDER_ID).setStatus(ru.yandex.market.logistics.lom.model.enums.OrderStatus.DRAFT),
            new OrderDto()
                .setId(LOM_ORDER_ID)
                .setStatus(ru.yandex.market.logistics.lom.model.enums.OrderStatus.CANCELLED)
        ));

        TaskExecutionResult result = execCancel();

        assertEquals(TaskExecutionResult.finish(), result);
        verify(checkouterOrderService).processChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);
        verify(lomClient).cancelOrder(
            LOM_ORDER_ID,
            CancelOrderDto.builder().reason(CancellationOrderReason.CUSTOM).build(),
            true
        );
    }

    @Test
    @DisplayName("Ошибка если для заказа чекаутера найдено более одного заказа в LOM")
    public void cancelOrderTwoOrderFound() {
        mockSearchOrders(List.of(
            new OrderDto().setId(LOM_ORDER_ID),
            new OrderDto().setId(LOM_ORDER_ID + 1)
        ));

        TaskExecutionResult result = execCancel();

        assertEquals(TaskExecutionResult.fail(), result);
        verify(checkouterOrderService, times(1)).processChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);
        verify(lomClient, never()).cancelOrder(
            anyLong(),
            any(CancelOrderDto.class),
            anyBoolean()
        );
    }

    @Test
    @DisplayName("Ошибка если для заказа чекаутера не найдено ни одного заказа в LOM")
    void cancelOrderNotFound() {
        mockSearchOrders(List.of());

        TaskExecutionResult result = execCancel();

        assertEquals(TaskExecutionResult.fail(), result);
        verify(checkouterOrderService, times(1)).processChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);
        verify(lomClient, never()).cancelOrder(
            anyLong(),
            any(CancelOrderDto.class),
            anyBoolean()
        );
    }

    @Test
    @DisplayName("Заказ уже отменен")
    void cancelOrderIsCancelled() {
        Order order = OrderEventSteps.buildBeruDropshipOrderWithCancelRequest(
            ORDER_ID,
            SHOP_ID,
            PARCEL_ID,
            UPDATE_REQUEST_ID
        );
        order.setStatus(OrderStatus.CANCELLED);
        when(checkouterOrderService.getOrderWithChangeRequests(ORDER_ID))
            .thenReturn(order);

        TaskExecutionResult result = execCancel();

        assertEquals(TaskExecutionResult.finish(), result);
        verifyZeroInteractions(lomClient);
    }

    @Test
    @DisplayName("Заказ уже доставлен")
    void cancelOrderIsDelivered() {
        mockSearchOrders(List.of(
            new OrderDto()
                .setId(LOM_ORDER_ID)
                .setStatus(ru.yandex.market.logistics.lom.model.enums.OrderStatus.DELIVERED)
        ));
        TaskExecutionResult result = execCancel();

        assertEquals(TaskExecutionResult.finish(), result);
        verify(lomClient, never()).cancelOrder(
            anyLong(),
            any(CancelOrderDto.class),
            anyBoolean()
        );
        verify(checkouterOrderService).processChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);
        verify(checkouterOrderService).rejectChangeRequest(
            ORDER_ID,
            UPDATE_REQUEST_ID,
            "Order is already delivered"
        );
    }

    @Test
    @DisplayName("Заказ уже отменен")
    void cancelOrderAlreadyCancelled() {
        mockSearchOrders(List.of(
            new OrderDto()
                .setId(LOM_ORDER_ID)
                .setCancellationOrderRequests(List.of(
                    CancellationOrderRequestDto.builder()
                        .status(CancellationOrderStatus.SUCCESS)
                        .build()
                ))
        ));
        TaskExecutionResult result = execCancel();

        assertEquals(TaskExecutionResult.finish(), result);
        verify(lomClient, never()).cancelOrder(
            anyLong(),
            any(CancelOrderDto.class),
            anyBoolean()
        );
        verify(checkouterOrderService).processChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);
        verify(checkouterOrderService).applyChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);
    }

    @Test
    @DisplayName("ChangeRequestException от чекаутера завершает задачу")
    void errorFromCheckouter() {
        mockSearchOrders(List.of(new OrderDto().setId(LOM_ORDER_ID)));
        doThrow(new ChangeRequestException("Invalid state"))
            .when(checkouterOrderService).processChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);

        TaskExecutionResult result = execCancel();

        assertEquals(TaskExecutionResult.finish(), result);
        verify(lomClient, never()).cancelOrder(
            LOM_ORDER_ID,
            CancelOrderDto.builder().reason(CancellationOrderReason.UNKNOWN).build(),
            true
        );
    }

    @Test
    @DisplayName("Посылка с ошибками, если заказ в ломе отсутствует")
    void cancelOrderParcelError() {
        Order order = OrderEventSteps.buildBeruDropshipOrderWithCancelRequest(
            ORDER_ID,
            SHOP_ID,
            PARCEL_ID,
            UPDATE_REQUEST_ID
        );
        order.getDelivery().getParcels().clear();
        Parcel parcel = new Parcel();
        parcel.setId(PARCEL_ID);
        parcel.setStatus(ParcelStatus.ERROR);
        order.getDelivery().addParcel(parcel);
        when(checkouterOrderService.getOrderWithChangeRequests(ORDER_ID))
            .thenReturn(order);
        mockSearchLomOrders(List.of());

        TaskExecutionResult result = execCancel();

        verify(checkouterOrderService).processChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);
        verify(checkouterOrderService).applyChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);

        assertEquals(TaskExecutionResult.finish(), result);
    }

    @Test
    @DisplayName("Посылка с ошибками, но в LOM есть заказ")
    void cancelOrderParcelErrorOrderPresent() {
        Order order = OrderEventSteps.buildBeruDropshipOrderWithCancelRequest(
            ORDER_ID,
            SHOP_ID,
            PARCEL_ID,
            UPDATE_REQUEST_ID
        );
        order.getDelivery().getParcels().clear();
        Parcel parcel = new Parcel();
        parcel.setId(PARCEL_ID);
        parcel.setStatus(ParcelStatus.ERROR);
        order.getDelivery().addParcel(parcel);
        when(checkouterOrderService.getOrderWithChangeRequests(ORDER_ID))
            .thenReturn(order);
        mockSearchLomOrders(List.of(new OrderDto().setId(LOM_ORDER_ID)));

        TaskExecutionResult result = execCancel();

        verify(checkouterOrderService).processChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);
        verify(checkouterOrderService).applyChangeRequest(ORDER_ID, UPDATE_REQUEST_ID);

        assertEquals(TaskExecutionResult.finish(), result);
        verify(lomClient).cancelOrder(
            LOM_ORDER_ID,
            CancelOrderDto.builder().reason(CancellationOrderReason.CUSTOM).build(),
            true
        );
    }

    @Nonnull
    public TaskExecutionResult execCancel() {
        return consumer.execute(new Task<>(
            new QueueShardId("id"),
            new ChangeRequestCancelParcelDto(ORDER_ID, PARCEL_ID, "track", UPDATE_REQUEST_ID, 51L),
            1L,
            ZonedDateTime.now(),
            null,
            null
        ));
    }

    private void mockSearchOrders(List<OrderDto> result) {
        mockSearchOrders(result, OrderSubstatus.CUSTOM);
    }

    private void mockSearchOrders(List<OrderDto> result, OrderSubstatus substatus) {

        when(checkouterOrderService.getOrderWithChangeRequests(1L))
            .thenReturn(OrderEventSteps.buildBeruDropshipOrderWithCancelRequest(
                ORDER_ID,
                SHOP_ID,
                PARCEL_ID,
                UPDATE_REQUEST_ID,
                substatus
            ));

        mockSearchLomOrders(result);
    }

    public void mockSearchLomOrders(List<OrderDto> result) {
        when(lomClient.searchOrders(
            eq(
                OrderSearchFilter.builder()
                    .externalIds(Set.of(String.valueOf(ORDER_ID)))
                    .senderIds(Set.of(SHOP_ID))
                    .build()
            ),
            eq(Set.of(OptionalOrderPart.CANCELLATION_REQUESTS)),
            any(),
            eq(true)
        ))
            .thenReturn(PageResult.of(result, 1, 0, 10));
    }
}
