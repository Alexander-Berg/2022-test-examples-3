package ru.yandex.market.sc.core.domain.route_so.repository;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType;
import ru.yandex.market.sc.core.domain.route_so.model.RouteType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RouteSoRepositoryTest {

    private final RouteSoRepository routeSoRepository;
    private final TestFactory testFactory;
    private final Clock clock;

    SortingCenter sortingCenter;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @ParameterizedTest(name = "Поиск для актуальных нынче маршрутов с типом {0}")
    @EnumSource(value = RouteType.class)
    void canFindRouteIfItIsActual(RouteType routeType) {
        // Подготовка
        var courier = testFactory.storedCourier();
        Long destinationId = courier.getId(); // На самом деле можно писать что угодно
        Instant now = Instant.now();
        RouteDestinationType routeDestinationType = RouteDestinationType.COURIER;
        String carNumber = "2128506";

        RouteSo route = new RouteSo(sortingCenter, routeType, destinationId,
                routeDestinationType, // легаси поле, не используется в данном запросе
                now.minus(1, ChronoUnit.MILLIS),
                now.plus(1, ChronoUnit.MILLIS),
                carNumber);
        routeSoRepository.save(route);

        // Действие
        var storedRoutes = routeSoRepository
                .findBySortingCenterAndTypeAndDestinationIdAndIntervalFromLessThanAndIntervalToGreaterThan(
                        sortingCenter, routeType, destinationId, now, now
                );

        // Проверка
        assertThat(storedRoutes).isNotEmpty();
        var storedRoute = storedRoutes.stream().findAny().orElseThrow();


        assertThat(storedRoute.getCarNumber()).isEqualTo(carNumber);
        assertThat(storedRoute.getType()).isEqualTo(routeType);
        assertThat(storedRoute.getDestinationType()).isEqualTo(routeDestinationType);
        assertThat(storedRoute.getDestinationId()).isEqualTo(destinationId);
    }

    @ParameterizedTest(name = "Старые маршруты с типом {0} не ищутся")
    @EnumSource(value = RouteType.class)
    void canFindRouteIfItIsOld(RouteType routeType) {
        // Подготовка
        var courier = testFactory.storedCourier();
        Long destinationId = courier.getId(); // На самом деле можно писать что угодно
        Instant now = Instant.now();
        RouteDestinationType routeDestinationType = RouteDestinationType.COURIER;
        String carNumber = "2128506";

        RouteSo route = new RouteSo(sortingCenter, routeType, destinationId,
                routeDestinationType, // легаси поле, не используется в данном запросе
                now.minus(1, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.MILLIS),
                carNumber);
        routeSoRepository.save(route);

        // Действие
        var storedRoutes = routeSoRepository
                .findBySortingCenterAndTypeAndDestinationIdAndIntervalFromLessThanAndIntervalToGreaterThan(
                        sortingCenter, routeType, destinationId, now, now
                );

        // Проверка
        assertThat(storedRoutes).isEmpty();
    }

    @ParameterizedTest(name = "Маршруты в будущем с типом {0} не ищутся")
    @EnumSource(value = RouteType.class)
    void canFindRouteIfItIsFuturistic(RouteType routeType) {
        // Подготовка
        var courier = testFactory.storedCourier();
        Long destinationId = courier.getId(); // На самом деле можно писать что угодно
        Instant now = Instant.now();
        RouteDestinationType routeDestinationType = RouteDestinationType.COURIER;
        String carNumber = "2128506";

        RouteSo route = new RouteSo(sortingCenter, routeType, destinationId,
                routeDestinationType, // легаси поле, не используется в данном запросе
                now.plus(1, ChronoUnit.MILLIS),
                now.plus(1, ChronoUnit.DAYS),
                carNumber);
        routeSoRepository.save(route);

        // Действие
        var storedRoutes = routeSoRepository
                .findBySortingCenterAndTypeAndDestinationIdAndIntervalFromLessThanAndIntervalToGreaterThan(
                        sortingCenter, routeType, destinationId, now, now
                );

        // Проверка
        assertThat(storedRoutes).isEmpty();
    }

    @Test
    void saveRouteCourier() {
        var courier = testFactory.storedCourier();
        RouteSo route = new RouteSo(sortingCenter, RouteType.IN_RETURN, courier, null, null,
                Instant.now(clock).minus(2, ChronoUnit.HOURS),
                Instant.now(clock).plus(2, ChronoUnit.HOURS), "О868АС198");
        var storedRoute = routeSoRepository.save(route);
        assertThat(storedRoute.getDestinationType()).isEqualTo(RouteDestinationType.COURIER);
        assertThat(storedRoute.getDestinationId()).isEqualTo(courier.getId());
    }

    @Test
    void saveRouteWarehouse() {
        var warehouse = testFactory.storedWarehouse();
        RouteSo route = new RouteSo(sortingCenter, RouteType.OUT_RETURN, warehouse, null, null,
                Instant.now(clock).minus(2, ChronoUnit.HOURS),
                Instant.now(clock).plus(2, ChronoUnit.HOURS), "О868АС198");
        var storedRoute = routeSoRepository.save(route);
        assertThat(storedRoute.getDestinationType()).isEqualTo(RouteDestinationType.WAREHOUSE);
        assertThat(storedRoute.getDestinationId()).isEqualTo(warehouse.getId());
    }

    @Test
    void saveRouteDeliveryService() {
        var deliveryService = testFactory.storedDeliveryService();
        RouteSo route = new RouteSo(sortingCenter, RouteType.OUT_DIRECT, deliveryService,
                null, null, Instant.now(clock).minus(2, ChronoUnit.HOURS),
                Instant.now(clock).plus(2, ChronoUnit.HOURS), null);
        var storedRoute = routeSoRepository.save(route);
        assertThat(storedRoute.getDestinationType()).isEqualTo(RouteDestinationType.DELIVERY_SERVICE);
        assertThat(storedRoute.getDestinationId()).isEqualTo(deliveryService.getId());
    }

    @Test
    void saveSortingCenter() {
        var destinationSc = testFactory.storedSortingCenter(10L);
        RouteSo route = new RouteSo(sortingCenter, RouteType.OUT_DIRECT,
                destinationSc, null, null,
                Instant.now(clock).minus(2, ChronoUnit.HOURS),
                Instant.now(clock).plus(2, ChronoUnit.HOURS), "О868АС198");
        var storedRoute = routeSoRepository.save(route);
        assertThat(storedRoute.getDestinationType()).isEqualTo(RouteDestinationType.SORTING_CENTER);
        assertThat(storedRoute.getDestinationId()).isEqualTo(destinationSc.getId());
    }
}
