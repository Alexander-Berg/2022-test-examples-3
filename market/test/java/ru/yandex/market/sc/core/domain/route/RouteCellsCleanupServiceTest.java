package ru.yandex.market.sc.core.domain.route;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route_so.RouteSoCellsCleanupService;
import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.sortWithRouteSo;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RouteCellsCleanupServiceTest {

    private final RouteCellsCleanupService routeCellsCleanupService;
    private final RouteSoCellsCleanupService routeSoCellsCleanupService;
    private final TestFactory testFactory;
    private final RouteNonBlockingQueryService routeNonBlockingQueryService;
    private final RouteSoMigrationHelper routeSoMigrationHelper;
    @MockBean
    Clock clock;

    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setupMockClock(clock);
    }

    @Test
    void removeCellForShippedRoute() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        testFactory.shipOrderRoute(order);
        testFactory.setupMockClock(clock, clock.instant().plus(31, ChronoUnit.MINUTES));
        RouteSoMigrationHelper.allowRouteReading();
        routeCellsCleanupService.removeCellsFromShippedOutgoingRoutes(sortingCenter);
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellsCleanupService.removeCellsFromShippedOutgoingRoutes(sortingCenter);

        assertCellsRemoved(route);
    }

    @Test
    void notRemoveBecauseOrderInCell() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow().allowReading();
        testFactory.shipOrderRoute(order);

        Cell cell = route.getCells(LocalDate.now(clock)).get(0);
        OrderLike order2 = testFactory.createForToday(order(sortingCenter, "o2").build())
                .accept().sort(cell.getId()).get();
        testFactory.setupMockClock(clock, clock.instant().plus(31, ChronoUnit.MINUTES));

        //todo: тестируется только легаси
        routeCellsCleanupService.removeCellsFromShippedOutgoingRoutes(sortingCenter);

        assertCellsNotRemoved(route);
    }

    @Test
    void notRemoveBecause30minutesNotPassed() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        testFactory.shipOrderRoute(order);
        RouteSoMigrationHelper.allowRouteReading();
        routeCellsCleanupService.removeCellsFromShippedOutgoingRoutes(sortingCenter);
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellsCleanupService.removeCellsFromShippedOutgoingRoutes(sortingCenter);

        assertCellsNotRemoved(route);
    }

    @Test
    void notRemoveBecauseNotShipped() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow().allowReading();
        testFactory.setupMockClock(clock, clock.instant().plus(31, ChronoUnit.MINUTES));

        routeCellsCleanupService.removeCellsFromShippedOutgoingRoutes(sortingCenter);

        assertCellsNotRemoved(route);
    }


    private void assertCellsRemoved(Route route) {

        var actualRoute = testFactory.getRoutable(route);
        assertThat(routeSoMigrationHelper.getCells(actualRoute, Instant.now(clock))).isEmpty();
        assertThat(actualRoute.getCellDistributionDisabledAt()).isNotNull();
    }

    private void assertCellsNotRemoved(Route route) {
        var actualRoute = testFactory.getRoutable(route);
        if (!sortWithRouteSo()) {
            actualRoute.route().allowReading();
        }
        assertThat(routeSoMigrationHelper.getCells(actualRoute, Instant.now(clock))).isNotEmpty();
        if (!sortWithRouteSo()) {
            actualRoute.route().revokeRouteReading();
        }

    }
}
