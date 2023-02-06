package ru.yandex.market.sc.internal.sqs.handler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.les.dto.CargoUnitDeleteSegmentDto;
import ru.yandex.market.logistics.les.dto.CargoUnitGroupType;
import ru.yandex.market.logistics.les.dto.CarrierType;
import ru.yandex.market.logistics.les.dto.CodeDto;
import ru.yandex.market.logistics.les.dto.CodeType;
import ru.yandex.market.logistics.les.dto.PointDto;
import ru.yandex.market.logistics.les.dto.PointType;
import ru.yandex.market.logistics.les.dto.ResultDto;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.internal.sqs.service.CargoProcessorService;
import ru.yandex.market.sc.internal.test.AbstractBaseIntTest;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;
import ru.yandex.market.sc.internal.util.les.LesModelFactory;
import ru.yandex.market.sc.internal.util.les.builder.CargoDtoBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_CANCELLED_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_CREATED_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author: dbryndin
 * @date: 3/21/22
 */
@EmbeddedDbIntTest
public class CancelReturnHandlerTest extends AbstractBaseIntTest {

    @Autowired
    private CargoProcessorService cargoProcessorService;

    @Autowired
    private Clock clock;

    private SortingCenter sc1;
    private SortingCenter sc2;

    @BeforeEach
    void setUp() {
        sc1 = testFactory.storedSortingCenter();
        sc2 = testFactory.storedSortingCenter2();
    }

