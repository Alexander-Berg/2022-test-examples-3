package ru.yandex.market.sc.core.domain.route;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.assertj.core.api.ThrowableAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.CellCommandService;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.policy.CellPolicy;
import ru.yandex.market.sc.core.domain.cell.policy.OrderCellParams;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.courier.model.PartnerCourierDto;
import ru.yandex.market.sc.core.domain.courier.model.PartnerCourierDto.CourierCompany;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.courier.repository.CourierRepository;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.inbound.repository.BoundRegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRegistryOrderStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.movement_courier.model.MovementCourierRequest;
import ru.yandex.market.sc.core.domain.order.OrderNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.outbound.OutboundCommandService;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundCreateRequest;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.jdbc.RouteJdbcRepository;
import ru.yandex.market.sc.core.domain.route.model.RouteCreateRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishByCellsRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinishOrder;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.route_so.Routable;
import ru.yandex.market.sc.core.domain.route_so.RouteSoCommandService;
import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;
import ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType;
import ru.yandex.market.sc.core.domain.route_so.model.RouteSoCreateRequest;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoRepository;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.exception.ScException;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.ScDateUtils;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.tpl.common.util.datetime.LocalDateInterval;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension.testNotMigrated;
import static ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension.useNewRouteSoStage1;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.useNewSortableFlow;

