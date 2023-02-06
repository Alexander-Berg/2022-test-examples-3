package ru.yandex.market.logistics.lom.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.WaybillSegmentStatusHistory;
import ru.yandex.market.logistics.lom.entity.embedded.DeliveryInterval;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;

@UtilityClass
@ParametersAreNonnullByDefault
public class WaybillSegmentFactory {

    public static final Instant FIXED_TIME = Instant.parse("2021-01-01T15:00:00.00Z");

    @Nonnull
    private WaybillSegmentStatusHistory createHistory(
        SegmentStatus status,
        Instant checkpointReceivedDatetime
    ) {
        return new WaybillSegmentStatusHistory()
            .setStatus(status)
            .setCreated(checkpointReceivedDatetime)
            .setDate(checkpointReceivedDatetime);
    }

    public void writeWaybillSegmentCheckpoint(
        WaybillSegment segment,
        SegmentStatus status,
        Instant checkpointReceivedDatetime
    ) {
        WaybillSegmentStatusHistory history = createHistory(status, checkpointReceivedDatetime);
        segment.addWaybillSegmentStatusHistory(history, history.getStatus());
    }

    @Nonnull
    public WaybillSegment createDsFfIntakeReturnSegments() {
        Order order = createOrder(null);
        WaybillSegment ffSegment = createWaybillSegment(1, 1, "externalId", SegmentType.FULFILLMENT);
        WaybillSegment dsSegment = createWaybillSegment(2, 2, "externalId 2", SegmentType.MOVEMENT);

        ffSegment.setPartnerType(PartnerType.FULFILLMENT);
        writeWaybillSegmentCheckpoint(dsSegment, SegmentStatus.RETURNED, FIXED_TIME);

        order.setWaybill(List.of(ffSegment, dsSegment));

        return ffSegment;
    }

    @Nonnull
    public WaybillSegment createOnDemandWaybillSegmentsLavka(
        @Nullable SegmentStatus currentCheckpoint,
        @Nullable SegmentStatus previousCheckpoint,
        @Nullable LocalDate deliveryDate
    ) {
        Order order = createOrder(deliveryDate);

        WaybillSegment previousSegment = createWaybillSegment(1, 1, "externalId", SegmentType.MOVEMENT);
        previousSegment.addTag(WaybillSegmentTag.ON_DEMAND);

        WaybillSegment currentSegment = createWaybillSegment(2, 2, "externalId 2", SegmentType.COURIER);

        Optional.ofNullable(currentCheckpoint).ifPresent(
            checkpoint -> WaybillSegmentFactory.writeWaybillSegmentCheckpoint(currentSegment, checkpoint, FIXED_TIME)
        );
        Optional.ofNullable(previousCheckpoint).ifPresent(
            checkpoint -> writeWaybillSegmentCheckpoint(previousSegment, checkpoint, FIXED_TIME)
        );

        order.setWaybill(List.of(previousSegment, currentSegment));

        return currentSegment;
    }

    @Nonnull
    public WaybillSegment createOnDemandWaybillSegmentsPvz(
        @Nullable SegmentStatus currentCheckpoint,
        @Nullable SegmentStatus previousCheckpoint,
        @Nullable LocalDate deliveryDate
    ) {
        Order order = createOrder(deliveryDate);

        WaybillSegment previousSegment = createWaybillSegment(1, 1, "externalId", SegmentType.MOVEMENT);
        previousSegment.addTag(WaybillSegmentTag.ON_DEMAND);

        WaybillSegment currentSegment = createWaybillSegment(2, 2, "externalId 2", SegmentType.PICKUP);

        Optional.ofNullable(currentCheckpoint).ifPresent(
            checkpoint -> WaybillSegmentFactory.writeWaybillSegmentCheckpoint(currentSegment, checkpoint, FIXED_TIME)
        );
        Optional.ofNullable(previousCheckpoint).ifPresent(
            checkpoint -> writeWaybillSegmentCheckpoint(previousSegment, checkpoint, FIXED_TIME)
        );

        order.setWaybill(List.of(previousSegment, currentSegment));

        return currentSegment;
    }

    @Nonnull
    public WaybillSegment createDsShipmentReturnSegments() {
        Order order = createOrder(null);
        WaybillSegment previousSegment = createWaybillSegment(1, 1, "externalId", SegmentType.SORTING_CENTER);
        WaybillSegment currentSegment = createWaybillSegment(2, 2, "externalId 2", SegmentType.MOVEMENT);

        writeWaybillSegmentCheckpoint(currentSegment, SegmentStatus.RETURN_ARRIVED, FIXED_TIME);

        order.setWaybill(List.of(previousSegment, currentSegment));

        return currentSegment;
    }

    @Nonnull
    public WaybillSegment createIntakeReturnSegments(
        SegmentType returnFrom,
        SegmentType returnTo,
        SegmentStatus checkpointFrom
    ) {
        Order order = createOrder(null);
        WaybillSegment toReturnSegment = createWaybillSegment(1, 2, "externalId", returnTo);
        WaybillSegment fromReturnSegment = createWaybillSegment(2, 2, "externalId 2", returnFrom);

        writeWaybillSegmentCheckpoint(fromReturnSegment, checkpointFrom, FIXED_TIME);

        order.setWaybill(List.of(toReturnSegment, fromReturnSegment));

        return toReturnSegment;
    }

    @Nonnull
    public WaybillSegment createScReturnSegment(
        Long returnSortingCenterId,
        Long partnerId,
        SegmentStatus checkpointFrom,
        SegmentType segmentType
    ) {
        Order order = createOrder(null);
        order.setReturnSortingCenterId(returnSortingCenterId);
        WaybillSegment scSegment = createWaybillSegment(0, partnerId, "externalId", segmentType);

        writeWaybillSegmentCheckpoint(scSegment, checkpointFrom, FIXED_TIME);

        order.setWaybill(List.of(scSegment));

        return scSegment;
    }

    @Nonnull
    public WaybillSegment createWaybillSegmentWithCheckpoint(
        @Nullable SegmentType segmentType,
        SegmentStatus checkpoint,
        @Nullable LocalDate deliveryDate
    ) {
        Order order = createOrder(deliveryDate);
        WaybillSegment segment = createWaybillSegment(0, 1, "externalId", segmentType);
        writeWaybillSegmentCheckpoint(segment, checkpoint, FIXED_TIME);
        order.setWaybill(List.of(segment));
        return segment;
    }

    @Nonnull
    public WaybillSegment createWaybillSegment(
        int index,
        long partnerId,
        String externalId,
        @Nullable SegmentType segmentType
    ) {
        WaybillSegment waybillSegment = new WaybillSegment()
            .setId(1L)
            .setWaybillSegmentIndex(index)
            .setPartnerId(partnerId)
            .setExternalId(externalId)
            .setPartnerType(PartnerType.DELIVERY);

        Optional.ofNullable(segmentType).ifPresent(waybillSegment::setSegmentType);

        return waybillSegment;
    }

    @Nonnull
    public Order createOrder(@Nullable LocalDate deliveryDate) {
        return Order.builder()
            .platformClient(PlatformClient.BERU)
            .deliveryInterval(
                Optional.ofNullable(deliveryDate)
                    .map(date -> new DeliveryInterval().setDateMax(date))
                    .orElse(null)
            )
            .build();
    }

    public Order joinInOrder(List<WaybillSegment> segments) {
        Order order = new Order()
            .setWaybill(segments);
        for (int index = 0; index < segments.size(); index++) {
            order.getWaybill().get(index).setWaybillSegmentIndex(index);
        }
        return order;
    }
}
