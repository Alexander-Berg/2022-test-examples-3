package ru.yandex.market.sc.api.controller;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.api.BaseApiControllerTest;
import ru.yandex.market.sc.api.utils.TestControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.ApiSortableSortDto;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author merak1t
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SortableControllerLotSortAvailableTest extends BaseApiControllerTest {

    @MockBean
    private Clock clock;

    private SortingCenter sortingCenter;
    private DeliveryService deliveryServiceWithLotSortEnabled;
    private Cell cell;
    private TestControllerCaller controllerCaller;
    private final SortableLotService sortableLotService;
    private TestFactory.CourierWithDs courierWithDs;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        courierWithDs = testFactory.magistralCourier("666");
        deliveryServiceWithLotSortEnabled = courierWithDs.deliveryService();
        cell = testFactory.storedMagistralCell(sortingCenter, courierWithDs.courier().getId());
        testFactory.storedUser(sortingCenter, UID);
        TestFactory.setupMockClock(clock);
        testFactory.increaseScOrderId();
        controllerCaller = TestControllerCaller.createCaller(mockMvc);
    }

    /*
    ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
    ░░░░ЗАПУСКАЕМ░ГУСЕЙ-РАЗВЕДЧИКОВ░░░░
    ░░░░░▄▀▀▀▄░░░▄▀▀▀▀▄░░░▄▀▀▀▄░░░░░
    ▄███▀░◐░░░▌░▐0░░░░0▌░▐░░░◐░▀███▄
    ░░░░▌░░░░░▐░▌░▐▀▀▌░▐░▌░░░░░▐░░░░
    ░░░░▐░░░░░▐░▌░▌▒▒▐░▐░▌░░░░░▌░░░░
     */
    @DisplayName("success сортировка многоместного заказа напрямую в лот")
    @Test
    @SneakyThrows
    void sortMultiPlaceOrderToLotWithLotSortAvailable() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var order = testFactory.createForToday(order(sortingCenter)
                .deliveryService(deliveryServiceWithLotSortEnabled)
                .dsType(DeliveryServiceType.TRANSIT)
                .places("p1", "p2")
                .build()
        ).acceptPlaces().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var courierCell = testFactory.determineRouteCell(route, order);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        var request1 = new SortableSortRequestDto(order.getExternalId(), "p1", lot.getBarcode());
        var sortResponse1 = controllerCaller.sortableBetaSort(request1)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        var sortDtoResp1 = readContentAsClass(sortResponse1, ApiSortableSortDto.class);

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

        var request2 = new SortableSortRequestDto(order.getExternalId(), "p2", lot.getBarcode());
        var sortResponse2 = controllerCaller.sortableBetaSort(request2)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        var sortDtoResp2 = readContentAsClass(sortResponse2, ApiSortableSortDto.class);

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("success сортировка многоместного заказа напрямую в разные лоты")
    @SneakyThrows
    @Test
    void sortMultiPlaceOrdersIntoDifferentPalletsWithLotSortAvailable() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var order = testFactory.createForToday(order(sortingCenter)
                .deliveryService(deliveryServiceWithLotSortEnabled)
                .dsType(DeliveryServiceType.TRANSIT)
                .places("p1", "p2")
                .build()
        ).acceptPlaces().get();

        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var courierCell = testFactory.determineRouteCell(route, order);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        var request1 = new SortableSortRequestDto(order.getExternalId(), "p1", lot.getBarcode());
        var sortResponse1 = controllerCaller.sortableBetaSort(request1)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        var sortDtoResp1 = readContentAsClass(sortResponse1, ApiSortableSortDto.class);

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);


        var request2 = new SortableSortRequestDto(order.getExternalId(), "p2", lot2.getBarcode());
        var sortResponse2 = controllerCaller.sortableBetaSort(request2)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot2)));
        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("success сортировка заказа из ячейки хранения напрямую в лот")
    @SneakyThrows
    @Test
    void sortOrdersIntoLotFromCellWithLotSortAvailable() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createForToday(order(sortingCenter)
                .deliveryService(deliveryServiceWithLotSortEnabled)
                .dsType(DeliveryServiceType.TRANSIT)
                .build())
                .accept()
                .keep(true)
                .getPlace();

        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);

        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        var courierCell = testFactory.determineRouteCell(route, place);
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }


    @DisplayName("success сортировка многоместного заказа в ячейку и лот после сортировка другого места напрямую в лот")
    @SneakyThrows
    @Test
    void sortMultiPlaceOrdersWithoutLotsWithLotSortAvailable() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var orderFactory = testFactory.createForToday(order(sortingCenter)
                .deliveryService(deliveryServiceWithLotSortEnabled)
                .dsType(DeliveryServiceType.TRANSIT)
                .places("p1", "p2")
                .build()
        ).acceptPlaces().sortPlace("p1");
        SortableLot lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        var order = orderFactory.sortPlace("p2").get();

        var request1 = new SortableSortRequestDto(order.getExternalId(), "p1", lot.getBarcode());
        var sortResponse1 = controllerCaller.sortableBetaSort(request1)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        var sortDtoResp1 = readContentAsClass(sortResponse1, ApiSortableSortDto.class);

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);

        var request2 = new SortableSortRequestDto(order.getExternalId(), "p2", lot.getBarcode());
        var sortResponse2 = controllerCaller.sortableBetaSort(request2)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));
        var sortDtoResp2 = readContentAsClass(sortResponse2, ApiSortableSortDto.class);

        assertThat(testFactory.getOrder(order.getId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("success сортируем заказ в ячейку при возможности сортировки напрямую в лот")
    @SneakyThrows
    @Test
    void sortOrderToCellWithLotSortAvailable() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .deliveryService(deliveryServiceWithLotSortEnabled)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept()
                .getPlace();
        var route0 = testFactory.findOutgoingCourierRoute(place).orElseThrow().allowReading();
        var routeCell = route0.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(place, routeCell))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInCell(routeCell)));

        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("success сортируем заказ напрямую в лот без ячейки")
    @SneakyThrows
    @Test
    void sortOrderToLotWithoutCell() {
        cell.setDeleted(true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .deliveryService(deliveryServiceWithLotSortEnabled)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept()
                .getPlace();
        var route0 = testFactory.findOutgoingCourierRoute(place).orElseThrow().allowReading();
        var routeCell = route0.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot)));

        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("success сортируем заказ напрямую в лоты без ячейки")
    @SneakyThrows
    @Test
    void sortOrderToLotWithoutCellWithLots() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .deliveryService(deliveryServiceWithLotSortEnabled)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept()
                .getPlace();
        var route = testFactory.findOutgoingRoute(place).orElseThrow().allowReading();
        var routeCell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(place, lot2))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot2)));
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @DisplayName("fail сортируем заказ напрямую в шипнутый лот")
    @SneakyThrows
    @Test
    void sortOrderToLotWithoutCellJustWithClosedLot() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createForToday(order(sortingCenter)
                        .deliveryService(deliveryServiceWithLotSortEnabled)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .accept()
                .getPlace();
        var route = testFactory.findOutgoingRoute(place).orElseThrow().allowReading();
        var routeCell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var lot = testFactory.storedLot(sortingCenter, routeCell, LotStatus.SHIPPED);

        controllerCaller.sortableBetaSort(new SortableSortRequestDto(place, lot))
                .andExpect(status().is4xxClientError());
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @DisplayName("success можем отсортировать заказ напрямую только в доступный лот")
    @SneakyThrows
    @Test
    void sortOrderToLotWithoutCellWithClosedLot() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createForToday(order(sortingCenter)
                        .deliveryService(deliveryServiceWithLotSortEnabled)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .accept()
                .getPlace();
        var route0 = testFactory.findOutgoingRoute(place).orElseThrow().allowReading();
        var routeCell = route0.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        var lot2 = testFactory.storedLot(sortingCenter, routeCell, LotStatus.SHIPPED);

        controllerCaller.sortableBetaSort(new SortableSortRequestDto(place, lot2))
                .andExpect(status().is4xxClientError());
        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);

        controllerCaller.sortableBetaSort(new SortableSortRequestDto(place, lot1))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(sortedInLot(lot1)));

        assertThat(testFactory.getOrder(place.getOrderId()).getFfStatus())
                .isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    /**
     * Нельзя отсортировать заказ прямого потока в лот для возвратов
     */
    @DisplayName("fail Сортировки заказа прямого потока в лот для возвратов")
    @SneakyThrows
    @Test
    void cantSortOrderForCourierLotIntoReturnLot() {
        testFactory.increaseScOrderId();
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createForToday(
                        order(sortingCenter)
                                .deliveryService(deliveryServiceWithLotSortEnabled)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .accept()
                .sort()
                .getPlace();
        var returnCell = testFactory.storedCell(sortingCenter, "r", CellType.RETURN, "w");
        var returnLot = testFactory.storedLot(sortingCenter, SortableType.PALLET, returnCell);
        controllerCaller.sortableBetaSort(new SortableSortRequestDto(place, returnLot))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"LOT_PARENT_CELL_FROM_ANOTHER_ROUTE\"}",
                        false));
    }

    /**
     * Нельзя отсортировать заказ прямого потока в лот не с этого маршрута
     */
    @DisplayName("fail Сортировки заказа напрямую в лот другого маршрута")
    @SneakyThrows
    @Test
    void cantSortOrderForCourierLotIntoLotFromAnotherRoute() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var order = testFactory.createForToday(order(sortingCenter)
                        .deliveryService(deliveryServiceWithLotSortEnabled)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .accept().get();

        var courier = order.getCourier();
        var courierCell = testFactory.storedCell(sortingCenter, "228", CellType.COURIER, courier.getId());
        var route = testFactory.findOutgoingRoute(order).orElseThrow().allowReading();
        var routeCell = route.getRouteCells().stream().findFirst().map(RouteCell::getCell).orElseThrow();
        SortableLot lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, routeCell);
        SortableLot lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, courierCell);
        var request = new SortableSortRequestDto(order.getExternalId(), order.getExternalId(), lot2.getBarcode());
        controllerCaller.sortableBetaSort(request)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"status\":400,\"error\":\"LOT_PARENT_CELL_FROM_ANOTHER_ROUTE\"}",
                        false));
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
