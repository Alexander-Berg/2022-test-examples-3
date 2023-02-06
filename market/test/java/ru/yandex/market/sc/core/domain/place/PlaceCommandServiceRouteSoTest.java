package ru.yandex.market.sc.core.domain.place;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServicePropertySource;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType;
import ru.yandex.market.sc.core.domain.route_so.model.RouteType;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
public class PlaceCommandServiceRouteSoTest {

    @Autowired
    TestFactory testFactory;

    @Autowired
    RouteSoRepository routeSoRepository;

    @Autowired
    OrderCommandService orderCommandService;

    @Autowired
    FfApiPlaceService ffApiPlaceService;

    @Autowired
    DeliveryServicePropertySource deliveryServicePropertySource;


    // Вспомогательные поля

    SortingCenter sortingCenter;
    User user;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.UPDATE_SORTABLE_ROUTES_IN_PLACE, "true");
        user = testFactory.storedUser(sortingCenter, 1L);
    }


    @Nested
    @DisplayName("Создание коробок")
    class PlaceCreatedTestCase {

        @Test
        @DisplayName("Входящий маршрут создается когда создается заказ и коробка в createOrder. " +
                "Исходящий маршрут создается при обновлении курьера на defaultCourier в createForToday")
        void incomingRouteSoCreatedWhenPlaceCreatedForToday() {
            ScOrder scOrder = testFactory.createForToday(order(sortingCenter).places("p1").build()).get();
            Place place = testFactory.orderPlace(scOrder);

            checkRoute(
                    place.getInRoute(),
                    RouteType.IN_DIRECT,
                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
            );

            checkRoute(place.getOutRoute(), RouteType.OUT_DIRECT,
                    RouteDestinationType.COURIER, Objects.requireNonNull(testFactory.defaultCourier().getId()));

        }

    }

    @Nested
    @DisplayName("Приемка коробкок в различных статусах")
    class PlaceAcceptTestCase {

        @Test
        @DisplayName("Входящий маршрут не должен остаться при приемке коробки. " +
                "Не создаем исходящий маршрут, так как нет курьера.")
        void incomingRouteSoCreatedWhenPlaceAccepted() {
            ScOrder scOrder = testFactory.createOrder(order(sortingCenter).places("p1").build())
                    .acceptPlace("p1")
                    .get();
            Place place = testFactory.orderPlace(scOrder);

            checkRouteIsNull(place.getInRoute());
//            checkRoute(
//                    place.getInRoute(),
//                    RouteType.IN_DIRECT,
//                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
//            );

            RouteSo outRoute = place.getOutRoute();
            assertThat(outRoute).isNull(); //нет курьера - нет маршрута
        }

        @Test
        @DisplayName("Создание исходящего курьерского маршрута склада при приемке коробки. " +
                "Когда storeOnDirectStream = false и isSortInAdvanceEnabled = false")
        void whenPlaceAcceptedAndStoreOnDirStreamFalseAndSortInAdvFalse() {
            // isSortInAdvanceEnabled = false, посокльку не включено свойство
            // storeOnDirectStream = false, поскольку last mile = true

            ScOrder scOrder = testFactory.createForToday(
                            order(sortingCenter)
                                    .places("p1")
                                    // storeOnDirectStream = false, поскольку transit
                                    .dsType(DeliveryServiceType.TRANSIT)
                                    .build()
                    )
                    .acceptPlace("p1")
                    .get();

            Place place = testFactory.orderPlace(scOrder);

            checkRouteIsNull(place.getInRoute());
//            checkRoute(
//                    place.getInRoute(),
//                    RouteType.IN_DIRECT,
//                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
//            );


            checkRoute(place.getOutRoute(), RouteType.OUT_DIRECT, RouteDestinationType.COURIER, Objects.requireNonNull(place.getCourier().getId())
            );
        }

        @Test
        @DisplayName("Создание исходящего курьерского маршрута при приемке коробки. " +
                "Входящий должен обнулится при приемке" +
                "Когда middle mile = true и isSortInAdvanceEnabled = false")
        void whenPlaceAcceptedAndStoreOnDirStreamFalseAndSortInAdvFalse2() {
            // isSortInAdvanceEnabled = false, поскольку не включено свойство

            // IS_DROPOFF - это второй способ проставить middle mile = true кроме DS type = TRANSIT
            testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);
            ScOrder scOrder = testFactory.createForToday(
                            order(sortingCenter)
                                    .places("p1")
                                    .build()
                    )
                    .acceptPlace("p1")
                    .get();

            Place place = testFactory.orderPlace(scOrder);

            checkRouteIsNull(place.getInRoute());
//            checkRoute(
//                    place.getInRoute(),
//                    RouteType.IN_DIRECT,
//                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
//            );

            checkRoute(
                    place.getOutRoute(),
                    RouteType.OUT_DIRECT,
                    RouteDestinationType.COURIER, Objects.requireNonNull(place.getCourier().getId())
            );
        }

        @Disabled("temp") // TODO: enablel
        @Test
        @DisplayName("Должны оставаться входящие и исходящие маршруты при приемке после отмены. " +
                "Входящий должен обнулится при приемке")
        void routePersistedAfterCancelledPlaceAccept() {
            ScOrder scOrder = testFactory.createForToday(
                            order(sortingCenter)
                                    .places("p1")
                                    .build()
                    )
                    .cancel()
//                    .acceptPlaces("p1")
                    .get();

            Place place = testFactory.orderPlace(scOrder);

            checkRoute(
                    place.getInRoute(),
                    RouteType.IN_DIRECT,
                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
            );

            checkRoute(
                    place.getOutRoute(),
                    RouteType.OUT_RETURN,
                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseReturn()).getId()
            );

        }

        @Test
        @DisplayName("Должны оставаться входящие и исходящие маршруты при приемке возврата. " +
                "Входящий должен обнулится при приемке")
        void routePersistedAfterReturnedOrderAccept() {
            ScOrder scOrder = testFactory.createForToday(
                            order(sortingCenter)
                                    .places("p1")
                                    .build()
                    )
                    .acceptPlaces("p1")
                    .sortPlaces("p1")
                    .shipPlaces("p1")
                    .cancel()
                    .acceptPlaces("p1")
                    .get();

            Place place = testFactory.orderPlace(scOrder);

            checkRouteIsNull(place.getInRoute());
//            checkRoute(
//                    place.getInRoute(),
//                    RouteType.IN_RETURN,
//                    RouteDestinationType.COURIER, Objects.requireNonNull(scOrder.getCourier()).getId()
//            );


            checkRoute(
                    place.getOutRoute(),
                    RouteType.OUT_RETURN,
                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseReturn()).getId()
            );

        }

    }

    @Nested
    @DisplayName("Тесты разных состояний коробок без курьера")
    class PlaceSimpleSortableStatesWithoutCoruierTestCase {

        @Test
        @DisplayName("Входящий маршрут создается когда создается заказ и коробка в createOrder. " +
                "Потом входящий обнуляется. " +
                "Исходящий маршрут не создается, так как не переда курьер. ")
        void incomingRouteSoCreatedWhenPlaceCreated() {
            ScOrder scOrder = testFactory.createOrder(order(sortingCenter).places("p1").build()).get();
            Place place = testFactory.orderPlace(scOrder);

            checkRouteIsNull(place.getInRoute());
//            checkRoute(
//                    place.getInRoute(),
//                    RouteType.IN_DIRECT,
//                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
//            );

            RouteSo outRoute = place.getOutRoute();
            assertThat(outRoute).isNull(); //нет курьера - нет маршрута
        }

        @Test
        @DisplayName("Входящий маршрут создается когда создается заказ и коробка в createOrder. " +
                "Потом входящий обнуляется. " +
                "Исходящий маршрут не создается, так как не переда курьер. ")
        void incomingRouteSoCreatedWhenPlaceAccepted() {
            ScOrder scOrder = testFactory.createOrder(order(sortingCenter).places("p1").build())
                    .accept()
                    .get();

            Place place = testFactory.orderPlace(scOrder);

            checkRouteIsNull(place.getInRoute());
//            checkRoute(
//                    place.getInRoute(),
//                    RouteType.IN_DIRECT,
//                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
//            );

            RouteSo outRoute = place.getOutRoute();
            assertThat(outRoute).isNull(); //нет курьера - нет маршрута

        }

        @Test
        @DisplayName("Входящий маршрут создается когда создается заказ и коробка в createOrder. " +
                "Исходящий маршрут не создается, так как не переда курьер. " +
                "Входящий обнуляется при приемке")
        void incomingRouteSoCreatedWhenPlaceKeeped() {
            ScOrder scOrder = testFactory.createOrder(order(sortingCenter).places("p1").build())
                    .acceptPlaces()
                    .keepPlaces()
                    .get();


            Place place = testFactory.orderPlace(scOrder);

//            checkRoute(
//                    place.getInRoute(),
//                    RouteType.IN_DIRECT,
//                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
//            );

            RouteSo outRoute = place.getOutRoute();
            assertThat(outRoute).isNull(); //нет курьера - нет маршрута

        }


        @Disabled("temp") // TODO: enable
        @Test
        @DisplayName("Должны оставаться входящие и исходящие маршруты при отмене, если когда-то был назначен курьер")
        void routePersistedAfterCancel() {
            ScOrder scOrder = testFactory.createForToday(
                            order(sortingCenter)
                                    .places("p1")
                                    .build()
                    )
                    .cancel()
                    .get();

            Place place = testFactory.orderPlace(scOrder);

            checkRoute(
                    place.getInRoute(),
                    RouteType.IN_DIRECT,
                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
            );

            checkRoute(
                    place.getOutRoute(),
                    RouteType.OUT_RETURN,
                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseReturn()).getId()
            );

        }

    }

    @Nested
    @DisplayName("Тесты разных состояний коробок с курьером")
    class PlaceSimpleSortableStatesWithCourierTestCase {

        @Test
        @DisplayName("Входящий маршрут создается когда создается заказ и коробка в createOrder. " +
                "Исходящий маршрут не создается, так как не передан курьер. ")
        void incomingRouteSoCreatedWhenPlaceCreated() {
            ScOrder scOrder = testFactory.createForToday(order(sortingCenter).places("p1").build()).get();
            Place place = testFactory.orderPlace(scOrder);

            checkRoute(
                    place.getInRoute(),
                    RouteType.IN_DIRECT,
                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
            );

            checkRoute(
                    place.getOutRoute(),
                    RouteType.OUT_DIRECT,
                    RouteDestinationType.COURIER, Objects.requireNonNull(scOrder.getCourier()).getId()
            );
        }

        @Test
        @DisplayName("Входящий маршрут создается когда создается заказ и коробка в createOrder. " +
                "Исходящий маршрут не создается, так как не переда курьер. " +
                "Входящий маршрут зануляется при приемке. ")
        void incomingRouteSoCreatedWhenPlaceAccepted() {
            ScOrder scOrder = testFactory.createForToday(order(sortingCenter).places("p1").build())
                    .accept()
                    .get();

            Place place = testFactory.orderPlace(scOrder);

            checkRouteIsNull(place.getInRoute());
//            checkRoute(
//                    place.getInRoute(),
//                    RouteType.IN_DIRECT,
//                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
//            );

            checkRoute(
                    place.getOutRoute(),
                    RouteType.OUT_DIRECT,
                    RouteDestinationType.COURIER,
                    Objects.requireNonNull(scOrder.getCourier()).getId()
            );
        }

        @Test
        @DisplayName("Входящий маршрут создается когда создается заказ и коробка в createOrder. " +
                "Исходящий маршрут не создается, так как не переда курьер. " +
                "Входящий маршрут зануляется при приемке. ")
        void incomingRouteSoCreatedWhenPlaceKeeped() {
            ScOrder scOrder = testFactory.createForToday(order(sortingCenter).places("p1").build())
                    .acceptPlaces()
                    .keepPlaces()
                    .get();


            Place place = testFactory.orderPlace(scOrder);

            checkRouteIsNull(place.getInRoute());
//            checkRoute(
//                    place.getInRoute(),
//                    RouteType.IN_DIRECT,
//                    RouteDestinationType.WAREHOUSE,
//                    Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
//            );

            checkRoute(
                    place.getOutRoute(),
                    RouteType.OUT_DIRECT,
                    RouteDestinationType.COURIER,
                    Objects.requireNonNull(scOrder.getCourier()).getId()
            );

        }


        @Disabled("temp") // TODO: enable
        @Test
        @DisplayName("Должны оставаться входящие и исходящие маршруты при отмене, если когда-то был назначен курьер")
        void routePersistedAfterCancel() {
            ScOrder scOrder = testFactory.createForToday(
                            order(sortingCenter)
                                    .places("p1")
                                    .build()
                    )
                    .cancel()
                    .get();

            Place place = testFactory.orderPlace(scOrder);

            checkRoute(
                    place.getInRoute(),
                    RouteType.IN_DIRECT,
                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseFrom()).getId()
            );

            checkRoute(
                    place.getOutRoute(),
                    RouteType.OUT_RETURN,
                    RouteDestinationType.WAREHOUSE, Objects.requireNonNull(scOrder.getWarehouseReturn()).getId()
            );


        }


    }

    private void checkRoute(
            RouteSo route, RouteType routeType, RouteDestinationType destinationType, Long destinationId
    ) {
        assertThat(route).isNotNull();
        assertThat(route.getType()).isEqualTo(routeType);
        assertThat(route.getDestinationId()).isEqualTo(destinationId);
        assertThat(route.getDestinationType()).isEqualTo(destinationType);
    }


    private void checkRouteIsNull(RouteSo route) {
        assertThat(route).isNull();
    }

}
