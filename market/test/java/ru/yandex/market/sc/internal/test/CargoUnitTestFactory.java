package ru.yandex.market.sc.internal.test;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.base.EventPayload;
import ru.yandex.market.logistics.les.dto.CargoUnitGroupType;
import ru.yandex.market.logistics.les.dto.CarrierType;
import ru.yandex.market.logistics.les.dto.CodeDto;
import ru.yandex.market.logistics.les.dto.CodeType;
import ru.yandex.market.logistics.les.dto.PartnerDto;
import ru.yandex.market.logistics.les.dto.PointDto;
import ru.yandex.market.logistics.les.dto.PointType;
import ru.yandex.market.logistics.les.tpl.StorageUnitCreateRequestEvent;
import ru.yandex.market.logistics.les.tpl.StorageUnitCreateResponseEvent;
import ru.yandex.market.logistics.les.tpl.StorageUnitDeleteSegmentResponseEvent;
import ru.yandex.market.logistics.les.tpl.StorageUnitUpdateSegmentRequestEvent;
import ru.yandex.market.logistics.les.tpl.StorageUnitUpdateSegmentResponseEvent;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.ScOrderWithPlace;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.sqs.SqsEvent;
import ru.yandex.market.sc.internal.sqs.SqsEventType;
import ru.yandex.market.sc.internal.sqs.handler.CargoUnitCreateHandler;
import ru.yandex.market.sc.internal.sqs.handler.CargoUnitUpdateSegmentHandler;
import ru.yandex.market.sc.internal.util.les.LesModelFactory;
import ru.yandex.market.sc.internal.util.les.builder.CargoDtoBuilder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author valter
 */
public class CargoUnitTestFactory {

    @Autowired
    TestFactory testFactory;
    @Autowired
    CargoUnitCreateHandler cargoUnitCreateHandler;
    @Autowired
    CargoUnitUpdateSegmentHandler cargoUnitUpdateSegmentHandler;
    @Autowired
    Clock clock;


    public CreateCargoUnitResponse createCargoUpdateSegment(SortingCenter sortingCenter,
                                                            String cargoUnitId,
                                                            String externalOrderId,
                                                            String placeBarcode,
                                                            String segmentUid,
                                                            PointDto warehouseReturn) {
        var cargoUnitGroup = CargoDtoBuilder.CargoUnitGroupDtoParams.builder()
                .id(externalOrderId)
                .type(CargoUnitGroupType.CANCELLATION_RETURN)
                .build();
        var cargoUnit = CargoDtoBuilder.CargoUnitDtoParams.builder()
                .id(cargoUnitId)
                .segmentUuid(segmentUid)
                .codesCodeDto(List.of(
                        new CodeDto(externalOrderId, CodeType.ORDER_BARCODE),
                        new CodeDto(placeBarcode, CodeType.CARGO_BARCODE)
                ))
                .cargoUnitGroupId(cargoUnitGroup.getId())
                .outboundShipment(LesModelFactory.createOutboundShipmentDto(
                        CargoDtoBuilder.OutboundShipmentDtoParams.builder()
                                .recipient(LesModelFactory.createCarrierDto(CarrierType.DELIVERY_SERVICE_WITH_COURIER,
                                        22L, LesModelFactory.createCourierDto(33L)))
                                .destination(warehouseReturn)
                                .dateTime(clock.instant())
                                .build()
                ))
                .segmentUuid(segmentUid)
                .build();
        String eventId = "event_" + segmentUid;
        var response = (StorageUnitUpdateSegmentResponseEvent) cargoUnitUpdateSegmentHandler.handle(new SqsEvent<>(
                "lrm", eventId, clock.millis(),
                SqsEventType.CHANGE_RETURN_SEGMENTS_SHIPMENT,
                new StorageUnitUpdateSegmentRequestEvent(
                        eventId,
                        partnerDto(sortingCenter),
                        List.of(LesModelFactory.createCargoUnit(cargoUnit)),
                        LesModelFactory.createCargoUnitGroup(cargoUnitGroup)
                )));
        var errors = response.getResult().getErrors();
        if (errors.isEmpty()) {
            var order = testFactory.findOrder(externalOrderId, sortingCenter);
            return new CreateCargoUnitResponse(
                    new ScOrderWithPlace(
                            order,
                            testFactory.orderPlace(order, placeBarcode)
                    ),
                    response
            );
        } else {
            return new CreateCargoUnitResponse(
                    null,
                    response
            );
        }
    }

    public CreateCargoUnitResponse createCargoUnitFromOrder(
            SortingCenter sortingCenter,
            String externalOrderId, String placeBarcode,
            String cargoUnitId, String segmentUid,
            PointDto warehouseReturn
    ) {
        var cargoUnitGroup = CargoDtoBuilder.CargoUnitGroupDtoParams.builder()
                .id(externalOrderId)
                .type(CargoUnitGroupType.CANCELLATION_RETURN)
                .build();
        var cargoUnit = CargoDtoBuilder.CargoUnitDtoParams.builder()
                .id(cargoUnitId)
                .segmentUuid(segmentUid)
                .codesCodeDto(List.of(
                        new CodeDto(externalOrderId, CodeType.ORDER_BARCODE),
                        new CodeDto(placeBarcode, CodeType.CARGO_BARCODE)
                ))
                .cargoUnitGroupId(cargoUnitGroup.getId())
                .inboundShipment(LesModelFactory.createInboundShipmentDto(
                        CargoDtoBuilder.InboundShipmentDtoParams.builder()
                                .sender(LesModelFactory.createCarrierDto(CarrierType.DELIVERY_SERVICE_WITH_COURIER,
                                        11L, LesModelFactory.createCourierDto(22L)))
                                .source(LesModelFactory.createPointDto(PointType.SORTING_CENTER, 33L, 33L, "sc1"))
                                .dateTime(clock.instant())
                                .build()
                ))
                .outboundShipment(LesModelFactory.createOutboundShipmentDto(
                        CargoDtoBuilder.OutboundShipmentDtoParams.builder()
                                .recipient(LesModelFactory.createCarrierDto(CarrierType.DELIVERY_SERVICE_WITH_COURIER,
                                        22L, LesModelFactory.createCourierDto(33L)))
                                .destination(warehouseReturn)
                                .dateTime(clock.instant())
                                .build()
                ))
                .segmentUuid(segmentUid)
                .build();
        String eventId = "event_" + segmentUid;
        var response = (StorageUnitCreateResponseEvent) cargoUnitCreateHandler.handle(new SqsEvent<>(
                "lrm", eventId, clock.millis(),
                SqsEventType.STORAGE_UNIT_CREATE,
                new StorageUnitCreateRequestEvent(
                        eventId,
                        partnerDto(sortingCenter),
                        List.of(LesModelFactory.createCargoUnit(cargoUnit)),
                        LesModelFactory.createCargoUnitGroup(cargoUnitGroup)
                )));
        var order = testFactory.findOrder(externalOrderId, sortingCenter);
        return new CreateCargoUnitResponse(
                new ScOrderWithPlace(
                        order,
                        testFactory.orderPlace(order, placeBarcode)
                ),
                response
        );
    }

    private PartnerDto partnerDto(SortingCenter sortingCenter) {
        return new PartnerDto(
                Long.parseLong(Objects.requireNonNull(sortingCenter.getPartnerId())),
                Objects.requireNonNull(sortingCenter.getToken()),
                Objects.requireNonNull(sortingCenter.getYandexId())
        );
    }

    public record CreateCargoUnitResponse(ScOrderWithPlace orderWithPlace, EventPayload response) {

    }

}
