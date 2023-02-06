package ru.yandex.market.sc.internal.sqs.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.les.dto.CargoUnitGroupType;
import ru.yandex.market.logistics.les.dto.CarrierType;
import ru.yandex.market.logistics.les.dto.CodeDto;
import ru.yandex.market.logistics.les.dto.CodeType;
import ru.yandex.market.logistics.les.dto.PartnerDto;
import ru.yandex.market.logistics.les.dto.PointDto;
import ru.yandex.market.logistics.les.dto.PointType;
import ru.yandex.market.logistics.les.dto.StorageUnitResponseErrorDto;
import ru.yandex.market.logistics.les.dto.StorageUnitResponseErrorType;
import ru.yandex.market.logistics.les.tpl.StorageUnitCreateRequestEvent;
import ru.yandex.market.logistics.les.tpl.StorageUnitCreateResponseEvent;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.courier.repository.CourierMapper;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.stage.Stages;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.sqs.SqsEventType;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.SqsEventFactory;
import ru.yandex.market.sc.internal.util.les.LesModelFactory;
import ru.yandex.market.sc.internal.util.les.builder.CargoDtoBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN;
import static ru.yandex.market.sc.core.domain.order.repository.FakeOrderType.CLIENT_RETURN;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author: dbryndin
 * @date: 11/29/21
 */

@EmbeddedDbIntTest
public class SqsCargoCreateHandlerTest {

    @Autowired
    private Clock clock;

    @Autowired
    private CargoUnitCreateHandler cargoUnitCreateHandler;

    @Autowired
    private TestFactory testFactory;

    @Autowired
    private SqsEventFactory sqsEventFactory;

    private SortingCenter sortingCenter;

