package ru.yandex.market.sc.core.domain.route_so.bind;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route_so.RouteSoCommandService;
import ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoRepository;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSite;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author mors741
 */
@EmbeddedDbTest
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RouteSoCellBinderTest {

    @Autowired
    RouteSoCellBinder routeSoCellBinder;

    @Autowired
    RouteSoCommandService routeSoCommandService;

    @Autowired
    RouteSoRepository routeSoRepository;

    @Autowired
    EntityManager entityManager;

    @MockBean
    Clock clock;

    @Autowired
    TestFactory testFactory;

    SortingCenter sortingCenter;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedWarehouse();
        testFactory.storedUser(sortingCenter, TestFactory.USER_UID_LONG);
        testFactory.setupMockClock(clock);
    }

    @Test
    void bindCellByPlace() {
        if (!SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
            return;
        }

        Place place = testFactory.createOrderForToday(sortingCenter).getPlace();
        RouteSo outRoute = place.getOutRoute();

        assertThat(outRoute.getDestinationType()).isEqualTo(RouteDestinationType.COURIER);

        long courierId = outRoute.getDestinationId();
        Cell cell = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER, courierId);

        // если не сбросить привязку ячеек, то новая ячейка не привяжется
        outRoute.getRouteSoSites().clear();
        entityManager.flush();

        routeSoCellBinder.bindCellsToOutgoingRoute(outRoute, CellBindingOptions.of(place));

        outRoute = routeSoRepository.findByIdOrThrow(outRoute.getId());

        List<Cell> boundCells = outRoute.getRouteSoSites().stream().map(RouteSoSite::getCell).toList();

        assertThat(boundCells).contains(cell);
    }

    @Test
    void bindCellByPreferredCellId() {
        if (!SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
            return;
        }

        Place place = testFactory.createOrderForToday(sortingCenter).getPlace();
        RouteSo outRoute = place.getOutRoute();

        assertThat(outRoute.getDestinationType()).isEqualTo(RouteDestinationType.COURIER);

        long courierId = outRoute.getDestinationId();
        Cell cell1 = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER, courierId);
        Cell cell2 = testFactory.storedCell(sortingCenter, "c-2", CellType.COURIER, courierId);

        // если не сбросить привязку ячеек, то новая ячейка не привяжется
        outRoute.getRouteSoSites().clear();
        entityManager.flush();

        routeSoCellBinder.bindCellsToOutgoingRoute(outRoute, CellBindingOptions.of(place, cell2.getId()));

        outRoute = routeSoRepository.findByIdOrThrow(outRoute.getId());

        List<Cell> boundCells = outRoute.getRouteSoSites().stream().map(RouteSoSite::getCell).toList();

        assertThat(boundCells).hasSize(1);
        assertThat(boundCells).contains(cell2);
    }

    @Test
    void bindCellOnCustomInterval() {
        if (!SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
            return;
        }

        Place place = testFactory.createOrderForToday(sortingCenter).getPlace();
        RouteSo outRoute = place.getOutRoute();

        assertThat(outRoute.getDestinationType()).isEqualTo(RouteDestinationType.COURIER);

        long courierId = outRoute.getDestinationId();
        Cell cell = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER, courierId);

        // если не сбросить привязку ячеек, то новая ячейка не привяжется
        outRoute.getRouteSoSites().clear();
        entityManager.flush();

        LocalDate today = LocalDate.now(clock);
        LocalDateInterval interval = new LocalDateInterval(
                today.minusDays(1),
                today.plusDays(1)
        );
        routeSoCellBinder.bindCellsToOutgoingRoute(outRoute, CellBindingOptions.of(place, interval));

        outRoute = routeSoRepository.findByIdOrThrow(outRoute.getId());

        assertThat(outRoute.getRouteSoSites()).hasSize(1);
        RouteSoSite routeCell = outRoute.getRouteSoSites().iterator().next();
        assertThat(routeCell.getCell()).isEqualTo(cell);
        assertThat(routeCell.isReservedOnDate(today)).isTrue();
        assertThat(routeCell.isReservedOnDate(today.plusDays(1))).isTrue();
    }

    @Test
    void bindCellWithoutCellSelectionParams() {
        if (!SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
            return;
        }

        Place place = testFactory.createOrderForToday(sortingCenter).getPlace();
        RouteSo outRoute = place.getOutRoute();

        assertThat(outRoute.getDestinationType()).isEqualTo(RouteDestinationType.COURIER);

        long courierId = outRoute.getDestinationId();
        Cell cell = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER, courierId);

        // если не сбросить привязку ячеек, то новая ячейка не привяжется
        outRoute.getRouteSoSites().clear();
        entityManager.flush();

        LocalDate today = LocalDate.now(clock);
        LocalDateInterval interval = new LocalDateInterval(today, today);
        routeSoCellBinder.bindCellsToOutgoingRoute(outRoute, CellBindingOptions.of(cell.getId(), interval));

        outRoute = routeSoRepository.findByIdOrThrow(outRoute.getId());

        List<Cell> boundCells = outRoute.getRouteSoSites().stream().map(RouteSoSite::getCell).toList();

        assertThat(boundCells).hasSize(1);
        assertThat(boundCells).contains(cell);
    }

    @Test
    void notBindCell() {
        if (!SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
            return;
        }

        Place place = testFactory.createOrderForToday(sortingCenter).getPlace();
        RouteSo outRoute = place.getOutRoute();
        routeSoCommandService.unbindCellFromRoutes(List.of(outRoute.getId()));

        routeSoCellBinder.bindCellsToOutgoingRoute(outRoute, CellBindingOptions.none());

        outRoute = routeSoRepository.findByIdOrThrow(outRoute.getId());

        List<Cell> boundCells = outRoute.getRouteSoSites().stream().map(RouteSoSite::getCell).toList();

        assertThat(boundCells).isEmpty();
    }

    @Test
    void pinCellDestination() {
        if (!SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
            return;
        }

        // middle mile order
        Place place = testFactory.createForToday(order(sortingCenter).dsType(DeliveryServiceType.TRANSIT).build())
                .getPlace();
        RouteSo outRoute = place.getOutRoute();

        assertThat(outRoute.getDestinationType()).isEqualTo(RouteDestinationType.COURIER);

        Cell cell = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);

        // если не сбросить привязку ячеек, то новая ячейка не привяжется
        outRoute.getRouteSoSites().clear();
        entityManager.flush();

        routeSoCellBinder.bindCellsToOutgoingRoute(outRoute, CellBindingOptions.of(place, cell.getId()));

        outRoute = routeSoRepository.findByIdOrThrow(outRoute.getId());

        List<Cell> boundCells = outRoute.getRouteSoSites().stream().map(RouteSoSite::getCell).toList();

        assertThat(boundCells).hasSize(1);
        assertThat(boundCells.get(0)).isEqualTo(cell);

        assertThat(boundCells.get(0).getCourierId()).isEqualTo(outRoute.getDestinationId());
    }

}
