package ru.yandex.market.delivery.mdbapp.components.queue.parcel.cancel;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.UpdateOrderStatusReasonDetails;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.order.changerequest.parcel.ParcelCancelChangeRequestPayload;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.cancel.result.CancelResultDto;
import ru.yandex.market.delivery.mdbapp.components.queue.cancel.result.CancellationResultQueueConsumer;
import ru.yandex.market.delivery.mdbapp.components.service.crm.client.OrderCommands;
import ru.yandex.market.delivery.mdbapp.configuration.CancellationResultProcessorConfiguration;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.CancellationOrderRequestReasonDetailsDto;
import ru.yandex.market.logistics.lom.model.dto.ChangedItemDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.MissingItemsCancellationOrderRequestReasonDetailsDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@DisplayName("Обработка отмены заказа")
public class CancellationResultQueueTest extends AllMockContextualTest {
    private static final long LOM_ORDER_ID = 123L;
    private static final long CHECKOUTER_ORDER_ID = 234L;
    private static final long REQUEST_ID = 15L;
    private static final long PARCEL_ID = 32;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private CancellationResultQueueConsumer cancellationResultQueueConsumer;

    @Autowired
    private CancellationResultProcessorConfiguration cancellationResultProcessorConfiguration;

    @Autowired
    private OrderCommands orderCommands;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Успех на необходимом сегменте с заявкой на отмену")
    void requiredSegmentSuccessWithParcelCancellationChangeRequest() {
        processTestWithCancellationRequest(
            CancellationOrderStatus.REQUIRED_SEGMENT_SUCCESS,
            ChangeRequestStatus.APPLIED,
            true,
            null,
            null
        );
    }

    @Test
    @DisplayName("Успех на необходимом сегменте без заявки на отмену")
    void requiredSegmentSuccessWithoutParcelCancellationChangeRequest() {
        processTestWithoutParcelCancellation(
            CancellationOrderStatus.REQUIRED_SEGMENT_SUCCESS,
            TaskExecutionResult.finish(),
            false,
            null
        );
    }

    @Test
    @DisplayName("Успех на необходимом сегменте без заявки на отмену - обновление статуса")
    void requiredSegmentSuccessWithoutParcelCancellationUpdateStatus() {
        processTestWithoutParcelCancellation(
            CancellationOrderStatus.REQUIRED_SEGMENT_SUCCESS,
            TaskExecutionResult.finish(),
            true,
            null
        );
    }

    @Test
    @DisplayName("Успех")
    void success() {
        MissingItemsCancellationOrderRequestReasonDetailsDto reasonDetailsDto =
            new MissingItemsCancellationOrderRequestReasonDetailsDto()
                .setItems(List.of(ChangedItemDto.builder().count(1L).article("article").vendorId(133L).build()));

        UpdateOrderStatusReasonDetails updateOrderStatusReasonDetails = new UpdateOrderStatusReasonDetails(
            new MissingItemsNotification(
                false,
                List.of(new ItemInfo(133L, "article", 1)),
                HistoryEventReason.ITEMS_NOT_FOUND
            )
        );
        processTestWithCancellationRequest(
            CancellationOrderStatus.SUCCESS,
            ChangeRequestStatus.APPLIED,
            true,
            updateOrderStatusReasonDetails,
            reasonDetailsDto
        );
    }

    @Test
    @DisplayName("Успех для dbs-заказа")
    void successDbs() {
        MissingItemsCancellationOrderRequestReasonDetailsDto reasonDetailsDto =
            new MissingItemsCancellationOrderRequestReasonDetailsDto()
                .setItems(List.of(ChangedItemDto.builder().count(1L).article("article").vendorId(133L).build()));

        UpdateOrderStatusReasonDetails updateOrderStatusReasonDetails = new UpdateOrderStatusReasonDetails(
            new MissingItemsNotification(
                false,
                List.of(new ItemInfo(133L, "article", 1)),
                HistoryEventReason.ITEMS_NOT_FOUND)
        );
        processTestWithCancellationRequest(
            CancellationOrderStatus.SUCCESS,
            ChangeRequestStatus.APPLIED,
            true,
            updateOrderStatusReasonDetails,
            reasonDetailsDto,
            PlatformClient.DBS.getId()
        );
    }

