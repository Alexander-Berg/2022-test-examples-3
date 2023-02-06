package ru.yandex.market.delivery.mdbapp.components.queue.track.add;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.request.ParcelPatchRequest;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.track.add.dto.AddTrackDto;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
@DisplayName("Пуш треков от пвз")
public class AddTrackQueueFilterMiddleMileTest extends AllMockContextualTest {
    public static final long LOM_ORDER_ID = 123L;
    public static final long LOM_WAYBILL_SEGMENT_ID = 111L;
    public static final long CHECKOUTER_ORDER_ID = 234L;
    public static final long PARCEL_ID = 345L;
    public static final String TRACK_CODE = "trackCode";
    public static final long POSTAMAT_MIDDLE_MILE_ID = 1005372L;
    public static final long POSTAMAT_LAST_MILE_ID = 1003562L;
    public static final long LAVKA_MIDDLE_MILE_ID = 1005373L;
    public static final long LAVKA_LAST_MILE_ID = 1005471L;
    public static final long FULFILLMENT_PARTNER_ID = 456;
    public static final Parcel PARCEL_STUB = new Parcel();

    @Autowired
    private QueueProducer<AddTrackDto> producer;

    @Autowired
    private TaskLifecycleListener taskListener;

    @MockBean
    private CheckouterAPI checkouterAPI;

    @Autowired
    private LomClient lomClient;

    private CountDownLatch countDownLatch;

    private final long middleMileId;
    private final long lastMileId;

