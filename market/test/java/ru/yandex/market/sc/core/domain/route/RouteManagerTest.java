package ru.yandex.market.sc.core.domain.route;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.OrderJdbcCommandService;
import ru.yandex.market.sc.core.domain.order.SortService;
import ru.yandex.market.sc.core.domain.order.model.BatchUpdateRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.place.PlaceCommandService;
import ru.yandex.market.sc.core.domain.place.model.PlaceScRequest;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.jdbc.RouteJdbcRepository;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinishOrder;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinishPlaceRepository;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.route_so.Routable;
import ru.yandex.market.sc.core.domain.route_so.RouteSoCommandService;
import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.CourierDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
class RouteManagerTest {

    @Autowired
    OrderCommandService orderCommandService;
    @Autowired
    SortService sortService;
    @Autowired
    PlaceCommandService placeCommandService;

    @Autowired
    RouteRepository routeRepository;
    @Autowired
    RouteNonBlockingQueryService routeNonBlockingQueryService;
    @Autowired
    RouteSoMigrationHelper routeSoMigrationHelper;
    @Autowired
    RouteSoRepository routeSoRepository;
    @Autowired
    TestFactory testFactory;
    @MockBean
    Clock clock;
    @Autowired
    TransactionTemplate transactionTemplate;
    @SpyBean
    RouteJdbcRepository routeJdbcRepository;
    @Autowired
    RouteCommandService routeCommandService;
    @Autowired
    RouteSoCommandService routeSoCommandService;
    @Autowired
    PlaceRepository placeRepository;
    @Autowired
    OrderJdbcCommandService orderJdbcCommandService;
    @Autowired
    RouteFinishPlaceRepository routeFinishPlaceRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 123L);
        testFactory.setupMockClock(clock);
    }

    @Test
    void ordersJdbcUpdatedEventWithDifferentRouteTypes() {
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().ship().cancel().get();
        testFactory.createForToday(order(sortingCenter).externalId("o2").build())
                .get();
        orderJdbcCommandService.batchUpdateOrderShipmentDateAndCourier(
                BatchUpdateRequest.builder()
                        .sortingCenter(sortingCenter)
                        .externalIds(List.of("o2", "o1"))
                        .courierDto(testFactory.defaultCourier())
                        .newShipmentDate(LocalDate.now(clock))
                        .isLastBatch(true)
                        .batchRegistries(null)
                        .build(),
                user
        );
        var routeO = routeRepository.findByExpectedDateAndSortingCenterAndWarehouseTo(
                LocalDate.now(clock), sortingCenter, order1.getWarehouseReturn()
        );
        assertThat(routeO).isPresent();
        Route route = routeO.get().allowReading();
        assertThat(route.getRouteCells()).hasSize(1);
        assertThat(route.getCells(LocalDate.now(clock))).hasSize(1);
        assertThat(route.getCells(LocalDate.now(clock)).get(0).getType()).isEqualTo(CellType.RETURN);
    }

    @Test
    @DisplayName("Не создаём больше 1-ой ячейки на маршрут, если в него попадают несколько товаров")
    void ordersJdbcUpdatedEventWithDifferentWarehouseReturn() {
        OrderLike order1 = testFactory.create(order(sortingCenter).externalId("o1").build())
                .get();
        OrderLike order2 = testFactory.create(order(sortingCenter)
                        .externalId("o2")
                        .warehouseReturnId("wr2-id")
                        .warehouseReturnName("another_wh")
                        .build())
                .get();

        orderJdbcCommandService.batchUpdateOrderShipmentDateAndCourier(
                BatchUpdateRequest.builder()
                        .sortingCenter(sortingCenter)
                        .externalIds(List.of("o2", "o1"))
                        .courierDto(testFactory.defaultCourier())
                        .newShipmentDate(LocalDate.now(clock))
                        .isLastBatch(true)
                        .batchRegistries(null)
                        .build(),
                user
        );

        Optional<Route> route1 = testFactory.findOutgoingCourierRoute(order1);
        Optional<Route> route2 = testFactory.findOutgoingCourierRoute(order2);
        assertThat(route1).isPresent();
        assertThat(route2).isPresent();
        Route route1get = route1.get().allowReading();
        Route route2get = route2.get().allowReading();
        assertThat(route1get.getRouteCells()).hasSize(1);
        assertThat(route1get.getId()).isEqualTo(route2get.getId());
    }

    @Test
    void acceptOldOrder() {

        var order = testFactory.createOrderForToday(sortingCenter).get();
        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));
        Route route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                    .orElseThrow().allowReading();
        routeCommandService.cleanRoutesCell(List.of(route.getId()));
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
            RouteSo routeSo = testFactory.getRouteSo(route);
            routeSoCommandService.unbindCellFromRoutes(List.of(routeSo.getId()));
        }

        order = testFactory.accept(order);
        assertThat(order.getFfStatus()).isEqualTo(ORDER_ARRIVED_TO_SO_WAREHOUSE);


    }

    @Test
    void retryOnRouteCreation() {
        AtomicInteger retryNum = new AtomicInteger(0);
        var orderBuilder = testFactory.createOrder(sortingCenter)
                .updateShipmentDate(LocalDate.now(clock)); // create incoming route
        doAnswer(invocation -> {
            if (retryNum.incrementAndGet() < 3) {
                //noinspection ConstantConditions
                throw new ObjectOptimisticLockingFailureException("", null);
            }
            return invocation.callRealMethod();
        }).when(routeJdbcRepository).setCellId(anyLong(), anyLong(),
                any(LocalDate.class), any(LocalDate.class));
        var order = orderBuilder.updateCourier(testFactory.storedCourier()).get();
        assertThat(testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))).isPresent();
    }

    @Test
    void incomingWarehouseRouteCreatedOnOrderUpdate() {
        OrderLike order = testFactory.createOrder(sortingCenter).get();
        assertThat(routeRepository.findAll()).isEmpty();

        order = testFactory.updateForTodayDelivery(order);
        Route route = testFactory.findPossibleIncomingWarehouseRoute(order).orElseThrow();
        route.allowNextRead();
        assertThat(route.getType()).isEqualTo(RouteType.INCOMING_WAREHOUSE);
    }

    @Test
    void incomingCourierRouteCreatedOnOrderReturnFromCourier() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().get();
        Route route = testFactory.findPossibleIncomingCourierRoute(order).orElseThrow().allowReading();
        assertThat(route.getType()).isEqualTo(RouteType.INCOMING_COURIER);
    }

    @Test
    void incomingCourierRouteCreatedOnOrderReturnFromCourierUpdateCourier() {
        long newCourierUid = 321L;
        OrderLike order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn()
                .updateCourier(new CourierDto(newCourierUid, "Другой курьер", null, null, null, null, null, false))
                .get();
        assertThat(order.getCourier().getId()).isEqualTo(newCourierUid);
        Route route = testFactory.findPossibleIncomingCourierRoute(order).orElseThrow().allowReading();
        assertThat(route.getCourier().get().getId()).isEqualTo(newCourierUid);
        assertThat(route.getType()).isEqualTo(RouteType.INCOMING_COURIER);
    }

    @Test
    void incomingCourierRouteNotCreatedOnOrderReturnFromSc() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter)
                .accept().makeReturn().get();
        assertThat(testFactory.findPossibleIncomingCourierRoute(order)).isNotPresent();
    }

    @Test
    void outgoingWarehouseRouteCreatedOnOrderReturnFromCourier() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().ship().makeReturn().get();
        Route route =
                testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        assertThat(route.getType()).isEqualTo(RouteType.OUTGOING_WAREHOUSE);
    }

    @Test
    void shipIncomingWarehouseRouteOnOrderAccept() {
        OrderLike order1 = testFactory.create(order(sortingCenter).externalId("1").build()).accept().get();
        testFactory.create(order(sortingCenter).externalId("2").build()).accept().get();
        transactionTemplate.execute(ts -> {
            var route = testFactory.findPossibleIncomingWarehouseRoute(order1)
                                                                                .orElseThrow().allowReading();
            assertThat(route.getFinalRouteFinishOrders().stream()
                    .map(RouteFinishOrder::getExternalId)
                    .toList()
            ).isEqualTo(List.of("1", "2"));
            return null;
        });
    }

    @Test
    void shipIncomingCourierRouteOnOrderAccept() {
        var courier = testFactory.storedCourier();
        OrderLike order1 = testFactory.create(order(sortingCenter).externalId("1").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock)).accept().sort().ship()
                .accept().get();
        testFactory.create(order(sortingCenter).externalId("2").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock)).accept().sort().ship().accept().get();
        transactionTemplate.execute(ts -> {
            var route = testFactory.findPossibleIncomingCourierRoute(order1).orElseThrow();
            Routable routable = testFactory.getRoutable(route);


        route.allowNextRead();
            assertThat(routable.getFinalRouteFinishOrders().stream()
                    .map(RouteFinishOrder::getExternalId)
                    .toList()
            ).isEqualTo(List.of("1", "2"));
            return null;
        });
    }

    @Test
    void acceptNonLastPlaceCheckRouteFinishPlace() {
        var order = testFactory.create(order(sortingCenter).places("p1", "p2", "p3").build())
                .acceptPlaces("p1")
                .get();
        assertThat(routeFinishPlaceRepository.findAll()).hasSize(1);
        testFactory.acceptPlace(order, "p2", user);
        assertThat(routeFinishPlaceRepository.findAll()).hasSize(2);
        transactionTemplate.execute(ts -> {
            var route = testFactory.findPossibleIncomingWarehouseRoute(order).orElseThrow().allowReading();
            assertThat(route.getAllRouteFinishOrders()).hasSize(2);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.ACCEPTED)
                    .count()).isEqualTo(2);
            assertThat(route.getAllRouteFinishPlaces()).hasSize(2);
            return null;
        });
    }

    @Test
    void acceptAllPlaces() {
        var order = testFactory.create(order(sortingCenter).places("p1", "p2", "p3").build())
                .acceptPlaces("p1", "p2", "p3")
                .get();
        assertThat(routeFinishPlaceRepository.findAll()).hasSize(3);
        transactionTemplate.execute(ts -> {
            var route = testFactory.findPossibleIncomingWarehouseRoute(order).orElseThrow().allowReading();
            assertThat(route.getAllRouteFinishOrders()).hasSize(3);
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_ARRIVED_TO_SO_WAREHOUSE)).hasSize(1);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.ACCEPTED)
                    .count()).isEqualTo(3);
            assertThat(route.getAllRouteFinishPlaces()).hasSize(3);
            return null;
        });
    }

    @Test
    void acceptOnePlace() {
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("partiallyAcceptedOrder").places(List.of("partiallyAcceptedOrder-1",
                        "partiallyAcceptedOrder-2")).build()
        ).acceptPlace("partiallyAcceptedOrder-1").get();
        assertThat(routeFinishPlaceRepository.findAll()).hasSize(1);
        transactionTemplate.execute(ts -> {
            var route = testFactory.findPossibleIncomingWarehouseRoute(order).orElseThrow().allowReading();
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_ARRIVED_TO_SO_WAREHOUSE)).hasSize(0);
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_READY_TO_BE_SEND_TO_SO_FF)).hasSize(0);
            assertThat(route.getAllRouteFinishOrders()).hasSize(1);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.ACCEPTED)
                    .count()).isEqualTo(1);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.SORTED)
                    .count()).isEqualTo(0);
            assertThat(route.getAllRouteFinishPlaces()).hasSize(1);
            return null;
        });
    }

    @Test
    void sortAllPlaces() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2", "p3").get();
        transactionTemplate.execute(ts -> {
            var route = testFactory.findPossibleIncomingWarehouseRoute(order).orElseThrow().allowReading();
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_ARRIVED_TO_SO_WAREHOUSE)).hasSize(3);
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_READY_TO_BE_SEND_TO_SO_FF)).hasSize(1);
            assertThat(route.getAllRouteFinishOrders()).hasSize(6);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.ACCEPTED)
                    .count()).isEqualTo(3);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.SORTED)
                    .count()).isEqualTo(3);
            assertThat(route.getAllRouteFinishPlaces()).hasSize(6);
            return null;
        });
    }

    @Test
    void sortNonLastPlace() {

        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1", "p2").get();
        transactionTemplate.execute(ts -> {
            var route = testFactory.getRoutable(
                            testFactory.findPossibleIncomingWarehouseRoute(order).orElseThrow().allowReading());
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_ARRIVED_TO_SO_WAREHOUSE)).hasSize(3);
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_READY_TO_BE_SEND_TO_SO_FF)).hasSize(0);
            assertThat(route.getAllRouteFinishOrders()).hasSize(5);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.ACCEPTED)
                    .count()).isEqualTo(3);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.SORTED)
                    .count()).isEqualTo(2);
            assertThat(route.getAllRouteFinishPlaces()).hasSize(5);
            return null;
        });
    }

    @Test
    void sortOnlyOnePlace() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1").places("p1", "p2", "p3")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .acceptPlaces("p1", "p2", "p3").sortPlaces("p1").get();
        transactionTemplate.execute(ts -> {
            var route = testFactory.findPossibleIncomingWarehouseRoute(order).orElseThrow().allowReading();
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_ARRIVED_TO_SO_WAREHOUSE)).hasSize(2);
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_READY_TO_BE_SEND_TO_SO_FF)).hasSize(0);
            assertThat(route.getAllRouteFinishOrders()).hasSize(4);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.ACCEPTED)
                    .count()).isEqualTo(3);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.SORTED)
                    .count()).isEqualTo(1);
            assertThat(route.getAllRouteFinishPlaces()).hasSize(4);
            return null;
        });
    }

    @Test
    void acceptNonMultiPlaceOrder() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .accept().get();
        assertThat(routeFinishPlaceRepository.findAll()).hasSize(1);//у одноместных заказов тоже есть посылка
        transactionTemplate.execute(ts -> {
            var route = testFactory.findPossibleIncomingWarehouseRoute(order).orElseThrow().allowReading();
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_ARRIVED_TO_SO_WAREHOUSE)).hasSize(1);
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_READY_TO_BE_SEND_TO_SO_FF)).hasSize(0);
            assertThat(route.getAllRouteFinishOrders()).hasSize(1);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.ACCEPTED)
                    .count()).isEqualTo(1);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.SORTED)
                    .count()).isEqualTo(0);
            assertThat(route.getAllRouteFinishPlaces()).hasSize(1);
            return null;
        });
    }

    @Test
    void sortNonMultiPlaceOrder() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .accept().sort().get();
        assertThat(routeFinishPlaceRepository.findAll()).hasSize(2);//у одноместных заказов тоже есть посылка
        transactionTemplate.execute(ts -> {
            var route = testFactory.findPossibleIncomingWarehouseRoute(order).orElseThrow().allowReading();
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_ARRIVED_TO_SO_WAREHOUSE)).hasSize(1);
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == ORDER_READY_TO_BE_SEND_TO_SO_FF)).hasSize(1);
            assertThat(route.getAllRouteFinishOrders()).hasSize(2);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.ACCEPTED)
                    .count()).isEqualTo(1);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.SORTED)
                    .count()).isEqualTo(1);
            assertThat(route.getAllRouteFinishPlaces()).hasSize(2);
            return null;
        });
    }

    @Test
    void acceptReturnFlowNonMultiPlaceOrder() {
        var order = testFactory
                .create(order(sortingCenter).externalId("1")
                        .deliveryDate(LocalDate.now(clock))
                        .shipmentDate(LocalDate.now(clock))
                        .dsType(DeliveryServiceType.TRANSIT)
                        .build())
                .accept()
                .sort()
                .ship()
                .makeReturn()
                .accept()
                .get();
        assertThat(routeFinishPlaceRepository.findAll()).hasSize(4);//у одноместных заказов тоже есть посылка
        transactionTemplate.execute(ts -> {
            var route = testFactory.findPossibleIncomingCourierRoute(order).orElseThrow().allowReading();
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == RETURNED_ORDER_AT_SO_WAREHOUSE)).hasSize(1);
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getStatus() == RETURNED_ORDER_READY_TO_BE_SENT_TO_IM)).hasSize(0);
            assertThat(route.getAllRouteFinishOrders()).hasSize(1);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.ACCEPTED)
                    .count()).isEqualTo(1);
            assertThat(route.getAllRouteFinishPlaces().stream()
                    .filter(item -> item.getFinishedPlaceStatus() == PlaceStatus.SORTED)
                    .count()).isEqualTo(0);
            assertThat(route.getAllRouteFinishPlaces()).hasSize(1);
            return null;
        });
    }

    @Test
    void finishOrderSortedTest() {
        var courier = testFactory.storedCourier();
        OrderLike order1 = testFactory.create(order(sortingCenter).externalId("1").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock)).accept().sort().get();
        testFactory.create(order(sortingCenter).externalId("2").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock)).accept().sort().get();
        testFactory.create(order(sortingCenter).externalId("3").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock)).accept().get();
        transactionTemplate.execute(ts -> {
            var route = testFactory.findPossibleIncomingWarehouseRoute(order1)
                                                                    .orElseThrow().allowReading();
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getFinishedOrderFfStatus() == ORDER_READY_TO_BE_SEND_TO_SO_FF)
                    .count()).isEqualTo(2);
            assertThat(route.getAllRouteFinishOrders().stream()
                    .filter(item -> item.getFinishedOrderFfStatus() == ORDER_ARRIVED_TO_SO_WAREHOUSE)
                    .count()).isEqualTo(3);
            return null;
        });
    }

    @Test
    void finishOrderShippedAndReturnTest() {
        var courier = testFactory.storedCourier();
        var orderBuilder = testFactory.create(
                        order(sortingCenter).externalId("1").places("11", "12").build()
                )
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock))
                .acceptPlaces().sortPlaces();
        var route = testFactory.findOutgoingCourierRoute(orderBuilder.get()).orElseThrow();
        orderBuilder.ship().makeReturn().acceptPlace("11").sortPlaces("11");
        transactionTemplate.execute(ts -> {
            Routable outgoingRoute = testFactory.getRoutable(route);
            assertThat(outgoingRoute.getAllRouteFinishPlaces().size()).isEqualTo(2);
            return null;
        });
    }

    @Test
    void outgoingWarehouseRouteCreatedOnOrderCancel() {
        OrderLike order = testFactory.createOrder(sortingCenter).cancel().get();
        Route route = testFactory.findPossibleRouteForCancelledOrder(order).orElse(null);
        // Теперь не создаем маршрут для отмененного заказа
        // https://st.yandex-team.ru/MARKETTPLSC-4346#62ab18390b9d7a0aae8032b0
        assertThat(route).isNull();
    }

    @Test
    void outgoingWarehouseRouteCreatedOnOrderReturnFromSc() {
        OrderLike order = testFactory.createOrderForToday(sortingCenter)
                .accept().makeReturn().get();
        Route route =
                testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        assertThat(route.getType()).isEqualTo(RouteType.OUTGOING_WAREHOUSE);
    }

    @Test
    void incomingWarehouseRouteCreatedOnOrderCreate() {
        OrderLike order = testFactory.createOrder(sortingCenter).updateShipmentDate(LocalDate.now(clock)).get();
        Route route = testFactory.findPossibleIncomingWarehouseRoute(order).orElseThrow().allowReading();
        assertThat(route.getType()).isEqualTo(RouteType.INCOMING_WAREHOUSE);
    }

    @Test
    void outgoingCourierRouteCreatedOnOrderUpdate() {
        OrderLike order = testFactory.createOrder(sortingCenter).get();
        assertThat(routeRepository.findAll()).isEmpty();

        order = testFactory.updateForTodayDelivery(order);
        Route route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                    .orElseThrow().allowReading();
        assertThat(route.getType()).isEqualTo(RouteType.OUTGOING_COURIER);
    }

    @Test
    void startedAtChangedOnOrderSort() {
        ScOrder order = testFactory.createOrderForToday(sortingCenter).accept().get();
        Route route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                        .orElseThrow().allowReading();
        assertThat(route.getSortingStartedAt()).isNull();
        var cell = testFactory.determineRouteCell(route, order);
        PlaceScRequest request = testFactory.placeScRequest(order, user);
        placeCommandService.sortPlace(request, Objects.requireNonNull(cell).getId(), false);
        route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow().allowReading();
        assertThat(route.getSortingStartedAt()).isEqualTo(clock.instant());
    }

}
