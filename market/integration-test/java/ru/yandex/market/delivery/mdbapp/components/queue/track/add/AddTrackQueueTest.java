package ru.yandex.market.delivery.mdbapp.components.queue.track.add;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
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
import ru.yandex.market.logistics.lom.model.enums.tags.WaybillSegmentTag;
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

@DisplayName("Прокидывание треков заказа из LOM в чекаутер")
class AddTrackQueueTest extends AllMockContextualTest {
    public static final long LOM_ORDER_ID = 123L;
    public static final long LOM_WAYBILL_SEGMENT_ID = 111L;
    public static final long CHECKOUTER_ORDER_ID = 234L;
    public static final long PARCEL_ID = 345L;
    public static final long LAST_MILE_DS_ID = 1003562L;
    public static final String TRACK_CODE = "trackCode";
    public static final long FULFILLMENT_PARTNER_ID = 456;
    public static final Parcel PARCEL_STUB = new Parcel();

    @Autowired
    private QueueProducer<AddTrackDto> producer;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private LomClient lomClient;

    private CountDownLatch countDownLatch;

    @BeforeEach
    public void setUp() {
        countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(new Track()).when(checkouterAPI).addTrack(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("В чекаутере добавится один трек службы доставки и статус парсела обновится в CREATED")
    public void addTrackProducerSuccess() throws InterruptedException {
        OrderDto lomOrder = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId(String.valueOf(CHECKOUTER_ORDER_ID))
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID)
                    .externalId(TRACK_CODE)
                    .partnerId(LAST_MILE_DS_ID)
                    .partnerType(PartnerType.DELIVERY)
                    .build()
            ));

        when(lomClient.getOrder(LOM_ORDER_ID)).thenReturn(Optional.of(lomOrder));

        ParcelPatchRequest expectedParcelPatchRequest = new ParcelPatchRequest();
        expectedParcelPatchRequest.setParcelStatus(ParcelStatus.CREATED);

        doReturn(PARCEL_STUB).when(checkouterAPI).updateParcel(
            eq(CHECKOUTER_ORDER_ID),
            eq(PARCEL_ID),
            refEq(expectedParcelPatchRequest),
            eq(ClientRole.SYSTEM),
            isNull()
        );

        tryToAddTracks();