/**
 * @author valter
 */
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RouteCommandServiceTest {

    private final RouteCommandService routeCommandService;
    private final RouteSoCommandService routeSoCommandService;
    private final RouteRepository routeRepository;
    private final RouteNonBlockingQueryService routeNonBlockingQueryService;
    private final TestFactory testFactory;
    private final RouteSoMigrationHelper routeSoMigrationHelper;
    private final CourierRepository courierRepository;
    private final OrderNonBlockingQueryService orderNonBlockingQueryService;
    private final ScOrderRepository scOrderRepository;
    private final TransactionTemplate transactionTemplate;
    private final CellCommandService cellCommandService;
    private final JdbcTemplate jdbcTemplate;
    private final PlaceRepository placeRepository;
    private final OutboundCommandService outboundCommandService;
    private final BoundRegistryRepository boundRegistryRepository;
    private final RegistryRepository registryRepository;

    private final RouteSoRepository routeSoRepository;


    @SpyBean
    RouteJdbcRepository routeJdbcRepository;
    @MockBean
    Clock clock;
    @SpyBean
    CellPolicy cellPolicy;

    SortingCenter sortingCenter;
    DeliveryService deliveryService;
    Cell cell;
    Courier courier;
    Warehouse warehouse;
    ScOrder order;
    User dispatcher;

    //короткий синоним
    private TestFactory tf;

    @BeforeEach
    void init() {
        testFactory.setupMockClock(clock);
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        cell = testFactory.storedCell(sortingCenter);
        courier = testFactory.storedCourier(999L);
        warehouse = testFactory.storedWarehouse();
        deliveryService = testFactory.storedDeliveryService("1");
        order = testFactory.createOrder(sortingCenter, deliveryService).get();
        dispatcher = testFactory.storedUser(sortingCenter, 123L);
        tf = testFactory;
    }

    @Test
    void cantReserveCellForAdvanceSortAfterLastAdvanceDay() {
        int daysInAdvance = 1;
        String deliveryYandexId = "1";
        String cutoff = "10";
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        var deliveryService = testFactory.storedDeliveryService(deliveryYandexId, false);
        testFactory.changeDeliveryServiceOn(sortingCenter, deliveryService, DeliveryServiceType.TRANSIT);
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, String.valueOf(sortingCenter.getId()));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.CUTOFF_SORT_IN_ADVANCE, cutoff);
        var order = testFactory.create(order(sortingCenter)
                        .externalId("o1")
                        .deliveryService(deliveryService)
                        .dsType(DeliveryServiceType.TRANSIT).build())
                .updateShipmentDate(LocalDate.now(clock).plusDays(3))
                .accept()
                .get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow().allowReading();
        assertThat(route.getExpectedDate()).isEqualTo(LocalDate.now(clock).plusDays(3));
        assertThat(route.getRouteCells()).hasSize(0);
    }

    @Test
    void reserveCellFor1Day() {
        int daysInAdvance = 0;
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        var cell = testFactory.storedCell(sortingCenter, "c-1", CellType.COURIER);
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER,
                testFactory.storedCourier(), LocalDate.now(clock).plusDays(daysInAdvance));
        var route = createRouteIfNotExistsAndSelect(request);

        assertThat(route.allowReading().getCells(LocalDate.now(clock).minusDays(1))).isEmpty();
        assertThat(route.getCells(LocalDate.now(clock))).isEqualTo(List.of(cell));
        assertThat(route.getCells(LocalDate.now(clock).plusDays(1))).isEmpty();
    }

    @Test
    void reserveCellFor2Days() {
        int daysInAdvance = 1;
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, String.valueOf(sortingCenter.getId()));
        testFactory.changeDeliveryServiceOn(sortingCenter, deliveryService, DeliveryServiceType.TRANSIT);
        var request = routeCreateRequestMiddleMile(LocalDate.now(clock).plusDays(daysInAdvance));
        var route = createRouteIfNotExistsAndSelect(request);

        assertThat(route.getCells(LocalDate.now(clock).minusDays(1))).isEmpty();
        assertThat(route.getCells(LocalDate.now(clock))).isNotEmpty();
        assertThat(route.getCells(LocalDate.now(clock).plusDays(1))).isNotEmpty();
        assertThat(route.getCells(LocalDate.now(clock).plusDays(2))).isEmpty();
    }

    @Test
    void reserveCellFor2DaysNotMiddleMile() {
        int daysInAdvance = 1;
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER,
                courier, LocalDate.now(clock).plusDays(daysInAdvance));
        var route = createRouteIfNotExistsAndSelect(request);

        assertThat(route.getCells(LocalDate.now(clock).minusDays(1))).isEmpty();
        assertThat(route.getCells(LocalDate.now(clock))).isEmpty();
        assertThat(route.getCells(LocalDate.now(clock).plusDays(1))).isNotEmpty();
        assertThat(route.getCells(LocalDate.now(clock).plusDays(2))).isEmpty();
    }

    @Test
    void createSetsCellForNewOutgoingCourierRoute() {
        var route = createRouteIfNotExistsAndSelect(RouteType.OUTGOING_COURIER);
        assertThat(route.getCells(LocalDate.now(clock))).isNotEmpty();
    }

    @Test
    void createRouteIsIdempotent() {
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER);
        var route1 = createRouteIfNotExistsAndSelect(request);
        var route2 = createRouteIfNotExistsAndSelect(request);
        assertThat(route1.getId()).isEqualTo(route2.getId());
        assertThat(route1.getRouteCells()).containsAll(route2.getRouteCells());
    }

    @Test
    void createSetsCellForExistingOutgoingCourierRoute() {
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER);
        var route = createRouteIfNotExistsAndSelect(request);
        routeCommandService.cleanRoutesCell(List.of(testFactory.getRouteIdForSortableFlow(route)));
        route = createRouteIfNotExistsAndSelect(request);
        assertThat(route.getCells(LocalDate.now(clock))).isNotEmpty();
    }

    @Test
    void createDoesNotSetCellForExistingRouteWithDisabledCellDistribution() {
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER);
        var route = createRouteIfNotExistsAndSelect(request);
        routeCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(route.getId()));
        routeSoCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(getRouteSo(route).getId()));
        route = createRouteIfNotExistsAndSelect(request);
        assertThat(route.getCells(LocalDate.now(clock))).isEmpty();
    }

    @Test
    void setRouteCellSetsCellForExistingRoute() {
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER);
        var route = createRouteIfNotExistsAndSelect(request);
        routeCommandService.cleanRoutesCell(List.of(testFactory.getRouteIdForSortableFlow(route)));
        routeCommandService.setRoutesCell(List.of(testFactory.getRouteIdForSortableFlow(route)), LocalDate.now(clock));
        route = createRouteIfNotExistsAndSelect(request);
        assertThat(route.allowNextRead().getCells(LocalDate.now(clock))).isNotEmpty();
    }

    @Test
    void setRouteCellDoesNotSetCellForExistingRouteWithDisabledCellDistribution() {
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER);
        var route = createRouteIfNotExistsAndSelect(request);
        routeCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(route.getId()));
        routeCommandService.setRoutesCell(List.of(testFactory.getRouteIdForSortableFlow(route)), LocalDate.now(clock));
        var routeSo = getRouteSo(route);
        routeSoCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(routeSo.getId()));
        routeSoCommandService.setRoutesCell(List.of(routeSo.getId()), LocalDate.now(clock));

        route = createRouteIfNotExistsAndSelect(request);
        assertThat(route.getCells(LocalDate.now(clock))).isEmpty();
    }

    @Test
    void deleteCellsAndAddNewCellsChangesRouteCells() {
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER);
        var route = createRouteIfNotExistsAndSelect(request);
        assertThat(route.getCells(LocalDate.now(clock))).isEqualTo(List.of(cell1));

        cellCommandService.deleteCell(sortingCenter, cell1.getId(), true);
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        routeCommandService.deleteCellsAndAddNewCells(route.getId(), List.of(cell1.getId()), LocalDate.now(clock));
        routeSoCommandService.deleteCellsAndAddNewCells(getRouteSo(route), List.of(cell1.getId()), LocalDate.now(clock));


        route = createRouteIfNotExistsAndSelect(request);
        assertThat(route.getCells(LocalDate.now(clock))).isEqualTo(List.of(cell2));
    }

    @Test
    void deleteCellsAndAddNewCellsChangesRouteCellsForTomorrow() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(1)
        );
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.SORT_IN_ADVANCE_ON_SC, String.valueOf(sortingCenter.getId()));
        testFactory.changeDeliveryServiceOn(sortingCenter, deliveryService, DeliveryServiceType.TRANSIT);

        var request = routeCreateRequestMiddleMile(
                LocalDate.now(clock).plusDays(1));
        var route = createRouteIfNotExistsAndSelect(request);
        var routeCell = route.getCells(LocalDate.now(clock)).get(0);
        assertThat(route.getCells(LocalDate.now(clock))).isEqualTo(List.of(routeCell));
        assertThat(route.getCells(LocalDate.now(clock).plusDays(1))).isEqualTo(List.of(routeCell));

        cellCommandService.deleteCell(sortingCenter, cell.getId(), true);
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        routeCommandService.deleteCellsAndAddNewCells(route.getId(), List.of(cell.getId()),
                LocalDate.now(clock));
        routeSoCommandService.deleteCellsAndAddNewCells(getRouteSo(route), List.of(cell.getId()),
                LocalDate.now(clock));

        route = createRouteIfNotExistsAndSelect(request);
        assertThat(route.getCells(LocalDate.now(clock))).isNotEmpty();
        assertThat(route.getCells(LocalDate.now(clock))).isNotEqualTo(List.of(cell2));
        assertThat(route.getCells(LocalDate.now(clock).plusDays(1))).isEqualTo(List.of(routeCell));
    }

    @Test
    void deleteCellsAndAddNewCellsDoesNotSetCellForExistingRouteWithDisabledCellDistribution() {
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER);
        var route = createRouteIfNotExistsAndSelect(request);
        cellCommandService.deleteCell(sortingCenter, cell1.getId(), true);
        testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);

        routeCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(route.getId()));
        routeCommandService.deleteCellsAndAddNewCells(route.getId(), List.of(cell1.getId()), LocalDate.now(clock));

        RouteSo routeSo = getRouteSo(route);
        routeSoCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(routeSo.getId()));
        routeSoCommandService.deleteCellsAndAddNewCells(routeSo, List.of(cell1.getId()), LocalDate.now(clock));

        route = createRouteIfNotExistsAndSelect(request);
        assertThat(route.getCells(LocalDate.now(clock))).isEmpty();
    }

    @Test
    //только старый флоу маршрутов
    void tryToChangeRouteCellsChangesRouteCells() {
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER);
        var route = createRouteIfNotExistsAndSelect(request);
        route.allowNextRead();
        assertThat(route.getCells(LocalDate.now(clock))).isEqualTo(List.of(cell1));

        cellCommandService.deleteCell(sortingCenter, cell1.getId(), true);
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        route.allowNextRead();
        routeCommandService.tryToReplaceRouteCells(route.getId(), List.of(cell1.getId()), LocalDate.now(clock));
        route.allowNextRead();
        RouteSo routeSo =
                routeSoRepository.findBySortingCenterAndDestinationTypeAndDestinationId(
                    sortingCenter, RouteDestinationType.COURIER, route.getCourierToId())
                            .stream().findFirst().orElseThrow();
        routeSoCommandService.tryToReplaceRouteCells(routeSo, List.of(cell1), LocalDate.now(clock));
        route = createRouteIfNotExistsAndSelect(request);
        route.allowNextRead();
        assertThat(route.getCells(LocalDate.now(clock))).isEqualTo(List.of(cell2));
    }

    @Test
    void tryToChangeRouteCellsDoesNotSetCellForExistingRouteWithDisabledCellDistribution() {
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER);
        var route = createRouteIfNotExistsAndSelect(request);
        cellCommandService.deleteCell(sortingCenter, cell1.getId(), true);
        testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);

        routeCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(route.getId()));
        routeCommandService.tryToReplaceRouteCells(route.getId(), List.of(cell1.getId()), LocalDate.now(clock));

        RouteSo routeSo = getRouteSo(route);
        routeSoCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(routeSo.getId()));
        routeSoCommandService.tryToReplaceRouteCells(routeSo, List.of(cell1), LocalDate.now(clock));



        route = createRouteIfNotExistsAndSelect(request);
        assertThat(route.getCells(LocalDate.now(clock))).isEmpty();
    }

    @Test
    void create() {
        for (RouteType type : RouteType.values()) {
            createRouteIfNotExistsAndSelect(type);
        }
    }

    private RouteCreateRequest routeCreateRequest(RouteType routeType) {
        return routeCreateRequest(routeType, courier);
    }

    private RouteCreateRequest routeCreateRequest(RouteType routeType, Courier courier) {
        return routeCreateRequest(routeType, courier, LocalDate.now(clock));
    }

    private RouteCreateRequest routeCreateRequest(RouteType routeType, Courier courier, LocalDate expectedDate) {
        return new RouteCreateRequest(
                routeType,
                sortingCenter,
                expectedDate,
                new LocalDateInterval(expectedDate, expectedDate),
                null,
                warehouse,
                courier,
                cellParams(routeType, courier, expectedDate, deliveryService, false),
                null
        );
    }

    private RouteCreateRequest routeCreateRequestMiddleMile(LocalDate expectedDate) {
        ScOrder order = (ScOrder) cellParams(RouteType.OUTGOING_COURIER, null, expectedDate, deliveryService, true);
        return new RouteCreateRequest(
                RouteType.OUTGOING_COURIER,
                sortingCenter,
                expectedDate,
                new LocalDateInterval(expectedDate, expectedDate),
                null,
                warehouse,
                Objects.requireNonNull(order).getCourier(),
                order,
                null
        );
    }

    private OrderCellParams cellParams(RouteType routeType, Courier courier, LocalDate expectedDate,
                                       DeliveryService deliveryService,
                                       boolean isMiddleMile) {
        if (routeType.isIncoming()) {
            return null;
        }
        if (routeType.isCourier()) {
            var builder = testFactory.create(order(sortingCenter)
                            .deliveryService(deliveryService)
                            .externalId("o" + routeType)
                            .warehouseReturnId(warehouse.getYandexId())
                            .dsType(isMiddleMile ? DeliveryServiceType.TRANSIT : DeliveryServiceType.LAST_MILE_COURIER)
                            .build())
                    .updateShipmentDate(expectedDate);
            if (!isMiddleMile) {
                builder = builder.updateCourier(courier);
            }
            return builder.get();
        } else {
            return testFactory.createForToday(order(sortingCenter)
                            .externalId("o" + routeType)
                            .warehouseReturnId(warehouse.getYandexId())
                            .build())
                    .accept().makeReturn().get();
        }
    }

    @Test
    void sortingStartedIsNullAfterCreation() {
        var route = createRouteIfNotExistsAndSelect(RouteType.OUTGOING_COURIER);
        assertThat(route.allowNextRead().getSortingStartedAt()).isNull();
    }

    @Test
    void updateSortingStarted() {
        for (RouteType type : RouteType.values()) {
            testSortingStarted(type);
        }
    }

    private void testSortingStarted(RouteType routeType) {
        var route = createRouteIfNotExistsAndSelect(routeType);
        routeCommandService.updateSortingStarted(route.getId());
        RouteSo routeSo = getRouteSo(route);
        routeSoCommandService.updateSortingStarted(routeSo);

        route = routeRepository.findByIdOrThrow(route.getId());
        assertThat(route.allowReading().getSortingStartedAt()).isEqualTo(clock.instant());
        assertThat(routeSo.getSortingStartedAt()).isEqualTo(clock.instant());
    }

    @Test
    void finishByCell() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build())
                .accept().sort().get();

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(order1).orElseThrow();

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var cell1 = testFactory.determineRouteCell(outgoingCourierRoute, order1);
        finishRouteFromCell(testFactory.getRouteIdForSortableFlow(outgoingCourierRoute), cell1.getId());


        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
    }

    private void finishRouteFromCell(long routeId, List<Long> cellIds) {
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                routeId, new ScContext(dispatcher),
                cellIds,
                null,
                false
        ));
    }

    private void finishRouteFromCell(long routeId, long cellId) {
        finishRouteFromCell(routeId, List.of(cellId));
    }

    private void finishRouteFromCell(long routeId, long cellId, String externalOrderId) {
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                routeId, new ScContext(dispatcher),
                List.of(cellId),
                null,
                externalOrderId,
                externalOrderId,
                null,
                false
        ));
    }

    @Test
    void simpleNotFullMultiPlaceOrderScenario() {
        var order = testFactory.createForToday(order(sortingCenter, "1").places("11", "12").build())
                .acceptPlaces("11").sortPlaces("11").keepPlacesIgnoreTodayRoute("11")
                .makeReturn().sortPlaces("11").ship().get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void sortNotFullMultiOrderToWarehouse() {
        var order = testFactory.createForToday(order(sortingCenter, "1").places("11", "12").build())
                .cancel().acceptPlaces("11").sortPlaces("11").get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void keepNotFullMultiPlaceOrder() {
        var order = testFactory.create(order(sortingCenter, "1").places("11", "12").build())
                .acceptPlaces("11").keepPlaces("11").get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.KEEPED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void keepAndTryToShipToCourierNotFullMultiPlaceOrder() {
        var order = testFactory.create(order(sortingCenter, "1").places("11", "12").build())
                .acceptPlaces("11").keepPlaces("11")
                .updateShipmentDate(LocalDate.now(clock).plusDays(1)).updateCourier(testFactory.defaultCourier())
                .setupClockPlusDays(1)
                .acceptPlaces("11").sortPlaces("11").get();

        var outgoingCourierRoute =
                testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(outgoingCourierRoute, order);
        assertThatThrownBy(
                () -> finishRouteFromCell(outgoingCourierRoute.getId(), cell.getId())
        );
    }

    @Test
    void keepAndShipToCourierFullMultiPlaceOrder() {
        var order = testFactory.create(order(sortingCenter, "1").places("11", "12").build())
                .acceptPlaces("11").keepPlaces("11")
                .updateShipmentDate(LocalDate.now(clock).plusDays(1)).updateCourier(testFactory.defaultCourier())
                .setupClockPlusDays(1)
                .acceptPlaces("11", "12").sortPlaces("11", "12")
                .get();

        var outgoingCourierRoute =
                testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(outgoingCourierRoute, order);
        finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingCourierRoute), cell.getId());
        order = scOrderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void keepAndShipToWarehouseNotFullMultiPlaceOrder() {
        var order = testFactory.create(order(sortingCenter, "1").places("11", "12").build())
                .acceptPlaces("11").keepPlaces("11").setupClockPlusDays(1)
                .makeReturn().sortPlaces("11").ship().get();

        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void keepAndShipToWarehouseFullMultiPlaceOrder() {
        var order = testFactory.create(order(sortingCenter, "1").places("11", "12").build())
                .acceptPlaces("11", "12").keepPlaces("11", "12").setupClockPlusDays(1)
                .makeReturn().sortPlaces("11", "12").ship().get();

        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }

    @Test
    void simpleFullMultiPlaceOrderScenario() {
        var order = testFactory.createForToday(order(sortingCenter, "1").places("11", "12").build())
                .acceptPlaces("11", "12").sortPlaces("11", "12").ship().acceptPlaces("11", "12")
                .makeReturn().sortPlaces("11", "12").ship().get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }

    @Test
    void courierReturnedOnlyOnePlace() {
        if (useNewSortableFlow()) {
            testNotMigrated();
            return;
        }
        var order = testFactory.createForToday(order(sortingCenter, "1").places("11", "12").build())
                .acceptPlaces("11", "12").sortPlaces("11", "12").ship().acceptPlaces("11")
                .makeReturn().sortPlaces("11").ship().get();
        assertThat(order.getOrderStatus()).isEqualTo(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN);
        transactionTemplate.execute(ts -> {
            List<Place> places = testFactory.orderPlaces(order);
            assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.RETURNED);
            assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
            assertThatPlaceHasHistoryItems(places.get(0), PlaceStatus.SHIPPED, 1);
            assertThatPlaceHasHistoryItems(places.get(0), PlaceStatus.RETURNED, 1);
            assertThatPlaceHasHistoryItems(places.get(1), PlaceStatus.SHIPPED, 1);
            assertThatPlaceHasHistoryItems(places.get(1), PlaceStatus.RETURNED, 0);
            return null;
        });
    }

    private void assertThatPlaceHasHistoryItems(Place place, PlaceStatus status, int cnt) {
        assertThat(
                place.getHistory().stream()
                        .filter(h -> h.getMutableState().getPlaceStatus() == status)
                        .count()
        ).isEqualTo(cnt);
    }

    @Test
    void finishWarehouseRouteByCellWithNotAcceptedPlace() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .cancel().accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").places("21", "22").build())
                .cancel().acceptPlaces("21").sortPlaces("21").get();

        var outgoingWarehouseRoute = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        var cell1 = testFactory.determineRouteCell(outgoingWarehouseRoute, order1);

        finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingWarehouseRoute), cell1.getId());

        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);

        List<Place> places = testFactory.orderPlaces(order2);
        places.sort(Comparator.comparing(Place::getMainPartnerCode));
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void finishWarehouseRouteByCellWithSortedMultiPlaceOrder() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .cancel().accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").places("21", "22").build())
                .cancel().acceptPlaces("21", "22").sortPlaces("21", "22").get();

        var outgoingWarehouseRoute = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        var cell1 = testFactory.determineRouteCell(outgoingWarehouseRoute, order1);

        finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingWarehouseRoute), cell1.getId());

        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);

        List<Place> places = testFactory.orderPlaces(order2);
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }

    @Test
    void finishWarehouseRouteByCellWithNoSortedPlaces() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .cancel().accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").places("21", "22").build())
                .cancel().acceptPlaces("21", "22").get();

        var outgoingWarehouseRoute = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);
        var cell1 = testFactory.determineRouteCell(outgoingWarehouseRoute, order1);

        finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingWarehouseRoute), cell1.getId());

        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        List<Place> places = testFactory.orderPlaces(order2);
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void finishWarehouseRouteByCellWithNotSortedPlace() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .cancel().accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").places("21", "22").build())
                .cancel().acceptPlaces("21", "22").sortPlaces("21").get();

        var outgoingWarehouseRoute = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        var cell1 = testFactory.determineRouteCell(outgoingWarehouseRoute, order1);

        finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingWarehouseRoute), cell1.getId());

        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        List<Place> places = testFactory.orderPlaces(order2);
        places.sort(Comparator.comparing(Place::getMainPartnerCode));
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.RETURNED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
    }

    @Test
    void finishWarehouseRouteByCellWithOnePlaceInCourierCell() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .cancel().accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").places("21", "22").build())
                .acceptPlaces("21", "22").makeReturn().sortPlaces("22").get();

        var outgoingWarehouseRoute = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        var cell1 = testFactory.determineRouteCell(outgoingWarehouseRoute, order1);

        finishRouteFromCell(testFactory.getRouteIdForSortableFlow(outgoingWarehouseRoute), cell1.getId());

        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE);

        List<Place> places = testFactory.orderPlaces(order2);
        places.sort(Comparator.comparing(Place::getMainPartnerCode));
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.ACCEPTED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.RETURNED);
    }

    @Test
    void finishCourierRouteByCellWithNotSortedMultiPlaceOrder() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").places("11", "12").build())
                .acceptPlaces("11").sortPlaces("11").get();

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        var cell1 = testFactory.determineRouteCell(outgoingCourierRoute, order1);

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);

        assertThatThrownBy(() -> finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingCourierRoute), cell1.getId()))
                .isInstanceOf(ScException.class);
    }

    @Test
    void finishCourierRouteByCellWithSortedMultiPlaceOrder() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").places("11", "12").build())
                .acceptPlaces("11", "12").sortPlaces("11", "12").get();

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(order1).orElseThrow();

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        var cell1 = testFactory.determineRouteCell(outgoingCourierRoute, order1);

        finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingCourierRoute), cell1.getId());

        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);

        List<Place> places = testFactory.orderPlaces(order2);
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.SHIPPED);
    }

    @Test
    void finishByCellAndSinglePlainOrderWithNotSortedMultiPlaceOrder() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").places("11", "12").build())
                .acceptPlaces("11").sortPlaces("11").get();

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(order1).orElseThrow();

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
        var cell1 = testFactory.determineRouteCell(outgoingCourierRoute, order1);

        finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingCourierRoute), cell1.getId(), order1.getExternalId());

        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);

        List<Place> places = testFactory.orderPlaces(order2);
        assertThat(places.get(0).getStatus()).isEqualTo(PlaceStatus.SORTED);
        assertThat(places.get(1).getStatus()).isEqualTo(PlaceStatus.CREATED);
    }

    @Test
    void finishByCellAndSingleMultiPlaceOrderWithNotSortedMultiPlaceOrder() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").places("11", "12").build())
                .acceptPlaces("11").sortPlaces("11").get();

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(order2).orElseThrow();

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE);
        var cell2 = testFactory.determineRouteCell(outgoingCourierRoute, order2);

        assertThatThrownBy(() -> finishRouteFromCell(
                tf.getRouteIdForSortableFlow(outgoingCourierRoute), cell2.getId(), order2.getExternalId()
        )).isInstanceOf(ScException.class);
    }

    @Test
    void finishWithSingleOrder() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").build())
                .accept().sort().get();

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        var cell1 = testFactory.determineRouteCell(outgoingCourierRoute, order1);

        finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingCourierRoute), cell1.getId(), order1.getExternalId());

        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());

        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF);
    }

    @Test
    void finishRouteWithoutCell() {
        var order = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().get();
        AtomicLong cellId = new AtomicLong();
        transactionTemplate.execute(ts -> {
            var outgoingCourierRoute =
                    testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
            var cell = testFactory.determineRouteCell(outgoingCourierRoute, order);
            cellId.set(Objects.requireNonNull(cell).getId());
            outgoingCourierRoute.removeAllCells();
            if (SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
                getRouteSo(outgoingCourierRoute).removeAllCells();
            }
            return null;
        });
        var outgoingCourierRoute =
                testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        assertThatThrownBy(() -> finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingCourierRoute), cellId.get()))
                .isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void finishRouteWrongCell() {
        var order = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().get();
        var outgoingCourierRoute =
                testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        assertThatThrownBy(() -> finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingCourierRoute),
                testFactory.storedCell(sortingCenter, "wrong_cell", CellType.COURIER).getId()
        )).isInstanceOf(ScException.class);
    }

    @Test
    void shipCellWithOrderFromAnotherRoute() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().get();
        var order2 = testFactory.create(order(sortingCenter, "2").build())
                .updateCourier(testFactory.storedCourier(777)).updateShipmentDate(LocalDate.now(clock))
                .accept().sort().get();
        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(order1).orElseThrow();
        var cell1 = testFactory.determineRouteCell(outgoingCourierRoute, order1);

        jdbcTemplate.update("UPDATE place SET cell_id = ? WHERE order_id = ?",
                cell1.getId(), order2.getId());

        assertThatThrownBy(() -> finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingCourierRoute), cell1.getId()))
                .isInstanceOf(ScException.class);
    }

    @Disabled
    @Test
    void finishReturnRouteWithAnyCell() {
        var cell1 = testFactory.storedCell(sortingCenter, "11", CellType.RETURN);
        var cell2 = testFactory.storedCell(sortingCenter, "12", CellType.RETURN);
        var order1 = testFactory.create(order(sortingCenter, "1").build())
                .accept().keep().makeReturn().sort(cell1.getId()).get();
        var order2 = testFactory.create(order(sortingCenter, "2").build())
                .accept().keep().makeReturn().sort(cell2.getId()).get();
        var route = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();
        assertThat(route).isEqualTo(testFactory.findOutgoingWarehouseRoute(order2).orElseThrow());
        finishRouteFromCell(testFactory.getRouteIdForSortableFlow(route), cell1.getId());
        assertThat(
                route.getFinalRouteFinishOrders().stream()
                        .map(RouteFinishOrder::getExternalId)
                        .toList()
        ).isEqualTo(List.of(order1.getExternalId()));

        finishRouteFromCell(testFactory.getRouteIdForSortableFlow(route), cell2.getId());
        assertThat(
                route.getFinalRouteFinishOrders().stream()
                        .map(RouteFinishOrder::getExternalId)
                        .toList()
        ).isEqualTo(List.of(order1.getExternalId(), order2.getExternalId()));
    }

    @Test
    void changeOutgoingCourierNewCourier() {
        var request = routeCreateRequest(RouteType.OUTGOING_COURIER);
        var route = createRouteIfNotExistsAndSelect(request);
        testFactory.create(order(sortingCenter, "1").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock))
                .accept().sort().get();

        var newPartnerCourier = new PartnerCourierDto(321L, "Петр Петрович", new CourierCompany("Рога и копыта"));
        assertThat(courierRepository.findById(newPartnerCourier.getId())).isEmpty();

        routeCommandService.changeOutgoingCourierForExistingRoute(route.getId(), newPartnerCourier,
                testFactory.getOrCreateAnyUser(sortingCenter));
        //Нужно запустить также метод для коробок, что бы проверка расхождения маршрутов не выдала ошибку
        if (useNewRouteSoStage1()) {
            routeSoCommandService.changeOutgoingCourierForExistingRoute(getRouteSo(route).getId(), newPartnerCourier,
                    testFactory.getOrCreateAnyUser(sortingCenter));
        }

        var updatedRoute = routeRepository.findByIdOrThrow(route.getId()).allowReading();
        Courier newCourier = new Courier(
                321L, "Петр Петрович", null, null, null, "Рога и копыта", null
        );
        assertThat(updatedRoute.getCourierTo()).isEqualTo(newCourier);
        transactionTemplate.execute(ts -> {
            List<Place> updatedPlaces = orderNonBlockingQueryService.getPlaces(updatedRoute);
            assertThat(updatedPlaces.stream().map(Place::getCourier).toList())
                    .containsOnly(newCourier);
            return null;
        });
    }


    @Test
    void changeOutgoingCourierExistingCourier() {
        var route = createRouteIfNotExistsAndSelect(RouteType.OUTGOING_COURIER).allowReading();

        List<Place> places = testFactory.create(order(sortingCenter, "1").build())
                .updateCourier(courier).updateShipmentDate(LocalDate.now(clock))
                .accept().sort().getPlacesList();

        transactionTemplate.execute(ts -> {
            var storedCourier = testFactory.storedCourier(321L);
            var existingPartnerCourier = new PartnerCourierDto(
                    storedCourier.getId(), storedCourier.getName(), new CourierCompany(storedCourier.getCompanyName()));
            assertThat(courierRepository.findById(storedCourier.getId())).isNotEmpty();

            routeCommandService.changeOutgoingCourierForExistingRoute(route.getId(), existingPartnerCourier,
                    testFactory.getOrCreateAnyUser(sortingCenter));
            if (useNewRouteSoStage1()) {
                //Нужно запустить также метод для коробок, что бы проверка расхождения маршрутов не выдала ошибку
                routeSoCommandService.changeOutgoingCourierForExistingRoute(getRouteSo(route).getId(),
                        existingPartnerCourier,
                        testFactory.getOrCreateAnyUser(sortingCenter));
                assertThat(places).isNotEmpty();
                Place place = placeRepository.findById(places.get(0).getId()).orElseThrow();
                Optional<Route> placeRoute = routeNonBlockingQueryService.findPlaceRoute(place,
                        RouteType.OUTGOING_COURIER);
                RouteSo routeSo = getRouteSo(placeRoute.orElseThrow());
                assertThat(place.getOutRoute()).isEqualTo(routeSo);
            }

            var updatedRoute = routeRepository.findByIdOrThrow(route.allowNextRead().getId()).allowReading();
            assertThat(updatedRoute.getCourierTo()).isEqualTo(storedCourier);
            List<Place> updatedPlaces = orderNonBlockingQueryService.getPlaces(updatedRoute);
            assertThat(updatedPlaces.stream().map(Place::getCourier).toList())
                    .containsOnly(storedCourier);
            return null;
        });
    }

    @NotNull
    public RouteSo getRouteSo(Route route) {
        return testFactory.getRouteSo(route);
    }

    @Test
    void shipReturnRouteFromTwoDesignatedCells() {
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN, "wh1");
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN, "wh1");
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("o1").warehouseReturnId("wh1").build())
                .cancel().accept().sort(cell1.getId()).get();
        var order2 = testFactory.createForToday(order(sortingCenter).externalId("o2").warehouseReturnId("wh1").build())
                .cancel().accept().sort(cell2.getId()).get();
        var route = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();
        finishRouteFromCell(testFactory.getRouteIdForSortableFlow(route), cell1.getId());
        finishRouteFromCell(testFactory.getRouteIdForSortableFlow(route), cell2.getId());
        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());
        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);

        assertThat(routeNonBlockingQueryService.findWithRouteFinish(route.allowNextRead().getId())
                                                                    .allowReading().getFinalRouteFinishOrders())
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt", "routeFinish")
                .isEqualTo(List.of(
                        new RouteFinishOrder(order1.getId(),
                                order1.getExternalId(),
                                order1.getFfStatus(),
                                cell1.getId()),
                        new RouteFinishOrder(order2.getId(),
                                order2.getExternalId(),
                                order2.getFfStatus(),
                                cell2.getId())
                ));
    }

    @Test
    void shipReturnRouteFromTwoDesignatedCellsUsingCellIds() {
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN, "wh1");
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.RETURN, "wh1");
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("o1").warehouseReturnId("wh1").build())
                .cancel().accept().sort(cell1.getId()).get();
        var order2 = testFactory.createForToday(order(sortingCenter).externalId("o2").warehouseReturnId("wh1").build())
                .cancel().accept().sort(cell2.getId()).get();
        var route = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow();
        finishRouteFromCell(testFactory.getRouteIdForSortableFlow(route), List.of(cell1.getId(), cell2.getId()));
        order1 = scOrderRepository.findByIdOrThrow(order1.getId());
        order2 = scOrderRepository.findByIdOrThrow(order2.getId());
        assertThat(order1.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
        assertThat(order2.getOrderStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);

        var actual = new HashSet<>(routeNonBlockingQueryService.findWithRouteFinish(route.allowNextRead().getId())
                                                                    .allowReading().getFinalRouteFinishOrders());
        assertThat(actual)
                .isEqualTo(Set.of(
                        new RouteFinishOrder(order1.getId(), order1.getExternalId(),
                                order1.getFfStatus(), cell1.getId()),
                        new RouteFinishOrder(order2.getId(), order2.getExternalId(),
                                order2.getFfStatus(), cell2.getId())
                ));
    }

    @DisplayName("Для клиентских возвратов должна создаваться ячейка типа Возврат с подтипом Клиентский возврат")
    @Test
    void clientReturnInClientReturnCellSubtype() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCell(sortingCenter, "buffer-1", CellType.BUFFER);

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";
        var order = testFactory.createClientReturnForToday(
                        sortingCenter.getId(),
                        sortingCenter.getToken(),
                        sortingCenter.getYandexId(),
                        courierDto,
                        clientReturnLocker
                )
                .accept().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        List<Cell> routeCells = route.getCells(LocalDate.now(clock));
        assertThat(routeCells.size()).isEqualTo(1);
        assertThat(routeCells.get(0).getType()).isEqualTo(CellType.RETURN);
        assertThat(routeCells.get(0).getSubtype()).isEqualTo(CellSubType.CLIENT_RETURN);
    }

    @DisplayName("Для клиентских возвратов выбираться ячейка типа Возврат с подтипом Клиентский возврат")
    @Test
    void clientReturnInClientReturnCellSubtypeWhenCellExist() {
        var warehouse = testFactory.storedWarehouse(
                ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId());
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCell(sortingCenter, "buffer-1", CellType.BUFFER);
        Cell clientReturnCell = testFactory
                .storedCell(sortingCenter, "client-return-1", CellType.RETURN, CellSubType.CLIENT_RETURN,
                        warehouse.getYandexId());

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";
        var order = testFactory.createClientReturnForToday(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker
        ).accept().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        List<Cell> routeCells = route.getCells(LocalDate.now(clock));
        assertThat(routeCells.size()).isEqualTo(1);
        assertThat(routeCells.get(0)).isEqualTo(clientReturnCell);
    }

    @DisplayName("Для клиентских возвратов создается ячейка типа Возврат с подтипом Клиентский возврат когда маршрут " +
            "уже создан")
    @Test
    void clientReturnInClientReturnCellSubtypeWhenRouteExists() {
        var warehouse = testFactory.storedWarehouse(
                ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId()
        );
        testFactory.storedFakeReturnDeliveryService();
        testFactory.storedCell(sortingCenter, "buffer-1", CellType.BUFFER);
        Cell clientReturnCell = testFactory
                .storedCell(sortingCenter, "client-return-1", CellType.RETURN, CellSubType.CLIENT_RETURN,
                        warehouse.getYandexId());

        Long courierId = 321L;
        var courierDto = new CourierDto(courierId, "Курьер с возратами", null);
        String clientReturnLocker = "VOZVRAT_SF_PS_1234";
        testFactory.create(order(sortingCenter)
                        .externalId("new order")
                        .shipmentDate(LocalDate.now(clock))
                        .warehouseReturnId(ClientReturnBarcodePrefix.CLIENT_RETURN_PS.getWarehouseReturnId())
                        .build())
                .updateCourier(courier)
                .accept().sort().ship().makeReturn()
                .accept().get();

        var order = testFactory.createClientReturnForToday(
                sortingCenter.getId(),
                sortingCenter.getToken(),
                sortingCenter.getYandexId(),
                courierDto,
                clientReturnLocker
        ).accept().get();
        var route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        List<Cell> routeCells = route.getCells(LocalDate.now(clock));
        assertThat(routeCells.size()).isEqualTo(2);
        assertThat(routeCells.stream().filter(cell -> CellSubType.CLIENT_RETURN.equals(cell.getSubtype())).count())
                .isEqualTo(1);
        assertThat(
                routeCells.stream()
                        .filter(cell -> CellSubType.CLIENT_RETURN.equals(cell.getSubtype()))
                        .findFirst().orElseThrow())
                .isEqualTo(clientReturnCell);
        assertThat(routeCells.stream().filter(cell -> CellSubType.DEFAULT.equals(cell.getSubtype())).count())
                .isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = RouteType.class, names = {"INCOMING_WAREHOUSE", "OUTGOING_WAREHOUSE", "INCOMING_COURIER"})
    void changeOutgoingCourierNotSuitableRouteType(RouteType routeType) {

        var route = createRouteIfNotExistsAndSelect(routeType);
        var newPartnerCourier = new PartnerCourierDto(321L, "Петр Петрович", new CourierCompany("Рога и копыта"));
        assertThatThrownBy(() -> routeCommandService.changeOutgoingCourierForExistingRoute(route.allowNextRead().getId(),
                newPartnerCourier,
                testFactory.getOrCreateAnyUser(sortingCenter)))
                .isInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void cleanRoutesCell() {
        var route = createRouteIfNotExistsAndSelect(
                routeCreateRequest(RouteType.OUTGOING_COURIER, testFactory.storedCourier())
        );
        assertThat(route.allowReading().getCells(LocalDate.now(clock))).isNotEmpty();
        routeCommandService.cleanRoutesCell(List.of(route.getId()));

        //Запускаем метод для routeSo
        RouteSo routeSo = getRouteSo(route);
        routeSoCommandService.unbindCellFromRoutes(List.of(routeSo.getId()));

        route = routeRepository.findByIdOrThrow(route.getId());
        assertThat(route.allowNextRead().getCells(LocalDate.now(clock))).isEmpty();
    }

    @Test
    void cleanRoutesCellAndDisableCellDistribution() {
        var route = createRouteIfNotExistsAndSelect(
                routeCreateRequest(RouteType.OUTGOING_COURIER, testFactory.storedCourier())
        );
        assertThat(route.getCells(LocalDate.now(clock))).isNotEmpty();
        assertThat(route.getCellDistributionDisabledAt()).isNull();
        routeCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(route.getId()));
        //Нужно запустить для маршрутов СО
        routeSoCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(getRouteSo(route).getId()));

        var actualRoute = routeRepository.findByIdOrThrow(route.getId()).allowReading();
        assertThat(actualRoute.getCells(LocalDate.now(clock))).isEmpty();
        assertThat(actualRoute.getCellDistributionDisabledAt()).isNotNull();
    }

    @Test
    @DisplayName("Отгрузка всех лотов по курьерскому маршруту")
    void shipMultipleLotsToCourier() {
        User user = testFactory.getOrCreateStoredUser(sortingCenter);
        var p1 = testFactory.createForToday(order(sortingCenter).externalId("o1").build()).accept().sort().getPlace();
        var p2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build()).accept().sort().getPlace();
        var p3 = testFactory.createForToday(order(sortingCenter).externalId("o3").build()).accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(p1).orElseThrow();
        var l1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, p1.getCell());
        var l2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, p2.getCell());
        var l3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, p3.getCell());
        testFactory.sortPlaceToLot(p1, l1, user);
        testFactory.sortPlaceToLot(p2, l2, user);
        testFactory.sortPlaceToLot(p3, l3, user);
        l1 = testFactory.prepareToShipLot(l1);
        l2 = testFactory.prepareToShipLot(l2);
        l3 = testFactory.prepareToShipLot(l3);
        testFactory.shipLots(testFactory.getRouteIdForSortableFlow(route), sortingCenter);

        checkLotWithPlacesShipped(l1, p1.getOrder());
        checkLotWithPlacesShipped(l2, p2.getOrder());
        checkLotWithPlacesShipped(l3, p3.getOrder());
        checkRouteHasShippedOrder(tf.convertToRouteSoIfneeded(route), p1.getOrderId(), p2.getOrderId(), p3.getOrderId());
    }

    @Test
    @DisplayName("Отгрузка одного лота с полным многоместным заказом по курьерскому маршруту")
    void shipSingleLotWithFullMultiplaceOrderToCourier() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var order = testFactory.create(order(sortingCenter)
                        .externalId("o2")
                        .places("1", "2")
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(testFactory.storedCourier())
                .acceptPlaces("1", "2").sortPlaces("1", "2").getPlaces();
        Place place1 = order.get("1");
        Place place2 = order.get("2");

        var route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, place1.getCell());
        testFactory.sortPlaceToLot(place1, lot, user);
        testFactory.sortPlaceToLot(place2, lot, user);
        lot = testFactory.prepareToShipLot(lot);
        lot = testFactory.shipLotRouteByParentCell(lot);

        checkLotWithPlacesShipped(lot, place1.getOrder());
        checkRouteHasShippedOrder(tf.convertToRouteSoIfneeded(route), place1.getOrderId());
    }


    @Test
    @DisplayName("Отгрузка двух лотов (одноместный заказ) последовательно")
    void shipTwoLotsSequentially() {
        sortingCenter = testFactory.storedSortingCenter(13);
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.storedUser(sortingCenter, 120000000000000013L, UserRole.ADMIN);
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var place1 = testFactory.create(order(sortingCenter)
                        .externalId("o1")
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().getPlace();
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, place1.getCell());

        var place2 = testFactory.create(order(sortingCenter)
                        .externalId("o2")
                        .shipmentDate(LocalDate.now(clock))
                        .build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().getPlace();
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, place2.getCell());
        var route = testFactory.findOutgoingCourierRoute(place1).orElseThrow();

        testFactory.sortPlaceToLot(place1, lot1, user);
        testFactory.prepareToShipLot(lot1);
        lot1 = testFactory.shipLotRouteByParentCell(lot1);

        testFactory.sortPlaceToLot(place2, lot2, user);
        testFactory.prepareToShipLot(lot2);
        lot2 = testFactory.shipLotRouteByParentCell(lot2);

        checkLotWithPlacesShipped(lot1, place1.getOrder());
        checkLotWithPlacesShipped(lot2, place2.getOrder());
        checkRouteHasShippedOrder(tf.convertToRouteSoIfneeded(route), place1.getOrderId(), place2.getOrderId());

    }

    @Test
    @DisplayName("Последовательная отгрузка сначала лот, затем заказ")
    void shipLotAndOrderSequentially() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var place1 = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place1).orElseThrow().allowReading();
        route = testFactory.getRoute(route.getId()).allowReading();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, place1.getCell());
        testFactory.sortPlaceToLot(place1, lot, user);
        lot = testFactory.prepareToShipLot(lot);
        lot = testFactory.shipLotRouteByParentCell(lot);

        var place2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build())
                .accept().sort().ship().getPlace();

        checkLotWithPlacesShipped(lot, place1.getOrder());
        checkRouteHasShippedOrder(tf.convertToRouteSoIfneeded(route), place1.getOrderId());

        assertThat(place2.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(place2.getSortableStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        assertThat(place2.getCell()).isNull();
        assertThat(place2.getParent()).isNull();
        assertThat(
                route.getAllRouteFinishOrders().stream()
                        .filter(r -> r.getOrderId() == place2.getOrderId())
                        .allMatch(o -> Objects.equals(
                                ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, o.getFinishedOrderFfStatus()))
        ).isTrue();
    }

    @Test
    @DisplayName("Последовательная отгрузка сначала заказ, затем лот")
    void shipOrderAndLotSequentially() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var place1 = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().ship().getPlace();
        var place2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build())
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place2).orElseThrow();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, place2.getCell());
        testFactory.sortOrderToLot(place2.getOrder(), lot, user);
        lot = testFactory.prepareToShipLot(lot);
        lot = testFactory.shipLotRouteByParentCell(lot);

        assertThat(place1.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(place1.getCell()).isNull();
        assertThat(place1.getLot()).isNull();
        checkLotWithPlacesShipped(lot, place2.getOrder());
        checkRouteHasShippedOrder(tf.convertToRouteSoIfneeded(route), place1.getOrderId(), place2.getOrderId());
    }

    @Test
    @DisplayName("Ошибка при отгрузке лота другого маршрута")
    void cantShipLotByWrongRoute() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var place1 = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().getPlace();
        var lot1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, place1.getCell());
        testFactory.sortPlaceToLot(place1, lot1, user);
        testFactory.prepareToShipLot(lot1);
        var route1 = testFactory.findOutgoingCourierRoute(place1).orElseThrow();

        var place2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build())
                .updateCourier(testFactory.storedCourier(2))
                .accept().sort().getPlace();
        var lot2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, place2.getCell());
        testFactory.sortPlaceToLot(place2, lot2, user);
        testFactory.prepareToShipLot(lot2);

        assertThatThrownBy(() -> testFactory.shipLotWithFilter(tf.getRouteIdForSortableFlow(route1), lot2))
                .isInstanceOf(ScException.class)
                .hasMessage(ScErrorCode.LOT_CANT_FIND.getMessage());
    }

    @Test
    void shipSingleLotToCourier() {
        var user = testFactory.storedUser(sortingCenter, 1L);
        var place = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingCourierRoute(place).orElseThrow();
        var lot = testFactory.storedLot(sortingCenter, SortableType.PALLET, place.getCell());

        place = testFactory.sortPlaceToLot(place, lot, user);

        assertThat(place.getCell()).isNull();
        assertThat(place.getParent()).isNotNull();
        lot = testFactory.prepareToShipLot(lot);
        lot = testFactory.shipLotRouteByParentCell(lot);

        checkLotWithPlacesShipped(lot, place.getOrder());
        checkRouteHasShippedOrder(tf.convertToRouteSoIfneeded(route), place.getOrderId());
    }

    private void checkLotWithPlacesShipped(SortableLot lot, ScOrder order) {
        lot = testFactory.getLot(lot.getLotId());
        List<Place> places = testFactory.orderPlaces(order);
        assertThat(lot.getOptLotStatus()).isEmpty();
        assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        assertThat(places.get(0).getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);

        assertThat(places).allMatch(place -> place.getStatus().equals(PlaceStatus.SHIPPED));
        assertThat(places).allMatch(place -> place.getCell() == null);
        assertThat(places).allMatch(place -> place.getParent() == null);
    }


    private void checkRouteHasShippedOrder(Routable routable, Long... orderIds) {
        List<RouteFinishOrder> allRouteFinishOrders = transactionTemplate.execute( t -> {

            var updRouteable = routeSoMigrationHelper.getRoutable(routable.getId(), routable.getSortingCenter());
            return updRouteable.getAllRouteFinishOrders();
        });

        List<Long> actualRouteFinish = StreamEx.of(allRouteFinishOrders)
                .map(RouteFinishOrder::getOrderId)
                .filter(orderId -> Arrays.asList(orderIds).contains(orderId))
                .toList();



        assertThat(actualRouteFinish).hasSize(orderIds.length);
        assertThat(actualRouteFinish).isEqualTo(List.of(orderIds));
        assertThat(
                allRouteFinishOrders.stream()
                        .allMatch(o -> Objects.equals(
                                ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, o.getFinishedOrderFfStatus()))
        ).isTrue();
    }

    @Test
    @DisplayName("Проверка записи штрих-кода машины при отгрузке лотов")
    void saveCarBarcodeWhenShippingLots() {
        // TODO покрыть после реализации
        //  функционала сортировки в лот на отгрузку
    }

    @Test
    void exceptionWhenShipLotsAnotherCell() {
        order = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .updateCourier(testFactory.storedCourier())
                .accept().sort().get();
        Route route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        cell = testFactory.determineRouteCell(route, order);
        testFactory.storedLot(sortingCenter, cell, LotStatus.READY);

        Courier courier2 = testFactory.storedCourier(123);
        var route2 = createRouteIfNotExistsAndSelect(
                routeCreateRequest(RouteType.OUTGOING_COURIER, courier2)
        );
        var order2 = testFactory.createForToday(order(sortingCenter).externalId("2").build())
                .updateCourier(courier2)
                .accept().sort().get();
        var cell2 = testFactory.determineRouteCell(route2, order2);
        SortableLot lot2 = testFactory.storedLot(sortingCenter, cell2, LotStatus.READY);

        ThrowableAssert.ThrowingCallable callable = () -> {
            routeCommandService.finishRouteWithLots(testFactory.getRouteIdForSortableFlow(route), "<car-barcode>", Set.of(lot2),
                    new ScContext(dispatcher), sortingCenter);
        };
        assertThatThrownBy(callable)
                .isInstanceOf(ScException.class)
                .hasMessage(ScErrorCode.LOT_CANT_FIND.getMessage());
    }

    @Test
    void dontThrowExceptionWhenNoLotsToShip() {
        order = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .cancel()
                .accept().sort().get();
        Route route =
                testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        route.allowNextRead();
        assertThatCode(() -> routeCommandService.finishRouteWithLots(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter))
                .doesNotThrowAnyException();
    }

    @Test
    void shipOnlyReadyLot() {
        order = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .cancel()
                .accept().sort().get();
        Route route =
                testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        cell = testFactory.determineRouteCell(route, order);
        var lotReady = testFactory.storedLot(sortingCenter, cell, LotStatus.PROCESSING);
        testFactory.sortOrderToLot(order, lotReady, dispatcher);
        testFactory.prepareToShipLot(lotReady);
        var lotCreated = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        var lotProcessing = testFactory.storedLot(sortingCenter, cell, LotStatus.PROCESSING);

        assertThatCode(() -> routeCommandService.finishRouteWithLots(testFactory.getRouteIdForSortableFlow(route), null, sortingCenter))
                .doesNotThrowAnyException();

        var lotReady1 = testFactory.getLot(lotReady.getLotId());
        assertThat(lotReady1.getOptLotStatus()).isEmpty();
        assertThat(lotReady1.getStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);

        var lotCreated1 = testFactory.getLot(lotCreated.getLotId());
        assertThat(lotCreated1.getLotStatusOrNull()).isEqualTo(LotStatus.CREATED);

        var lotProcessing1 = testFactory.getLot(lotProcessing.getLotId());
        assertThat(lotProcessing1.getLotStatusOrNull()).isEqualTo(LotStatus.PROCESSING);
    }

    @Test
    void shipOnlyFilteredLot() {
        order = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .cancel()
                .accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter).externalId("2").build())
                .cancel()
                .accept().sort().get();
        Route route =
                testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        cell = testFactory.determineRouteCell(route, order);

        var lotReady1 = testFactory.storedLot(sortingCenter, cell, LotStatus.PROCESSING);
        testFactory.sortOrderToLot(order, lotReady1, dispatcher);
        testFactory.prepareToShipLot(lotReady1);
        var lotReady2 = testFactory.storedLot(sortingCenter, cell, LotStatus.PROCESSING);
        testFactory.sortOrderToLot(order2, lotReady2, dispatcher);
        testFactory.prepareToShipLot(lotReady2);
        var lotCreated = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        var lotProcessing = testFactory.storedLot(sortingCenter, cell, LotStatus.PROCESSING);

        assertThatCode(() -> routeCommandService.finishRouteWithLots(testFactory.getRouteIdForSortableFlow(route), null, Set.of(lotReady1),
                new ScContext(dispatcher), sortingCenter))
                .doesNotThrowAnyException();

        var lotReadyAfter1 = testFactory.getLot(lotReady1.getLotId());
        assertThat(lotReadyAfter1.getOptLotStatus()).isEmpty();
        assertThat(lotReadyAfter1.getStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
        var lotReadyAfter2 = testFactory.getLot(lotReady2.getLotId());
        assertThat(lotReadyAfter2.getLotStatusOrNull()).isEqualTo(LotStatus.READY);

        var lotCreatedAfter = testFactory.getLot(lotCreated.getLotId());
        assertThat(lotCreatedAfter.getLotStatusOrNull()).isEqualTo(LotStatus.CREATED);

        var lotProcessingAfter = testFactory.getLot(lotProcessing.getLotId());
        assertThat(lotProcessingAfter.getLotStatusOrNull()).isEqualTo(LotStatus.PROCESSING);

        assertThatCode(() -> routeCommandService.finishRouteWithLots(testFactory.getRouteIdForSortableFlow(route), null, Set.of(lotReady2),
                new ScContext(dispatcher), sortingCenter))
                .doesNotThrowAnyException();
        var lotReadyAfter22 = testFactory.getLot(lotReady2.getLotId());
        assertThat(lotReadyAfter22.getOptLotStatus()).isEmpty();
        assertThat(lotReadyAfter22.getStatus()).isEqualTo(SortableStatus.SHIPPED_RETURN);
    }

    @Test
    void shipLotMultiplaceOrderShipSecondOrder() {
        order = testFactory.createForToday(order(sortingCenter).externalId("o1").places("p1", "p2").build())
                .updateCourier(testFactory.storedCourier())
                .cancel()
                .acceptPlaces().sortPlaces().get();
        List<Place> places = placeRepository.findAllByOrderIdInOrderById(List.of(order.getId()));
        Route route =
                testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        cell = testFactory.determineRouteCell(route, order);
        SortableLot lot = testFactory.storedLot(sortingCenter, cell, LotStatus.CREATED);
        var place0 = testFactory.sortPlaceToLot(places.get(0), lot, dispatcher);
        testFactory.prepareToShipLot(lot);
        testFactory.shipLotWithFilter(testFactory.getRouteIdForSortableFlow(route), lot);
        var order1 = scOrderRepository.findByIdOrThrow(order.getId());
        List<Place> places1 = placeRepository.findAllByOrderIdInOrderById(List.of(order.getId()));
        assertThat(order1.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM);
        assertThat(places1.stream().filter(p -> p.getId().equals(place0.getId())).findFirst().orElseThrow())
                .extracting(Place::getPlaceStatus).isEqualTo(PlaceStatus.RETURNED);
        assertThat(places1.stream().filter(p -> !p.getId().equals(place0.getId())).findFirst().orElseThrow())
                .extracting(Place::getPlaceStatus).isEqualTo(PlaceStatus.SORTED);
        var order2 = testFactory.shipOrderRoute(order);
        assertThat(order2.getFfStatus()).isEqualTo(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM);
    }

    @Test
    void assignCellToMiddleMileOutgoingCourier() {
        jdbcTemplate.update("delete from delivery_service_property where delivery_service_yandex_id = ?",
                deliveryService.getYandexId());
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.TYPE_ON_SC_PREFIX + sortingCenter.getId(), "TRANSIT");
        var request = routeCreateRequestMiddleMile(LocalDate.now(clock));
        var route = createRouteIfNotExistsAndSelect(request);
        assertThat(route.getCells(LocalDate.now(clock))).hasSize(1);
        assertThat(route.getCells(LocalDate.now(clock)).get(0).getCourierId()).isEqualTo(request.getCourier().getId());
    }

    @Test
    void assignCellOutgoingWarehouse() {
        var request = routeCreateRequest(RouteType.OUTGOING_WAREHOUSE);
        var route = createRouteIfNotExistsAndSelect(request);
        assertThat(route.allowReading().getCells(LocalDate.now(clock))).hasSize(1);
        assertThat(request.getWarehouse()).isNotNull();
        assertThat(route.getCells(LocalDate.now(clock)).get(0).getWarehouseYandexId()).isEqualTo(request.getWarehouse().getYandexId());
    }

    private Route createRouteIfNotExistsAndSelect(RouteType type) {
        var request = routeCreateRequest(type);
        return createRouteIfNotExistsAndSelect(request).allowReading();
    }

    private Route createRouteIfNotExistsAndSelect(RouteCreateRequest request) {
        routeCommandService.createRouteIfNotExistsAndSetCell(request);
        long routeId = routeCommandService.findRouteIdByRequest(request).orElseThrow();

        var destType =
                testFactory.routeTypeToRouteSoDestinationType(request.getRouteType());
        Long destinationId = switch (destType) {
            case WAREHOUSE -> request.getWarehouse().getId();
            case COURIER -> request.getCourier().getId();
            default -> throw new IllegalStateException("Unexpected value: " + destType);
        };
        routeSoCommandService.createRouteIfNotExistsAndSetCell(
                new RouteSoCreateRequest(
                        testFactory.routeTypeToRouteSoType(request.getRouteType()),
                        request.getSortingCenter().getId(),
                        testFactory.routeTypeToRouteSoDestinationType(request.getRouteType()),
                        destinationId,
                        ScDateUtils.toBeginningOfDay(request.getExpectedDate()),
                        ScDateUtils.toEndOfDay(request.getExpectedDate()),
                        null,
                        null,
                        null,
                        request.getExpectedDate(),
                        request.getSortInterval(),
                        request.getExpectedTime(),
                        request.getCellParams(),
                        request.getPreferCellId()



                )
        );

        return routeRepository.findByIdWithRouteFinish(routeId).orElseThrow()
                .allowReading(); //сам рут не передается в классы и используется только в тестах, поэтому можно читать
    }

    @Test
    void shipSinglePlaceToCourierOnLastMileThrowsScException() {
        var cell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var order = testFactory.createForToday(
                order(sortingCenter)
                        .externalId("o1")
                        .dsType(DeliveryServiceType.LAST_MILE_COURIER)
                        .places("p1", "p2")
                        .build()
        ).acceptPlace("p1").sortPlace("p1", cell.getId()).get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        assertThatThrownBy(() -> routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                testFactory.getRouteIdForSortableFlow(route), new ScContext(dispatcher), List.of(cell.getId()), null, "o1", "p1",
                null, false
        ))).isInstanceOf(ScException.class);
    }

    @Test
    @DisplayName("Маршрут уже отгружен")
    void exceptionWhenRouteShippedRetry() {
        var user = testFactory.getOrCreateStoredUser(sortingCenter);
        var order = testFactory.createForToday(
                        order(sortingCenter, "o123")
                                .places("1", "2", "3")
                                .dsType(DeliveryServiceType.TRANSIT)
                                .build())
                .acceptPlaces()
                .sortPlaces()
                .get();
        Place place = testFactory.anyOrderPlace(order);

        var route = testFactory.findOutgoingRoute(place).orElseThrow();
        Long cellId = place.getCellId().orElseThrow();
        var request = new RouteFinishByCellsRequest(testFactory.getRouteIdForSortableFlow(route), new ScContext(user), List.of(cellId), null, false);

        routeCommandService.finishOutgoingRouteWithCell(request);
        assertThatCode(() -> routeCommandService.finishOutgoingRouteWithCell(request))
                .doesNotThrowAnyException();
    }

    @Test
    void checkFinishRoutePlaceCell() {
        var order1 = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter, "2").places("11", "12").build())
                .acceptPlaces("11", "12").sortPlaces("11", "12").get();

        var outgoingCourierRoute = testFactory.findOutgoingCourierRoute(order1).orElseThrow().allowReading();

        var cell1 = testFactory.determineRouteCell(outgoingCourierRoute, order1);

        finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingCourierRoute), cell1.getId());

        transactionTemplate.execute(ts -> {
            var route = testFactory.getRoute(outgoingCourierRoute.getId());
            assertThat(
                    route.getAllRouteFinishPlaces().stream()
                            .allMatch(p -> Objects.equals(cell1.getId(), p.getCellId()))
            ).isTrue();
            return null;
        });
    }

    @Test
    void checkFinishRoutePlaceLotCellId() {
        order = testFactory.createForToday(order(sortingCenter).externalId("1").build())
                .cancel()
                .accept().sort().get();
        var order2 = testFactory.createForToday(order(sortingCenter).externalId("2").build())
                .cancel()
                .accept().sort().get();
        Route route =
                testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        cell = testFactory.determineRouteCell(route, order);

        var lotReady1 = testFactory.storedLot(sortingCenter, cell, LotStatus.PROCESSING);
        testFactory.sortOrderToLot(order, lotReady1, dispatcher);
        testFactory.prepareToShipLot(lotReady1);
        var lotReady2 = testFactory.storedLot(sortingCenter, cell, LotStatus.PROCESSING);
        testFactory.sortOrderToLot(order2, lotReady2, dispatcher);
        testFactory.prepareToShipLot(lotReady2);

        long routeId = testFactory.getRouteIdForSortableFlow(route);

        routeCommandService.finishRouteWithLots(routeId, null,
                Set.of(lotReady1, lotReady2), new ScContext(dispatcher), sortingCenter);

        transactionTemplate.execute(ts -> {
            route.allowNextRead();

            var routeAfterShip = testFactory.getRoute(route.getId());
            routeAfterShip.allowReading();
            assertThat(
                    routeAfterShip.getAllRouteFinishPlaces().stream()
                            .filter(p -> lotReady1.getLotId().equals(p.getLotId()))
                            .count()
            ).isEqualTo(1);
            assertThat(
                    routeAfterShip.getAllRouteFinishPlaces().stream()
                            .filter(p -> lotReady1.getLotId().equals(p.getLotId()))
                            .allMatch(p -> Objects.equals(lotReady1.getParentCellId(), p.getLotCellId()))
            ).isTrue();

            assertThat(
                    routeAfterShip.getAllRouteFinishPlaces().stream()
                            .filter(p -> lotReady2.getLotId().equals(p.getLotId()))
                            .count()
            ).isEqualTo(1);
            assertThat(
                    routeAfterShip.getAllRouteFinishPlaces().stream()
                            .filter(p -> lotReady2.getLotId().equals(p.getLotId()))
                            .allMatch(p -> Objects.equals(lotReady2.getParentCellId(), p.getLotCellId()))
            ).isTrue();
            route.revokeRouteReading();
            return null;
        });
    }

    @Test
    void checkSendOrderShippingOutboundEvent() {
        var externalId = "1";
        var fromTime = Instant.parse("2021-06-01T12:00:00Z");
        var toTime = Instant.parse("2021-06-01T15:00:00Z");
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.IS_DROPOFF, "true");
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.RETURN_OUTBOUND_ENABLED, "true");

        var order1 = testFactory.createForToday(order(sortingCenter).externalId(externalId).build())
                .cancel()
                .accept().sort().get();

        var orderLikeForRouteLookup = testFactory.getOrderLikeForRouteLookup(order1);
        var outgoingCourierRoute = testFactory.findOutgoingWarehouseRoute(orderLikeForRouteLookup).orElseThrow();

        MovementCourierRequest courierRequest = MovementCourierRequest.builder()
                .externalId("fffdjhwkh3j4jgkbc")
                .name("Courier name")
                .legalName("Courier legal name")
                .carNumber("О868АС198")
                .uid(212_85_06L)
                .phone("phone2345")
                .build();

        outboundCommandService.put(OutboundCreateRequest.builder()
                .externalId(externalId)
                .type(OutboundType.ORDERS_RETURN)
                .fromTime(fromTime)
                .toTime(toTime)
                .courierRequest(courierRequest)
                .locationCreateRequest(TestFactory.locationCreateRequest())
                .comment("some comment")
                .sortingCenter(sortingCenter)
                .type(OutboundType.ORDERS_RETURN)
                .logisticPointToExternalId(Objects.requireNonNull(outgoingCourierRoute.getWarehouseTo()).getYandexId())
                .build());

        var cell1 = testFactory.determineRouteCell(outgoingCourierRoute, order1);
        finishRouteFromCell(tf.getRouteIdForSortableFlow(outgoingCourierRoute), cell1.getId());

        RegistryOrder registryOrder = boundRegistryRepository.findByExternalId(externalId).orElseThrow();
        assertThat(registryOrder.getStatus()).isEqualTo(InboundRegistryOrderStatus.FIXED);

        Registry registry = registryRepository.findByIdOrThrow(registryOrder.getRegistryId());
        assertThat(registry.getType()).isEqualTo(RegistryType.FACTUAL_UNDELIVERED_ORDERS_RETURN);
        assertThat(registry.getOutbound().getType()).isEqualTo(OutboundType.ORDERS_RETURN);
        assertThat(registry.getOutbound().getStatus()).isEqualTo(OutboundStatus.CREATED);
        assertThat(registry.getOutbound().getFromTime()).isEqualTo(fromTime);
        assertThat(registry.getOutbound().getToTime()).isEqualTo(toTime);
    }

}
