//package ru.yandex.market.sc.core.dbqueue.finish_route;
//
//import java.time.Clock;
//import java.util.Optional;
//import java.util.Set;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.Import;
//
////import ru.yandex.market.sc.core.ScTmsTestConfig;
//import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
//import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
//import ru.yandex.market.sc.core.domain.route.model.RouteDocumentTypeResolver;
//import ru.yandex.market.sc.core.domain.route.repository.Route;
//import ru.yandex.market.sc.core.domain.route.repository.RouteFinish;
//import ru.yandex.market.sc.core.domain.route.repository.RouteFinishOrder;
//import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
//import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
//import ru.yandex.market.sc.core.domain.user.repository.User;
//import ru.yandex.market.sc.core.test.EmbeddedDbTest;
//import ru.yandex.market.sc.core.test.TestFactory;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@EmbeddedDbTest
////@Import(ScTmsTestConfig.class)
//class FinishRouteServiceTest {
//
//    private static final long UID = 123L;
//
//
//    @Autowired
//    private TestFactory testFactory;
//
//    @Autowired
//    protected FinishRouteService finishRouteService;
//    @MockBean
//    Clock clock;
//
//    SortingCenter sortingCenter;
//    User user;
//
//    @BeforeEach
//    void init() {
//        sortingCenter = testFactory.storedSortingCenter(12);
//        user = testFactory.storedUser(sortingCenter, UID);
//        testFactory.setupMockClock(clock);
//    }
//
//    @Test
//    @Disabled
//    void successSaveFinishRoute() {
//        testFactory.setConfiguration("ENABLE_ASYNC_CREATE_FINISH_INCOMING_ROUTE", true);
//        var scOrderWithPlaces = testFactory.createOrderForToday(sortingCenter)
//                .accept().getOrderWithPlaces();
//        var scOrder = scOrderWithPlaces.order();
//        var place = scOrderWithPlaces.place(scOrder.getExternalId());
//        Optional<Route> route = testFactory.findPossibleIncomingWarehouseRoute(scOrder);
//        assertThat(route).isNotEmpty();
//        assertThat(route.get().getRouteFinishes()).isEmpty();
//
//        var orderPayload = new FinishRoutePayload.Order(
//                scOrder.getId(),
//                scOrder.getExternalId(),
//                ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE,
//                null,
//                RouteDocumentTypeResolver.resolve(scOrder)
//        );
//
//        var placePayload = new FinishRoutePayload.Place(
//                place.getId(),
//                place.getOrderId(),
//                place.getMainPartnerCode(),
//                PlaceStatus.ACCEPTED,
//                SortableStatus.ARRIVED_DIRECT,
//                null,
//                null,
//                null,
//                null
//        );
//
//        Long routeId = route.orElseThrow().getId();
//        finishRouteService.processPayload(
//                new FinishRoutePayload(
//                        "", sortingCenter.getId(), user.getUid(),
//                        orderPayload,
//                        placePayload,
//                        routeId,
//                        null,
//                        null,
//                        null, true)
//        );
//
//        Optional<Route> possibleIncomingWarehouseRoute = testFactory.findPossibleIncomingWarehouseRoute(scOrder);
//        assertThat(possibleIncomingWarehouseRoute).isNotEmpty();
//        Set<RouteFinish> routeFinishes = possibleIncomingWarehouseRoute.get().getRouteFinishes();
//        assertThat(routeFinishes)
//                .isNotEmpty()
//                .size().isEqualTo(1);
//        RouteFinish routeFinish = routeFinishes.stream().iterator().next();
//        Set<RouteFinishOrder> routeFinishOrders = routeFinish.getRouteFinishOrders();
//        assertThat(routeFinishOrders)
//                .isNotEmpty()
//                .size().isEqualTo(1);
//        RouteFinishOrder routeFinishOrder = routeFinishOrders.iterator().next();
//        assertThat(routeFinishOrder.getStatus())
//                .isEqualTo(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE);
//    }
//
//    @Test
//    @Disabled
//    void errorSaveFinishRoute() {
//        testFactory.setConfiguration("ENABLE_ASYNC_CREATE_FINISH_INCOMING_ROUTE", true);
//        var scOrderWithPlaces = testFactory.createOrderForToday(sortingCenter)
//                .accept().getOrderWithPlaces();
//        var scOrder = scOrderWithPlaces.order();
//        var place = scOrderWithPlaces.place(scOrder.getExternalId());
//
//        Optional<Route> route = testFactory.findPossibleIncomingWarehouseRoute(scOrder);
//        assertThat(route).isNotEmpty();
//        assertThat(route.get().getRouteFinishes()).isEmpty();
//
//        var orderPayload = new FinishRoutePayload.Order(
//                scOrder.getId(),
//                scOrder.getExternalId(),
//                ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE,
//                null,
//                RouteDocumentTypeResolver.resolve(scOrder)
//        );
//
//        var placePayload = new FinishRoutePayload.Place(
//                place.getId(),
//                place.getOrderId(),
//                place.getMainPartnerCode(),
//                PlaceStatus.ACCEPTED,
//                SortableStatus.ARRIVED_DIRECT,
//                null,
//                null,
//                null,
//                null
//        );
//
//        Long routeId = route.orElseThrow().getId();
//        var ex = Assertions.assertThrows(IllegalArgumentException.class, () ->
//                finishRouteService.processPayload(
//                        new FinishRoutePayload(
//                                "", sortingCenter.getId(), -1L,
//                                orderPayload,
//                                placePayload,
//                                routeId,
//                                null,
//                                null,
//                                null, true)
//                ));
//        assertThat(ex.getMessage()).isEqualTo("User not found!");
//
//        Optional<Route> b = testFactory.findPossibleIncomingWarehouseRoute(scOrder);
//        assertThat(b).isNotEmpty();
//        Set<RouteFinish> routeFinishes = b.get().getRouteFinishes();
//        assertThat(routeFinishes).isEmpty();
//    }
//
//}
