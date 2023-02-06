package ru.yandex.market.sc.tms.order;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.logistic.api.model.fulfillment.DeliveryType;
import ru.yandex.market.logistic.api.model.fulfillment.PaymentType;
import ru.yandex.market.sc.core.domain.cell.CellCommandService;
import ru.yandex.market.sc.core.domain.cell.model.CellRequestDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.OrdersScRequest;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.PlaceCommandService;
import ru.yandex.market.sc.core.domain.place.model.PlaceId;
import ru.yandex.market.sc.core.domain.place.model.PlaceScRequest;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.RouteCommandService;
import ru.yandex.market.sc.core.domain.route.RouteFacade;
import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishByCellsRequest;
import ru.yandex.market.sc.core.domain.route.repository.InAdvanceReserveRepository;
import ru.yandex.market.sc.core.domain.route.repository.ReserveStatus;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.ScDateUtils;
import ru.yandex.market.sc.tms.domain.order.OrderRescheduler;
import ru.yandex.market.sc.tms.domain.place.PlaceRescheduler;
import ru.yandex.market.sc.tms.domain.route.RouteCellDistributor;
import ru.yandex.market.sc.tms.domain.routeso.RouteSoCellDistributor;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.monitoring.Monitorings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTmsTest
class OrderReschedulerTest {

    @Autowired
    OrderRescheduler orderRescheduler;

    @Autowired
    PlaceRescheduler placeRescheduler;

    @Autowired
    RouteCellDistributor routeCellDistributor;

    @Autowired
    RouteSoCellDistributor routeSoCellDistributor;

    @Autowired
    OrderCommandService orderCommandService;

    @Autowired
    ScOrderRepository scOrderRepository;

    @Autowired
    TestFactory testFactory;
    @Autowired
    RouteRepository routeRepository;
    @Autowired
    RouteSoMigrationHelper routeSoMigrationHelper;
    @Autowired
    InAdvanceReserveRepository inAdvanceReserveRepository;
    @Autowired
    RouteCommandService routeCommandService;
    @Autowired
    CellCommandService cellCommandService;
    @Autowired
    PlaceCommandService placeCommandService;
    @Autowired
    PlaceRepository placeRepository;
    @Autowired
    RouteFacade routeFacade;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    TransactionTemplate transactionTemplate;

    @MockBean
    Clock clock;

