package ru.yandex.market.sc.internal.util.les;

import java.util.List;
import java.util.UUID;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.dto.CarDto;
import ru.yandex.market.logistics.les.dto.CargoUnitDto;
import ru.yandex.market.logistics.les.dto.CargoUnitGroupDto;
import ru.yandex.market.logistics.les.dto.CarrierDto;
import ru.yandex.market.logistics.les.dto.CarrierType;
import ru.yandex.market.logistics.les.dto.CourierDto;
import ru.yandex.market.logistics.les.dto.InboundShipmentDto;
import ru.yandex.market.logistics.les.dto.OutboundShipmentDto;
import ru.yandex.market.logistics.les.dto.PartnerDto;
import ru.yandex.market.logistics.les.dto.PersonDto;
import ru.yandex.market.logistics.les.dto.PhoneDto;
import ru.yandex.market.logistics.les.dto.PointDto;
import ru.yandex.market.logistics.les.dto.PointType;
import ru.yandex.market.logistics.les.tpl.StorageUnitCreateRequestEvent;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.internal.sqs.SqsEventType;
import ru.yandex.market.sc.internal.util.les.builder.CargoDtoBuilder;
import ru.yandex.market.sc.internal.util.les.builder.EventBuilder;

/**
 * @author: dbryndin
 * @date: 3/21/22
 */
public class LesModelFactory {

    public static Event createStorageCargoUnitCreateEvent(CargoDtoBuilder.CargoUnitDtoParams paramsCargoUnit,
                                                          CargoDtoBuilder.CargoUnitGroupDtoParams cargoUnitGroupDtoParams,
                                                          SortingCenter sc) {

        return createEvent(
                EventBuilder.EventParams.builder()
                        .eventType(SqsEventType.STORAGE_UNIT_CREATE.getEventTypeName())
                        .payload(new StorageUnitCreateRequestEvent(
                                UUID.randomUUID().toString(),
                                createPartner(sc),
                                List.of(createCargoUnit(paramsCargoUnit)),
                                createCargoUnitGroup(cargoUnitGroupDtoParams)
                        ))
                        .build()
        );
    }

    public static PartnerDto createPartner(SortingCenter sc) {
        return new PartnerDto(
                sc.getId(),
                sc.getToken(),
                sc.getYandexId()
        );
    }

    public static CargoUnitDto createCargoUnit(CargoDtoBuilder.CargoUnitDtoParams params) {
        return new CargoUnitDto(
                params.getId(),
                params.getTimestamp(),
                params.getSegmentUuid(),
                params.getParentId(),
                params.getCargoUnitGroupId(),
                params.getCodesCodeDto(),
                params.getKorobyte(),
                params.getIsDamaged(),
                params.getAssessedCost(),
                params.getInboundShipment(),
                params.getOutboundShipment()
        );
    }

    public static CargoUnitGroupDto createCargoUnitGroup(CargoDtoBuilder.CargoUnitGroupDtoParams params) {
        return new CargoUnitGroupDto(
                params.getId(),
                params.getCheckoutId(),
                params.getCheckoutOrderId(),
                params.getAssessedCost(),
                params.getCargoUnitCount(),
                params.getIsComplete(),
                params.getType()
        );
    }

    public static InboundShipmentDto createInboundShipmentDto(CargoDtoBuilder.InboundShipmentDtoParams params) {
        return new InboundShipmentDto(params.getDateTime(), params.getSender(), params.getSource());
    }

    public static OutboundShipmentDto createOutboundShipmentDto(CargoDtoBuilder.OutboundShipmentDtoParams params) {
        return new OutboundShipmentDto(params.getDateTime(), params.getRecipient(), params.getDestination());
    }

    public static Event createEvent(EventBuilder.EventParams eventParams) {
        return new Event(
                eventParams.getSource(),
                eventParams.getEventId(),
                eventParams.getTimestamp(),
                eventParams.getEventType(),
                eventParams.getPayload(),
                eventParams.getDescription()
        );
    }

    public static PointDto createPointDto(PointType type, Long id,  Long shopId, String name) {
        return new PointDto(type, id, shopId, name);
    }

    public static CarrierDto createCarrierDto(CarrierType type, Long id, CourierDto courierDto) {
        return new CarrierDto(type, id, "", courierDto);
    }

    public static CourierDto createCourierDto(Long id) {
        return new CourierDto(id, id, 2L,
                new PersonDto("Наруто", "Узумаки", null),
                new PhoneDto("88002000600", null),
                new CarDto("333", "car"),
                null);
    }
    public static CourierDto unknownCourier() {
        return new CourierDto(404L, null, 2L,
                new PersonDto("NOT_FOUND", "NOT_FOUND", null),
                new PhoneDto("88002000600", null),
                new CarDto("333", "car"),
                null);
    }
}