    @Test
    @DisplayName("Успех без причины")
    void successWithoutReason() {
        processTestWithCancellationRequest(
            CancellationOrderStatus.SUCCESS,
            ChangeRequestStatus.APPLIED,
            true,
            null,
            null
        );
    }

    @Test
    @DisplayName("Ручное подтверждение")
    void manuallyConfirmed() {
        processTestWithCancellationRequest(
            CancellationOrderStatus.MANUALLY_CONFIRMED,
            ChangeRequestStatus.APPLIED,
            true,
            null,
            null
        );
    }

    @Test
    @DisplayName("Ручное подтверждение доставленного заказа")
    void manuallyConfirmedDelivered() {
        processTestDelivered(CancellationOrderStatus.MANUALLY_CONFIRMED, ChangeRequestStatus.APPLIED);
        verifyZeroInteractions(orderCommands);
    }

    @Test
    @DisplayName("Обработка ошибки")
    void fail() {
        processTestWithCancellationRequest(
            CancellationOrderStatus.FAIL,
            ChangeRequestStatus.REJECTED,
            true,
            null,
            null
        );
    }

    @Test
    @DisplayName("Обработка технической ошибки")
    void techFail() {
        processTestWithCancellationRequest(
            CancellationOrderStatus.TECH_FAIL,
            ChangeRequestStatus.REJECTED,
            true,
            null,
            null
        );
    }

    @Test
    @DisplayName("Обработка отклоненной заявки")
    void rejected() {
        processTestWithCancellationRequest(
            CancellationOrderStatus.REJECTED,
            ChangeRequestStatus.REJECTED,
            true,
            null,
            null
        );
    }

    @Test
    @DisplayName("Обработка заявки в статусе PROCESSING")
    void processing() {
        execPayload(CancellationOrderStatus.PROCESSING, null);

        verifyZeroInteractions(orderCommands);
        verifyZeroInteractions(checkouterAPI);
        verifyZeroInteractions(lomClient);
    }

    @Test
    @DisplayName("Обработка даас-заказа")
    void daasCancel() {
        OrderDto lomOrder = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId(String.valueOf(CHECKOUTER_ORDER_ID))
            .setPlatformClientId(3L);

        Mockito.when(lomClient.getOrder(LOM_ORDER_ID, Set.of(), false))
            .thenReturn(Optional.of(lomOrder));

        execPayload(CancellationOrderStatus.SUCCESS, null);

        verifyZeroInteractions(orderCommands);
        verify(lomClient).getOrder(eq(LOM_ORDER_ID), eq(Set.of()), eq(false));
    }

    @Test
    @DisplayName("Обработка статуса REQUIRED_SEGMENT_FAIL")
    void requiredSegmentFail() {
        processTestWithCancellationRequest(
            CancellationOrderStatus.REQUIRED_SEGMENT_FAIL,
            ChangeRequestStatus.REJECTED,
            true,
            null,
            null
        );

        verifyZeroInteractions(orderCommands);
    }

    @Test
    @DisplayName("Обработка доставленного заказа")
    void delivered() {
        processTestDelivered(CancellationOrderStatus.SUCCESS, ChangeRequestStatus.PROCESSING);

        verifyZeroInteractions(orderCommands);
    }

    private void processTestWithCancellationRequest(
        CancellationOrderStatus cancelStatus,
        ChangeRequestStatus resultStatus,
        boolean useUpdateOrderStatus,
        UpdateOrderStatusReasonDetails updateOrderStatusReasonDetails,
        CancellationOrderRequestReasonDetailsDto reasonDetails
    ) {
        processTestWithCancellationRequest(
            cancelStatus,
            resultStatus,
            useUpdateOrderStatus,
            updateOrderStatusReasonDetails,
            reasonDetails,
            1L
        );
    }

