package ru.yandex.market.sc.tms.route;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.CellCommandService;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.cell.repository.CellRepository;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.order.OrderNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.place.PlaceNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;
import ru.yandex.market.sc.core.domain.route_so.RouteSoQueryService;
import ru.yandex.market.sc.core.domain.route_so.RouteSoSiteService;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.ScDateUtils;
import ru.yandex.market.sc.tms.domain.route.RouteCellDistributor;
import ru.yandex.market.sc.tms.domain.routeso.RouteSoCellDistributor;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.bindCellsToRouteSo;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.sortWithRouteSo;

/**
 * @author valter
 */
@EmbeddedDbTmsTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RouteCellDistributorTest {
    private final RouteCellDistributor routeCellDistributor;
    private final RouteSoCellDistributor routeSoCellDistributor;
    private final OrderNonBlockingQueryService orderNonBlockingQueryService;
    private final PlaceNonBlockingQueryService placeNonBlockingQueryService;
    private final RouteSoSiteService routeSoSiteService;
    private final CellCommandService cellCommandService;
    private final TestFactory testFactory;
    private final TransactionTemplate transactionTemplate;
    private final RouteRepository routeRepository;
    private final RouteSoRepository routeSoRepository;
    private final RouteSoQueryService routeSoQueryService;
    private final RouteSoMigrationHelper routeSoMigrationHelper;
    private final CellRepository cellRepository;
    private final JdbcTemplate jdbcTemplate;

    @MockBean
    Clock clock;

    private SortingCenter sortingCenter;
    private Courier courier;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        courier = testFactory.storedCourier();
        testFactory.setupMockClock(clock);
    }

    @Test
    void setActiveCellForOutgoingRouteWithManyOrders() {
        RouteSoMigrationHelper.allowRouteReading();
        testFactory.storedCell(sortingCenter, null,
                CellStatus.NOT_ACTIVE, CellType.COURIER, null);
        var order = testFactory.createForToday(order(sortingCenter).externalId("o1").build()).get();
        testFactory.createForToday(order(sortingCenter).externalId("o2").build()).get();
        var expectedCell = testFactory.storedCell(sortingCenter, "2",
                CellStatus.ACTIVE, CellType.COURIER, null);

        routeCellDistributor.distributeCells();
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.distributeCells();

        transactionTemplate.execute(ts -> {
            RouteSoMigrationHelper.allowRouteReading();
            var actualRoute = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
            assertThat(actualRoute.getCells(LocalDate.now(clock))).isEqualTo(List.of(expectedCell));
            RouteSoMigrationHelper.revokeRouteReadingPermission();
            return null;
        });
    }

    @Test
    void assignCellToMiddleMileOutgoingCourier() {
        RouteSoMigrationHelper.allowRouteReading();
        testFactory.setDeliveryServiceProperty(TestFactory.deliveryService(),
                DeliveryServiceProperty.TYPE_ON_SC_PREFIX + sortingCenter.getId(), "TRANSIT");
        var order1 = testFactory.createOrder(sortingCenter).updateShipmentDate(LocalDate.now(clock))
                .get();
        jdbcTemplate.update("update cell set deleted=true");

        routeCellDistributor.distributeCells();
        RouteSoMigrationHelper.revokeRouteReadingPermission();
        routeSoCellDistributor.distributeCells();

        testFactory.findAllCells().forEach(c -> assertThat(c.getCourierId()).isNotNull());
    }

    @Test
    void removeCellFromEmptyRoute() {
        var route = createEmptyRoute();
        RouteSoMigrationHelper.allowRouteReading();
        assertThat(route.getCells(LocalDate.now(clock))).isNotEmpty();

        routeCellDistributor.distributeCells();
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.distributeCells();

        transactionTemplate.execute(ts -> {
            RouteSoMigrationHelper.allowRouteReading();
            var actualRoute = routeRepository.findByIdOrThrow(route.getId());
            assertThat(actualRoute.getCells(LocalDate.now(clock))).isEmpty();

            if (sortWithRouteSo()) {
                var routeSo = routeSoRepository.findByIdOrThrow(testFactory.getRoutable(route).getId());

                List<Cell> cells = routeSoMigrationHelper.getCells(routeSo, Instant.now(clock));

                assertThat(cells).isEmpty();
            }

            RouteSoMigrationHelper.revokeRouteReadingPermission();
            return null;
        });
    }

    private Route createEmptyRoute() {
        var order = testFactory.createOrderForToday(sortingCenter).get();
        return transactionTemplate.execute(ts -> {
            var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
            testFactory.cancelOrder(order.getId());
            RouteSoMigrationHelper.allowRouteReading();
            assertThat(orderNonBlockingQueryService.getPlaces(route)).isEmpty();
            assertThat(route.getCells(LocalDate.now(clock))).isNotEmpty();
            RouteSoMigrationHelper.revokeRouteReadingPermission();
            return route;
        });
    }

    @Test
    void doNotRemoveCellFromNonEmptyRoute() {
        var route = createNonEmptyRoute();
        RouteSoMigrationHelper.allowRouteReading();
        assertThat(route.getCells(LocalDate.now(clock))).isNotEmpty();

        routeCellDistributor.distributeCells();
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.distributeCells();

        assertThat(route.allowNextRead().getCells(LocalDate.now(clock))).isNotEmpty();
    }

    private Route createNonEmptyRoute() {
        return createNonEmptyRoute(LocalDate.now(clock));
    }

    private Route createNonEmptyRoute(LocalDate date) {
        var order = testFactory.createOrder(sortingCenter)
                .updateShipmentDate(date).updateCourier(courier).get();
        return transactionTemplate.execute(ts -> {
            var result = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
            RouteSoMigrationHelper.allowRouteReading();
            assertThat(orderNonBlockingQueryService.getPlaces(result.allowNextRead())).isNotEmpty();
            assertThat(result.getCells(date)).isNotEmpty();
            RouteSoMigrationHelper.revokeRouteReadingPermission();
            return result;
        });
    }

    @Test
    void deleteNotUsedNonActiveCellsWithCellForRouteSo() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, true);
        Warehouse warehouse = testFactory.storedWarehouse();

        // Зоны нужны, чтобы сбить совпадение в id для cell и site
        testFactory.storedZone(sortingCenter, "1");
        testFactory.storedZone(sortingCenter, "2");
        testFactory.storedZone(sortingCenter, "3");

        List<Cell> cells = Arrays.stream(CellType.values())
                .map(type -> testFactory.storedCell(sortingCenter, null, type))
                .toList();

        testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .type(OutboundType.XDOC)
                .fromTime(Instant.now(clock))
                .toTime(Instant.now(clock))
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(warehouse.getYandexId())
                .partnerToExternalId(warehouse.getPartnerId())
                .build());
        Cell cellWithRouteSo =
                cellRepository.findAllBySortingCenterAndTypeAndDeletedOrderByScNumberAscIdDesc(sortingCenter,
                CellType.COURIER, false).stream()
                .filter(cell -> cell.getSubtype() == CellSubType.SHIP_XDOC)
                .findFirst()
                .orElseThrow();

        assertThat(cells).isNotEmpty();
        assertThat(cells).allMatch(c -> c.getStatus() == CellStatus.NOT_ACTIVE);
        assertThat(cells).noneMatch(Cell::isDeleted);
        long cellsSize = cells.size();

        routeCellDistributor.distributeCells();
        routeSoCellDistributor.distributeCells();

        cells = cellRepository.findAllById(cells.stream().map(Cell::getId).toList());
        assertThat(cells.size()).isEqualTo(cellsSize);
        assertThat(cells).allMatch(Cell::isDeleted);
        assertThat(cellRepository.findByIdOrThrow(cellWithRouteSo.getId()).isDeleted()).isFalse();
    }

    @Test
    void deleteNotUsedNonActiveCells() {
        List<Cell> cells = Arrays.stream(CellType.values())
                .map(type -> testFactory.storedCell(sortingCenter, null, type))
                .toList();
        assertThat(cells).isNotEmpty();
        assertThat(cells).allMatch(c -> c.getStatus() == CellStatus.NOT_ACTIVE);
        assertThat(cells).noneMatch(Cell::isDeleted);
        long cellsSize = cells.size();

        routeCellDistributor.distributeCells();
        routeSoCellDistributor.distributeCells();

        cells = cellRepository.findAllById(cells.stream().map(Cell::getId).toList());
        assertThat(cells.size()).isEqualTo(cellsSize);
        assertThat(cells).allMatch(Cell::isDeleted);
    }

    @Test
    void doNotDeleteUsedActiveCells() {
        List<Cell> cells = Arrays.stream(CellType.values())
                .map(type -> testFactory.storedCell(sortingCenter, type.name() + "-1", type))
                .toList();
        assertThat(cells).allMatch(c -> c.getStatus() == CellStatus.ACTIVE);
        assertThat(cells).noneMatch(Cell::isDeleted);

        routeCellDistributor.distributeCells();
        routeSoCellDistributor.distributeCells();

        assertThat(cells).noneMatch(Cell::isDeleted);
    }

    @Test
    void reuseActiveCellFromEmptyRoute() {
        var courier1 = testFactory.storedCourier(1);
        var courier2 = testFactory.storedCourier(2);

        var activeCell1 = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);
        var order1 = testFactory.create(order(sortingCenter).externalId("1").build())
                .updateCourier(courier1).updateShipmentDate(LocalDate.now(clock)).get();

        RouteSoMigrationHelper.allowRouteReading();
        var route1 = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        assertThat(route1.getCells(LocalDate.now(clock))).containsOnly(activeCell1);
        testFactory.cancelOrder(order1.getId()); // route1 now is empty

        var order2 = testFactory.createForToday(order(sortingCenter).externalId("2").build())
                .updateCourier(courier2).updateShipmentDate(LocalDate.now(clock)).get();
        var route2 = testFactory.findOutgoingCourierRoute(order2).orElseThrow();
        assertThat(route2.getCells(LocalDate.now(clock))).hasSize(1);
        var nonActiveCell = route2.getCells(LocalDate.now(clock)).iterator().next();
        assertThat(nonActiveCell).isNotNull();
        assertThat(nonActiveCell).isNotEqualTo(activeCell1);
        assertThat(nonActiveCell.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
        assertThat(nonActiveCell.isDeleted()).isFalse();

        routeCellDistributor.distributeCells();
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.distributeCells();

        transactionTemplate.execute(ts -> {
            RouteSoMigrationHelper.allowRouteReading();
            var actualRoute1 = routeRepository.findByIdOrThrow(route1.getId());
            var actualRoute2 = routeRepository.findByIdOrThrow(route2.getId());
            var actualNonActiveCell = cellRepository.findByIdOrThrow(nonActiveCell.getId());
            assertThat(actualRoute1.getCells(LocalDate.now(clock))).isEmpty();
            assertThat(actualRoute2.getCells(LocalDate.now(clock))).isNotEmpty();
            assertThat(actualRoute2.getCells(LocalDate.now(clock))).containsOnly(activeCell1);
            assertThat(actualRoute2.getCells(LocalDate.now(clock)).iterator().next().getStatus()).isEqualTo(CellStatus.ACTIVE);
            assertThat(actualNonActiveCell.isDeleted()).isTrue();
            RouteSoMigrationHelper.revokeRouteReadingPermission();
            return null;
        });
    }


    @Test
    void useActiveCellInsteadOfNonActive() {
        RouteSoMigrationHelper.allowRouteReading();
        var route = createNonEmptyRoute();
        assertThat(route.getCells(LocalDate.now(clock))).hasSize(1);
        var nonActiveCell = route.getCells(LocalDate.now(clock)).iterator().next();
        assertThat(nonActiveCell).isNotNull();
        assertThat(nonActiveCell.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);

        var activeCell = testFactory.storedCell(sortingCenter, "c-1-active", CellType.COURIER);

        routeCellDistributor.distributeCells();

        RouteSoMigrationHelper.revokeRouteReadingPermission();
        routeSoCellDistributor.distributeCells();

        transactionTemplate.execute(ts -> {
            RouteSoMigrationHelper.allowRouteReading();
            var actualRoute = routeRepository.findByIdOrThrow(route.getId());

            var actualNonActiveCell = cellRepository.findByIdOrThrow(nonActiveCell.getId());
            assertThat(actualRoute.getCells(LocalDate.now(clock))).isNotEmpty();
            assertThat(actualRoute.getCells(LocalDate.now(clock))).containsOnly(activeCell);
            assertThat(actualRoute.getCells(LocalDate.now(clock)).iterator().next().getStatus()).isEqualTo(CellStatus.ACTIVE);
            assertThat(actualNonActiveCell.isDeleted()).isTrue();

            if (sortWithRouteSo()) {
                var routeSo = routeSoRepository.findByIdOrThrow(testFactory.getRoutable(route).getId());

                List<Cell> cells = routeSoMigrationHelper.getCells(routeSo, Instant.now(clock));

                assertThat(cells).isNotEmpty();
                assertThat(cells).containsOnly(activeCell);
                assertThat(cells.iterator().next().getStatus()).isEqualTo(CellStatus.ACTIVE);
                assertThat(actualNonActiveCell.isDeleted()).isTrue();
            }
            RouteSoMigrationHelper.revokeRouteReadingPermission();
            return null;
        });
    }

    @Test
    void useActiveCellInsteadOfNonActiveForTomorrowRoute() {
        var date = LocalDate.now(clock).plusDays(1);
        var route = createNonEmptyRoute(date);

        RouteSoMigrationHelper.allowRouteReading();
        assertThat(route.getCells(date)).hasSize(1);
        var nonActiveCell = route.getCells(date).iterator().next();
        assertThat(nonActiveCell).isNotNull();
        assertThat(nonActiveCell.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);

        var activeCell = testFactory.storedCell(sortingCenter, "c-1-active", CellType.COURIER);

        routeCellDistributor.distributeCells();
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.distributeCells();

        transactionTemplate.execute(ts -> {
            RouteSoMigrationHelper.allowRouteReading();
            var actualRoute = routeRepository.findByIdOrThrow(route.getId());
            var actualNonActiveCell = cellRepository.findByIdOrThrow(nonActiveCell.getId());
            assertThat(actualRoute.getCells(date)).isNotEmpty();
            assertThat(actualRoute.getCells(date)).containsOnly(activeCell);
            assertThat(actualRoute.getCells(date).iterator().next().getStatus()).isEqualTo(CellStatus.ACTIVE);
            assertThat(actualNonActiveCell.isDeleted()).isTrue();

            if (sortWithRouteSo()) {
                var routeSo = routeSoRepository.findByIdOrThrow(testFactory.getRoutable(route).getId());

                List<Cell> cells = routeSoMigrationHelper.getCells(routeSo, ScDateUtils.toNoon(date));

                assertThat(cells).isNotEmpty();
                assertThat(cells).containsOnly(activeCell);
                assertThat(cells.iterator().next().getStatus()).isEqualTo(CellStatus.ACTIVE);
                assertThat(actualNonActiveCell.isDeleted()).isTrue();
            }

            RouteSoMigrationHelper.revokeRouteReadingPermission();
            return null;
        });
    }

    @Test
    void doNotCreateActiveCellInsteadOfNonActive() {
        var route = createNonEmptyRoute();
        RouteSoMigrationHelper.allowRouteReading();
        assertThat(route.getCells(LocalDate.now(clock))).hasSize(1);
        var cell = route.getCells(LocalDate.now(clock)).iterator().next();
        assertThat(cell.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);

        routeCellDistributor.distributeCells();
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.distributeCells();

        transactionTemplate.execute(ts -> {
            RouteSoMigrationHelper.allowRouteReading();
            var actualRoute = routeRepository.findByIdOrThrow(route.getId());
            assertThat(actualRoute.getCells(LocalDate.now(clock))).isNotEmpty();
            assertThat(actualRoute.getCells(LocalDate.now(clock))).containsOnly(cell);
            assertThat(actualRoute.getCells(LocalDate.now(clock)).iterator().next().getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);

            if (sortWithRouteSo()) {
                var routeSo = routeSoRepository.findByIdOrThrow(testFactory.getRoutable(route).getId());

                List<Cell> cells = routeSoMigrationHelper.getCells(routeSo, Instant.now(clock));

                assertThat(cells).isNotEmpty();
                assertThat(cells).containsOnly(cell);
                assertThat(cells.iterator().next().getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
            }
            RouteSoMigrationHelper.revokeRouteReadingPermission();

            return null;

        });
    }

    @Test
    void useActiveCellInsteadOfDeleted() {
        RouteSoMigrationHelper.allowRouteReading();
        var route = createNonEmptyRouteWithDeletedCell();
        Cell activeCell = testFactory.storedCell(sortingCenter, "active", CellType.COURIER);

        routeCellDistributor.distributeCells();
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.distributeCells();

        transactionTemplate.execute(ts -> {
            RouteSoMigrationHelper.allowRouteReading();
            var actualRoute = routeRepository.findByIdOrThrow(route.getId());
            assertThat(actualRoute.getCells(LocalDate.now(clock))).isNotEmpty();
            assertThat(actualRoute.getCells(LocalDate.now(clock))).containsOnly(activeCell);
            assertThat(actualRoute.getCells(LocalDate.now(clock)).iterator().next().isDeleted()).isFalse();
            assertThat(actualRoute.getCells(LocalDate.now(clock)).iterator().next().getStatus()).isEqualTo(CellStatus.ACTIVE);


            if (sortWithRouteSo()) {
                var routeSo = routeSoRepository.findByIdOrThrow(testFactory.getRoutable(route).getId());

                List<Cell> cells = routeSoMigrationHelper.getCells(routeSo, Instant.now(clock));

                assertThat(cells).hasSize(1);
                assertThat(cells).containsOnly(activeCell);
                assertThat(cells.iterator().next().isDeleted()).isFalse();
                assertThat(cells.iterator().next().getStatus()).isEqualTo(CellStatus.ACTIVE);
            }
            RouteSoMigrationHelper.revokeRouteReadingPermission();
            return null;
        });
    }

    @Test
    void createNonActiveCellInsteadOfDeleted() {
        var route = createNonEmptyRouteWithDeletedCell();
        RouteSoMigrationHelper.allowRouteReading();
        routeCellDistributor.distributeCells();
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.distributeCells();

        transactionTemplate.execute(ts -> {
            RouteSoMigrationHelper.allowRouteReading();

            var actualRoute = routeRepository.findByIdOrThrow(route.getId());
            assertThat(actualRoute.getCells(LocalDate.now(clock))).hasSize(1);
            assertThat(actualRoute.getCells(LocalDate.now(clock)).iterator().next().isDeleted()).isFalse();
            assertThat(actualRoute.getCells(LocalDate.now(clock)).iterator().next().getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);

            if (sortWithRouteSo()) {
                var routeSo = routeSoRepository.findByIdOrThrow(testFactory.getRoutable(route).getId());

                List<Cell> cells = routeSoMigrationHelper.getCells(routeSo, Instant.now(clock));

                assertThat(cells).hasSize(1);
                assertThat(cells.iterator().next().isDeleted()).isFalse();
                assertThat(cells.iterator().next().getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
            }

            RouteSoMigrationHelper.revokeRouteReadingPermission();
            return null;
        });
    }

    private Route createNonEmptyRouteWithDeletedCell() {
        RouteSoMigrationHelper.allowRouteReading();
        var route = createEmptyRoute();
        assertThat(route.getCells(LocalDate.now(clock))).hasSize(1);
        var firstCell = route.getCells(LocalDate.now(clock)).iterator().next();
        assertThat(Objects.requireNonNull(firstCell).isDeleted()).isFalse();

        routeCellDistributor.distributeCells(); // cleanup route cell
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        routeSoCellDistributor.distributeCells();


        RouteSoMigrationHelper.allowRouteReading();

        assertThat(routeRepository.findByIdOrThrow(route.getId()).getCells(LocalDate.now(clock))).isEmpty();
        if (sortWithRouteSo()) {
            var routeSo = routeRepository.findByIdOrThrow(route.getId());
            assertThat(routeSoMigrationHelper.getCells(routeSo, Instant.now(clock))).isEmpty();
        }

        if (bindCellsToRouteSo()) {
            assertThat(routeSoSiteService.getRouteSoSites(
                    testFactory.getRouteSo(route), Instant.now(clock)).isEmpty());
        }
        cellCommandService.deleteCell(sortingCenter, firstCell.getId(), true);

        testFactory.createForToday(order(sortingCenter).externalId("new_one").build()).get(); // make route non-empty

        transactionTemplate.execute(ts -> {
            if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
                var actualRouteSo = routeSoRepository.findByIdOrThrow(testFactory.getRoutable(route).getId());
                assertThat(placeNonBlockingQueryService.getByRoute(actualRouteSo)).isNotEmpty();
            }
            assertThat(orderNonBlockingQueryService.getPlaces(route)).isNotEmpty();

            return null;
        });



        firstCell = cellRepository.findByIdOrThrow(firstCell.getId());
        assertThat(Objects.requireNonNull(firstCell).isDeleted()).isTrue();

        RouteSoMigrationHelper.revokeRouteReadingPermission();
        return route;
    }
    @Test
    void doNotRemoveCellFromShippedRoute() {

        var order = testFactory.createOrderForToday(sortingCenter).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        testFactory.shipOrderRoute(order);
        testFactory.setupMockClock(clock, clock.instant().plusSeconds(1));

        routeCellDistributor.distributeCells(); // DO NOT cleanup route cell
        routeSoCellDistributor.distributeCells();

        RouteSoMigrationHelper.allowRouteReading();
        assertThat(routeRepository.findByIdOrThrow(route.getId()).getCells(LocalDate.now(clock))).isNotEmpty();
        if (sortWithRouteSo()) {
            var routeSo = routeSoRepository.findByIdOrThrow(testFactory.getRoutable(route).getId());
            List<Cell> cells = routeSoMigrationHelper.getCells(routeSo, Instant.now(clock));

            assertThat(cells).isNotEmpty();
        }
        RouteSoMigrationHelper.revokeRouteReadingPermission();
    }
}