        Track expectedTrack = new Track(TRACK_CODE, LAST_MILE_DS_ID);
        expectedTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);

        verify(checkouterAPI)
            .updateParcel(
                eq(CHECKOUTER_ORDER_ID),
                eq(PARCEL_ID),
                refEq(expectedParcelPatchRequest),
                eq(ClientRole.SYSTEM),
                isNull()
            );

        verify(checkouterAPI).addTrack(
            eq(CHECKOUTER_ORDER_ID),
            eq(PARCEL_ID),
            refEq(expectedTrack),
            eq(ClientRole.SYSTEM),
            isNull()
        );
    }

    @Test
    @DisplayName("В чекаутере обновится только статус парсела")
    public void updateParcelStatus() throws InterruptedException {
        OrderDto lomOrder = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId(String.valueOf(CHECKOUTER_ORDER_ID))
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID)
                    .externalId(TRACK_CODE)
                    .partnerId(LAST_MILE_DS_ID)
                    .partnerType(PartnerType.DELIVERY)
                    .build()
            ));

        when(lomClient.getOrder(LOM_ORDER_ID)).thenReturn(Optional.of(lomOrder));

        ParcelPatchRequest expectedParcelPatchRequest = new ParcelPatchRequest();
        expectedParcelPatchRequest.setParcelStatus(ParcelStatus.CREATED);
        doReturn(PARCEL_STUB).when(checkouterAPI).updateParcel(
            eq(CHECKOUTER_ORDER_ID),
            eq(PARCEL_ID),
            refEq(expectedParcelPatchRequest),
            eq(ClientRole.SYSTEM),
            isNull()
        );

        Track expectedTrack = new Track(TRACK_CODE, LAST_MILE_DS_ID);
        expectedTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);

        tryToAddTracks(ParcelStatus.NEW, null, List.of(expectedTrack));

        verify(checkouterAPI)
            .updateParcel(
                eq(CHECKOUTER_ORDER_ID),
                eq(PARCEL_ID),
                refEq(expectedParcelPatchRequest),
                eq(ClientRole.SYSTEM),
                isNull()
            );

        verify(checkouterAPI, never()).addTrack(
            eq(CHECKOUTER_ORDER_ID),
            eq(PARCEL_ID),
            refEq(expectedTrack),
            eq(ClientRole.SYSTEM),
            isNull()
        );
    }

    @Test
    @DisplayName("В чекаутере добавится один трек фулфиллмент-партнера и статус парсела обновится в READY_TO_SHIP")
    public void addTrackProducerNonPostamatFulfillmentAdded() throws InterruptedException {
        Track expectedTrack = new Track(TRACK_CODE + ".FULFILLMENT", FULFILLMENT_PARTNER_ID);
        expectedTrack.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        OrderDto lomOrder = new OrderDto()
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
                    .waybillSegmentTags(List.of(WaybillSegmentTag.DIRECT, WaybillSegmentTag.RETURN))
                    .build()
            ));

        when(lomClient.getOrder(LOM_ORDER_ID)).thenReturn(Optional.of(lomOrder));

        tryToAddTracks();

        verify(checkouterAPI)
            .addTrack(
                eq(CHECKOUTER_ORDER_ID),
                eq(PARCEL_ID),
                refEq(expectedTrack),
                eq(ClientRole.SYSTEM),
                isNull()
            );
    }

    @Test
    @DisplayName("В чекаутере не добавится трек возвратного фулфиллмент-партнера")
    public void addTrackProducerNonPostamatFulfillmentOnlyReturnNotAdded() throws InterruptedException {
        OrderDto lomOrder = new OrderDto()
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
                    .waybillSegmentTags(List.of(WaybillSegmentTag.RETURN))
                    .build()
            ));

        when(lomClient.getOrder(LOM_ORDER_ID)).thenReturn(Optional.of(lomOrder));

        tryToAddTracks();

        verifyNoMoreInteractions(checkouterAPI);
    }

    @Test
    @DisplayName("В чекаутере не добавится трек сортировочного центра")
    public void addTrackProducerSortingCenterNotAdded() throws InterruptedException {
        OrderDto lomOrder = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId(String.valueOf(CHECKOUTER_ORDER_ID))
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID)
                    .externalId(TRACK_CODE + ".FULFILLMENT")
                    .partnerId(FULFILLMENT_PARTNER_ID)
                    .partnerType(PartnerType.SORTING_CENTER)
                    .segmentType(SegmentType.SORTING_CENTER)
                    .build()
            ));

        when(lomClient.getOrder(LOM_ORDER_ID))
            .thenReturn(Optional.of(lomOrder));

        tryToAddTracks();

        verifyNoMoreInteractions(checkouterAPI);
    }

    @Test
    @DisplayName("В чекаутере не добавится трек дропоффа")
    public void addTrackDropoffNotAdded() throws InterruptedException {
        OrderDto lomOrder = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId(String.valueOf(CHECKOUTER_ORDER_ID))
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID)
                    .externalId(TRACK_CODE + ".DROPOFF")
                    .partnerId(LAST_MILE_DS_ID)
                    .partnerType(PartnerType.DELIVERY)
                    .segmentType(SegmentType.SORTING_CENTER)
                    .build()
            ));

        when(lomClient.getOrder(LOM_ORDER_ID))
            .thenReturn(Optional.of(lomOrder));

        tryToAddTracks();

        verifyNoMoreInteractions(checkouterAPI);
    }

    @Test
    @DisplayName("Парсел уже в финальном статусе READY_TO_SHIP, обновления не будет")
    public void finalParcelStatusStateIsNotUpdated() throws InterruptedException {
        OrderDto lomOrder = new OrderDto()
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
                    .build()
            ));

        when(lomClient.getOrder(LOM_ORDER_ID))
            .thenReturn(Optional.of(lomOrder));

        tryToAddTracks(ParcelStatus.READY_TO_SHIP);

        verify(checkouterAPI, never()).updateParcel(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("Парсел уже в статусе CREATED, обновления не будет")
    public void parcelAlreadyInCreatedStatus() throws InterruptedException {
        OrderDto lomOrder = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId(String.valueOf(CHECKOUTER_ORDER_ID))
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID)
                    .externalId(TRACK_CODE)
                    .partnerId(LAST_MILE_DS_ID)
                    .partnerType(PartnerType.DELIVERY)
                    .build()
            ));

        when(lomClient.getOrder(LOM_ORDER_ID)).thenReturn(Optional.of(lomOrder));

        tryToAddTracks(ParcelStatus.CREATED);

        Track expectedTrack = new Track(TRACK_CODE, LAST_MILE_DS_ID);
        expectedTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);

        verify(checkouterAPI, never()).updateParcel(anyLong(), anyLong(), any(), any(), any());

        verify(checkouterAPI).addTrack(
            eq(CHECKOUTER_ORDER_ID),
            eq(PARCEL_ID),
            refEq(expectedTrack),
            eq(ClientRole.SYSTEM),
            isNull()
        );
    }

    @Test
    @DisplayName("В чекаутере не добавится трек MOVEMENT-сегмента")
    public void middleMilesSkippedForOnDemandOrder() throws InterruptedException {
        long fulfillmentPartnerId = 172;
        long movementPartnerId = 1005705;
        long pickupPartnerId = 1005474;
        long courierPartnerId = 1006422;

        String fulfillmentTrackCode = "fulfillment-track-code";
        String movementTrackCode = "movement-track-code";
        String pickupTrackCode = "pickup-track-code";
        String courierTrackCode = "courier-track-code";

        OrderDto lomOrder = new OrderDto()
            .setId(LOM_ORDER_ID)
            .setExternalId(String.valueOf(CHECKOUTER_ORDER_ID))
            .setPlatformClientId(PlatformClient.BERU.getId())
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID)
                    .externalId(fulfillmentTrackCode)
                    .partnerId(fulfillmentPartnerId)
                    .segmentType(SegmentType.FULFILLMENT)
                    .partnerType(PartnerType.FULFILLMENT)
                    .waybillSegmentTags(List.of())
                    .build(),
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID + 1)
                    .externalId(movementTrackCode)
                    .partnerId(movementPartnerId)
                    .segmentType(SegmentType.MOVEMENT)
                    .partnerType(PartnerType.DELIVERY)
                    .build(),
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID + 2)
                    .externalId(pickupTrackCode)
                    .partnerId(pickupPartnerId)
                    .segmentType(SegmentType.PICKUP)
                    .partnerType(PartnerType.DELIVERY)
                    .build(),
                WaybillSegmentDto.builder()
                    .id(LOM_WAYBILL_SEGMENT_ID + 3)
                    .externalId(courierTrackCode)
                    .partnerId(courierPartnerId)
                    .segmentType(SegmentType.COURIER)
                    .partnerType(PartnerType.DELIVERY)
                    .build()
            ));

        when(lomClient.getOrder(LOM_ORDER_ID)).thenReturn(Optional.of(lomOrder));

        tryToAddTracks(ParcelStatus.CREATED, Set.of(DeliveryFeature.ON_DEMAND_MARKET_PICKUP), null);

        Track expectedFulfillmentTrack = new Track(fulfillmentTrackCode, fulfillmentPartnerId);
        expectedFulfillmentTrack.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        verify(checkouterAPI).addTrack(
            eq(CHECKOUTER_ORDER_ID),
            eq(PARCEL_ID),
            refEq(expectedFulfillmentTrack),
            eq(ClientRole.SYSTEM),
            isNull()
        );

        Track expectedMovementTrack = new Track(movementTrackCode, movementPartnerId);
        expectedFulfillmentTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        verify(checkouterAPI, never())
            .addTrack(
                eq(CHECKOUTER_ORDER_ID),
                eq(PARCEL_ID),
                refEq(expectedMovementTrack),
                eq(ClientRole.SYSTEM),
                isNull()
            );

        Track expectedPickupTrack = new Track(pickupTrackCode, pickupPartnerId);
        verify(checkouterAPI, never())
            .addTrack(
                eq(CHECKOUTER_ORDER_ID),
                eq(PARCEL_ID),
                refEq(expectedPickupTrack),
                eq(ClientRole.SYSTEM),
                isNull()
            );

        Track expectedCourierTrack = new Track(courierTrackCode, courierPartnerId);
        expectedCourierTrack.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        verify(checkouterAPI, never())
            .addTrack(
                eq(CHECKOUTER_ORDER_ID),
                eq(PARCEL_ID),
                refEq(expectedCourierTrack),
                eq(ClientRole.SYSTEM),
                isNull()
            );
    }

    private void tryToAddTracks() throws InterruptedException {
        tryToAddTracks(null);
    }

    private void tryToAddTracks(@Nullable ParcelStatus parcelStatus) throws InterruptedException {
        tryToAddTracks(parcelStatus, null, null);
    }

    private void tryToAddTracks(
        @Nullable ParcelStatus parcelStatus,
        @Nullable Set<DeliveryFeature> features,
        @Nullable List<Track> tracks
    ) throws InterruptedException {
        Parcel parcel = new Parcel();
        parcel.setId(PARCEL_ID);
        parcel.setStatus(parcelStatus);
        parcel.setTracks(tracks);

        List<Parcel> parcels = new ArrayList<>();
        parcels.add(parcel);

        Delivery delivery = new Delivery();
        delivery.setParcels(parcels);
        delivery.setFeatures(features);

        Order order = new Order();
        order.setDelivery(delivery);

        doReturn(order).when(checkouterAPI).getOrder(CHECKOUTER_ORDER_ID, ClientRole.SYSTEM, 0L);

        producer.enqueue(EnqueueParams.create(new AddTrackDto(LOM_ORDER_ID, LOM_WAYBILL_SEGMENT_ID)));
        countDownLatch.await(2, TimeUnit.SECONDS);
    }
}