    private void processTestWithCancellationRequest(
        CancellationOrderStatus cancelStatus,
        ChangeRequestStatus resultStatus,
        boolean useUpdateOrderStatus,
        UpdateOrderStatusReasonDetails updateOrderStatusReasonDetails,
        CancellationOrderRequestReasonDetailsDto reasonDetails,
        Long platformClientId
    ) {
        cancellationResultProcessorConfiguration.setUseUpdateOrderStatus(useUpdateOrderStatus);
        mockLomOrder(platformClientId);
        mockCheckouterOrder(true, OrderStatus.PROCESSING);
        mockCheckouterOrderUpdateOrderStatus(OrderSubstatus.MISSING_ITEM, updateOrderStatusReasonDetails);
        mockCheckouterChangeRequest(resultStatus);
        mockCheckouterOrderEditRequest();

        TaskExecutionResult result = execPayload(cancelStatus, CancellationOrderReason.MISSING_ITEM, reasonDetails);

        if (useUpdateOrderStatus && resultStatus == ChangeRequestStatus.APPLIED) {
            verifyCheckouterUpdateOrderStatus(updateOrderStatusReasonDetails);
        } else {
            verifyCheckouterChangeRequest(resultStatus);
        }

        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());
        verifyZeroInteractions(orderCommands);
        verify(lomClient).getOrder(eq(LOM_ORDER_ID), eq(Set.of()), eq(false));
    }

    private void processTestDelivered(CancellationOrderStatus cancelStatus, ChangeRequestStatus resultStatus) {
        cancellationResultProcessorConfiguration.setUseUpdateOrderStatus(true);
        mockLomOrder();
        mockCheckouterOrder(true, OrderStatus.DELIVERED);
        mockCheckouterChangeRequest(resultStatus);
        mockCheckouterOrderUpdateOrderStatus(OrderSubstatus.DELIVERY_PROBLEMS, null);

        TaskExecutionResult result = execPayload(cancelStatus, null);
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(lomClient).getOrder(eq(LOM_ORDER_ID), eq(Set.of()), eq(false));
    }

    private void processTestWithoutParcelCancellation(
        CancellationOrderStatus cancelStatus,
        TaskExecutionResult resultStatus,
        boolean useUpdateOrderStatus,
        UpdateOrderStatusReasonDetails updateOrderStatusReasonDetails
    ) {
        cancellationResultProcessorConfiguration.setUseUpdateOrderStatus(useUpdateOrderStatus);
        mockLomOrder();
        mockCheckouterOrder(false, OrderStatus.PROCESSING);
        mockCheckouterOrderUpdateOrderStatus(OrderSubstatus.MISSING_ITEM, updateOrderStatusReasonDetails);
        mockCheckouterOrderEditRequest();

        TaskExecutionResult result = execPayload(
            cancelStatus,
            CancellationOrderReason.MISSING_ITEM
        );

        if (useUpdateOrderStatus) {
            verifyCheckouterUpdateOrderStatus(updateOrderStatusReasonDetails);
        } else {
            verifyCheckouterOrderEditRequest();
        }

        softly.assertThat(result).isEqualTo(resultStatus);
        verifyZeroInteractions(orderCommands);
        verify(lomClient).getOrder(eq(LOM_ORDER_ID), eq(Set.of()), eq(false));
    }

    private void mockLomOrder(Long platformClientId) {
        OrderDto lomOrder = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId(String.valueOf(CHECKOUTER_ORDER_ID))
            .setItems(List.of(ItemDto.builder().article("article").vendorId(133L).count(2).build()))
            .setPlatformClientId(platformClientId);

        Mockito.when(lomClient.getOrder(eq(LOM_ORDER_ID), eq(Set.of()), eq(false)))
            .thenReturn(Optional.of(lomOrder));
    }

    private void mockLomOrder() {
        mockLomOrder(1L);
    }

    private void mockCheckouterOrderUpdateOrderStatus(
        OrderSubstatus orderSubstatus,
        UpdateOrderStatusReasonDetails reasonDetails
    ) {
        Order order = new Order();
        order.setId(CHECKOUTER_ORDER_ID);
        Mockito.doReturn(order).when(checkouterAPI).updateOrderStatus(
            eq(CHECKOUTER_ORDER_ID),
            eq(ClientRole.SYSTEM),
            eq(null),
            eq(null),
            eq(OrderStatus.CANCELLED),
            eq(orderSubstatus),
            reasonDetails == null ? isNull() : any(UpdateOrderStatusReasonDetails.class)
        );
    }

    private void mockCheckouterOrder(boolean withChangeRequest, OrderStatus orderStatus) {
        Order order = new Order();
        order.setId(CHECKOUTER_ORDER_ID);
        order.setStatus(orderStatus);
        if (withChangeRequest) {
            order.setChangeRequests(List.of(new ChangeRequest(
                REQUEST_ID,
                CHECKOUTER_ORDER_ID,
                new ParcelCancelChangeRequestPayload(PARCEL_ID, OrderSubstatus.STARTED, "", null),
                ChangeRequestStatus.PROCESSING,
                Instant.now(),
                null,
                ClientRole.SYSTEM
            )));
        }

        Mockito.doReturn(order)
            .when(checkouterAPI).getOrder(
                eq(CHECKOUTER_ORDER_ID),
                eq(ClientRole.SYSTEM),
                isNull(),
                eq(Set.of(OptionalOrderPart.CHANGE_REQUEST))
            );

        Mockito.doReturn(order)
            .when(checkouterAPI).getOrder(
                eq(CHECKOUTER_ORDER_ID),
                eq(ClientRole.SYSTEM),
                isNull()
            );
    }

    private void mockCheckouterChangeRequest(ChangeRequestStatus resultStatus) {
        Mockito.doReturn(true).when(checkouterAPI).updateChangeRequestStatus(
            eq(CHECKOUTER_ORDER_ID),
            eq(REQUEST_ID),
            eq(ClientRole.SYSTEM),
            any(),
            refEq(new ChangeRequestPatchRequest(resultStatus, null, null))
        );
    }

    private void mockCheckouterOrderEditRequest() {
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setCancellationRequest(new CancellationRequest(
            OrderSubstatus.MISSING_ITEM,
            "Заказ отменён логистикой"
        ));
        Mockito.doReturn(List.of()).when(checkouterAPI).editOrder(
            eq(CHECKOUTER_ORDER_ID),
            eq(ClientRole.SYSTEM),
            eq(null),
            eq(List.of(Color.BLUE)),
            eq(orderEditRequest)
        );
    }

    private void verifyCheckouterOrderEditRequest() {
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setCancellationRequest(new CancellationRequest(
            OrderSubstatus.MISSING_ITEM,
            "Заказ отменён логистикой"
        ));
        verify(checkouterAPI).editOrder(
            eq(CHECKOUTER_ORDER_ID),
            eq(ClientRole.SYSTEM),
            eq(null),
            eq(List.of(Color.BLUE)),
            eq(orderEditRequest)
        );
    }

    private void verifyCheckouterChangeRequest(ChangeRequestStatus resultStatus) {
        verify(checkouterAPI).updateChangeRequestStatus(
            eq(CHECKOUTER_ORDER_ID),
            eq(REQUEST_ID),
            eq(ClientRole.SYSTEM),
            any(),
            refEq(new ChangeRequestPatchRequest(resultStatus, null, null))
        );
    }

    private void verifyCheckouterUpdateOrderStatus(@Nullable UpdateOrderStatusReasonDetails reasonDetails) {
        ArgumentCaptor<UpdateOrderStatusReasonDetails> captor =
            ArgumentCaptor.forClass(UpdateOrderStatusReasonDetails.class);
        verify(checkouterAPI).updateOrderStatus(
            eq(CHECKOUTER_ORDER_ID),
            eq(ClientRole.SYSTEM),
            eq(null),
            eq(null),
            eq(OrderStatus.CANCELLED),
            eq(OrderSubstatus.MISSING_ITEM),
            captor.capture()
        );
        if (reasonDetails == null) {
            return;
        }
        MissingItemsNotification actualMissingItemsNotification = captor.getValue().getMissingItemsNotification();
        MissingItemsNotification expectedMissingItemsNotification = captor.getValue().getMissingItemsNotification();
        softly.assertThat(actualMissingItemsNotification.getRemainedItems())
            .containsExactlyInAnyOrderElementsOf(expectedMissingItemsNotification.getRemainedItems());
        softly.assertThat(actualMissingItemsNotification.getReason())
            .isEqualTo(expectedMissingItemsNotification.getReason());
    }

    @Nonnull
    private TaskExecutionResult execPayload(
        CancellationOrderStatus cancelStatus,
        CancellationOrderReason<?> reason,
        CancellationOrderRequestReasonDetailsDto reasonDetails
    ) {
        return cancellationResultQueueConsumer.execute(new Task<>(
            new QueueShardId("id"),
            new CancelResultDto(LOM_ORDER_ID, cancelStatus, reason, reasonDetails),
            1L,
            ZonedDateTime.now(),
            null,
            null
        ));
    }

    @Nonnull
    private TaskExecutionResult execPayload(CancellationOrderStatus cancelStatus, CancellationOrderReason<?> reason) {
        return execPayload(cancelStatus, reason, null);
    }
}
