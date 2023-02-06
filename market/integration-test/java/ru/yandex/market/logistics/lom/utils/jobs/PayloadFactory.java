package ru.yandex.market.logistics.lom.utils.jobs;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;

import ru.yandex.market.logistics.lom.admin.dto.ChangeOrderReturnSegmentDto;
import ru.yandex.market.logistics.lom.controller.tracker.dto.DeliveryTrack;
import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.entity.enums.ApiType;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderShipmentDatePayload;
import ru.yandex.market.logistics.lom.jobs.model.CreateOrderErrorPayload;
import ru.yandex.market.logistics.lom.jobs.model.CreateOrderSuccessPayload;
import ru.yandex.market.logistics.lom.jobs.model.DeliveryTrackIdsPayload;
import ru.yandex.market.logistics.lom.jobs.model.ExportOrderFromShipmentExclusionFinishedPayload;
import ru.yandex.market.logistics.lom.jobs.model.LesOrderArrivedPickupPointEventPayload;
import ru.yandex.market.logistics.lom.jobs.model.LesOrderEventPayload;
import ru.yandex.market.logistics.lom.jobs.model.LesWaybillEventPayload;
import ru.yandex.market.logistics.lom.jobs.model.LogbrokerSourceIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.MdsFileIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.MultipleChangeOrderReturnSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.NotifyOrderErrorToMqmPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderCancellationRequestIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdDeliveryTrackPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPartnerIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdSegmentStatusesPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessOrderProcessingDelayPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingErrorPayload;
import ru.yandex.market.logistics.lom.jobs.model.RegistryIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.ReturnRegistryIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.SegmentCancellationRequestIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.ShipmentApplicationIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.TrustOrderIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.UploadOrderLabelPayload;
import ru.yandex.market.logistics.lom.jobs.model.WaybillSegmentIdPayload;
import ru.yandex.market.logistics.lom.jobs.processor.OrderIdPartnerIdWaybillSegmentIdPayload;
import ru.yandex.market.logistics.lom.model.async.CreateOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.CreateOrderSuccessDto;
import ru.yandex.market.logistics.mqm.model.enums.EventType;
import ru.yandex.market.logistics.mqm.model.enums.ProcessType;

import static ru.yandex.market.logistics.lom.AbstractContextualTest.REQUEST_ID;

@ParametersAreNonnullByDefault
public final class PayloadFactory {

    private PayloadFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static OrderIdPayload createOrderIdPayload(long orderId, long... subRequestIds) {
        return createOrderIdPayload(orderId, null, subRequestIds);
    }