    @Autowired
    private ScOrderRepository scOrderRepository;
    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");
        testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
    }

    @Test
    @DisplayName("success создание клиентского возврата")
    public void successClientReturnCreate() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var event = sqsEventFactory.createClientReturnEvent(sortingCenter);
        var responsePayload = cargoUnitCreateHandler.handle(event);
        List<StorageUnitResponseErrorDto> errors =
                ((StorageUnitCreateResponseEvent) responsePayload).getResult().getErrors();
        assertTrue(errors.isEmpty(), "must be empty, but :" + errors);

        var clientReturnLockerOrder = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
        assertThat(clientReturnLockerOrder.getWarehouseFrom().getId())
                .isNotEqualTo(clientReturnLockerOrder.getWarehouseReturn().getId());

    }

    @Test
    @DisplayName("success создание клиентского возврата")
    public void successClientReturnCreate1() {
        testFactory.storedWarehouse();
        testFactory.storedFakeReturnDeliveryService();
        var event = sqsEventFactory.createClientReturnEvent(sortingCenter);
        var responsePayload = cargoUnitCreateHandler.handle(event);
        List<StorageUnitResponseErrorDto> errors =
                ((StorageUnitCreateResponseEvent) responsePayload).getResult().getErrors();
        assertTrue(errors.isEmpty(), "must be empty, but :" + errors);

        var clientReturnLockerOrder = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
    }

    @Test
    @DisplayName("success создание клиентского возврата (по токену получаем несколько партнеров и ищем нужный)")
    public void successClientReturnCreate2() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();

        var sortingCenterPartner = testFactory.storedSortingCenterPartner(150, "nonuniq_token");

        var sortingCenter = testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .sortingCenterPartnerId(sortingCenterPartner.getId())
                .token("nonuniq_token")
                .yandexId("yandex-id-0")
                .build());
        testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .id(1)
                .token("nonuniq_token1")
                .yandexId("yandex-id-1")
                .sortingCenterPartnerId(sortingCenterPartner.getId())
                .build());
        testFactory.storedSortingCenter(TestFactory.SortingCenterParams.builder()
                .id(2)
                .token("nonuniq_token1")
                .yandexId("yandex-id-2")
                .sortingCenterPartnerId(sortingCenterPartner.getId())
                .build());

        var event = sqsEventFactory.createClientReturnEvent("nonuniq_token", "yandex-id-0");

        var responsePayload = cargoUnitCreateHandler.handle(event);
        List<StorageUnitResponseErrorDto> errors =
                ((StorageUnitCreateResponseEvent) responsePayload).getResult().getErrors();
        assertTrue(errors.isEmpty(), "must be empty, but :" + errors);

        var clientReturnLockerOrder = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.getSortingCenter().getYandexId()).isEqualTo("yandex-id-0");
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
    }

    @Test
    @DisplayName("success создание клиентского возврата с проставлением стейджей на посылках прямого потока")
    public void successClientReturnCreate3() {
        testFactory.storedWarehouse();
        testFactory.storedFakeReturnDeliveryService();
        var order = testFactory.createForToday(order(sortingCenter).createTwoPlaces(true).build())
                .accept().sort().ship().get();
        var event = sqsEventFactory
                .createClientReturnEventWithCheckoutOrderId(sortingCenter, order.getExternalId());
        var responsePayload = cargoUnitCreateHandler.handle(event);
        List<StorageUnitResponseErrorDto> errors =
                ((StorageUnitCreateResponseEvent) responsePayload).getResult().getErrors();
        assertTrue(errors.isEmpty(), "must be empty, but :" + errors);

        var places = placeRepository.findAllByOrderIdOrderById(order.getId());
        places.forEach(place -> assertThat(place.getStageId()).isEqualTo(Stages.SHIPPED_DIRECT_REPLACED.getId()));

        //сканируют ШК посылки прямого потока
        assertThatThrownBy(() -> testFactory.acceptPlace(places.get(0)))
                .hasMessage(ScErrorCode.CANT_ACCEPT_SHIPPED_DIRECT_REPLACED_PLACE.getMessage())
                .isInstanceOf(ScException.class);
    }

    @Test
    @DisplayName("success создание клиентского возврата sender type DELIVERY_SERVICE_WITH_COURIER")
    public void successClientReturnCreateDeliveryServiceWithCourier() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var carrierDto = sqsEventFactory.createCarrierDto(CarrierType.DELIVERY_SERVICE_WITH_COURIER);
        var event = sqsEventFactory.createClientReturnEventWithSender(sortingCenter,
                carrierDto);

        var responsePayload = cargoUnitCreateHandler.handle(event);
        List<StorageUnitResponseErrorDto> errors =
                ((StorageUnitCreateResponseEvent) responsePayload).getResult().getErrors();
        assertTrue(errors.isEmpty(), "must be empty, but :" + errors);

        var clientReturnLockerOrder = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();

        assertThat(clientReturnLockerOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);

        assertThat(clientReturnLockerOrder.getCourier().getId()).isEqualTo(carrierDto.getCourier().getUid());
        assertThat(clientReturnLockerOrder.getCourier().getDeliveryServiceId()).isNull();
        assertThat(clientReturnLockerOrder.getDeliveryService().getYandexId()).isEqualTo(String.valueOf(carrierDto.getId()));
    }

    @Test
    @DisplayName("success создание клиентского возврата sender type DELIVERY_SERVICE")
    public void successClientReturnCreateDeliveryService() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var carrierDto = sqsEventFactory.createCarrierDto(CarrierType.DELIVERY_SERVICE);
        var event = sqsEventFactory.createClientReturnEventWithSender(sortingCenter,
                carrierDto);

        var responsePayload = cargoUnitCreateHandler.handle(event);
        List<StorageUnitResponseErrorDto> errors =
                ((StorageUnitCreateResponseEvent) responsePayload).getResult().getErrors();
        assertTrue(errors.isEmpty(), "must be empty, but :" + errors);

        var clientReturnLockerOrder = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();

        assertThat(clientReturnLockerOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);

        assertThat(clientReturnLockerOrder.getCourier().getId()).isEqualTo(CourierMapper.mapDeliveryServiceIdToCourierId(carrierDto.getId()));
        assertThat(clientReturnLockerOrder.getDeliveryService().getYandexId()).isEqualTo(String.valueOf(carrierDto.getId()));
    }

    @Test
    @DisplayName("success создание клиентского возврата sender type SHOP")
    public void successClientReturnCreateShop() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var carrierDto = sqsEventFactory.createCarrierDto(CarrierType.SHOP);
        var event = sqsEventFactory.createClientReturnEventWithSender(sortingCenter,
                carrierDto);

        var responsePayload = cargoUnitCreateHandler.handle(event);
        List<StorageUnitResponseErrorDto> errors =
                ((StorageUnitCreateResponseEvent) responsePayload).getResult().getErrors();
        assertTrue(errors.isEmpty(), "must be empty, but :" + errors);

        var clientReturnLockerOrder = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();

        assertThat(clientReturnLockerOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);

        assertThat(clientReturnLockerOrder.getCourier().getName()).isEqualTo(ClientReturnService.CLIENT_RETURN_COURIER);
        assertThat(clientReturnLockerOrder.getDeliveryService().getYandexId()).isEqualTo(ClientReturnService.DELIVERY_SERVICE_YA_ID);
    }

    @Test
    @DisplayName("success создание и после обновление сегмента клиентского возврата ")
    public void successClientReturnUpdate() {
        testFactory.storedWarehouse();
        testFactory.storedFakeReturnDeliveryService();
        var event = sqsEventFactory.createClientReturnEvent(sortingCenter);
        var responsePayload = cargoUnitCreateHandler.handle(event);
        List<StorageUnitResponseErrorDto> errors =
                ((StorageUnitCreateResponseEvent) responsePayload).getResult().getErrors();
        assertTrue(errors.isEmpty(), "must be empty, but :" + errors);

        var clientReturnLockerOrder = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);

        var uploadEvent = sqsEventFactory.createClientReturnEvent(sortingCenter);
        var responsePayload0 = cargoUnitCreateHandler.handle(uploadEvent);
        errors = ((StorageUnitCreateResponseEvent) responsePayload0).getResult().getErrors();
        assertTrue(errors.isEmpty(), "must be empty, but :" + errors);

        var updatedClientReturnOrder = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();
        assertThat(updatedClientReturnOrder.getSegmentUid()).isEqualTo(uploadEvent.payload().getCargoUnits().get(0).getSegmentUuid());

        var updatedPlace = placeRepository.findAllByOrderIdOrderById(updatedClientReturnOrder.getId());

        assertThat(updatedPlace.get(0).getSegmentUid()).isEqualTo(uploadEvent.payload().getCargoUnits().get(0).getSegmentUuid());
    }

    @Test
    @DisplayName("fail ошибка при создании клиентского возврата")
    public void failClientReturnCreate() {
        var event = sqsEventFactory.createClientReturnEvent(sortingCenter);
        var responsePayload = cargoUnitCreateHandler.handle(event);
        List<StorageUnitResponseErrorDto> errors =
                ((StorageUnitCreateResponseEvent) responsePayload).getResult().getErrors();
        assertFalse(errors.isEmpty());
    }

    @Test
    @DisplayName("fail ошибка при повторном создании клиентского возврата")
    public void failDuplicateClientReturnCreate() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var event = sqsEventFactory.createClientReturnEvent(sortingCenter);
        var responsePayload = cargoUnitCreateHandler.handle(event);
        var errors = ((StorageUnitCreateResponseEvent) responsePayload).getResult().getErrors();
        assertTrue(errors.isEmpty(), "must be empty, but :" + errors);

        var clientReturnLockerOrder = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);


        var responsePayload0 = cargoUnitCreateHandler.handle(event);
        List<StorageUnitResponseErrorDto> errors0 =
                ((StorageUnitCreateResponseEvent) responsePayload0).getResult().getErrors();

        assertEquals(errors0.get(0).getType(), StorageUnitResponseErrorType.CARGO_ALREADY_CREATED);
    }

    @Test
    @DisplayName("success создание клиентского возврата без inboundShipment")
    public void successClientReturnCreateWithoutInbound() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var barcode = "123";

        var params = createCargoUnit(barcode);
        params.getCargoUnitDtoParams().setInboundShipment(null);
        params.getCargoUnitDtoParams().setCodesCodeDto(List.of(new CodeDto(barcode, CodeType.CARGO_BARCODE)));

        var event = sqsEventFactory.makeSqsEvent(SqsEventType.STORAGE_UNIT_CREATE, System.currentTimeMillis(),
                new StorageUnitCreateRequestEvent(
                        "",
                        new PartnerDto(1, sortingCenter.getToken(), sortingCenter.getYandexId()),
                        List.of(LesModelFactory.createCargoUnit(params.getCargoUnitDtoParams())),
                        LesModelFactory.createCargoUnitGroup(params.getCargoUnitGroupDtoParams())
                ));
        List<StorageUnitResponseErrorDto> errorsUpdate =
                ((StorageUnitCreateResponseEvent) cargoUnitCreateHandler.handle(event)).getResult().getErrors();
        assertTrue(errorsUpdate.isEmpty(), "must be empty, but :" + errorsUpdate);

        var order = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(order.isClientReturn()).isTrue();
        assertThat(order.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
        // проверяем что фолбек сработал
        assertThat(order.getWarehouseFrom().getId()).isEqualTo(order.getWarehouseReturn().getId());

    }

    @Test
    @DisplayName("success обновление  клиентского возврата без inboundShipment")
    public void successClientReturnUpdateWithoutInbound() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var barcode = "123";

        var event = sqsEventFactory.createClientReturnEvent(sortingCenter);
        var responsePayload = cargoUnitCreateHandler.handle(event);
        List<StorageUnitResponseErrorDto> errors =
                ((StorageUnitCreateResponseEvent) responsePayload).getResult().getErrors();
        assertTrue(errors.isEmpty(), "must be empty, but :" + errors);

        var clientReturnLockerOrder = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();
        assertThat(clientReturnLockerOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(clientReturnLockerOrder.isClientReturn()).isTrue();
        assertThat(clientReturnLockerOrder.getFakeOrderType()).isEqualTo(CLIENT_RETURN);


        var params = createCargoUnit(barcode);
        params.getCargoUnitDtoParams().setSegmentUuid(UUID.randomUUID().toString());
        params.getCargoUnitDtoParams().setInboundShipment(null);
        params.getCargoUnitDtoParams().setCodesCodeDto(List.of(new CodeDto(barcode, CodeType.CARGO_BARCODE)));

        var updateEvent = sqsEventFactory.makeSqsEvent(SqsEventType.STORAGE_UNIT_CREATE, System.currentTimeMillis(),
                new StorageUnitCreateRequestEvent(
                        "",
                        new PartnerDto(1, sortingCenter.getToken(), sortingCenter.getYandexId()),
                        List.of(LesModelFactory.createCargoUnit(params.getCargoUnitDtoParams())),
                        LesModelFactory.createCargoUnitGroup(params.getCargoUnitGroupDtoParams())
                ));
        List<StorageUnitResponseErrorDto> updateErrors =
                ((StorageUnitCreateResponseEvent) cargoUnitCreateHandler.handle(updateEvent)).getResult().getErrors();
        assertTrue(updateErrors.isEmpty(), "must be empty, but :" + updateErrors);

        var order = scOrderRepository.findBySortingCenterAndExternalId(
                sortingCenter, event.payload().getCargoUnits().get(0).getCodes().get(0).getValue()
        ).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(order.isClientReturn()).isTrue();
        assertThat(order.getFakeOrderType()).isEqualTo(CLIENT_RETURN);
    }

    private CargoDtoBuilder.CargoUnitWithGroupsDtoParams createCargoUnit(String barcode) {
        return createCargoUnit(barcode,
                LesModelFactory.createPointDto(PointType.SHOP, 44L, 44L, "мерч1"),
                UUID.randomUUID().toString());
    }

    private CargoDtoBuilder.CargoUnitWithGroupsDtoParams createCargoUnit(String barcode, PointDto destination,
                                                                         String segmentUuid) {
        var cargoUnitGroup = CargoDtoBuilder.CargoUnitGroupDtoParams.builder()
                .id("1")
                .type(CargoUnitGroupType.RETURN_CLIENT)
                .build();
        var cargoUnit = CargoDtoBuilder.CargoUnitDtoParams.builder()
                .id("1")
                .codesCodeDto(List.of(new CodeDto(barcode, CodeType.ORDER_BARCODE)))
                .cargoUnitGroupId(cargoUnitGroup.getId())
                .inboundShipment(LesModelFactory.createInboundShipmentDto(CargoDtoBuilder.InboundShipmentDtoParams.builder()
                        .dateTime(Instant.now(clock))
                        .sender(LesModelFactory.createCarrierDto(CarrierType.DELIVERY_SERVICE_WITH_COURIER,
                                11L, LesModelFactory.createCourierDto(22L)))
                        .source(LesModelFactory.createPointDto(PointType.SORTING_CENTER, 33L, 33L, "sc1"))
                        .build()))
                .outboundShipment(LesModelFactory.createOutboundShipmentDto(CargoDtoBuilder.OutboundShipmentDtoParams.builder()
                        .dateTime(Instant.now(clock))
                        .recipient(LesModelFactory.createCarrierDto(CarrierType.DELIVERY_SERVICE_WITH_COURIER,
                                22L, LesModelFactory.createCourierDto(33L)))
                        .destination(destination)
                        .build()))
                .segmentUuid(segmentUuid)
                .build();
        return CargoDtoBuilder.CargoUnitWithGroupsDtoParams.builder()
                .cargoUnitDtoParams(cargoUnit)
                .cargoUnitGroupDtoParams(cargoUnitGroup)
                .build();
    }
}
