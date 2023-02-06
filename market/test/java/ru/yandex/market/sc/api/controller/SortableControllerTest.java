package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.RouteCommandService;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishByCellsRequest;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.ApiSortableSortDto;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.domain.order.model.ApiOrderListStatus.ACCEPTED_AT_SORTING_CENTER;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author mors741, merak1t
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SortableControllerTest extends BaseApiControllerTest {

    private final SortableQueryService sortableQueryService;

    private final RouteCommandService routeCommandService;

    private final SortableLotService sortableLotService;

    @MockBean
    private Clock clock;

    private SortingCenter sortingCenter;
    private TestControllerCaller caller;
    private TestFactory.CourierWithDs magistralCourierWithDs;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        testFactory.storedUser(sortingCenter, UID);
        TestFactory.setupMockClock(clock);
        testFactory.increaseScOrderId();
        caller = TestControllerCaller.createCaller(mockMvc);
        magistralCourierWithDs = testFactory.magistralCourier();
    }

    private record CellAndOrderBuilder(Cell cell, TestFactory.CreateOrderParams.CreateOrderParamsBuilder orderBuilder) {
    }

    private CellAndOrderBuilder getCellAndOrderBuilder(DeliveryServiceType dsType) {
        var orderBuilder =
                order(sortingCenter).externalId("o1").dsType(dsType);
        Cell cell;
        if (dsType == DeliveryServiceType.TRANSIT) {
            cell = testFactory.storedMagistralCell(sortingCenter, "c1",
                    CellSubType.DEFAULT, magistralCourierWithDs.courier().getId());
            orderBuilder = orderBuilder.deliveryService(magistralCourierWithDs.deliveryService());
        } else {
            cell = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        }
        return new CellAndOrderBuilder(cell, orderBuilder);
    }

    @DisplayName("success сортировка многоместного заказа в дефолтную ячейку")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void sortMultiPlaceOrder(DeliveryServiceType dsType) {
        var cellAndOrderBuilder = getCellAndOrderBuilder(dsType);
        var order =
                testFactory.createForToday(cellAndOrderBuilder.orderBuilder().places("1", "2").build())
                        .acceptPlaces().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var routeCell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));

        var request0 = new SortableSortRequestDto(order.getExternalId(), "1",
                routeCell.getId().toString());

        var sortResponse0 = caller.sortableBetaSort(request0)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInCell(cellAndOrderBuilder.cell()), false));
        var sortDtoResp0 = readContentAsClass(sortResponse0, ApiSortableSortDto.class);

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);

        var request1 = new SortableSortRequestDto(order.getExternalId(), "2",
                routeCell.getId().toString());
        var sortResponse1 = caller.sortableBetaSort(request1)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInCell(cellAndOrderBuilder.cell()), false));
        var sortDtoResp1 = readContentAsClass(sortResponse0, ApiSortableSortDto.class);
        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    /**
     * принимаем и сортируем возвратный заказ через приемку возвратов,
     * а потом снова сканируем через приемку возвратов и получаем ответ, что заказ уже в своей ячейке
     * (Поступила информация о возврате)
     */
    @DisplayName("success принимаем и сортируем возвратный заказ через приемку возвратов")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void acceptReturnedOrderTwiceViaReturnEndpointCheckFromCourierField4(DeliveryServiceType dsType) {
        var place = testFactory.createForToday(order(sortingCenter).dsType(dsType).build())
                .accept()
                .sort()
                .ship()
                .makeReturn()
                .getPlace();
        caller.acceptReturn(new AcceptOrderRequestDto(place.getExternalId(), null))
                .andExpect(status().is2xxSuccessful());
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();
        var routeCell = Objects.requireNonNull(testFactory.determineRouteCell(route, place));
        caller.sortableBetaSort(new SortableSortRequestDto(place, routeCell))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInCell(routeCell)));

        caller.acceptReturn(new AcceptOrderRequestDto(place.getExternalId(), null))
                .andExpect(status().is4xxClientError())
                .andExpect(content().
                        json("{\"status\":400,\"error\":\"ORDER_FROM_STRAIGHT_STREAM\"}", false));
    }

    /**
     * прием и сортировка одного места из многоместного возвратного заказа через приемку возвратов
     * (Поступила информация о возврате)
     */
    @DisplayName("success прием и сортировка одного места из многоместного возвратного заказа через приемку возвратов")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void acceptAndSortOnePlaceReturnedOrderViaReturnEndpointMultiPlace(DeliveryServiceType dsType) {
        var order = testFactory.createForToday(order(sortingCenter)
                        .dsType(dsType).places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2").ship().makeReturn().get();

        caller.acceptReturn(new AcceptOrderRequestDto(order.getExternalId(), "1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}"));
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var routeCell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));
        var request = new SortableSortRequestDto(order.getExternalId(), "1", routeCell.getId().toString());
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInCell(routeCell)));

        caller.acceptReturn(new AcceptOrderRequestDto(order.getExternalId(), "2"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"SORT_TO_WAREHOUSE\"}"));
    }

    /**
     * прием и сортировка одного места из многоместного возвратного заказа через приемку возвратов
     * (заказ был просто отгружен и его вернул курьер)
     */
    @DisplayName("success прием и сортировка одного места из многоместного возвратного заказа через приемку возвратов")
    @Test
    @SneakyThrows
    void acceptAndSortOnePlaceReturnedOrderViaReturnEndpointMultiPlace2() {
        var order = testFactory.createForToday(order(sortingCenter).places("1", "2").build())
                .acceptPlaces("1", "2").sortPlaces("1", "2").ship().get();
        caller.acceptReturn(new AcceptOrderRequestDto(order.getExternalId(), "1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"KEEP\"}"));
        var bufferCell = testFactory.storedCell(sortingCenter, CellType.BUFFER, null);
        var request = new SortableSortRequestDto(order.getExternalId(), "1", bufferCell.getId().toString());
        caller.sortableBetaSort(request)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInCell(bufferCell)));
        caller.acceptReturn(new AcceptOrderRequestDto(order.getExternalId(), "2"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"status\":\"KEEP\"}"));
    }

    @DisplayName("success сортировка выкинутого из маршрутизации заказов")
    @Test
    @SneakyThrows
    void sortDroppedOrder() {
        var place = testFactory.create(order(sortingCenter)
                        .shipmentDate(LocalDate.now(clock)).build())
                .accept()
                .getPlace();
        var bufferCell = testFactory.storedCell(sortingCenter, CellType.BUFFER, CellSubType.DROPPED_ORDERS, null);
        caller.sortableBetaSort(new SortableSortRequestDto(place, bufferCell))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInCell(bufferCell)));
    }

    @DisplayName("fail сортировка выкинутого из маршрутизации заказов")
    @Test
    @SneakyThrows
    void sortDroppedOrderInDefaultBufferCell() {
        var place = testFactory.create(order(sortingCenter)
                        .shipmentDate(LocalDate.now(clock)).build())
                .accept()
                .getPlace();
        var bufferCell = testFactory.storedCell(sortingCenter, CellType.BUFFER, CellSubType.DEFAULT, null);
        caller.sortableBetaSort(new SortableSortRequestDto(place, bufferCell))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"error\":\"NOT_SUPPORTED_CELL_SUBTYPE\",\"message\"" +
                                ":\"Cell Cell[" + bufferCell.getId()
                                + "] has wrong subtype DEFAULT. Expected: DROPPED_ORDERS\"}", false));
    }

    @DisplayName("fail сортировка на хранение выкинутого из маршрутизации заказов")
    @Test
    @SneakyThrows
    void keepOrderIntoBufferCellDroppedSubtype() {
        var place = testFactory.createOrder(sortingCenter)
                .updateShipmentDate(LocalDate.now(clock).plus(2, ChronoUnit.DAYS))
                .accept()
                .getPlace();
        var droppedCell = testFactory.storedCell(sortingCenter, CellType.BUFFER, CellSubType.DROPPED_ORDERS, null);
        caller.sortableBetaSort(new SortableSortRequestDto(place, droppedCell))
                .andExpect(status().is4xxClientError())
                .andExpect(content()
                        .json("{\"status\":400,\"error\":\"NOT_SUPPORTED_CELL_SUBTYPE\",\"message\"" +
                                ":\"Cell Cell[" + droppedCell.getId()
                                + "] has wrong subtype DROPPED_ORDERS. Expected: DEFAULT\"}", false));
    }

    /**
     * Сортируем клиентский возвратный заказ в возвратную ячейку подтипа CLIENT_RETURN
     */
    @DisplayName("success cортируем клиентский возвратный заказ в возвратную ячейку подтипа CLIENT_RETURN")
    @Test
    @SneakyThrows
    void sortUnknownClientReturnOrderToClientReturnCell() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        var clientReturnCell = testFactory.storedCell(
                sortingCenter,
                "default return",
                CellType.RETURN,
                CellSubType.CLIENT_RETURN,
                ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        var returnId = "VOZVRAT_SF_PS_1";
        caller
                .acceptReturn(new AcceptOrderRequestDto(returnId, null))
                .andExpect(status().is2xxSuccessful());

        var request = new SortableSortRequestDto(returnId, returnId, clientReturnCell.getId().toString());
        caller.sortableBetaSort(request)
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInCell(clientReturnCell)));
    }

    /**
     * Сортируем поврежденные заказы только в возвратные ячейки подтипа "Поврежденные"
     */
    @DisplayName("Сортируем поврежденные заказы только в возвратные ячейки подтипа \"Поврежденные\"")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void sortDamagedOrderOnlyInReturnDamagedCell(DeliveryServiceType dsType) {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        var damagedCell = testFactory.storedCell(sortingCenter, "damaged cell",
                CellType.RETURN, CellSubType.RETURN_DAMAGED,
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        var clientReturn = testFactory.storedCell(sortingCenter, "client return",
                CellType.RETURN, CellSubType.CLIENT_RETURN,
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        var defaultReturn = testFactory.storedCell(sortingCenter, "default return",
                CellType.RETURN, CellSubType.DEFAULT,
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        var courierCell = testFactory.storedCell(sortingCenter, "courier default",
                CellType.COURIER, CellSubType.DEFAULT,
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());
        var bufferCell = testFactory.storedCell(sortingCenter, "buffer default",
                CellType.BUFFER, CellSubType.DEFAULT,
                ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId());

        var damagedPlace = testFactory.createForToday(order(sortingCenter, "1")
                        .dsType(dsType)
                        .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId())
                        .warehouseCanProcessDamagedOrders(true)
                        .build())
                .accept().markOrderAsDamaged()
                .getPlace();

        for (var failCell : List.of(clientReturn, defaultReturn, courierCell, bufferCell)) {
            caller.sortableBetaSort(new SortableSortRequestDto(damagedPlace, failCell))
                    .andExpect(status().is4xxClientError());
        }
        caller.sortableBetaSort(new SortableSortRequestDto(damagedPlace, damagedCell))
                .andExpect(status().isOk())
                .andExpect(content().json(sortedInCell(damagedCell)));
    }

    /**
     * не можем отсортировать обычный возвратный заказ в возвратную ячейку подтипа CLIENT_RETURN
     * и не можем отсортировать его в возвратную ячейку подтипа RETURN_DAMAGED
     */
    @DisplayName("fail cортируем обычный возвратный заказ в возвратную ячейку подтипа CLIENT_RETURN и RETURN_DAMAGED")
    @Test
    @SneakyThrows
    void cantSortRegularReturnToClientReturnCell() {
        var place = testFactory.createOrder(sortingCenter)
                .accept()
                .keep()
                .makeReturn()
                .getPlace();
        var clientReturnCell = testFactory.storedCell(sortingCenter, "client-return",
                CellType.RETURN, CellSubType.CLIENT_RETURN, place.getWarehouseReturn().getYandexId());
        var damageReturnCell = testFactory.storedCell(sortingCenter, "damaged-return",
                CellType.RETURN, CellSubType.RETURN_DAMAGED, place.getWarehouseReturn().getYandexId());

        testFactory.acceptPlace(place);

        caller.sortableBetaSort(new SortableSortRequestDto(place, clientReturnCell))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"CELL_FROM_ANOTHER_ROUTE\"}",
                        false));

        caller.sortableBetaSort(new SortableSortRequestDto(place, damageReturnCell))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"CELL_FROM_ANOTHER_ROUTE\"}",
                        false));
    }

    /**
     * не можем отсортировать многоместный без места
     */
    @DisplayName("fail cортировка многоместного заказа без сканирования плейса")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void cantSortMultiplaceOrderWithoutPlace(DeliveryServiceType dsType) {
        var order = testFactory.createForToday(order(sortingCenter)
                        .dsType(dsType).places("1", "2").build())
                .acceptPlaces().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var routeCell = Objects.requireNonNull(testFactory.determineRouteCell(route, order));
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        var request1 = new SortableSortRequestDto(order.getExternalId(), null, routeCell.getId().toString());
        caller.sortableBetaSort(request1)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"PLACE_NOT_FOUND\"}",
                        false));

        var request2 = new SortableSortRequestDto(order.getExternalId(), null, lot.getBarcode());
        caller.sortableBetaSort(request2)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"PLACE_NOT_FOUND\"}",
                        false));
    }


    /*
    ░░░░░░░░░░░░░░░░░░░░
    ░ВЗГЛЯНИ ░░ВОКРУГ,░░░░░
    ░ОГЛЯНИСЬ░НАЗАД!░░░░░░
    ░ГУСИ░▄▀▀▀▄░С░ТОБОЮ░░
    ░░░░▀░░░◐░▀███▄░░░░░
    ░░░░▌░░░░░▐░░░░░░░░░
    ░░░░▐░░░░░▐░░░░░░░░░
    ░░░░▌░░░░░▐▄▄░░░░░░░
    ░░░░▌░░░░▄▀▒▒▀▀▀▀▄
    ░░░▐░░░░▐▒▒▒▒▒▒▒▒▀▀▄
    ░░░▐░░░░▐▄▒▒▒▒▒▒▒▒▒▒▀▄
    ░░░░▀▄░░░░▀▄▒▒▒▒▒▒▒▒▒▒▀▄
    ░░░░░░▀▄▄▄▄▄█▄▄▄▄▄▄▄▄▄▄▄▀▄
    ░СВЯЗАТЬСЯ░░▌▌░▌▌░░░░░
    ░░░ХОТЯТ░░░░▌▌░▌▌░░░░░
    ░░░░░░░░░░░▄▄▌▌▄▌▌░░
     */

    @DisplayName("success сортируем заказ из курьерской ячейки в лот")
    @Test
    @SneakyThrows
    void sortOrderToCourierLot() {
        var courier = testFactory.storedCourier();
        var courierCell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER, courier.getId());
        var place = testFactory.createOrderForToday(sortingCenter)
                .updateCourier(courier)
                .accept()
                .sort()
                .getPlace();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        caller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("success можем отсортировать заказ только в ячейку после приемки без пропертей сортировки напрямую")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void sortOrderWithNotLotSortAvailable(DeliveryServiceType dsType) {
        var cellWithOrderBuilder = getCellAndOrderBuilder(dsType);
        var place = testFactory.createForToday(cellWithOrderBuilder.orderBuilder().build())
                .accept()
                .getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        var routeCell = Objects.requireNonNull(testFactory.determineRouteCell(route, place));
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);

        caller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is4xxClientError());
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);

        caller.sortableBetaSort(new SortableSortRequestDto(place, routeCell))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInCell(routeCell)));
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("success можем отсортировать заказ в лот после сортировки в ячейку")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void sortOrderInCellWithoutLotSortAvailable(DeliveryServiceType dsType) {
        var cellAndOrderBuilder = getCellAndOrderBuilder(dsType);
        var place = testFactory.createForToday(cellAndOrderBuilder.orderBuilder().build())
                .accept()
                .sort()
                .getPlace();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cellAndOrderBuilder.cell());
        var sortResponse = caller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    /**
     * Заказ из прямого потока может быть отсортирован в любой лот привязанный к ячейке
     */
    @DisplayName("success Заказ из прямого потока может быть отсортирован в любой лот привязанный к ячейке")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void canSortOrderToAnyLotInCell(DeliveryServiceType dsType) {
        var place = testFactory.createForToday(order(sortingCenter, "1").dsType(dsType).build())
                .accept()
                .sort()
                .getPlace();
        var anotherPlace = testFactory.createForToday(order(sortingCenter, "2").dsType(dsType).build())
                .accept()
                .sort()
                .getPlace();

        var route = testFactory.findOutgoingRoute(place).orElseThrow().allowReading();
        var anotherRoute = testFactory.findOutgoingRoute(anotherPlace).orElseThrow().allowReading();
        assertThat(route).isEqualTo(anotherRoute);
        var routeCell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        SortableLot lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        caller.sortableBetaSort(new SortableSortRequestDto(place, lot1))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot1)));
        caller.sortableBetaSort(new SortableSortRequestDto(anotherPlace, lot2))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot2)));
    }

    @DisplayName("success сортировка многоместного заказа из ячейки в лот")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void sortMultiPlaceOrderToCourierLot(DeliveryServiceType dsType) {
        var cellAndOrderBuilder = getCellAndOrderBuilder(dsType);
        var order = testFactory.createForToday(
                        cellAndOrderBuilder.orderBuilder()
                                .places("p1", "p2")
                                .build())
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlaces(List.of("p1", "p2"))
                .get();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cellAndOrderBuilder.cell());

        var request1 = new SortableSortRequestDto(order.getExternalId(), "p1", lot.getBarcode());
        var sortResponse1 = caller.sortableBetaSort(request1)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        var request2 = new SortableSortRequestDto(order.getExternalId(), "p2", lot.getBarcode());
        var sortResponse2 = caller.sortableBetaSort(request2)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
    }


    @DisplayName("success сортировка многоместного заказа из ячейки в разные лоты")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void sortMultiPlaceOrdersIntoDifferentPallets(DeliveryServiceType dsType) {
        var cellAndOrderBuilder = getCellAndOrderBuilder(dsType);
        var order = testFactory.createForToday(
                        cellAndOrderBuilder.orderBuilder()
                                .places("p1", "p2")
                                .build())
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlaces(List.of("p1", "p2"))
                .get();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cellAndOrderBuilder.cell());
        SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, cellAndOrderBuilder.cell());

        var request1 = new SortableSortRequestDto(order.getExternalId(), "p1", lot.getBarcode());
        var sortResponse1 = caller.sortableBetaSort(request1)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        var request2 = new SortableSortRequestDto(order.getExternalId(), "p2", lot2.getBarcode());
        var sortResponse2 = caller.sortableBetaSort(request2)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot2)));
    }

    @DisplayName("success сортировка отмененного заказа из ячейки возвратов в лот")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void lotSortOrder(DeliveryServiceType dsType) {
        Cell returnCell = testFactory.storedCell(sortingCenter, "return-1", CellType.RETURN);
        Place place = testFactory.createForToday(order(sortingCenter).dsType(dsType).build())
                .accept()
                .cancel()
                .sort()
                .getPlace();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);

        caller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
    }

    @DisplayName("Попытка отсортировать заказ в удаленный лот")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void deletedLotSortOrder(DeliveryServiceType dsType) {
        Cell returnCell = testFactory.storedCell(sortingCenter, "return-1", CellType.RETURN);
        Place place = testFactory.createForToday(order(sortingCenter).dsType(dsType).build())
                .accept()
                .cancel()
                .sort()
                .getPlace();
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);

        testFactory.deleteLot(lot);

        caller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is4xxClientError());
    }

    @DisplayName("success финиш маршрута после сортировки многоместного в лот")
    @ParameterizedTest
    @EnumSource
    @SneakyThrows
    void finishOutgoingRouteAfterLotSortOrderMultiPlace(DeliveryServiceType dsType) {
        Cell returnCell = testFactory.storedCell(sortingCenter, "return-1", CellType.RETURN);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);
        var orderMultiPlace =
                testFactory.createForToday(order(sortingCenter, "5").dsType(dsType).places("1", "2").build())
                        .cancel().acceptPlaces().sortPlaces().getPlaces();
        Place place1 = orderMultiPlace.get("1");
        Place place2 = orderMultiPlace.get("2");
        Place place3 = testFactory.createForToday(order(sortingCenter, "3").dsType(dsType).build())
                .accept().cancel().sort().getPlace();
        Place place4 = testFactory.createForToday(order(sortingCenter, "4").dsType(dsType).build())
                .accept().sort().getPlace();

        caller.sortableBetaSort(new SortableSortRequestDto(place1, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        caller.sortableBetaSort(new SortableSortRequestDto(place2, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));

        caller.getOrderIdList(null, place1.getCell().getId(), null)
                .andExpect(jsonPath("$.orders", hasSize(2)))
                .andExpect(jsonPath("$.orders[1].places", hasSize(2)))
                .andExpect(jsonPath("$.orders[1].places[0].status", is(ACCEPTED_AT_SORTING_CENTER.name())))
                .andExpect(jsonPath("$.orders[1].places[0].cell").doesNotExist())
                .andExpect(jsonPath("$.orders[1].places[1].status", is(ACCEPTED_AT_SORTING_CENTER.name())))
                .andExpect(jsonPath("$.orders[1].places[1].cell").doesNotExist())
                .andExpect(status().isOk()).andReturn();

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(place4).orElseThrow().allowReading();
        var cell1 = testFactory.determineRouteCell(outgoingCourierRoute, place4);

        assertDoesNotThrow(() -> routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(outgoingCourierRoute.getId()),
                new ScContext(testFactory.getOrCreateStoredUser(sortingCenter)),
                List.of(cell1.getId()),
                null,
                false
        )));
    }

    @DisplayName("fail сортируем заказ из курьерской ячейки в отгруженный лот")
    @Test
    @SneakyThrows
    void sortOrderToCourierShippedLot() {
        var courier = testFactory.storedCourier();
        var courierCell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER, courier.getId());
        var place = testFactory.createForToday(order(sortingCenter, "p1").build())
                .updateCourier(courier)
                .accept()
                .sort()
                .getPlace();
        var place2 = testFactory.createForToday(order(sortingCenter, "p2").build())
                .updateCourier(courier)
                .accept()
                .sort()
                .getPlace();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        caller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        testFactory.prepareToShipLot(lot);
        testFactory.shipLotRouteByParentCell(lot);
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(testFactory.getLot(lot.getLotId()).getStatus())
                .isEqualTo(SortableStatus.SHIPPED_DIRECT);
        assertThat(testFactory.getOrder(place2.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        caller.sortableBetaSort(new SortableSortRequestDto(place2, lot))
                .andExpect(content().json("{\"status\":400,\"error\":\"BAD_REQUEST\", " +
                                "\"message\":\"Невозможно прикрепить лот " + lot.getBarcode() + ", находящийся в " +
                                "статусе SHIPPED_DIRECT\"}",
                        false));
    }

    @Test
    @SneakyThrows
    @DisplayName("Попытка отсортировать не запакованную паллету при сортировки")
    void sortUnpackedPallet() {
        Warehouse warehouse = testFactory.storedWarehouse("WHS-123");
        String barcode = "XDOC-PALLET123";
        Outbound outbound = testFactory.createOutbound(
                TestFactory.CreateOutboundParams.builder()
                        .externalId("OUT-123")
                        .sortingCenter(sortingCenter)
                        .partnerToExternalId(warehouse.getYandexId())
                        .logisticPointToExternalId(warehouse.getYandexId())
                        .toTime(clock.instant())
                        .fromTime(clock.instant())
                        .carNumber("A777MP77")
                        .type(OutboundType.XDOC)
                        .build()
        );
        Inbound inbound = testFactory.createInbound(
                TestFactory.CreateInboundParams.builder()
                        .fromDate(OffsetDateTime.now(clock))
                        .toDate(OffsetDateTime.now(clock))
                        .inboundType(InboundType.XDOC_TRANSIT)
                        .sortingCenter(sortingCenter)
                        .inboundExternalId("IN-123")
                        .nextLogisticPointId(warehouse.getYandexId())
                        .build()
        );

        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.XDOC_BASKET,
                testFactory.storedCell(sortingCenter, "123123", CellType.COURIER), LotStatus.CREATED, false,
                barcode
        );

        Sortable sortable = sortableQueryService.find(sortingCenter, barcode).orElseThrow();

        var request = new SortableSortRequestDto(sortable.getRequiredBarcodeOrThrow(), "123", "123123");
        var sortResponse = caller.sortableBetaSort(request)
                .andExpect(status().is4xxClientError());
    }

    private String sortedInCell(Cell cell) {
        return "{" +
                "\"destination\":{\"id\":\"" + cell.getId() + "\",\"name\":\"" + cell.getScNumber() + "\",\"type" +
                "\":\"CELL\"}," +
                "\"parentRequired\":false" +
                "}";
    }

    private String sortedInLot(SortableLot lot) {
        return "{" +
                "\"destination\":{\"id\":\"" + lot.getBarcode() + "\",\"name\":\"" + lot.getNameForApi() + "\",\"type" +
                "\":\"LOT\"}," +
                "\"parentRequired\":false" +
                "}";
    }

}