    @Nonnull
    public static OrderIdWaybillSegmentPayload createWaybillSegmentPayload(
        long orderId,
        long waybillSegmentId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new OrderIdWaybillSegmentPayload(
            computeRequestId(subRequestIds),
            orderId,
            waybillSegmentId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static OrderIdWaybillSegmentPayload createWaybillSegmentPayload(
        long orderId,
        long waybillSegmentId,
        long... subRequestIds
    ) {
        return createWaybillSegmentPayload(orderId, waybillSegmentId, null, subRequestIds);
    }

    @Nonnull
    public static OrderIdPartnerIdWaybillSegmentIdPayload createOrderIdPartnerIdWaybillSegmentIdPayload(
        long orderId,
        long partnerId,
        long waybillSegmentId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new OrderIdPartnerIdWaybillSegmentIdPayload(
            computeRequestId(subRequestIds),
            orderId,
            partnerId,
            waybillSegmentId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static WaybillSegmentIdPayload createWaybillSegmentIdPayload(
        long waybillSegmentId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new WaybillSegmentIdPayload(
            computeRequestId(subRequestIds),
            waybillSegmentId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static OrderIdPayload createOrderIdPayload(
        long orderId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new OrderIdPayload(
            computeRequestId(subRequestIds),
            orderId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static TrustOrderIdPayload createTrustOrderIdPayload(
        long orderId,
        long trustOrderId,
        long... subRequestIds
    ) {
        return new TrustOrderIdPayload(computeRequestId(subRequestIds), orderId, trustOrderId);
    }

    @Nonnull
    public static ShipmentApplicationIdPayload createShipmentApplicationIdPayload(
        long shipmentApplicationId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new ShipmentApplicationIdPayload(
            computeRequestId(subRequestIds),
            shipmentApplicationId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static ShipmentApplicationIdPayload createShipmentApplicationIdPayload(
        long shipmentApplicationId,
        long... subRequestIds
    ) {
        return createShipmentApplicationIdPayload(shipmentApplicationId, null, subRequestIds);
    }

    @Nonnull
    public static OrderIdPartnerIdPayload createOrderIdPartnerIdPayload(
        long orderId,
        long partnerId,
        long... subRequestIds
    ) {
        return createOrderIdPartnerIdPayload(orderId, partnerId, null, subRequestIds);
    }

    @Nonnull
    public static OrderIdPartnerIdPayload createOrderIdPartnerIdPayload(
        long orderId,
        long partnerId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new OrderIdPartnerIdPayload(
            computeRequestId(subRequestIds),
            orderId,
            partnerId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static RegistryIdPayload createRegistryIdPayload(long registryId, long... subRequestIds) {
        return createRegistryIdPayload(registryId, null, subRequestIds);
    }

    @Nonnull
    public static RegistryIdPayload createRegistryIdPayload(
        long registryId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new RegistryIdPayload(computeRequestId(subRequestIds), registryId);
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static OrderIdSegmentStatusesPayload createOrderIdSegmentStatusesPayload(
        long orderId,
        List<LomSegmentCheckpoint> checkpoints,
        long... subRequestIds
    ) {
        return createOrderIdSegmentStatusesPayload(orderId, checkpoints, new OrderHistoryEventAuthor(), subRequestIds);
    }

    @Nonnull
    public static OrderIdSegmentStatusesPayload createOrderIdSegmentStatusesPayload(
        long orderId,
        List<LomSegmentCheckpoint> checkpoints,
        @Nullable OrderHistoryEventAuthor author,
        long... subRequestIds
    ) {
        return new OrderIdSegmentStatusesPayload(computeRequestId(subRequestIds), orderId, checkpoints, author);
    }

    @Nonnull
    public static UploadOrderLabelPayload createUploadOrderLabelPayload(
        long orderId,
        long partnerId,
        String url,
        long... subRequestIds
    ) {
        return new UploadOrderLabelPayload(computeRequestId(subRequestIds), orderId, partnerId, url);
    }

    @Nonnull
    public static UploadOrderLabelPayload createUploadOrderLabelPayload(
        long orderId,
        long partnerId,
        long... subRequestIds
    ) {
        return createUploadOrderLabelPayload(orderId, partnerId, "https://some.url", subRequestIds);
    }

    @Nonnull
    public static ReturnRegistryIdPayload createReturnRegistryIdPayload(
        long returnRegistryIdPayload,
        long... subRequestIds
    ) {
        return createReturnRegistryIdPayload(returnRegistryIdPayload, null, subRequestIds);
    }

    @Nonnull
    public static ReturnRegistryIdPayload createReturnRegistryIdPayload(
        long returnRegistryIdPayload,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new ReturnRegistryIdPayload(
            computeRequestId(subRequestIds),
            returnRegistryIdPayload
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static OrderCancellationRequestIdPayload createOrderCancellationRequestIdPayload(
        long orderCancellationRequestId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new OrderCancellationRequestIdPayload(
            computeRequestId(subRequestIds),
            orderCancellationRequestId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static OrderCancellationRequestIdPayload createOrderCancellationRequestIdPayload(
        long orderCancellationRequestId,
        long... subRequestIds
    ) {
        return createOrderCancellationRequestIdPayload(orderCancellationRequestId, null, subRequestIds);
    }

    @Nonnull
    public static SegmentCancellationRequestIdPayload createSegmentCancellationRequestIdPayload(
        long segmentCancellationRequestId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new SegmentCancellationRequestIdPayload(
            computeRequestId(subRequestIds),
            segmentCancellationRequestId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static SegmentCancellationRequestIdPayload createSegmentCancellationRequestIdPayload(
        long segmentCancellationRequestId,
        long... subRequestIds
    ) {
        return createSegmentCancellationRequestIdPayload(segmentCancellationRequestId, null, subRequestIds);
    }

    @Nonnull
    public static OrderIdDeliveryTrackPayload createOrderIdDeliveryTrackPayload(
        long orderId,
        DeliveryTrack deliveryTrack,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        return createOrderIdDeliveryTrackPayload(
            orderId,
            deliveryTrack,
            new OrderHistoryEventAuthor(),
            sequenceId,
            subRequestIds
        );
    }

    @Nonnull
    public static OrderIdDeliveryTrackPayload createOrderIdDeliveryTrackPayload(
        long orderId,
        DeliveryTrack deliveryTrack,
        long absServiceId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        OrderHistoryEventAuthor author = new OrderHistoryEventAuthor();
        author.setTvmServiceId(absServiceId);
        return createOrderIdDeliveryTrackPayload(
            orderId,
            deliveryTrack,
            author,
            sequenceId,
            subRequestIds
        );
    }

    @Nonnull
    public static OrderIdDeliveryTrackPayload createOrderIdDeliveryTrackPayload(
        long orderId,
        DeliveryTrack deliveryTrack,
        OrderHistoryEventAuthor author,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new OrderIdDeliveryTrackPayload(
            computeRequestId(subRequestIds),
            orderId,
            deliveryTrack,
            author
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static OrderIdDeliveryTrackPayload createOrderIdDeliveryTrackPayload(
        long orderId,
        DeliveryTrack deliveryTrack,
        long... subRequestIds
    ) {
        return createOrderIdDeliveryTrackPayload(orderId, deliveryTrack, null, subRequestIds);
    }

    @Nonnull
    public static ChangeOrderRequestPayload createChangeOrderRequestPayload(
        long changeOrderRequestId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new ChangeOrderRequestPayload(
            computeRequestId(subRequestIds),
            changeOrderRequestId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static DeliveryTrackIdsPayload createDeliveryTrackIdsPayload(
        Set<Long> trackIds,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new DeliveryTrackIdsPayload(
            computeRequestId(subRequestIds),
            trackIds
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static ChangeOrderSegmentRequestPayload createChangeOrderSegmentRequestPayload(
        long changeOrderSegmentRequestId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new ChangeOrderSegmentRequestPayload(
            computeRequestId(subRequestIds),
            changeOrderSegmentRequestId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static CreateOrderSuccessPayload createOrderSuccessPayload(
        ApiType apiType,
        long partnerId,
        long waybillId,
        long orderId,
        CreateOrderSuccessDto createOrderSuccessDto,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new CreateOrderSuccessPayload(
            computeRequestId(subRequestIds),
            apiType,
            partnerId,
            waybillId,
            orderId,
            createOrderSuccessDto
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static CreateOrderErrorPayload createOrderErrorPayload(
        ApiType apiType,
        long partnerId,
        long waybillId,
        long orderId,
        CreateOrderErrorDto createOrderErrorDto,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new CreateOrderErrorPayload(
            computeRequestId(subRequestIds),
            apiType,
            partnerId,
            waybillId,
            orderId,
            createOrderErrorDto
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static OrderIdAuthorPayload createOrderIdAuthorPayload(
        long orderId,
        @Nullable OrderHistoryEventAuthor author,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new OrderIdAuthorPayload(
            computeRequestId(subRequestIds),
            orderId,
            author
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static MultipleChangeOrderReturnSegmentPayload multipleChangeOrderReturnSegmentPayload(
        List<ChangeOrderReturnSegmentDto> changes,
        OrderHistoryEventAuthor author,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new MultipleChangeOrderReturnSegmentPayload(
            computeRequestId(subRequestIds),
            changes,
            author
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static MdsFileIdAuthorPayload mdsFileIdAuthorPayload(
        long mdsFileId,
        OrderHistoryEventAuthor author,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new MdsFileIdAuthorPayload(
            computeRequestId(subRequestIds),
            mdsFileId,
            author
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static ChangeOrderShipmentDatePayload changeOrderShipmentDatePayload(
        long orderId,
        LocalDate shipmentDate,
        OrderHistoryEventAuthor author,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new ChangeOrderShipmentDatePayload(
            computeRequestId(subRequestIds),
            orderId,
            shipmentDate,
            author
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static ProcessingErrorPayload processingErrorPayload(
        ProcessType processType,
        Long entityId,
        Long processId,
        Integer errorCode,
        String errorMessage,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new ProcessingErrorPayload(
            computeRequestId(subRequestIds),
            processType,
            entityId,
            processId,
            errorCode,
            errorMessage
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static LogbrokerSourceIdPayload logbrokerSourceIdPayload(
        int logbrokerSourceId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new LogbrokerSourceIdPayload(computeRequestId(subRequestIds), logbrokerSourceId);
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    private static String computeRequestId(long... subRequestIds) {
        return StreamEx.of(REQUEST_ID)
            .append(Arrays.stream(subRequestIds).mapToObj(Long::toString))
            .collect(Collectors.joining("/"));
    }

    @Nonnull
    public static LesOrderEventPayload lesOrderEventPayload(
        long eventId,
        long orderId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new LesOrderEventPayload(
            computeRequestId(subRequestIds),
            eventId,
            orderId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static LesWaybillEventPayload lesWaybillEventPayload(
        long eventId,
        long waybillSegmentId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new LesWaybillEventPayload(computeRequestId(subRequestIds), eventId, waybillSegmentId);
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static LesOrderArrivedPickupPointEventPayload lesOrderArrivedPickupPointEventPayload(
        long eventId,
        long orderId,
        Instant deliveryDate,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new LesOrderArrivedPickupPointEventPayload(
            computeRequestId(subRequestIds),
            eventId,
            orderId,
            deliveryDate
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static ProcessOrderProcessingDelayPayload processOrderProcessingDelayPayload(
        long orderId,
        long waybillSegmentId,
        long excludeOrderFromShipmentRequestId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        var payload = new ProcessOrderProcessingDelayPayload(
            computeRequestId(subRequestIds),
            orderId,
            waybillSegmentId,
            excludeOrderFromShipmentRequestId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    public static ExportOrderFromShipmentExclusionFinishedPayload exportOrderFromShipmentExclusionFinishedPayload(
        long eventId,
        long orderId,
        long excludeOrderFromShipmentRequestId,
        long changeOrderRequestId,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        ExportOrderFromShipmentExclusionFinishedPayload payload = new ExportOrderFromShipmentExclusionFinishedPayload(
            computeRequestId(subRequestIds),
            eventId,
            orderId,
            excludeOrderFromShipmentRequestId,
            changeOrderRequestId
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }

    @Nonnull
    @SuppressWarnings("ParameterNumber")
    public static NotifyOrderErrorToMqmPayload notifyOrderErrorToMqmPayload(
        long orderId,
        Long errorProcessSequenceId,
        String externalId,
        EventType eventType,
        @Nullable String eventCode,
        Map<String, String> mqmPayloadParams,
        @Nullable String sequenceId,
        long... subRequestIds
    ) {
        NotifyOrderErrorToMqmPayload payload = new NotifyOrderErrorToMqmPayload(
            computeRequestId(subRequestIds),
            orderId,
            errorProcessSequenceId,
            externalId,
            eventType,
            eventCode,
            mqmPayloadParams
        );
        Optional.ofNullable(sequenceId).map(Long::parseLong).ifPresent(payload::setSequenceId);
        return payload;
    }
}