    SortingCenter sortingCenter;
    User user;
    TestFactory.CourierWithDs courierWithDs;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 123L);
        TestFactory.setupMockClock(clock);
        courierWithDs = testFactory.magistralCourier();
    }


    @Disabled("MARKETTPLSC-2808")
    @Test
    void middleMileExpiredDateMultiplaceOrder() {
        var bufferCell = testFactory.storedCell(sortingCenter, "buffer", CellType.BUFFER);
        var order = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2", "p3").dsType(DeliveryServiceType.TRANSIT).build()
        ).enableSortMiddleMileToLot().acceptPlaces("p1", "p2", "p3").get();
        var fakeOrderForTomorrow = testFactory.create(order(sortingCenter).externalId("f1")
                .dsType(DeliveryServiceType.TRANSIT)
                .places("q1", "q2")
                .deliveryDate(LocalDate.now(clock).plusDays(1))
                .shipmentDate(LocalDate.now(clock).plusDays(1))
                .build()).get();//заказ нужен только для того чтобы был маршрут на завтра
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.findRouteCell(route, order).orElseThrow();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        var place1 = placeRepository
                .findByOrderIdAndMainPartnerCode(order.getId(), "p1")
                .orElseThrow();
        var place2 = placeRepository
                .findByOrderIdAndMainPartnerCode(order.getId(), "p2")
                .orElseThrow();
        var place3 = placeRepository
                .findByOrderIdAndMainPartnerCode(order.getId(), "p3")
                .orElseThrow();
        testFactory.sortPlaceToLot(place1, lot, user);
        testFactory.sortPlaceToLot(place2, lot, user);
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
        tomorrowAthHour(1);//наступил новый день, но дата заказа не перенеслась


        testFactory.accept(order);
        final long orderId = order.getId();
        assertThatThrownBy(() -> placeCommandService
                .sortPlace(new PlaceScRequest(new PlaceId(orderId, place3.getMainPartnerCode()), user),
                        bufferCell.getId(), false));
        testFactory.sortPlaceToLot(place3, lot, user);
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var newRoute =
                testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        assertThat(newRoute.getId()).isNotEqualTo(testFactory.getRouteIdForSortableFlow(route));//потому что перенесли заказ при приемке
        testFactory.prepareToShipLot(lot);
        assertThatThrownBy(() -> testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter));//не можем отгрузить лоты (используя старый
        // маршрут)
        //теперь добавим еще один новый заказ с актуальной датой
        var order2 = testFactory.createForToday(
                order(sortingCenter).externalId("2").places("t1", "t2")
                        .dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("t1", "t2").get();
        var actualNewRoute = testFactory.findOutgoingCourierRoute(order2).orElseThrow();
        var cellForNewRoute = testFactory.findRouteCell(actualNewRoute, order2).orElseThrow();
        assertThat(actualNewRoute.getId()).isNotEqualTo(testFactory.getRouteIdForSortableFlow(route));
        assertThat(actualNewRoute.getId()).isEqualTo(newRoute.getId());
        assertThat(cell.getId()).isEqualTo(cellForNewRoute.getId());//ячейка осталась та же что и была на вчерашнем
        // маршруте
        testFactory.shipLots(actualNewRoute.getId(), sortingCenter);
        order = testFactory.getOrder(orderId);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
    }

    @Disabled("MARKETTPLSC-2808")
    @Test
    void middleMileExpiredDateMultiplaceOrderAfterRescheduler() {
        var bufferCell = testFactory.storedCell(sortingCenter, "buffer", CellType.BUFFER);
        var order = testFactory.createForToday(
                order(sortingCenter).places("p1", "p2", "p3").dsType(DeliveryServiceType.TRANSIT).build()
        ).enableSortMiddleMileToLot().acceptPlaces("p1", "p2", "p3").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.findRouteCell(route, order).orElseThrow();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, cell);
        var place1 = placeRepository
                .findByOrderIdAndMainPartnerCode(order.getId(), "p1")
                .orElseThrow();
        var place2 = placeRepository
                .findByOrderIdAndMainPartnerCode(order.getId(), "p2")
                .orElseThrow();
        var place3 = placeRepository
                .findByOrderIdAndMainPartnerCode(order.getId(), "p3")
                .orElseThrow();
        testFactory.sortPlaceToLot(place1, lot, user);
        testFactory.sortPlaceToLot(place2, lot, user);
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
        tomorrowAthHour(1);//наступил новый день, но дата заказа не перенеслась

        orderRescheduler.rescheduleAll(LocalDate.now(clock));//теперь перенесли заказы
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock)));

        final long orderId = order.getId();
        assertThatThrownBy(() -> placeCommandService
                .sortPlace(new PlaceScRequest(new PlaceId(orderId, place3.getMainPartnerCode()), user),
                        bufferCell.getId(), false));
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
        testFactory.sortPlaceToLot(place3, lot, user);
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var newRoute =
                testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        assertThat(newRoute.getId()).isNotEqualTo(testFactory.getRouteIdForSortableFlow(route));
        testFactory.prepareToShipLot(lot);
        testFactory.shipLots(newRoute.getId(), sortingCenter);
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    void doNotSetCellForInAdvanceRouteAfterMidnightAndBeforeCutoffTime() {
        var deliveryService = prepareDeliveryServiceForSortInAdvance(1, "3");

        testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        testFactory.storedCell(sortingCenter, "b1", CellType.BUFFER);
        testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        LocalDate firstDayOfTest = LocalDate.now(clock);
        LocalDate secondDayOfTest = LocalDate.now(clock).plusDays(1);
        LocalDate thirdDayOfTest = LocalDate.now(clock).plusDays(2);
        var orderd1o1 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o1")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(firstDayOfTest)
                        .accept()
                        .get());
        tomorrowAthHour(1);
        var orderd2o1 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o2")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(thirdDayOfTest)
                        .accept()
                        .get());
        var route2 = testFactory.findOutgoingCourierRoute(orderd2o1).orElseThrow();
        assertThat(route2.allowNextRead().getRouteCells()).hasSize(0);
    }

    @Test
    void inAdvanceGoToInAdvanceSutbype() {
        testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        testFactory.storedCell(sortingCenter, "b1", CellType.BUFFER);
        testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        var deliveryService = prepareDeliveryServiceForSortInAdvance(1, "3");
        LocalDate firstDayOfTest = LocalDate.now(clock);
        var place1 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o1")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(firstDayOfTest)
                        .accept()
                        .sort()
                        .getPlace());
        var cell = place1.getCell();
        assertThat(cell.getType()).isEqualTo(CellType.COURIER);
        assertThat(cell.getSubtype()).isEqualTo(CellSubType.IN_ADVANCE_COURIER);
    }

    @Test
    void doNotAssignCellOnAdvanceRoute() {
        var deliveryService = prepareDeliveryServiceForSortInAdvance(1, "3");

        testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        testFactory.storedCell(sortingCenter, "b1", CellType.BUFFER);
        testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        testFactory.storedMagistralCell(sortingCenter, "T1", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());
        testFactory.storedMagistralCell(sortingCenter, "T2", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());
        LocalDate firstDayOfTest = LocalDate.now(clock);
        LocalDate secondDayOfTest = LocalDate.now(clock).plusDays(1);
        LocalDate thirdDayOfTest = LocalDate.now(clock).plusDays(2);
        var orderd1o1 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o1")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(firstDayOfTest)
                        .accept()
                        .get());
        var orderd1o2 =
                testFactory.create(order(sortingCenter).externalId("o2")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(firstDayOfTest)
                        .accept()
                        .sort()
                        .get();
        var orderd1o3 =
                testFactory.create(order(sortingCenter).externalId("o3")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(firstDayOfTest)
                        .accept()
                        .sort()
                        .shipPlace("o3")
                        .get();
        var orderd2o1 =
                testFactory.create(order(sortingCenter).externalId("o4")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(secondDayOfTest)
                        .accept()
                        .get();
        var orderd2o2 =
                testFactory.create(order(sortingCenter).externalId("o5")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(secondDayOfTest)
                        .accept()
                        .sort()
                        .get();
        var orderd2o3 =
                testFactory.create(order(sortingCenter).externalId("o6")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(secondDayOfTest)
                        .accept()
                        .sort()
                        .get();
        var order3 =
                testFactory.create(order(sortingCenter).externalId("o7")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(thirdDayOfTest)
                        .accept()
                        .get();
        var route1 = testFactory.findOutgoingCourierRoute(orderd1o1).orElseThrow();
        var route2 = testFactory.findOutgoingCourierRoute(orderd2o1).orElseThrow();
        var route3 = testFactory.findOutgoingCourierRoute(order3).orElseThrow();

        RouteSoMigrationHelper.allowRouteReading();
        assertThat(route1.getRouteCells()).hasSize(1);
        assertThat(route1.getRouteCells(firstDayOfTest)).hasSize(1);
        var routeCellRoute1 = route1.getRouteCells(firstDayOfTest).get(0);
        var cellT1 = routeCellRoute1.getCell();
        assertThat(routeCellRoute1.getCell().getSubtype()).isEqualTo(CellSubType.IN_ADVANCE_COURIER);
        assertThat(routeCellRoute1.isReservedOnDate(firstDayOfTest)).isTrue();

        assertThat(route2.getRouteCells()).hasSize(2);
        assertThat(route2.getRouteCells(firstDayOfTest)).hasSize(1);
        assertThat(route2.getRouteCells(secondDayOfTest)).hasSize(1);
        var routeCellRoute2D1 = route2.getRouteCells(firstDayOfTest).get(0);
        var routeCellRoute2D2 = route2.getRouteCells(secondDayOfTest).get(0);
        var cellT2 = routeCellRoute2D1.getCell();
        assertThat(routeCellRoute2D2.getCell()).isEqualTo(cellT2);
        assertThat(routeCellRoute2D1.getCell().getSubtype()).isEqualTo(CellSubType.IN_ADVANCE_COURIER);
        assertThat(routeCellRoute2D2.getCell().getSubtype()).isEqualTo(CellSubType.IN_ADVANCE_COURIER);
        assertThat(cellT2).isNotEqualTo(cellT1);

        assertThat(route3.getRouteCells()).hasSize(0);//не назначаем ячейки на пределами макс даты предсорта
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        tomorrowAthHour(1);

        // Запускаем переназначение
        orderRescheduler.inAdvanceRescheduler(LocalDate.now(clock));//переносим заказ 1
        //Мы автоматически проверяем, что маршруты коробок и заказов совпадают после любого теста, поэтому
        //запускать order и place rescheduler нужно вместе
        placeRescheduler.inAdvanceRescheduler(Instant.now(clock));

        orderRescheduler.inAdvanceRescheduler(LocalDate.now(clock));//если вызовем два раза не должны упасть
        placeRescheduler.inAdvanceRescheduler(Instant.now(clock));

        orderd1o1 = testFactory.getOrder(orderd1o1.getId());
        orderd1o2 = testFactory.getOrder(orderd1o2.getId());
        orderd1o3 = testFactory.getOrder(orderd1o3.getId());
        orderd2o1 = testFactory.getOrder(orderd2o1.getId());
        order3 = testFactory.getOrder(order3.getId());
        assertThat(orderd1o1.getOutgoingRouteDate()).isEqualTo(secondDayOfTest);
        assertThat(orderd1o2.getOutgoingRouteDate()).isEqualTo(secondDayOfTest);
        assertThat(orderd1o3.getOutgoingRouteDate()).isEqualTo(firstDayOfTest);//потому что уже отгружен
        assertThat(orderd2o1.getOutgoingRouteDate()).isEqualTo(secondDayOfTest);
        assertThat(order3.getOutgoingRouteDate()).isEqualTo(thirdDayOfTest);
        //кроме того, на маршрут 2 должна была назначится ячейка cellT1
        RouteSoMigrationHelper.allowRouteReading();

        route2 = testFactory.getRoute(route2.getId());
        assertThat(route2.getRouteCells()).hasSize(3);
        routeCellRoute2D1 = route2.getRouteCells(firstDayOfTest).get(0);
        assertThat(routeCellRoute2D1.getCell()).isEqualTo(cellT2);
        assertThat(route2.getRouteCells(secondDayOfTest)).hasSize(2);
        assertThat(route2.getRouteCells(secondDayOfTest)
                .stream()
                .filter(routeCell -> routeCell.getCell().equals(cellT1))
                .findFirst()).isPresent();
        assertThat(route2.getRouteCells(secondDayOfTest)
                .stream()
                .filter(routeCell -> routeCell.getCell().equals(cellT2))
                .findFirst()).isPresent();
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeCellDistributor.distributeCells();//ячейки на 3 день предсорта не должны назначиться
        route3 = testFactory.getRoute(route3.allowNextRead().getId());
        assertThat(route3.allowNextRead().getRouteCells()).hasSize(0);
        routeSoCellDistributor.releaseRouteCellsAfterCutoff();//перераспределение ячеек до cutoff'а. ничего не меняется

        RouteSoMigrationHelper.allowRouteReading();
        route2 = testFactory.getRoute(route2.getId());
        assertThat(route2.getRouteCells()).hasSize(3);
        routeCellRoute2D1 = route2.getRouteCells(firstDayOfTest).get(0);
        assertThat(routeCellRoute2D1.getCell()).isEqualTo(cellT2);
        assertThat(route2.getRouteCells(secondDayOfTest)).hasSize(2);
        assertThat(route2.getRouteCells(secondDayOfTest)
                .stream()
                .filter(routeCell -> routeCell.getCell().equals(cellT1))
                .findFirst()).isPresent();
        assertThat(route2.getRouteCells(secondDayOfTest)
                .stream()
                .filter(routeCell -> routeCell.getCell().equals(cellT2))
                .findFirst()).isPresent();
        testFactory.setupMockClock(clock, clock.instant().plus(5, ChronoUnit.HOURS));

        routeCellDistributor.releaseRouteCellsAfterCutoff();//cutoff наступил. убираем резерв
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.releaseRouteCellsAfterCutoff();//два раза не должны упасть

        RouteSoMigrationHelper.allowRouteReading();
        route2 = testFactory.getRoute(route2.getId());
        assertThat(route2.getRouteCells()).hasSize(2);
        routeCellRoute2D1 = route2.getRouteCells(firstDayOfTest).get(0);
        routeCellRoute2D2 = route2.getRouteCells(secondDayOfTest).get(0);
        assertThat(routeCellRoute2D1.getCell()).isEqualTo(cellT2);
        assertThat(routeCellRoute2D2.getCell()).isEqualTo(cellT2);

        route1 = testFactory.getRoute(route1.getId());
        routeCellRoute1 = route1.getRouteCells(firstDayOfTest).get(0);
        assertThat(routeCellRoute1.getCell()).isEqualTo(cellT1);
        assertThat(routeCellRoute1.isReservedOnDate(firstDayOfTest)).isTrue();

        route3 = testFactory.getRoute(route3.getId());
        assertThat(route3.getRouteCells()).hasSize(2);
        assertThat(route3.getRouteCells(secondDayOfTest)).hasSize(1);
        assertThat(route3.getRouteCells(thirdDayOfTest)).hasSize(1);
        var routeCellRoute3D1 = route3.getRouteCells(secondDayOfTest).get(0);
        var routeCellRoute3D2 = route3.getRouteCells(thirdDayOfTest).get(0);
        assertThat(routeCellRoute3D1.getCell()).isEqualTo(cellT1);
        assertThat(routeCellRoute3D2.getCell()).isEqualTo(cellT1);


        //наступил 3 день теста
        tomorrowAthHour(1);
        assertThat(LocalDate.now(clock)).isEqualTo(thirdDayOfTest);

        // Запускаем переназначение
        orderRescheduler.inAdvanceRescheduler(LocalDate.now(clock));//переносим теперь заказы 2-го дня теста
        //Мы автоматически проверяем, что маршруты коробок и заказов совпадают после любого теста, поэтому
        //запускать order и place rescheduler нужно вместе
        RouteSoMigrationHelper.revokeRouteReadingPermission();
        placeRescheduler.inAdvanceRescheduler(Instant.now(clock));

        orderd1o1 = testFactory.getOrder(orderd1o1.getId());
        orderd1o2 = testFactory.getOrder(orderd1o2.getId());
        orderd1o3 = testFactory.getOrder(orderd1o3.getId());
        orderd2o1 = testFactory.getOrder(orderd2o1.getId());
        orderd2o2 = testFactory.getOrder(orderd2o2.getId());
        orderd2o3 = testFactory.getOrder(orderd2o3.getId());
        order3 = testFactory.getOrder(order3.getId());
        assertThat(orderd1o1.getOutgoingRouteDate()).isEqualTo(thirdDayOfTest);
        assertThat(orderd1o2.getOutgoingRouteDate()).isEqualTo(thirdDayOfTest);
        assertThat(orderd1o3.getOutgoingRouteDate()).isEqualTo(firstDayOfTest);//потому что уже отгружен
        assertThat(orderd2o1.getOutgoingRouteDate()).isEqualTo(thirdDayOfTest);
        assertThat(orderd2o2.getOutgoingRouteDate()).isEqualTo(thirdDayOfTest);
        assertThat(orderd2o3.getOutgoingRouteDate()).isEqualTo(thirdDayOfTest);
        assertThat(order3.getOutgoingRouteDate()).isEqualTo(thirdDayOfTest);

        RouteSoMigrationHelper.allowRouteReading();
        route3 = testFactory.getRoute(route3.getId());
        assertThat(route3.getRouteCells()).hasSize(3);
        assertThat(route3.getRouteCells(secondDayOfTest).get(0).getCell()).isEqualTo(cellT1);
        assertThat(route3.getRouteCells(thirdDayOfTest)).hasSize(2);
        assertThat(route3.getRouteCells(thirdDayOfTest)
                .stream()
                .filter(routeCell -> routeCell.getCell().equals(cellT1))
                .findFirst()).isPresent();
        assertThat(route3.getRouteCells(thirdDayOfTest)
                .stream()
                .filter(routeCell -> routeCell.getCell().equals(cellT2))
                .findFirst()).isPresent();
        var routeFinishByCellsRequest = new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route3.getId()), new ScContext(testFactory.storedUser(sortingCenter, 7L)),

                List.of(cellT2.getId()),
                null,
                false
        );
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        assertThatCode(() -> routeCommandService.finishOutgoingRouteWithCell(routeFinishByCellsRequest))
                .doesNotThrowAnyException();
        orderd2o1 = testFactory.getOrder(orderd2o1.getId());
        orderd2o2 = testFactory.getOrder(orderd2o2.getId());
        orderd2o3 = testFactory.getOrder(orderd2o3.getId());
        order3 = testFactory.getOrder(order3.getId());
        assertThat(orderd2o1.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
        assertThat(orderd2o2.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(orderd2o3.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(order3.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
    }

    @Test
    void doNotDeleteRouteCellForInAdvanceRoute() {
        var deliveryService = prepareDeliveryServiceForSortInAdvance(1, "3");

        var cell1 = testFactory.storedMagistralCell(sortingCenter, "T1", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());
        var cell2 = testFactory.storedMagistralCell(sortingCenter, "T2", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());


        var deliveryDateForOrder1 = LocalDate.now(clock);

        var order1 = testFactory.create(order(sortingCenter).externalId("o1")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT).build())
                .updateShipmentDate(deliveryDateForOrder1)
                .cancel()
                .get();

        var route1 = routeRepository
                .findByExpectedDateAndSortingCenterAndCourierToId(deliveryDateForOrder1,
                        sortingCenter,
                        order1.getCourier().getId()).orElseThrow();
        RouteSoMigrationHelper.allowRouteReading();
        assertThat(route1.getRouteCells()).hasSize(1);
        var routeCellsForToday = route1.getRouteCells(LocalDate.now(clock));
        var routeCellsForTomorrow = route1.getRouteCells(LocalDate.now(clock).plusDays(1));
        assertThat(routeCellsForToday).hasSize(1);
        assertThat(List.of(cell1, cell2).contains(routeCellsForToday.get(0).getCell())).isTrue();
        assertThat(routeCellsForTomorrow).hasSize(0);
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeCellDistributor.distributeCells();//не должны удалять резервы ячеек для "револьверных" маршрутов

        RouteSoMigrationHelper.allowRouteReading();
        route1 = routeRepository
                .findByExpectedDateAndSortingCenterAndCourierToId(deliveryDateForOrder1,
                        sortingCenter,
                        order1.getCourier().getId()).orElseThrow();
        routeCellsForToday = route1.getRouteCells(LocalDate.now(clock));
        routeCellsForTomorrow = route1.getRouteCells(LocalDate.now(clock).plusDays(1));
        assertThat(routeCellsForToday).hasSize(1);
        assertThat(List.of(cell1, cell2).contains(routeCellsForToday.get(0).getCell())).isTrue();
        assertThat(routeCellsForTomorrow).hasSize(0);
        RouteSoMigrationHelper.revokeRouteReadingPermission();
    }

    @Test
    void testRescheduleInAdvanceWhenOrderInLot() {
        var deliveryService = prepareDeliveryServiceForSortInAdvance(1, "3");

        var c1 = testFactory.storedMagistralCell(sortingCenter, "T1", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());
        var c2 = testFactory.storedMagistralCell(sortingCenter, "T2", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());


        var deliveryDateForOrder1 = LocalDate.now(clock);
        var deliveryDateForOrder2 = LocalDate.now(clock).plusDays(1);

        var place1 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o1")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder1)
                        .accept()
                        .sort()
                        .getPlace());
        var place2 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o2")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder2)
                        .accept()
                        .sort()
                        .getPlace());

        var orderCell1 = place1.getCell();
        var orderCell2 = place2.getCell();
        assertThat(orderCell1.getSubtype()).isEqualTo(CellSubType.IN_ADVANCE_COURIER);
        assertThat(orderCell2.getSubtype()).isEqualTo(CellSubType.IN_ADVANCE_COURIER);

        //так как отсортировали place1 в лот, то нужно назначить новую ячейку, в которой лежит этот лот
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, orderCell1);
        testFactory.sortPlaceToLot(place1, lot, user);
        tomorrowAthHour(1);

        // Запускаем переназначение
        orderRescheduler.inAdvanceRescheduler(LocalDate.now(clock));
        //Мы автоматически проверяем, что маршруты коробок и заказов совпадают после любого теста, поэтому
        //запускать order и place rescheduler нужно вместе
        placeRescheduler.inAdvanceRescheduler(Instant.now(clock));

        place1 = testFactory.updated(place1);
        place2 = testFactory.updated(place2);
        assertThat(place1.getOutgoingRouteDate()).isEqualTo(deliveryDateForOrder2);
        assertThat(place2.getOutgoingRouteDate()).isEqualTo(deliveryDateForOrder2);
        var route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();
        assertThat(route.allowNextRead().getCells(LocalDate.now(clock))
                .stream().map(Cell::getId).toList())
                .containsOnly(c1.getId(), c2.getId());
    }

    @Test
    void testRescheduleSortInAdvance() {
        var deliveryService = prepareDeliveryServiceForSortInAdvance(1, "3");

        testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        testFactory.storedCell(sortingCenter, "b1", CellType.BUFFER);
        testFactory.storedCell(sortingCenter, "r1", CellType.RETURN);
        testFactory.storedMagistralCell(sortingCenter, "T1", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());
        testFactory.storedMagistralCell(sortingCenter, "T2", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());


        var deliveryDateForOrder1 = LocalDate.now(clock);
        var deliveryDateForOrder2 = LocalDate.now(clock).plusDays(1);

        var place1 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o1")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder1)
                        .accept()
                        .sort()
                        .getPlace());
        var place3 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o3")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder1)
                        .accept()
                        .getPlace());
        var place2 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o2")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder2)
                        .accept()
                        .sort()
                        .getPlace());
        var place4 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o4")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder2)
                        .accept()
                        .sort()
                        .getPlace());

        var cell1 = place1.getCell();
        var cell2 = place2.getCell();

        assertThat(cell1.getSubtype()).isEqualTo(CellSubType.IN_ADVANCE_COURIER);
        assertThat(cell2.getSubtype()).isEqualTo(CellSubType.IN_ADVANCE_COURIER);

        var route1 = testFactory.findOutgoingCourierRoute(place1).orElseThrow().allowReading();
        var route2 = testFactory.findOutgoingCourierRoute(place2).orElseThrow().allowReading();

        assertThat(route1.getRouteCells()).hasSize(1);
        assertThat(route2.getRouteCells()).hasSize(2);
        var routeCellsForToday = route1.getRouteCells(LocalDate.now(clock));
        var routeCellsForTomorrow = route1.getRouteCells(LocalDate.now(clock).plusDays(1));
        assertThat(routeCellsForToday).hasSize(1);
        assertThat(routeCellsForToday.get(0).getCell()).isEqualTo(cell1);
        assertThat(routeCellsForTomorrow).hasSize(0);

        var routeCellsForToday2 = route2.getRouteCells(LocalDate.now(clock));
        var routeCellsForTomorrow2 = route2.getRouteCells(LocalDate.now(clock).plusDays(1));
        assertThat(routeCellsForToday2).hasSize(1);
        assertThat(routeCellsForTomorrow2).hasSize(1);
        assertThat(routeCellsForToday2.get(0).getCell()).isEqualTo(cell2);
        assertThat(routeCellsForToday2.get(0).getCell()).isEqualTo(cell2);

        Place finalPlace2 = place2;
        assertThatThrownBy(() -> testFactory.shipOrderRoute(finalPlace2)).isInstanceOf(ScException.class);

        tomorrowAthHour(1);
        RouteSoMigrationHelper.allowRouteReading();
        orderRescheduler.rescheduleAll(LocalDate.now(clock));//ничего не должно поменяться

        RouteSoMigrationHelper.revokeRouteReadingPermission();
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock)));

        place1 = testFactory.updated(place1);
        route1 = testFactory.findOutgoingCourierRoute(place1).orElseThrow().allowReading();
        assertThat(place1.getOutgoingRouteDate()).isEqualTo(deliveryDateForOrder1);
        assertThat(place1.getCell()).isEqualTo(cell1);
        assertThat(place1.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var routeCellsForTodayAfterRecheduleAll = route1.getRouteCells(LocalDate.now(clock));
        var routeCellsForTomorrowAfterRecheduleAll = route1.getRouteCells(LocalDate.now(clock).minusDays(1));
        assertThat(routeCellsForTodayAfterRecheduleAll).hasSize(0);
        assertThat(routeCellsForTomorrowAfterRecheduleAll).hasSize(1);
        assertThat(routeCellsForTomorrowAfterRecheduleAll.get(0).getCell()).isEqualTo(cell1);
        assertThat(place1.getOutgoingRouteDate()).isEqualTo(deliveryDateForOrder1);

        // Запускаем переназначение
        RouteSoMigrationHelper.allowRouteReading();
        orderRescheduler.inAdvanceRescheduler(LocalDate.now(clock));//заказ 1 не успели отгрузить. его хотим перенести

        RouteSoMigrationHelper.revokeRouteReadingPermission();
        //Мы автоматически проверяем, что маршруты коробок и заказов совпадают после любого теста, поэтому
        //запускать order и place rescheduler нужно вместе
        placeRescheduler.inAdvanceRescheduler(Instant.now(clock));

        place1 = testFactory.updated(place1);
        place2 = testFactory.updated(place2);

        assertThat(place1.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(place1.getCell()).isEqualTo(cell1);
        assertThat(place1.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var routeAfterRescheduleInAdvance = testFactory.findOutgoingCourierRoute(place1)
                                                                            .orElseThrow().allowReading();
        assertThat(routeAfterRescheduleInAdvance).isEqualTo(route2);
        assertThat(routeAfterRescheduleInAdvance.getExpectedDate()).isEqualTo(LocalDate.now(clock));
        assertThat(routeAfterRescheduleInAdvance.getRouteCells()).hasSize(3);
        assertThat(routeAfterRescheduleInAdvance.getRouteCells(LocalDate.now(clock)).get(0)
                .getCell()).isEqualTo(cell1);

        testFactory.setupMockClock(clock, clock.instant().plus(4, ChronoUnit.HOURS));
        //должны убрать резерв и перенести на последний день предсорта
        RouteSoMigrationHelper.allowRouteReading();
        routeCellDistributor.releaseRouteCellsAfterCutoff();

        RouteSoMigrationHelper.revokeRouteReadingPermission();
        routeSoCellDistributor.releaseRouteCellsAfterCutoff();

        place1 = testFactory.updated(place1);
        assertThat(place1.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(place1.getCell()).isEqualTo(cell1);
        assertThat(place1.getFfStatus()).isEqualTo(ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var routeAfterCellRelease = testFactory.findOutgoingCourierRoute(place1).orElseThrow().allowReading();
        assertThat(routeAfterCellRelease).isEqualTo(route2);
        assertThat(routeAfterCellRelease.getExpectedDate()).isEqualTo(LocalDate.now(clock));
        assertThat(routeAfterCellRelease.getRouteCells()).hasSize(2);

        var futureRoute = routeRepository
                .findByExpectedDateAndSortingCenterAndCourierToId(LocalDate.now(clock).plusDays(1),
                        sortingCenter,
                        place1.getCourier().getId());
        assertThat(futureRoute).isPresent();
        Route fRoute = futureRoute.get().allowReading();
        assertThat(fRoute.getRouteCells()).hasSize(2);
        assertThat(fRoute.getRouteCells(LocalDate.now(clock).plusDays(1))
                .get(0).getCell()).isEqualTo(cell1);

        assertThat(inAdvanceReserveRepository.findAll())
                .hasSize(SortableFlowSwitcherExtension.useNewRouteSoStage1_2() ? 2 : 1); //для второго рескедьюлера
        assertThat(inAdvanceReserveRepository.findAll().stream()
                .allMatch(reserve -> reserve.getReserveStatus() == ReserveStatus.PROCESSED)).isTrue();
    }

    @Test
    void createOrUpdateInAdvanceCellWhenInAdvanceRouteExists() {
        var deliveryService = prepareDeliveryServiceForSortInAdvance(1, "2");
        LocalDate deliveryDateForOrder = LocalDate.now(clock);
        var targetCell = testFactory.storedActiveCell(
                sortingCenter, CellType.COURIER, CellSubType.IN_ADVANCE_COURIER, "T1");
        var order1 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o1")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder)
                        .accept()
                        .sort()
                        .get());
        var order2 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o2")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder.plusDays(1))
                        .get());
        var order3 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o3")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder.plusDays(2))
                        .get());
        var order4 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o4")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder.plusDays(3))
                        .get());
        assertThatThrownBy(() -> cellCommandService.createCellAndBindRoutes(sortingCenter,
                new CellRequestDto("1",
                        CellStatus.ACTIVE,
                        CellType.COURIER,
                        CellSubType.IN_ADVANCE_COURIER,
                        null,
                        order1.getCourierId().get(),
                        null,
                        null)));
        cellCommandService.updateCell(sortingCenter,
                targetCell.getId(),
                new CellRequestDto("1",
                        CellStatus.ACTIVE,
                        CellType.COURIER,
                        CellSubType.IN_ADVANCE_COURIER,
                        null,
                        order1.getCourierId().get(),
                        null,
                        null));

        RouteSoMigrationHelper.allowRouteReading();
        assertThat(testFactory.findOutgoingCourierRoute(order1).orElseThrow().getRouteCells()).hasSize(1);
        assertThat(testFactory.findOutgoingCourierRoute(order2).orElseThrow().getRouteCells()).hasSize(2);
        assertThat(testFactory.findOutgoingCourierRoute(order3).orElseThrow().getRouteCells()).hasSize(0);
        assertThat(testFactory.findOutgoingCourierRoute(order4).orElseThrow().getRouteCells()).hasSize(0);
        RouteSoMigrationHelper.revokeRouteReadingPermission();
    }


    @Test
    void rescheduleSortInAdvanceWithZeroCutoff() {

        var deliveryService = prepareDeliveryServiceForSortInAdvance(1, "0");
        LocalDate deliveryDateForOrder = LocalDate.now(clock);
        testFactory.storedMagistralCell(sortingCenter, "T1", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());

        testFactory.storedMagistralCell(sortingCenter, "T2", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());

        Place place1 = transactionTemplate.execute(ts -> testFactory.create(order(sortingCenter).externalId("o1")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT).build())
                .updateShipmentDate(deliveryDateForOrder)
                .accept()
                .sort()
                .getPlace());

        Place place2 = transactionTemplate.execute(ts -> testFactory.create(order(sortingCenter).externalId("o2")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT).build())
                .updateShipmentDate(deliveryDateForOrder.plusDays(1))
                .accept()
                .sort()
                .getPlace());

        var cell1 = place1.getCell();
        var cell2 = place2.getCell();
        Set<RouteCell> routeCells = testFactory.findOutgoingCourierRoute(place1)
                                            .orElseThrow().allowNextRead().getRouteCells();
        assertThat(routeCells).hasSize(1);
        assertThat(routeCells
                .stream()
                .map(RouteCell::getCell)
                .toList())
                .containsOnly(cell1);
        Set<RouteCell> routeCells2 = testFactory.findOutgoingCourierRoute(place2).
                                            orElseThrow().allowNextRead().getRouteCells();
        assertThat(routeCells2).hasSize(2);
        assertThat(routeCells2
                .stream()
                .map(RouteCell::getCell)
                .toList())
                .containsOnly(cell2);
        tomorrowAthHour(1);

        RouteSoMigrationHelper.allowRouteReading();
        // Тестируем
        orderRescheduler.inAdvanceRescheduler(LocalDate.now(clock));

        RouteSoMigrationHelper.revokeRouteReadingPermission();
        //здесь должны сразу назначить одну из ячеек на
        //Мы автоматически проверяем, что маршруты коробок и заказов совпадают после любого теста, поэтому
        //запускать order и place rescheduler нужно вместе
        placeRescheduler.inAdvanceRescheduler(Instant.now(clock));

        // новый день
        place1 = testFactory.updated(place1);
        assertThat(place1.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        Route route = testFactory.findOutgoingCourierRoute(place1).orElseThrow().allowReading();
        assertThat(route.getRouteCells()).hasSize(2);
        assertThat(route.getRouteCells(LocalDate.now(clock)))
                .hasSize(1);
        assertThat(route.getRouteCells()
                .stream()
                .map(RouteCell::getCell)
                .toList())
                .containsOnly(cell2);
        var futureRoute = routeRepository.findByExpectedDateAndSortingCenterAndCourierToId(
                deliveryDateForOrder.plusDays(2), sortingCenter, place1.getCourier().getId())
                                                                        .orElseThrow().allowReading();
        assertThat(futureRoute.getRouteCells()).hasSize(2);
        assertThat(futureRoute.getRouteCells()
                .stream()
                .map(RouteCell::getCell)
                .toList())
                .containsOnly(cell1);
        assertThat(futureRoute.hasAnyCellOnDate(deliveryDateForOrder.plusDays(1))).isTrue();
        assertThat(futureRoute.hasAnyCellOnDate(deliveryDateForOrder.plusDays(2))).isTrue();

        place1 = testFactory.updated(place1);
        OrderLike finalPlace1 = place1;
        assertThatCode(() -> testFactory.shipOrderRoute(finalPlace1)).doesNotThrowAnyException();

        place1 = testFactory.updated(place1);
        assertThat(place1.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);//отгружается
        // только второй заказа. этот нужно пересортировывать

        place2 = testFactory.updated(place2);
        assertThat(place2.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        var requestDto = new FinishRouteRequestDto(
                null, null, cell1.getId(), false, null, null
        );
        final Place finalFinalPlace1 = place1;

        var routableId = testFactory.getRouteIdForSortableFlow(
                testFactory.findOutgoingCourierRoute(finalFinalPlace1).orElseThrow()
        );
        assertThatThrownBy(() -> routeFacade.finishOutgoingRoute(routableId, requestDto, new ScContext(user)));
    }

    @Test
    void rescheduleSortInAdvanceOnce() {
        var deliveryService = prepareDeliveryServiceForSortInAdvance(1, "2");
        LocalDate deliveryDateForOrder = LocalDate.now(clock);
        testFactory.storedMagistralCell(sortingCenter, "T1", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());
        var place1 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o1")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder)
                        .accept()
                        .sort()
                        .getPlace());
        var cell = place1.getCell();
        assertThat(testFactory.findOutgoingCourierRoute(place1).orElseThrow()
                .allowNextRead()
                .getRouteCells()).hasSize(1);
        tomorrowAthHour(1);

        RouteSoMigrationHelper.allowRouteReading();
        // Запускаем переназначение
        orderRescheduler.inAdvanceRescheduler(LocalDate.now(clock));
        //Мы автоматически проверяем, что маршруты коробок и заказов совпадают после любого теста, поэтому
        //запускать order и place rescheduler нужно вместе
        RouteSoMigrationHelper.revokeRouteReadingPermission();
        placeRescheduler.inAdvanceRescheduler(Instant.now(clock));

        place1 = testFactory.updated(place1);
        assertThat(place1.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        Route route = testFactory.findOutgoingCourierRoute(place1).orElseThrow().allowReading();
        assertThat(route.getRouteCells()).hasSize(1);
        assertThat(route.getRouteCells().stream().map(RouteCell::getCell).toList()).contains(cell);

        RouteSoMigrationHelper.allowRouteReading();
        routeCellDistributor.releaseRouteCellsAfterCutoff();//cutoff еще не наступил
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.releaseRouteCellsAfterCutoff();//cutoff еще не наступил

        place1 = testFactory.updated(place1);
        assertThat(place1.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(testFactory.findOutgoingCourierRoute(place1).orElseThrow()
                                                .allowNextRead().getRouteCells()).hasSize(1);
        testFactory.setupMockClock(clock, clock.instant().plus(4, ChronoUnit.HOURS));


        RouteSoMigrationHelper.allowRouteReading();
        routeCellDistributor.releaseRouteCellsAfterCutoff();
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.releaseRouteCellsAfterCutoff();

        place1 = testFactory.updated(place1);
        assertThat(place1.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(testFactory.findOutgoingCourierRoute(place1).orElseThrow()
                                                .allowNextRead().getRouteCells()).hasSize(0);
        var futureRoute = routeRepository
                .findByExpectedDateAndSortingCenterAndCourierToId(LocalDate.now(clock).plusDays(1),
                        sortingCenter,
                        place1.getCourier().getId()).orElseThrow().allowReading();
        assertThat(futureRoute.getRouteCells()).hasSize(2);
        assertThat(futureRoute.getRouteCells(LocalDate.now(clock).plusDays(1)).get(0).getCell()).isEqualTo(cell);

        place1 = testFactory.updated(place1);
        OrderLike finalPlace1 = place1;
        assertThatThrownBy(() -> testFactory.shipOrderRoute(finalPlace1)).isInstanceOf(Exception.class);
    }

    @Test
    void rescheduleSortInAdvanceCanShip() {
        var deliveryService = prepareDeliveryServiceForSortInAdvance(1, "2");
        LocalDate deliveryDateForOrder = LocalDate.now(clock);
        testFactory.storedMagistralCell(sortingCenter, "T1", CellSubType.IN_ADVANCE_COURIER,
                courierWithDs.courier().getId());
        var place1 = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).externalId("o1")
                                .deliveryService(deliveryService)
                                .dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(deliveryDateForOrder)
                        .accept()
                        .sort()
                        .getPlace());
        var cell = place1.getCell();
        assertThat(testFactory.findOutgoingCourierRoute(place1).orElseThrow().allowNextRead().getRouteCells()).hasSize(1);
        tomorrowAthHour(1);

        // Запускаем переназначение
        orderRescheduler.inAdvanceRescheduler(LocalDate.now(clock));
        //Мы автоматически проверяем, что маршруты коробок и заказов совпадают после любого теста, поэтому
        //запускать order и place rescheduler нужно вместе
        placeRescheduler.inAdvanceRescheduler(Instant.now(clock));

        place1 = testFactory.updated(place1);
        assertThat(place1.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        Set<RouteCell> routeCells = testFactory.findOutgoingCourierRoute(place1).orElseThrow()
                                                                            .allowNextRead().getRouteCells();
        assertThat(routeCells).hasSize(1);
        assertThat(routeCells.stream().map(RouteCell::getCell).toList()).contains(cell);

        routeCellDistributor.releaseRouteCellsAfterCutoff();//cutoff еще не наступил
        routeSoCellDistributor.releaseRouteCellsAfterCutoff();//cutoff еще не наступил

        place1 = testFactory.updated(place1);
        assertThat(place1.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        Set<RouteCell> routeCells1 = testFactory.findOutgoingCourierRoute(place1).orElseThrow()
                .allowNextRead()
                .getRouteCells();
        assertThat(routeCells1).hasSize(1);
        OrderLike finalPlace1 = place1;
        assertThatCode(() -> testFactory.shipOrderRoute(finalPlace1)).doesNotThrowAnyException();

        place1 = testFactory.updated(place1);
        assertThat(place1.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        var futureRouteO = routeRepository
                .findByExpectedDateAndSortingCenterAndCourierToId(LocalDate.now(clock).plusDays(1),
                        sortingCenter,
                        place1.getCourier().getId());
        assertThat(futureRouteO).isNotPresent();//ячейку еще не сняли после cutoff

        testFactory.setupMockClock(clock, clock.instant().plus(4, ChronoUnit.HOURS));
        RouteSoMigrationHelper.allowRouteReading();
        routeCellDistributor.releaseRouteCellsAfterCutoff();
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.releaseRouteCellsAfterCutoff();

        var futureRoute = routeRepository
                .findByExpectedDateAndSortingCenterAndCourierToId(LocalDate.now(clock).plusDays(1),
                        sortingCenter,
                        place1.getCourier().getId()).orElseThrow().allowReading();
        assertThat(futureRoute.getRouteCells()).hasSize(2);
        assertThat(futureRoute.getRouteCells(LocalDate.now(clock).plusDays(1)).get(0).getCell()).isEqualTo(cell);
    }

    @Test
    @Disabled("long")
    void rescheduleALotOfReturns() {
        int totalOrders = 10_000;
        LocalDate date = prepareALotOfOrders(totalOrders);
        checkOrdersTotalAndAllOrdersHasOutgoingRouteDate(totalOrders, date);

        orderRescheduler.rescheduleAll(date.plusDays(1));
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(date.plusDays(1)));


        checkOrdersTotalAndAllOrdersHasOutgoingRouteDate(totalOrders, date.plusDays(1));
        assertThat(Monitorings.getMonitoring().getResult().getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @NotNull
    private LocalDate prepareALotOfOrders(int ordersCount) {
        var deliveryService = testFactory.storedDeliveryService();
        var warehouse = testFactory.storedWarehouse();
        var date = LocalDate.now(clock);
        var location = testFactory.storedLocation();
        var measurements = testFactory.storedMeasurements();
        jdbcTemplate.batchUpdate("" +
                        "insert into orders (" +
                        "   created_at, updated_at, " +
                        "   sorting_center_id, external_id, ff_status, " +
                        "   warehouse_from_id, warehouse_return_id, " +
                        "   delivery_service_id, delivery_date, shipment_date," +
                        "   payment_method, delivery_type, location_to_id, measurements_id, " +
                        "   cargo_cost, assessed_cost, recipient_name, recipient_phones, " +
                        "   total, amount_prepaid, " +
                        "   incoming_route_date, outgoing_route_date, " +
                        "   is_damaged, is_middle_mile, status_message, order_history_updated " +
                        "   ) " +
                        "values (" +
                        "   now(), now(), " +
                        "   ?, ?, ?, " +
                        "   ?, ?, " +
                        "   ?, ?, ?, " +
                        "   ?, ?, ?, ?, " +
                        "   100, 100, 'Клиент', '{}'::text[], " +
                        "   100, 0, " +
                        "   ?, ?, " +
                        "   'f', 'f', 'no_status_message', now() " +
                        ")",
                IntStream.range(0, ordersCount)
                        .mapToObj(id -> "o-" + id)
                        .map(externalId -> new Object[]{
                                sortingCenter.getId(), externalId,
                                ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE.name(),
                                warehouse.getId(), warehouse.getId(),
                                deliveryService.getId(), date, date,
                                PaymentType.CARD.name(), DeliveryType.COURIER.name(), location.getId(),
                                measurements.getId(),
                                date, date
                        })
                        .toList()
        );
        return date;
    }

    private void checkOrdersTotalAndAllOrdersHasOutgoingRouteDate(int expectedOrdersCount, LocalDate expectedDate) {
        List<LocalDate> outgoingRouteDates = jdbcTemplate.queryForList(
                "select outgoing_route_date from orders", LocalDate.class);
        assertThat(outgoingRouteDates).hasSize(expectedOrdersCount);
        assertThat(outgoingRouteDates).allMatch(outgoingRouteDate -> Objects.equals(outgoingRouteDate, expectedDate));
    }

    @Test
    void rescheduleAndShipSortedTransit() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(1)
        );
        OrderLike order = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(LocalDate.now(clock)).accept().sort().get());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        orderRescheduler.rescheduleAll(LocalDate.now(clock));
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock)));

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        order = testFactory.shipOrderRoute(order);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    void rescheduleSortedTransit() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(1)
        );
        OrderLike order = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(LocalDate.now(clock)).accept().sort().get());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        orderRescheduler.rescheduleAll(LocalDate.now(clock));
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock)));

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
    }

    @Test
    void checkCorrectWorkRescheduleJob() {
        var order = testFactory.createForToday(
                        order(sortingCenter)
                                .dsType(DeliveryServiceType.TRANSIT)
                                .places("p1", "p2").externalId("o1").build()
                )
                .get();

        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        testFactory.acceptPlace(order, "p1");
        testFactory.sortPlace(order, "p1");
        testFactory.acceptPlace(order, "p2");
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));
        OrderLike finalOrder = order;
        assertThatThrownBy(() -> orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of()), new ScContext(user)
        ), Objects.requireNonNull(finalOrder.getCourier()).getId(), null))
                .isInstanceOf(ScException.class);

        orderRescheduler.rescheduleAll(LocalDate.now(clock));
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock)));

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
        assertThat(order.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);
        orderCommandService.shipOrders(OrdersScRequest.ofExternalIds(
                Map.of("o1", List.of()), new ScContext(user)
        ), Objects.requireNonNull(order.getCourier()).getId(), null);
        order = testFactory.getOrder(order.getId());
        assertThat(order.getFfStatus()).isEqualTo(ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
    }

    @Test
    void rescheduleAndShipSortedMiddleMile() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(1)
        );

        OrderLike order = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(LocalDate.now(clock)).accept().sort().get());

        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        orderRescheduler.rescheduleAll(LocalDate.now(clock));
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock)));

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        order = testFactory.shipOrderRoute(order);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
    }

    @Test
    void rescheduleSortedMiddleMile() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(1)
        );

        OrderLike order = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(LocalDate.now(clock)).accept().sort().get());

        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        TestFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        orderRescheduler.rescheduleAll(LocalDate.now(clock));
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock)));

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
    }

    @Test
    void rescheduleAllReturns() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().accept().sort().get();
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        orderRescheduler.rescheduleAll(LocalDate.now(clock).plusDays(1));
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock).plusDays(1)));

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock).plusDays(1));
    }

    @Test
    void doNotRescheduleMiddleMileOrdersIfPresortEnabled() {
        var deliveryService = prepareDeliveryServiceForSortInAdvance(1, "1");
        OrderLike order = transactionTemplate.execute(ts -> testFactory.create(order(sortingCenter)
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(deliveryService)
                        .build())
                .updateShipmentDate(LocalDate.now(clock)).get());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        testFactory.accept(order);
        orderRescheduler.rescheduleAll(LocalDate.now(clock).plusDays(1));
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock).plusDays(1)));

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
    }

    @Test
    void rescheduleAllMiddleMileOrders() {
        OrderLike order = transactionTemplate.execute(ts ->
                testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(LocalDate.now(clock)).get());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        testFactory.accept(order);
        orderRescheduler.rescheduleAll(LocalDate.now(clock).plusDays(1));
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock).plusDays(1)));

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock).plusDays(1));
    }

    @Test
    void rescheduleAllTransitOrders() {
        OrderLike order = transactionTemplate.execute(
                ts -> testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                        .updateShipmentDate(LocalDate.now(clock)).get());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        testFactory.accept(order);

        orderRescheduler.rescheduleAll(LocalDate.now(clock).plusDays(1));
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock).plusDays(1)));

        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock).plusDays(1));
    }

    @Test
    void doNotRescheduleNonDropships() {
        OrderLike order = transactionTemplate.execute(
                ts -> testFactory.create(order(sortingCenter).dsType(DeliveryServiceType.LAST_MILE_COURIER).build())
                        .updateShipmentDate(LocalDate.now(clock)).get());
        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));

        orderRescheduler.rescheduleAll(LocalDate.now(clock).plusDays(1));
        // Нужно также обновить коробки, чтобы SortableFlowSwitcher
        // не упал при сверке маршрутов между заказами и коробками
        placeRescheduler.rescheduleAll(ScDateUtils.toNoon(LocalDate.now(clock).plusDays(1)));

        assertThat(order.getOutgoingRouteDate()).isEqualTo(LocalDate.now(clock));
    }

    @SuppressWarnings("SameParameterValue")
    private DeliveryService prepareDeliveryServiceForSortInAdvance(int daysInAdvance,
                                                                   String cutoff) {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        testFactory.setDeliveryServiceProperty(courierWithDs.deliveryService(),
                DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, String.valueOf(sortingCenter.getId()));
        testFactory.setDeliveryServiceProperty(courierWithDs.deliveryService(),
                DeliveryServiceProperty.CUTOFF_SORT_IN_ADVANCE, cutoff);
        return courierWithDs.deliveryService();
    }

    private void tomorrowAthHour(int hour) {
        ZoneId systemZone = ZoneId.systemDefault();
        TestFactory.setupMockClock(clock, DateTimeUtil.tomorrowAtHour(hour,
                clock,
                systemZone.getRules().getOffset(Instant.now(clock))));
    }

}
