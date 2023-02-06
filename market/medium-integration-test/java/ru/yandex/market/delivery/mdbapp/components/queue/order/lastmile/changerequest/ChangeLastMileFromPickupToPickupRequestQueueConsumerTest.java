package ru.yandex.market.delivery.mdbapp.components.queue.order.lastmile.changerequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import steps.UpdateLastMileSteps;
import steps.UpdateRecipientSteps;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryLastMileEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryLastMileChangeRequestPayload;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileFromPickupToPickupRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileFromPickupToPickupRequestQueueConsumer;
import ru.yandex.market.delivery.mdbapp.components.queue.order.lastmile.AbstractLastMileRequestQueueConsumerTest;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DisplayName("Обработка нового статуса заявки на изменение последней мили с самовывоза на самовывоз")
public class ChangeLastMileFromPickupToPickupRequestQueueConsumerTest
    extends AbstractLastMileRequestQueueConsumerTest<ChangeLastMileFromPickupToPickupRequestDto> {

    private static final String SHARD_ID = "order.lastmile.frompickuptopickup.changed.changerequest";
    private static final String PAYLOAD_PATH =
        "/components/queue/order/delivery/lastmile/changerequest/payload_from_pickup_to_pickup.json";
    private static final String PAYLOAD_USER_PATH =
        "/components/queue/order/delivery/lastmile/changerequest/payload_from_pickup_to_pickup_user.json";

    @Autowired
    private ChangeLastMileFromPickupToPickupRequestQueueConsumer consumer;

    @Captor
    private ArgumentCaptor<ChangeRequestPatchRequest> captor;

    @BeforeEach
    void setUp() {
        consumer = new ChangeLastMileFromPickupToPickupRequestQueueConsumer(
            logisticsOrderService,
            checkouterOrderService,
            changeLastMileService,
            backLogOrdersTskvLogger,
            objectMapper
        );
        doReturn(UpdateLastMileSteps.createCheckouterOrder())
            .when(checkouterAPI)
            .getOrder(anyLong(), any(), any());
        mockCheckouterApi();
    }

    @AfterEach
    void tearDown() {
        verifyParcel();
        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Причина заявки - посылка не влезла в постамат, заявка успешно применилась, треки обновились")
    void changeRequestAppliedAndTracksUpdatedWhenDimensionsExceeded() {
        long orderId = Long.parseLong(UpdateLastMileSteps.BARCODE);
        doReturn(Optional.of(UpdateLastMileSteps.createLomOrderDtoAfterLastMileChangeFromPickupToPickup(
            ChangeOrderRequestStatus.SUCCESS,
            ChangeOrderRequestReason.DIMENSIONS_EXCEEDED_LOCKER,
            createPayload(PAYLOAD_PATH)
        )))
            .when(lomClient)
            .getOrder(anyLong(), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)), eq(false));
        doReturn(createOrderEditResponse(ChangeRequestStatus.APPLIED))
            .when(checkouterAPI)
            .editOrder(
                orderId,
                ClientRole.SYSTEM,
                null,
                List.of(Color.BLUE),
                createOrderEditRequest()
            );
        doReturn(createTracksForUpdate())
            .when(checkouterAPI)
            .updateDeliveryTracks(
                orderId,
                UpdateLastMileSteps.PARCEL_ID,
                createTracksForUpdate(),
                ClientRole.SYSTEM, null
            );
        var requestDto = UpdateLastMileSteps.createChangeLastMileFromPickupToPickupRequestDto();
        TaskExecutionResult result = consumer.execute(createTask(requestDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(checkouterAPI).editOrder(
            orderId,
            ClientRole.SYSTEM,
            null,
            List.of(Color.BLUE),
            createOrderEditRequest()
        );
        verifyGetOrderWithoutCheckouterChangeRequests();
        verify(checkouterAPI).updateDeliveryTracks(
            orderId,
            UpdateLastMileSteps.PARCEL_ID,
            createTracksForUpdate(),
            ClientRole.SYSTEM,
            null
        );
    }

    @Test
    @DisplayName("Причина заявки - посылка не влезла в постамат, заявка не применилась, треки не обновляются")
    void changeRequestRejectedAndTracksAreNotUpdatedWhenDimensionsExceeded() {
        long orderId = Long.parseLong(UpdateLastMileSteps.BARCODE);
        doReturn(Optional.of(UpdateLastMileSteps.createLomOrderDtoAfterLastMileChangeFromPickupToPickup(
            ChangeOrderRequestStatus.SUCCESS,
            ChangeOrderRequestReason.DIMENSIONS_EXCEEDED_LOCKER,
            createPayload(PAYLOAD_PATH)
        )))
            .when(lomClient)
            .getOrder(anyLong(), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)), eq(false));
        doReturn(createOrderEditResponse(ChangeRequestStatus.REJECTED))
            .when(checkouterAPI)
            .editOrder(
                orderId,
                ClientRole.SYSTEM,
                null,
                List.of(Color.BLUE),
                createOrderEditRequest()
            );
        var requestDto = UpdateLastMileSteps.createChangeLastMileFromPickupToPickupRequestDto();
        TaskExecutionResult result = consumer.execute(createTask(requestDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verify(checkouterAPI).editOrder(
            orderId,
            ClientRole.SYSTEM,
            null,
            List.of(Color.BLUE),
            createOrderEditRequest()
        );
        verify(lomClient).getOrder(
            eq(UpdateRecipientSteps.ORDER_ID),
            eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)),
            eq(false)
        );
        verify(checkouterAPI, never()).getOrder(anyLong(), any(), anyLong());
        verify(checkouterAPI, never()).updateDeliveryTracks(anyLong(), anyLong(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("Заявка была создана пользователем - успешно применилась и треки обновились")
    void changeRequestAppliedAndTracksUpdatedForUserRequest() {
        Long orderId = Long.valueOf(UpdateLastMileSteps.BARCODE);
        doReturn(Optional.of(UpdateLastMileSteps.createLomOrderDtoAfterLastMileChangeFromPickupToPickup(
            ChangeOrderRequestStatus.SUCCESS,
            null,
            createPayload(PAYLOAD_USER_PATH)
        )))
            .when(lomClient)
            .getOrder(anyLong(), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)), eq(false));
        doReturn(createTracksForUpdate())
            .when(checkouterAPI)
            .updateDeliveryTracks(
                orderId,
                UpdateLastMileSteps.PARCEL_ID,
                createTracksForUpdate(),
                ClientRole.SYSTEM, null
            );
        var requestDto = UpdateLastMileSteps.createChangeLastMileFromPickupToPickupRequestDto();
        TaskExecutionResult result = consumer.execute(createTask(requestDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verifyGetOrder();
        verify(checkouterAPI).updateChangeRequestStatus(
            eq(orderId),
            eq(UpdateLastMileSteps.REQUEST_ID),
            eq(ClientRole.SYSTEM),
            isNull(),
            captor.capture()
        );
        verify(checkouterAPI).updateDeliveryTracks(
            eq(orderId),
            eq(UpdateLastMileSteps.PARCEL_ID),
            safeRefEq(createTracksForUpdate()),
            eq(ClientRole.SYSTEM),
            isNull()
        );

        softly.assertThat(captor.getValue().getStatus()).isEqualTo(ChangeRequestStatus.APPLIED);
    }

    @Test
    @DisplayName("Заявка была создана пользователем - отклонена и треки не обновились")
    void changeRequestRejectedAndTracksAreNotUpdatedForUserRequest() {
        Long orderId = Long.valueOf(UpdateLastMileSteps.BARCODE);
        doReturn(Optional.of(UpdateLastMileSteps.createLomOrderDtoAfterLastMileChangeFromPickupToPickup(
            ChangeOrderRequestStatus.FAIL,
            null,
            createPayload(PAYLOAD_USER_PATH)
        )))
            .when(lomClient)
            .getOrder(anyLong(), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)), eq(false));
        var requestDto = UpdateLastMileSteps.createChangeLastMileFromPickupToPickupRequestDto();
        TaskExecutionResult result = consumer.execute(createTask(requestDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

        verifyGetOrder();
        verify(checkouterAPI).updateChangeRequestStatus(
            eq(orderId),
            eq(UpdateLastMileSteps.REQUEST_ID),
            eq(ClientRole.SYSTEM),
            isNull(),
            captor.capture()
        );
        verify(checkouterAPI, never()).updateDeliveryTracks(
            eq(orderId),
            eq(UpdateLastMileSteps.PARCEL_ID),
            any(),
            eq(ClientRole.SYSTEM),
            isNull()
        );

        softly.assertThat(captor.getValue().getStatus()).isEqualTo(ChangeRequestStatus.REJECTED);
    }

    @Nonnull
    private OrderEditRequest createOrderEditRequest() {
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        DeliveryLastMileEditRequest lastMileEditRequest = new DeliveryLastMileEditRequest();
        lastMileEditRequest.setReason(HistoryEventReason.DIMENSIONS_EXCEEDED_LOCKER);
        lastMileEditRequest.setDeliveryType(DeliveryType.PICKUP);
        lastMileEditRequest.setFromDate(LocalDate.of(2022, 4, 21));
        lastMileEditRequest.setToDate(LocalDate.of(2022, 4, 21));
        lastMileEditRequest.setTimeInterval(new TimeInterval(
            LocalTime.of(9, 0),
            LocalTime.of(14, 0)
        ));
        lastMileEditRequest.setRegionId(Long.valueOf(UpdateLastMileSteps.PICKUP_GEO_ID));
        lastMileEditRequest.setOutletId(UpdateLastMileSteps.PICKUP_OUTLET_ID);
        orderEditRequest.setDeliveryLastMileEditRequest(lastMileEditRequest);
        return orderEditRequest;
    }

    @Nonnull
    private List<ChangeRequest> createOrderEditResponse(ChangeRequestStatus status) {
        return List.of(new ChangeRequest(
            1L,
            Long.parseLong(UpdateLastMileSteps.BARCODE),
            new DeliveryLastMileChangeRequestPayload(),
            status,
            clock.instant(),
            "",
            ClientRole.SYSTEM
        ));
    }

    @Nonnull
    private List<Track> createTracksForUpdate() {
        Track ffTrack = new Track(UpdateLastMileSteps.FF_EXTERNAL_ID, UpdateLastMileSteps.FF_PARTNER_ID);
        ffTrack.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        Track pickupTrack = new Track(
            UpdateLastMileSteps.NEW_PICKUP_EXTERNAL_ID,
            UpdateLastMileSteps.NEW_PICKUP_PARTNER_ID
        );
        pickupTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        return List.of(
            ffTrack,
            pickupTrack
        );
    }
}
