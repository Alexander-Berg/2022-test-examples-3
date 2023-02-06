package ru.yandex.market.delivery.mdbapp.components.queue.order.lastmile.changerequest;

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
import steps.UpdateLastMileSteps;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileToPickupRequestDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.lastmile.changerequest.ChangeLastMileToPickupRequestQueueConsumer;
import ru.yandex.market.delivery.mdbapp.components.queue.order.lastmile.AbstractLastMileRequestQueueConsumerTest;
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

@DisplayName("Финальная обработка заявок на изменение последней мили на ПВЗ")
public class ChangeLastMileToPickupRequestQueueConsumerTest
    extends AbstractLastMileRequestQueueConsumerTest<ChangeLastMileToPickupRequestDto> {

    private static final String SHARD_ID = "order.lastmile.topickup.changed.changerequest";

    private static final String PAYLOAD_PATH =
        "/components/queue/order/delivery/lastmile/changerequest/payload_to_pickup.json";

    private ChangeLastMileToPickupRequestQueueConsumer consumer;

    @Captor
    private ArgumentCaptor<ChangeRequestPatchRequest> captor;

    @BeforeEach
    void setUp() {
        consumer = new ChangeLastMileToPickupRequestQueueConsumer(
            logisticsOrderService,
            checkouterOrderService,
            changeLastMileService,
            backLogOrdersTskvLogger,
            objectMapper
        );
        mockCheckouterApi();
    }

    @AfterEach
    void tearDown() {
        verifyGetOrder();
        verifyParcel();

        verifyNoMoreInteractions(lomClient);
    }

    @Test
    @DisplayName("Статус заявки обновляется до APPLIED, отправляется запрос на обновление треков,"
        + "статус парсела не обновляется потому что уже READY_TO_SHIP")
    void changeRequestApplied() {
        Long orderId = Long.valueOf(UpdateLastMileSteps.BARCODE);
        doReturn(Optional.of(UpdateLastMileSteps.createLomOrderDtoAfterLastMileChangeToPickup(
            ChangeOrderRequestStatus.SUCCESS,
            createPayload(PAYLOAD_PATH)
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
        ChangeLastMileToPickupRequestDto requestDto = UpdateLastMileSteps.createChangeLastMileToPickupRequestDto();
        TaskExecutionResult result = consumer.execute(createTask(requestDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

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
    @DisplayName("Статус заявки обновляется до REJECTED, треки в чекаутере не обновляем")
    void changeRequestRejected() {
        Long orderId = Long.valueOf(UpdateLastMileSteps.BARCODE);
        doReturn(Optional.of(UpdateLastMileSteps.createLomOrderDtoAfterLastMileChangeToPickup(
            ChangeOrderRequestStatus.FAIL,
            createPayload(PAYLOAD_PATH)
        )))
            .when(lomClient)
            .getOrder(anyLong(), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)), eq(false));
        ChangeLastMileToPickupRequestDto requestDto = UpdateLastMileSteps.createChangeLastMileToPickupRequestDto();
        TaskExecutionResult result = consumer.execute(createTask(requestDto, SHARD_ID));
        softly.assertThat(result).isEqualTo(TaskExecutionResult.finish());

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
    private List<Track> createTracksForUpdate() {
        Track ffTrack = new Track(UpdateLastMileSteps.FF_EXTERNAL_ID, UpdateLastMileSteps.FF_PARTNER_ID);
        ffTrack.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        Track pickupTrack = new Track(UpdateLastMileSteps.PICKUP_EXTERNAL_ID, UpdateLastMileSteps.PICKUP_PARTNER_ID);
        pickupTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        return List.of(
            ffTrack,
            pickupTrack
        );
    }
}
