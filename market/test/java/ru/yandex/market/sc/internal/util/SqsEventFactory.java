package ru.yandex.market.sc.internal.util;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import ru.yandex.market.logistics.les.DiscrepancyActGeneratedEvent;
import ru.yandex.market.logistics.les.dto.CargoUnitDeleteSegmentDto;
import ru.yandex.market.logistics.les.dto.CargoUnitGroupType;
import ru.yandex.market.logistics.les.dto.CarrierDto;
import ru.yandex.market.logistics.les.dto.CarrierType;
import ru.yandex.market.logistics.les.dto.CodeDto;
import ru.yandex.market.logistics.les.dto.CodeType;
import ru.yandex.market.logistics.les.dto.InboundShipmentDto;
import ru.yandex.market.logistics.les.dto.OutboundShipmentDto;
import ru.yandex.market.logistics.les.dto.PartnerDto;
import ru.yandex.market.logistics.les.dto.PointDto;
import ru.yandex.market.logistics.les.dto.PointType;
import ru.yandex.market.logistics.les.tpl.Company;
import ru.yandex.market.logistics.les.tpl.CourierForSc;
import ru.yandex.market.logistics.les.tpl.StorageUnitCreateRequestEvent;
import ru.yandex.market.logistics.les.tpl.StorageUnitDeleteSegmentRequestEvent;
import ru.yandex.market.logistics.les.tpl.TplCourierReassignEvent;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.internal.sqs.SqsEvent;
import ru.yandex.market.sc.internal.sqs.SqsEventType;
import ru.yandex.market.sc.internal.util.les.LesModelFactory;
import ru.yandex.market.sc.internal.util.les.builder.CargoDtoBuilder;

import static ru.yandex.market.tpl.common.util.DateTimeUtil.DEFAULT_ZONE_ID;

/**
 * @author: dbryndin
 * @date: 12/3/21
 */
@Service
@AllArgsConstructor
public class SqsEventFactory {

    private final Clock clock;

    public SqsEvent<StorageUnitDeleteSegmentRequestEvent> createSegmentDeleteEvent(
            String segmentUuid,
            String cargoUnitId
    ) {
        var payload = new StorageUnitDeleteSegmentRequestEvent(
                "test_request_id",
                List.of(new CargoUnitDeleteSegmentDto(cargoUnitId, segmentUuid))

        );
        return makeSqsEvent(SqsEventType.STORAGE_UNIT_DELETE_SEGMENT_EVENT_TYPE, System.currentTimeMillis(), payload);
    }

    public SqsEvent<StorageUnitCreateRequestEvent> createClientReturnEvent(SortingCenter sc) {
        return createClientReturnEvent(sc.getToken(), sc.getYandexId(), UUID.randomUUID().toString(),
                "cargoUnitId", "123", null, null
        );
    }

    public SqsEvent<DiscrepancyActGeneratedEvent> createDiscrepancyActGeneratedEventSqsEvent(
            String transportationId,
            String bucket,
            String fileName,
            Boolean isDiscrepancyExists
    ) {
        var payload = new DiscrepancyActGeneratedEvent(transportationId, bucket, fileName, isDiscrepancyExists);
        return makeSqsEvent(SqsEventType.DISCREPANCY_ACT_GENERATED_EVENT, System.currentTimeMillis(), payload);
    }

    public SqsEvent<StorageUnitCreateRequestEvent> createClientReturnEvent(String token, String logisticsPointId) {
        return createClientReturnEvent(token, logisticsPointId, UUID.randomUUID().toString(),
                "cargoUnitId", "123", null, null
        );
    }

    public SqsEvent<StorageUnitCreateRequestEvent> createClientReturnEventWithSender(
            SortingCenter sc,
            CarrierDto sender
    ) {
        return createClientReturnEvent(sc.getToken(), sc.getYandexId(), UUID.randomUUID().toString(),
                "cargoUnitId", "123", sender, null
        );
    }

    public SqsEvent<StorageUnitCreateRequestEvent> createClientReturnEventWithCheckoutOrderId(SortingCenter sc,
                                                                                          String checkoutOrderId) {
        return createClientReturnEvent(sc.getToken(), sc.getYandexId(), UUID.randomUUID().toString(),
                "cargoUnitId", "123", null, checkoutOrderId
        );
    }

    public SqsEvent<StorageUnitCreateRequestEvent> createClientReturnEvent(
            String token, String logisticsPointId,
            String segmentUuid,
            String cargoUnitId, String barcode,
            CarrierDto sender, String checkoutOrderId
    ) {
        var payload = new StorageUnitCreateRequestEvent(
                cargoUnitId,
                new PartnerDto(1, token, logisticsPointId),
                List.of(new ru.yandex.market.logistics.les.dto.CargoUnitDto(
                        "1",
                        System.currentTimeMillis(),
                        segmentUuid,
                        null,
                        "cargoUnitGroupID",
                        List.of(new CodeDto(barcode, CodeType.CARGO_BARCODE), new CodeDto(barcode, CodeType.CARGO_BARCODE)),
                        null,
                        false,
                        null,
                        new InboundShipmentDto(LocalDateTime.now(clock).toInstant(DEFAULT_ZONE_ID), sender == null ?
                                new CarrierDto(CarrierType.SHOP, 1L, "name-0", null) : sender,
                                new PointDto(PointType.SHOP, 1L, 1L, "whname-0")
                        ),
                        new OutboundShipmentDto(
                                LocalDateTime.now(clock).toInstant(DEFAULT_ZONE_ID),
                                new CarrierDto(CarrierType.SHOP, 2L, "name-2", null),
                                new PointDto(PointType.SHOP, 2L, 2L, "whname-1")
                        )
                )),
                LesModelFactory.createCargoUnitGroup(CargoDtoBuilder.CargoUnitGroupDtoParams.builder()
                        .id("1")
                        .checkoutOrderId(checkoutOrderId != null
                                ? new CodeDto(checkoutOrderId, CodeType.ORDER_BARCODE) : null)
                        .assessedCost(new BigDecimal(1200))
                        .cargoUnitCount(1)
                        .isComplete(true)
                        .type(CargoUnitGroupType.RETURN_CLIENT)
                        .build()));

        return makeSqsEvent(SqsEventType.STORAGE_UNIT_CREATE, System.currentTimeMillis(), payload);
    }

    public SqsEvent<TplCourierReassignEvent> createCourierReassignEvent(
            Courier courierFrom,
            Courier courierTo,
            SortingCenter sortingCenter,
            LocalDate date) {
        var courierFromForSc = new CourierForSc(courierFrom.getId(), null, null);
        var courierToForSc = new CourierForSc(
                courierTo.getId(),
                courierTo.getName(),
                new Company(courierTo.getCompanyName())
        );
        var payload = new TplCourierReassignEvent(
                courierFromForSc,
                courierToForSc,
                date,
                sortingCenter.getId(),
                sortingCenter.getToken()
        );
        return makeSqsEvent(SqsEventType.COURIER_REASSIGN, System.currentTimeMillis(), payload);
    }

    public <T> SqsEvent<T> makeSqsEvent(SqsEventType sqsEventType, Long timestamp, T payload) {
        return new SqsEvent<>(
                "source-1",
                "event-1",
                timestamp,
                sqsEventType,
                payload
        );
    }

    public CarrierDto createCarrierDto(CarrierType carrierType) {
        return LesModelFactory.createCarrierDto(carrierType, 1L, LesModelFactory.createCourierDto(22L));
    }
}
