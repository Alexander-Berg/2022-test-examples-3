package ru.yandex.market.sc.core.domain.order;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author mors741
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MissingRoutesServiceTest {
    private final MissingRoutesService missingRoutesService;
    private final TestFactory testFactory;
    private final RouteRepository routeRepository;
    @MockBean
    Clock clock;

    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setupMockClock(clock);
    }

    @Test
    void createMissingCourierRoute() {
        OrderLike order = testFactory.createForToday(order(sortingCenter).externalId("o1").build()).get();
        routeRepository.deleteAll();
        assertThat(testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))).isEmpty();

        missingRoutesService.createMissingCourierRoutes(LocalDate.now(clock), sortingCenter);
        assertThat(testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))).isPresent();
    }

    @Test
    void createMissingCourierRouteUseNotDeletedCells() {
        OrderLike order = testFactory.createForToday(order(sortingCenter).externalId("o1").build()).get();
        var cell = testFactory.storedCell(order.getSortingCenter(), "c1", CellType.COURIER,
                order.getCourierId().orElseThrow());
        routeRepository.deleteAll();
        assertThat(testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))).isEmpty();

        missingRoutesService.createMissingCourierRoutes(LocalDate.now(clock), sortingCenter);
        var newRoute = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order));
        assertThat(newRoute).isPresent();
        assertThat(newRoute.get().getRouteCells().stream().map(RouteCell::getCell).toList())
                .containsOnly(cell);
    }

    @Test
    void createMissingCourierRouteDontUseDeletedCells() {
        OrderLike order = testFactory.createForToday(order(sortingCenter).externalId("o1").build()).get();
        var cell = testFactory.storedCell(order.getSortingCenter(), "c1", CellType.COURIER, order.getCourierId().orElseThrow());
        RouteSoMigrationHelper.allowRouteReading();
        testFactory.deleteCellForce(cell);
        routeRepository.deleteAll();
        RouteSoMigrationHelper.revokeRouteReadingPermission();
        assertThat(testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))).isEmpty();

        missingRoutesService.createMissingCourierRoutes(LocalDate.now(clock), sortingCenter);
        var newRoute = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order));
        assertThat(newRoute).isPresent();
        assertThat(newRoute.get().allowReading().getRouteCells().stream().map(RouteCell::getCell).toList())
                .isEmpty();
    }

    @Test
    void createMissingCourierRouteDontUseCellFromOtherSc() {
        OrderLike order = testFactory.createForToday(order(sortingCenter).externalId("o1").build()).get();
        var newSc = testFactory.storedSortingCenter(100500L);
        assertThat(newSc).isNotEqualTo(order.getSortingCenter());
        testFactory.storedCell(newSc, "c1", CellType.COURIER, order.getCourierId().orElseThrow());
        routeRepository.deleteAll();
        assertThat(testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))).isEmpty();

        missingRoutesService.createMissingCourierRoutes(LocalDate.now(clock), sortingCenter);
        var newRoute = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order));
        assertThat(newRoute).isPresent();
        assertThat(newRoute.get().getRouteCells().stream().map(RouteCell::getCell).toList())
                .isEmpty();
    }

    @Test
    void createMissingWarehouseRoute() {
        OrderLike order = testFactory.createForToday(order(sortingCenter).externalId("o1").build()).accept().cancel().get();

        var outgoingRoute = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        //todo: тестируется только старый флоу
        routeRepository.delete(outgoingRoute);
        assertThat(testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))).isEmpty();

        missingRoutesService.createMissingWarehouseRoutes(LocalDate.now(clock), sortingCenter);
        assertThat(testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))).isPresent();
    }

    @Test
    void createMissingWarehouseRouteUseNotDeletedCells() {
        OrderLike order = testFactory.createForToday(order(sortingCenter).externalId("o1").build()).accept().cancel().get();
        testFactory.storedCell(order.getSortingCenter(), "r1", CellType.RETURN, order.getWarehouseReturnYandexId().orElseThrow());
        var outgoingRoute = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        //todo: тестируется только старый флоу
        routeRepository.delete(outgoingRoute);

        assertThat(testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))).isEmpty();

        missingRoutesService.createMissingWarehouseRoutes(LocalDate.now(clock), sortingCenter);
        var newRoute = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order));
        assertThat(newRoute).isPresent();
        assertThat(newRoute.get().getRouteCells().stream().map(RouteCell::getCell).toList())
                .hasSize(2);
    }

    @Test
    void createMissingWarehouseRouteDontUseDeletedCells() {
        OrderLike order =
                testFactory.createForToday(order(sortingCenter).externalId("o1").build()).accept().cancel().get();
        var cell = testFactory.storedCell(order.getSortingCenter(), "r1", CellType.RETURN,
                order.getWarehouseReturnYandexId().orElseThrow());
        testFactory.deleteCellForce(cell);
        var outgoingRoute = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        routeRepository.delete(outgoingRoute);
        //todo: тестируется только старый флоу
        assertThat(testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))).isEmpty();

        missingRoutesService.createMissingWarehouseRoutes(LocalDate.now(clock), sortingCenter);
        var newRoute = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order));
        assertThat(newRoute).isPresent();
        assertThat(newRoute.get().getRouteCells().stream().map(RouteCell::getCell).toList())
                .hasSize(1);
    }

    @Test
    void createMissingWarehouseRouteDontUseCellsFromOtherRoutes() {
        OrderLike order =
                testFactory.createForToday(order(sortingCenter).externalId("o1").build()).accept().cancel().get();
        var newSc = testFactory.storedSortingCenter(100500L);
        assertThat(newSc).isNotEqualTo(order.getSortingCenter());
        testFactory.storedCell(newSc, "r1", CellType.RETURN, order.getWarehouseReturnYandexId().orElseThrow());

        var outgoingRoute = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        routeRepository.delete(outgoingRoute);
        assertThat(testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))).isEmpty();

        missingRoutesService.createMissingWarehouseRoutes(LocalDate.now(clock), sortingCenter);
        var newRoute = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order));
        assertThat(newRoute).isPresent();
        assertThat(newRoute.get().getRouteCells().stream().map(RouteCell::getCell).toList())
                .hasSize(1);
    }

}
