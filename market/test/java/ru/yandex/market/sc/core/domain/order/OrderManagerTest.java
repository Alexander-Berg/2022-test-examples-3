package ru.yandex.market.sc.core.domain.order;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.sc.core.domain.cell.CellQueryService;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderState;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.RouteCommandService;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishByCellsRequest;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseProperty;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderManagerTest {
    private final RouteCommandService routeCommandService;
    private final ScOrderRepository scOrderRepository;
    private final PlaceRepository placeRepository;
    private final TestFactory testFactory;
    private final OrderQueryService orderQueryService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final CellQueryService cellQueryService;

    SortingCenter sortingCenter;
    User stockman;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        stockman = testFactory.storedUser(sortingCenter, 123L);
    }

    @Test
    void droppedOrdersCellCreatedOnOrderAccept() {
        var order = testFactory.create(order(sortingCenter).shipmentDate(LocalDate.now(clock)).build())
                .accept().get();
        var availableCells = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null).getAvailableCells();
        assertThat(CollectionUtils.isNonEmpty(availableCells)).isTrue();
        availableCells.forEach(cell ->
                assertThat(cell.getSubType()).isEqualTo(CellSubType.DROPPED_ORDERS));
    }

    @Test
    void defaultBufferCellNotCreatedOnOrderAccept() {
        var order = testFactory.create(order(sortingCenter).build()).accept().get();
        var apiOrderDto = orderQueryService.getOrderForApi(sortingCenter, order.getExternalId(), null);
        assertThat(CollectionUtils.isEmpty(apiOrderDto.getAvailableCells())).isTrue();
    }

    @Test
    void shipOrdersOnRouteShip() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();

        Route outgoingCourierRoute = testFactory.findOutgoingCourierRoute(
                testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
        long cellId = Objects.requireNonNull(testFactory.determineRouteCell(outgoingCourierRoute, order)).getId();
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(outgoingCourierRoute),
                new ScContext(stockman),
                List.of(cellId),
                null,
                false
        ));

        assertThat(scOrderRepository.findByIdOrThrow(order.getId()).getState())
                .isEqualTo(ScOrderState.SHIPPED);
    }

    @Test
    void shipOrdersOnReturnRouteShip() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().get();

        Route outgoingWarehouseRoute = testFactory.findOutgoingWarehouseRoute(
                testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
        long cellId = Objects.requireNonNull(testFactory.determineRouteCell(outgoingWarehouseRoute, order)).getId();
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(outgoingWarehouseRoute),
                new ScContext(stockman),
                List.of(cellId),
                null,
                false
        ));

        assertThat(scOrderRepository.findByIdOrThrow(order.getId()).getState())
                .isEqualTo(ScOrderState.SHIPPED);
    }

    /**
     * Можем отгрузить неполный многоместный заказ при отгрузке на склад
     */
    @Test
    void shipMultiPlaceIncompleteOnReturnRouteShip() {
        OrderLike order = testFactory.create(
                        order(sortingCenter)
                                .externalId("o1")
                                .shipmentDate(LocalDate.now(clock))
                                .warehouseReturnId(testFactory.storedWarehouse().getYandexId())
                                .places("p1", "p2")
                                .build())
                .updateCourier(testFactory.storedCourier())
                .acceptPlaces(List.of("p1", "p2"))
                .sortPlaces(List.of("p1", "p2"))
                .ship().makeReturn()
                .acceptPlaces(List.of("p1"))
                .sortPlaces(List.of("p1"))
                .get();

        Route outgoingWarehouseRoute = testFactory.findOutgoingWarehouseRoute(
                testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
        long cellId = Objects.requireNonNull(testFactory.determineRouteCell(outgoingWarehouseRoute, order)).getId();
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(outgoingWarehouseRoute),
                new ScContext(stockman),
                List.of(cellId),
                null,
                false
        ));

        assertThat(scOrderRepository.findByIdOrThrow(order.getId()).getState())
                .isEqualTo(ScOrderState.SHIPPED);
        assertThat(placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "p1")
                .get().getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), "p2")
                .get().getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void acceptSinglePlaceChangesOrderStatus() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1")
                .get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void acceptAllPlacesChangesOrderStatus() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1", "p2")
                .get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }


    @Test
    void prepareToShipSinglePlaceDoesNotChangeOrderStatus() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").preparePlace("p1")
                .get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @Test
    void prepareToShipAllPlacesChangesOrderStatus() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").preparePlace("p1").preparePlace("p2")
                .get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO);
    }


    @Test
    void acceptSinglePlaceChangesOrderStatusKeep() {
        var order = testFactory.create(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1")
                .get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void acceptTwoOfThreePlaces() {
        var order = testFactory.create(order(sortingCenter).places("p1", "p2", "p3").build())
                .acceptPlaces("p1")
                .acceptPlaces("p2")
                .get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void acceptAllPlacesChangesOrderStatusKeep() {
        var order = testFactory.create(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1", "p2")
                .get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void sortSinglePlaceDoesntChangeOrderStatus() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1", "p2")
                .sortPlace("p1")
                .get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void sortAllPlacesChangesOrderStatus() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1", "p2")
                .sortPlaces("p1", "p2")
                .get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @Test
    void keepSinglePlaceChangesOrderStatusAndCell() {
        var bufferCell = testFactory.storedCell(sortingCenter, "1", CellType.BUFFER);
        var places = testFactory.create(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1", "p2")
                .sortPlace("p1", bufferCell.getId())
                .getPlaces();
        assertThat(places.get("p1").getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
        assertThat(places.get("p1").getCell()).isNotNull();
    }

    @Test
    void keepAllPlacesChangesOrderStatus() {
        var bufferCell = testFactory.storedCell(sortingCenter, "1", CellType.BUFFER);
        var order = testFactory.create(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1", "p2")
                .sortPlace("p1", bufferCell.getId())
                .sortPlace("p2", bufferCell.getId())
                .get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF);
    }

    @Test
    void returnOrderWithoutOnePlace() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1").makeReturn().get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
    }

    @Test
    void shipToWarehouseOrderWithoutOnePlace() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1").makeReturn().sortPlace("p1").ship().get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    @Test
    void shipToWarehouseCancelledOrderWithoutOnePlace() {
        var order = testFactory.create(order(sortingCenter).places("p1", "p2").build())
                .cancel().acceptPlaces("p1").sortPlace("p1").ship().get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    @Test
    void shipToWarehouseDamagedOrderWithoutOnePlace() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        var order = testFactory.createForToday(
                order(sortingCenter).warehouseCanProcessDamagedOrders(true).places("p1", "p2").build()
        ).acceptPlaces("p1").markOrderAsDamaged().makeReturn().sortPlace("p1", cell.getId()).ship().get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    @Test
    void shipToWarehouseDamagedCancelledOrder() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        var order = testFactory.create(
                order(sortingCenter).warehouseCanProcessDamagedOrders(true).places("p1", "p2").build()
        ).cancel().acceptPlaces("p1", "p2").markOrderAsDamaged()
                .sortPlace("p1", cell.getId()).sortPlace("p2", cell.getId()).ship().get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    @Test
    void cantShipOrderToCourierWithoutOnePlace() {
        var order = testFactory.createForToday(order(sortingCenter).places("p1", "p2").build())
                .acceptPlaces("p1").sortPlace("p1").get();
        Route outgoingCourierRoute = testFactory.findOutgoingCourierRoute(
                testFactory.getOrderLikeForRouteLookup(order))
                    .orElseThrow();
        long cellId = Objects.requireNonNull(testFactory.determineRouteCell(outgoingCourierRoute, order)).getId();
        assertThatThrownBy(() -> routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(outgoingCourierRoute), new ScContext(stockman),
                List.of(cellId),
                null,
                false
        ))).isInstanceOf(ScException.class);
    }

    @Test
    void changeCellCapacityOnOrderSortWhenCellIsFull() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        var whShop = testFactory.storedWarehouse("whShop-1", WarehouseType.SHOP);
        testFactory.setWarehouseProperty(String.valueOf(whShop.getYandexId()),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS,
                "true");
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("o1").places("p1").warehouseReturnId(whShop.getYandexId()).build())
                .acceptPlaces("p1").cancel().keepPlaces(bufferCell.getId(), "p1").get();
        var order2 = testFactory.createForToday(order(sortingCenter).externalId("o2").places("p2").warehouseReturnId(whShop.getYandexId()).build())
                .acceptPlaces("p2").cancel().keepPlaces(bufferCell.getId(), "p2").get();

        testFactory.setFullnessToCell(bufferCell.getId(), true);

        testFactory.sortOrder(order1);

        var actualFullness = transactionTemplate.execute(
                ts -> cellQueryService.getCellsByIds(sortingCenter,
                        List.of(bufferCell.getId())).stream().findFirst().orElseThrow().isFull());
        assertThat(actualFullness).isFalse();
    }

    @Test
    void dontChangeCellCapacityOnOrderSortWhenSortToTheSameCell() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        var whShop = testFactory.storedWarehouse("whShop-1", WarehouseType.SHOP);
        testFactory.setWarehouseProperty(String.valueOf(whShop.getYandexId()),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS,
                "true");
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("o1").places("p1").warehouseReturnId(whShop.getYandexId()).build())
                .acceptPlaces("p1").cancel().keepPlaces(bufferCell.getId(), "p1").get();
        var order2 = testFactory.createForToday(order(sortingCenter).externalId("o2").places("p2").warehouseReturnId(whShop.getYandexId()).build())
                .acceptPlaces("p2").cancel().keepPlaces(bufferCell.getId(), "p2").get();

        testFactory.setFullnessToCell(bufferCell.getId(), true);

        assertDoesNotThrow(() -> testFactory.sortOrder(order1, bufferCell.getId()));

        var actualFullness = transactionTemplate.execute(
                ts -> cellQueryService.getCellsByIds(sortingCenter,
                        List.of(bufferCell.getId())).stream().findFirst().orElseThrow().isFull());
        assertThat(actualFullness).isTrue();
    }

    @Test
    void dontChangeCellCapacityOnOrderSortWhenCellIsNotFull() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, true);
        var whShop = testFactory.storedWarehouse("whShop-1", WarehouseType.SHOP);
        testFactory.setWarehouseProperty(String.valueOf(whShop.getYandexId()),
                WarehouseProperty.CAN_PROCESS_BUFFER_RETURNS,
                "true");
        var bufferCell = testFactory.storedCell(sortingCenter, "br", CellType.BUFFER, CellSubType.BUFFER_RETURNS);
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("o1").places("p1").warehouseReturnId(whShop.getYandexId()).build())
                .acceptPlaces("p1").cancel().keepPlaces(bufferCell.getId(), "p1").get();
        var order2 = testFactory.createForToday(order(sortingCenter).externalId("o2").places("p2").warehouseReturnId(whShop.getYandexId()).build())
                .acceptPlaces("p2").cancel().keepPlaces(bufferCell.getId(), "p2").get();

        testFactory.sortOrder(order1);
        var actualFullness1 = transactionTemplate.execute(
                ts -> cellQueryService.getCellsByIds(sortingCenter, List.of(bufferCell.getId()))
                        .stream().findFirst().orElseThrow().isFull());
        assertThat(actualFullness1).isFalse();

        testFactory.sortOrder(order2);
        var actualFullness2 = transactionTemplate.execute(
                ts -> cellQueryService.getCellsByIds(sortingCenter, List.of(bufferCell.getId()))
                        .stream().findFirst().orElseThrow().isFull());
        assertThat(actualFullness2).isFalse();
    }
}