    @Test
    @DisplayName("success создание невыкупа если его не было на СЦ")
    public void successCreateReturn() {
        var orderBarcode = "order_barcode_1";
        var placeBarcode = "p1";
        var params = createCargoUnit(new CargoBarcode(orderBarcode, placeBarcode));
        var result = processCargoUnit(params);
        assertThat(result.getErrors()).isEmpty();
        var cargoUnit = params.getCargoUnitDtoParams();
        var savedOrder = testFactory.findOrder(orderBarcode, sc1);
        var places = testFactory.orderPlaces(savedOrder.getId());

        // deprecated by order
        assertThat(savedOrder.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(savedOrder.getWarehouseReturn().getYandexId())
                .isEqualTo(String.valueOf(cargoUnit.getOutboundShipment().getDestination().getShopId()));
        assertThat(savedOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);

        // new by place
        assertThat(places.size()).isEqualTo(1);
        var place = places.get(0);
        assertThat(place.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(place.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);

        var historyList = testFactory.findPlaceHistory(place.getId());
        var historyItem = historyList.stream()
                .filter( h -> h.getMutableState().getSortableStatus().equals(SortableStatus.AWAITING_RETURN))
                .findFirst();

        assertThat(historyItem.isPresent()).isTrue();
        assertThat(historyItem.get().getMutableState().getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
    }

    @Test
    @DisplayName("success отмена невыкупа если он есть на СЦ в статусе ORDER_CREATED_FF")
    public void successCancelReturn_in_ORDER_CREATED_FF() {
        var orderBarcode = "order_barcode_1";
        var placeBarcode = "p1";
        var order = testFactory.createOrder(order(sc1, orderBarcode).places(placeBarcode).build());
        assertThat(order.get().getStatus()).isEqualTo(ORDER_CREATED_FF);
        var params = createCargoUnit(new CargoBarcode(orderBarcode, placeBarcode));
        params.getCargoUnitDtoParams().setInboundShipment(null);
        var result = processCargoUnit(params);
        assertThat(result.getErrors()).isEmpty();
        var cargoUnit = params.getCargoUnitDtoParams();

        // deprecated by order
        var savedOrder = testFactory.findOrder(orderBarcode, sc1);
        var places = testFactory.orderPlaces(savedOrder.getId());
        assertThat(savedOrder.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(savedOrder.getWarehouseReturn().getYandexId())
                .isEqualTo(String.valueOf(cargoUnit.getOutboundShipment().getDestination().getShopId()));
        assertThat(savedOrder.getStatus()).isEqualTo(ORDER_CANCELLED_FF);

        // new by place
        assertThat(places.size()).isEqualTo(1);
        var place = places.get(0);
        assertThat(place.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(place.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);

        var historyList = testFactory.findPlaceHistory(place.getId());
        var historyItem = historyList.stream()
                .filter(h -> h.getMutableState().getSortableStatus().equals(SortableStatus.AWAITING_RETURN))
                .findFirst();

        assertThat(historyItem.isPresent()).isTrue();
        assertThat(historyItem.get().getMutableState().getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
    }

    @Test
    @DisplayName("success отмена невыкупа если он есть на СЦ в статусе ORDER_CREATED_FF")
    public void successCancelReturn_in_ORDER_CANCELLED_FF() {
        var orderBarcode = "order_barcode_1";
        var placeBarcode = "p1";
        var order = testFactory.createOrder(order(sc1, orderBarcode).places(placeBarcode).build()).cancel().get();
        assertThat(order.getStatus()).isEqualTo(ORDER_CANCELLED_FF);
        assertThat(order.getSegmentUid()).isNull();

        var params = createCargoUnit(new CargoBarcode(orderBarcode, placeBarcode));
        params.getCargoUnitDtoParams().setInboundShipment(null);
        var result = processCargoUnit(params);
        assertThat(result.getErrors()).isEmpty();
        var cargoUnit = params.getCargoUnitDtoParams();
        var savedOrder = testFactory.findOrder(orderBarcode, sc1);
        var places = testFactory.orderPlaces(savedOrder.getId());

        // deprecated by order
        assertThat(savedOrder.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(savedOrder.getWarehouseReturn().getYandexId())
                .isEqualTo(String.valueOf(cargoUnit.getOutboundShipment().getDestination().getShopId()));
        assertThat(savedOrder.getStatus()).isEqualTo(ORDER_CANCELLED_FF);

        // new by place
        assertThat(places.size()).isEqualTo(1);
        var place = places.get(0);
        assertThat(place.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(place.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(place.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);

        var historyList = testFactory.findPlaceHistory(place.getId());
        var historyItem = historyList.stream()
                .filter(h -> h.getMutableState().getSortableStatus().equals(SortableStatus.AWAITING_RETURN) &&
                        cargoUnit.getSegmentUuid().equals(h.getMutableState().getSegmentUid()))
                .findFirst();

        assertThat(historyItem.isPresent()).isTrue()
                .withFailMessage("history must contains row with segment and " +
                        "status SortableStatus.CANCELLED");
    }

    @Test
    @DisplayName("success отмена невыкупа если он есть на СЦ в статусе ORDER_ARRIVED_TO_SO_WAREHOUSE")
    public void successCancelReturn_in_ORDER_ARRIVED_TO_SO_WAREHOUSE() {
        var orderBarcode = "order_barcode_1";
        var placeBarcode = "p1";
        var order = testFactory.createOrder(order(sc1, orderBarcode).places(placeBarcode).build()).accept().get();
        assertThat(order.getStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);

        var params = createCargoUnit(new CargoBarcode(orderBarcode, placeBarcode));
        params.getCargoUnitDtoParams().setInboundShipment(null);
        var result = processCargoUnit(params);
        assertThat(result.getErrors()).isEmpty();
        var cargoUnit = params.getCargoUnitDtoParams();
        var savedOrder = testFactory.findOrder(orderBarcode, sc1);
        var places = testFactory.orderPlaces(savedOrder.getId());
        assertThat(savedOrder.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(places.get(0).getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getWarehouseReturn().getYandexId())
                .isEqualTo(String.valueOf(cargoUnit.getOutboundShipment().getDestination().getShopId()));
        assertThat(savedOrder.getStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    @DisplayName("success отмена невыкупа если он есть на СЦ в статусе ORDER_SHIPPED_TO_SO_FF")
    public void successCancelReturn_in_ORDER_SHIPPED_TO_SO_FF() {
        var orderBarcode = "order_barcode_1";
        var placeBarcode = "p1";
        var order =
                testFactory.createForToday(order(sc1, orderBarcode).places(placeBarcode).build()).accept().sort().ship().get();
        assertThat(order.getStatus()).isEqualTo(ORDER_SHIPPED_TO_SO_FF);

        var cargoUnit = processCargoUnit(new CargoBarcode(orderBarcode, placeBarcode));
        var savedOrder = testFactory.findOrder(orderBarcode, sc1);
        var places = testFactory.orderPlaces(savedOrder.getId());
        assertThat(savedOrder.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(places.get(0).getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getWarehouseReturn().getYandexId())
                .isEqualTo(String.valueOf(cargoUnit.getOutboundShipment().getDestination().getShopId()));
        assertThat(savedOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
    }

    @Test
    @DisplayName("success повторный евент на смену СВ меняет СВ тк заказ в статусе RETURNED_ORDER_AT_SO_WAREHOUSE")
    public void successCantChangeSegment() {
        var orderBarcode = "order_barcode_1";
        var placeBarcode = "p1";
        var order = testFactory.createOrder(order(sc1, orderBarcode).places(placeBarcode).build()).accept().get();
        assertThat(order.getStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);

        var cargoBarcode = new CargoBarcode(orderBarcode, placeBarcode);
        var params = createCargoUnit(cargoBarcode);
        params.getCargoUnitDtoParams().setInboundShipment(null);
        var result = processCargoUnit(params);
        assertThat(result.getErrors()).isEmpty();
        var cargoUnit = params.getCargoUnitDtoParams();
        var oldSegment = cargoUnit.getSegmentUuid();
        var savedOrder = testFactory.findOrder(orderBarcode, sc1);
        var places = testFactory.orderPlaces(savedOrder.getId());
        assertThat(savedOrder.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(places.get(0).getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getWarehouseReturn().getYandexId())
                .isEqualTo(String.valueOf(cargoUnit.getOutboundShipment().getDestination().getShopId()));
        assertThat(savedOrder.getStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);


        // отсылаем евент на изменение склад возврата
        // данные не должны были поменяться тк статус возврата не позволяет обновить сегмент
        var newSegmentUuid = UUID.randomUUID().toString();
        processCargoUnit(cargoBarcode, LesModelFactory.createPointDto(PointType.SHOP, 55L, 5L, "мерч1"),
                newSegmentUuid);
        var savedOrder0 = testFactory.findOrder(orderBarcode, sc1);
        var places0 = testFactory.orderPlaces(savedOrder.getId());
        assertThat(savedOrder0.getSegmentUid()).isEqualTo(newSegmentUuid);
        assertThat(savedOrder.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(places0.get(0).getSegmentUid()).isEqualTo(newSegmentUuid);
        assertThat(savedOrder0.getStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    @DisplayName("success отмена невыкупа и смена склада возврата на СЦ где заказ был отгружен")
    public void successCancelReturnTwiceAccept() {
        var orderBarcode = "order_barcode_1";
        var placeBarcode = "p1";
        var order = testFactory.createForToday(order(sc1, orderBarcode).places(placeBarcode).build()).accept().get();
        assertThat(order.getStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
        var cargoBarcode = new CargoBarcode(orderBarcode, placeBarcode);
        var params = createCargoUnit(cargoBarcode);
        params.getCargoUnitDtoParams().setInboundShipment(null);
        var result = processCargoUnit(params);
        assertThat(result.getErrors()).isEmpty();
        var cargoUnit = params.getCargoUnitDtoParams();
        var savedOrder = testFactory.findOrder(orderBarcode, sc1);
        var places = testFactory.orderPlaces(savedOrder.getId());
        assertThat(savedOrder.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(places.get(0).getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(savedOrder.getStatus()).isEqualTo(RETURNED_ORDER_AT_SO_WAREHOUSE);
        assertThat(savedOrder.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getWarehouseReturn().getYandexId())
                .isEqualTo(String.valueOf(cargoUnit.getOutboundShipment().getDestination().getShopId()));
        // отгружаем коробку
        testFactory.sortOrder(order);
        testFactory.shipOrderRoute(order);
        var s = testFactory.findOrder(orderBarcode, sc1);
        assertThat(s.getStatus()).isEqualTo(RETURNED_ORDER_DELIVERED_TO_IM);

        // отсылаем евент на изменение склад возврата
        var newSegmentUuid = UUID.randomUUID().toString();
        var cargoUnit0 = processCargoUnit(cargoBarcode, LesModelFactory.createPointDto(PointType.SHOP, 55L, 5L, "мерч1"),
                newSegmentUuid);
        var savedOrder0 = testFactory.findOrder(orderBarcode, sc1);
        var places0 = testFactory.orderPlaces(savedOrder.getId());
        assertThat(savedOrder0.getSegmentUid()).isEqualTo(cargoUnit0.getSegmentUuid());
        assertThat(savedOrder0.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(places0.get(0).getSegmentUid()).isEqualTo(cargoUnit0.getSegmentUuid());
        assertThat(savedOrder0.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(savedOrder0.getSegmentUid()).isEqualTo(newSegmentUuid);
    }


    @Test
    @DisplayName("success создание возврата с фейковым курьером если он не пришел от lrm")
    public void successCreateClientReturnWithFakeCourier() {
        var fakeCourier = testFactory.storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
        var orderBarcode = "order_barcode_1";
        var placebarcode = "p1";
        var cargoBarcode = new CargoBarcode(orderBarcode, placebarcode);
        var params = createCargoUnit(cargoBarcode);
        params.getCargoUnitDtoParams().setInboundShipment(
                LesModelFactory.createInboundShipmentDto(CargoDtoBuilder.InboundShipmentDtoParams.builder()
                        .dateTime(Instant.now(clock))
                        .sender(LesModelFactory.createCarrierDto(CarrierType.DELIVERY_SERVICE_WITH_COURIER,
                                11L, LesModelFactory.unknownCourier()))
                        .source(LesModelFactory.createPointDto(PointType.SORTING_CENTER, 33L, 33L, "sc1"))
                        .build()));
        var result = processCargoUnit(params);
        assertThat(result.getErrors()).isEmpty();
        var cargoUnit = params.getCargoUnitDtoParams();
        var savedOrder = testFactory.findOrder(orderBarcode, sc1);
        var places = testFactory.orderPlaces(savedOrder.getId());
        assertThat(savedOrder.getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getCargoUnitId()).isEqualTo(cargoUnit.getId());
        assertThat(savedOrder.getCourier().getId()).isEqualTo(fakeCourier.getId());
        assertThat(savedOrder.getCourier().getName()).isEqualTo(ClientReturnService.CLIENT_RETURN_COURIER);
        assertThat(places.get(0).getSegmentUid()).isEqualTo(cargoUnit.getSegmentUuid());
        assertThat(savedOrder.getWarehouseReturn().getYandexId())
                .isEqualTo(String.valueOf(cargoUnit.getOutboundShipment().getDestination().getShopId()));
        assertThat(savedOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
    }

    @Test
    @DisplayName("success создание-удаление-создание Проверяем что невыкуп возвращается из статуса DELETED в " +
            "SO_GOT_INFO_ABOUT_PLANNED_RETURN если повторно присылается запрос на создание")
    public void successCancelReturn_in_DELETED() {
        var orderBarcode = "order_barcode_1";
        var placeBarcode = "p1";
        // создаем возврат
        var cargoBarcode = new CargoBarcode(orderBarcode, placeBarcode);
        var createdCargoParams = createCargoUnit(cargoBarcode);
        assertThat(processCargoUnit(createdCargoParams).getErrors()).isEmpty();

        // удаляем возврат
        deleteCargoUnit(createdCargoParams.getCargoUnitDtoParams().getId(),
                createdCargoParams.getCargoUnitDtoParams().getSegmentUuid());
        var deletedOrder = testFactory.findOrder(orderBarcode, sc1);

        var deletedPlace = testFactory.orderPlace(deletedOrder.getId());
        assertThat(deletedPlace.getSortableStatus()).isEqualTo(SortableStatus.DELETED);
        assertThat(deletedPlace.getSegmentUid()).isNull();

        // создаем возврат снова на сц
        var againCreatedCargoParams = createCargoUnit(cargoBarcode);
        assertThat(processCargoUnit(againCreatedCargoParams).getErrors()).isEmpty();

        var againCreatedOrder = testFactory.findOrder(orderBarcode, sc1);
        assertThat(againCreatedOrder.getStatus()).isEqualTo(SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        assertThat(againCreatedOrder.getSegmentUid()).isEqualTo(againCreatedCargoParams.getCargoUnitDtoParams().getSegmentUuid());
        assertThat(againCreatedOrder.getCargoUnitId()).isEqualTo(againCreatedCargoParams.getCargoUnitDtoParams().getId());

        var againCreatedPlace = testFactory.orderPlace(againCreatedOrder.getId());
        assertThat(againCreatedPlace.getSortableStatus()).isEqualTo(SortableStatus.AWAITING_RETURN);
        assertThat(againCreatedPlace.getSegmentUid()).isEqualTo(againCreatedCargoParams.getCargoUnitDtoParams().getSegmentUuid());
        assertThat(againCreatedPlace.getCargoUnitId()).isEqualTo(againCreatedCargoParams.getCargoUnitDtoParams().getId());
    }


    @Test
    @DisplayName("fail не найден штрихкод с типом ORDER_BARCODE")
    public void failBadBarcode() {
        assertThatThrownBy(() -> {
            var cargoUnitGroup = CargoDtoBuilder.CargoUnitGroupDtoParams.builder()
                    .id("1")
                    .type(CargoUnitGroupType.CANCELLATION_RETURN)
                    .build();
            var cargoUnit = CargoDtoBuilder.CargoUnitDtoParams.builder()
                    .id("1")
                    .codesCodeDto(List.of(new CodeDto("order_barcode_1")))
                    .cargoUnitGroupId(cargoUnitGroup.getId())
                    .inboundShipment(LesModelFactory.createInboundShipmentDto(CargoDtoBuilder.InboundShipmentDtoParams.builder()
                            .sender(LesModelFactory.createCarrierDto(CarrierType.DELIVERY_SERVICE_WITH_COURIER,
                                    11L, LesModelFactory.createCourierDto(22L)))
                            .source(LesModelFactory.createPointDto(PointType.SORTING_CENTER, 33L, 33L, "sc1"))
                            .dateTime(Instant.now(clock))
                            .build()))
                    .outboundShipment(LesModelFactory.createOutboundShipmentDto(CargoDtoBuilder.OutboundShipmentDtoParams.builder()
                            .recipient(LesModelFactory.createCarrierDto(CarrierType.DELIVERY_SERVICE_WITH_COURIER,
                                    22L, LesModelFactory.createCourierDto(33L)))
                            .destination(LesModelFactory.createPointDto(PointType.SHOP, 44L, 44L, "мерч1"))
                            .dateTime(Instant.now(clock))
                            .build()))
                    .segmentUuid("123")
                    .build();
            cargoProcessorService.processReturnCargoUnit(LesModelFactory.createCargoUnitGroup(cargoUnitGroup),
                    LesModelFactory.createCargoUnit(cargoUnit), sc1);
        }).hasMessageContaining("Not found code type [ORDER_BARCODE]");
    }

    private CargoDtoBuilder.CargoUnitDtoParams processCargoUnit(CargoBarcode barcode) {
        return processCargoUnit(barcode, LesModelFactory.createPointDto(PointType.SHOP, 44L, 44L, "мерч1"),
                UUID.randomUUID().toString());
    }

    private CargoDtoBuilder.CargoUnitDtoParams processCargoUnit(CargoBarcode barcode, PointDto destination,
                                                                String segmentUuid) {
        var params = createCargoUnit(barcode, destination, segmentUuid);
        cargoProcessorService.processReturnCargoUnit(LesModelFactory.createCargoUnitGroup(params.getCargoUnitGroupDtoParams()),
                LesModelFactory.createCargoUnit(params.getCargoUnitDtoParams()), sc1);
        return params.getCargoUnitDtoParams();
    }


    public record CargoBarcode(String orderBarcode, String placeBarcode) {}


    private CargoDtoBuilder.CargoUnitWithGroupsDtoParams createCargoUnit(CargoBarcode barcode) {
        return createCargoUnit(barcode,
                LesModelFactory.createPointDto(PointType.SHOP, 44L, 44L, "мерч1"),
                UUID.randomUUID().toString());
    }

    private CargoDtoBuilder.CargoUnitWithGroupsDtoParams createCargoUnit(CargoBarcode cargoBarcode, PointDto destination,
                                                                         String segmentUuid) {
        var cargoUnitGroup = CargoDtoBuilder.CargoUnitGroupDtoParams.builder()
                .id("1")
                .type(CargoUnitGroupType.CANCELLATION_RETURN)
                .build();
        var cargoUnit = CargoDtoBuilder.CargoUnitDtoParams.builder()
                .id("1")
                .codesCodeDto(List.of(new CodeDto(cargoBarcode.orderBarcode, CodeType.ORDER_BARCODE),
                        new CodeDto(cargoBarcode.placeBarcode, CodeType.CARGO_BARCODE)))
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

    private ResultDto processCargoUnit(CargoDtoBuilder.CargoUnitWithGroupsDtoParams params) {
        return cargoProcessorService.processReturnCargoUnit(LesModelFactory.createCargoUnitGroup(params.getCargoUnitGroupDtoParams()),
                LesModelFactory.createCargoUnit(params.getCargoUnitDtoParams()), sc1);
    }

    private ResultDto deleteCargoUnit(String cargoUnitId, String segmentUuid) {
        return cargoProcessorService.deleteCargoUnit(new CargoUnitDeleteSegmentDto(cargoUnitId, segmentUuid));
    }
}