    public AddTrackQueueFilterMiddleMileTest(long middleMileId, long lastMileId) {
        this.middleMileId = middleMileId;
        this.lastMileId = lastMileId;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
            new Object[][]{
                {POSTAMAT_MIDDLE_MILE_ID, POSTAMAT_LAST_MILE_ID},
                {LAVKA_MIDDLE_MILE_ID, LAVKA_LAST_MILE_ID}
            }
        );
    }

    @Before
    public void setUp() {
        countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(new Track()).when(checkouterAPI).addTrack(anyLong(), anyLong(), any(), any(), any());
    }

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(checkouterAPI, lomClient);
    }

    @Test
    @DisplayName("Успех для беру")
    public void addTrackProducerSuccess() throws InterruptedException {
        OrderDto lomOrder = lomOrder();
        ParcelPatchRequest expectedParcelPatchCreatedRequest = new ParcelPatchRequest();
        ParcelPatchRequest expectedParcelPatchReadyToShipRequest = new ParcelPatchRequest();

        mockSuccess(lomOrder, expectedParcelPatchCreatedRequest, expectedParcelPatchReadyToShipRequest);

        tryToAddTracks();

        verifySuccess(expectedParcelPatchCreatedRequest, expectedParcelPatchReadyToShipRequest);
    }

    @Test
    @DisplayName("Успех для DBS")
    public void addTrackProducerDbsSuccess() throws InterruptedException {
        OrderDto lomOrder = lomOrder().setPlatformClientId(PlatformClient.DBS.getId());
        ParcelPatchRequest expectedParcelPatchCreatedRequest = new ParcelPatchRequest();
        ParcelPatchRequest expectedParcelPatchReadyToShipRequest = new ParcelPatchRequest();

        mockSuccess(lomOrder, expectedParcelPatchCreatedRequest, expectedParcelPatchReadyToShipRequest);

        tryToAddTracks();

        verifySuccess(expectedParcelPatchCreatedRequest, expectedParcelPatchReadyToShipRequest);
    }

    @Test
    @DisplayName("Для платформы недоступен пуш")
    public void addTrackProducerFilterMiddleMileInvalidPlatformClient() throws InterruptedException {
        OrderDto lomOrder = lomOrder().setPlatformClientId(PlatformClient.YANDEX_DELIVERY.getId());

        when(lomClient.getOrder(LOM_ORDER_ID))
            .thenReturn(Optional.of(lomOrder));

        tryToAddTracks();
    }

    @Nonnull
    private OrderDto lomOrder() {
        return new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId(String.valueOf(CHECKOUTER_ORDER_ID))
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID)
                    .externalId(TRACK_CODE + ".FULFILLMENT")
                    .partnerId(FULFILLMENT_PARTNER_ID)
                    .partnerType(PartnerType.FULFILLMENT)
                    .segmentType(SegmentType.FULFILLMENT)
                    .waybillSegmentTags(List.of())
                    .build(),
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID + 1)
                    .externalId(TRACK_CODE + ".SHIPMENT")
                    .partnerId(middleMileId)
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.MOVEMENT)
                    .build(),
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID + 2)
                    .externalId(TRACK_CODE + ".PICKUP")
                    .partnerId(lastMileId)
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.PICKUP)
                    .waybillSegmentTags(Collections.emptyList())
                    .build(),
                //еще не созданный сегмент в ЛОМе
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID + 3)
                    .externalId(null)
                    .partnerId(lastMileId)
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.PICKUP)
                    .build()
            ));
    }

    private void verifySuccess(
        ParcelPatchRequest expectedParcelPatchCreatedRequest,
        ParcelPatchRequest expectedParcelPatchReadyToShipRequest
    ) {
        Track expectedFulfillmentTrack = new Track(TRACK_CODE + ".FULFILLMENT", FULFILLMENT_PARTNER_ID);
        expectedFulfillmentTrack.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        Track expectedShipmentTrack = new Track(TRACK_CODE + ".SHIPMENT", middleMileId);
        expectedShipmentTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        Track expectedPickupTrack = new Track(TRACK_CODE + ".PICKUP", lastMileId);
        expectedPickupTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);

        verify(checkouterAPI)
            .updateParcel(
                eq(CHECKOUTER_ORDER_ID),
                eq(PARCEL_ID),
                refEq(expectedParcelPatchCreatedRequest),
                eq(ClientRole.SYSTEM),
                isNull()
            );

        verify(checkouterAPI)
            .updateParcel(
                eq(CHECKOUTER_ORDER_ID),
                eq(PARCEL_ID),
                refEq(expectedParcelPatchReadyToShipRequest),
                eq(ClientRole.SYSTEM),
                isNull()
            );

        verify(checkouterAPI)
            .addTrack(
                eq(CHECKOUTER_ORDER_ID),
                eq(PARCEL_ID),
                refEq(expectedFulfillmentTrack),
                eq(ClientRole.SYSTEM),
                isNull()
            );
        verify(checkouterAPI, never())
            .addTrack(
                eq(CHECKOUTER_ORDER_ID),
                eq(PARCEL_ID),
                refEq(expectedPickupTrack),
                eq(ClientRole.SYSTEM),
                isNull()
            );
    }

    private void mockSuccess(
        OrderDto lomOrder,
        ParcelPatchRequest expectedParcelPatchCreatedRequest,
        ParcelPatchRequest expectedParcelPatchReadyToShipRequest
    ) {
        when(lomClient.getOrder(LOM_ORDER_ID)).thenReturn(Optional.of(lomOrder));

        expectedParcelPatchCreatedRequest.setParcelStatus(ParcelStatus.CREATED);

        when(checkouterAPI.updateParcel(
            eq(CHECKOUTER_ORDER_ID),
            eq(PARCEL_ID),
            refEq(expectedParcelPatchCreatedRequest),
            eq(ClientRole.SYSTEM),
            isNull()
        ))
            .thenReturn(PARCEL_STUB);

        expectedParcelPatchReadyToShipRequest.setParcelStatus(ParcelStatus.READY_TO_SHIP);

        when(checkouterAPI.updateParcel(
            eq(CHECKOUTER_ORDER_ID),
            eq(PARCEL_ID),
            refEq(expectedParcelPatchReadyToShipRequest),
            eq(ClientRole.SYSTEM),
            isNull()
        ))
            .thenReturn(PARCEL_STUB);
    }

    private void tryToAddTracks() throws InterruptedException {
        Parcel parcel = new Parcel();
        parcel.setId(PARCEL_ID);
        parcel.setStatus(null);

        List<Parcel> parcels = new ArrayList<>();
        parcels.add(parcel);

        Delivery delivery = new Delivery();
        delivery.setParcels(parcels);

        Order order = new Order();
        order.setDelivery(delivery);

        doReturn(order).when(checkouterAPI).getOrder(CHECKOUTER_ORDER_ID, ClientRole.SYSTEM, 0L);

        producer.enqueue(EnqueueParams.create(new AddTrackDto(LOM_ORDER_ID, LOM_WAYBILL_SEGMENT_ID)));
        countDownLatch.await(2, TimeUnit.SECONDS);
    }
}
