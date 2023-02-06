package ru.yandex.market.sc.core.domain.route_so;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.place.model.InCourierRouteRequest;
import ru.yandex.market.sc.core.domain.place.model.InWarehouseRouteRequest;
import ru.yandex.market.sc.core.domain.place.model.OutCourierRouteRequest;
import ru.yandex.market.sc.core.domain.place.model.OutWarehouseRouteRequest;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType;
import ru.yandex.market.sc.core.domain.route_so.model.RouteType;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoRepository;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSite;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.route_so.RouteSoCommandService.CELL_RESERVE_MARGIN;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@Slf4j
@EmbeddedDbTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RouteSoCommandServiceTest {

    private final TestFactory testFactory;
    private final RouteSoCommandService routeSoCommandService;
    private final RouteSoRepository routeSoRepository;
    private final TransactionTemplate transactionTemplate;
    private final XDocFlow flow;
    private final RouteSoQueryService routeSoQueryService;

    private static final String INBOUND_ID = "inbound-1";
    private static final String OUTBOUND_ID = "outbound-1";

    @MockBean
    Clock clock;
    User user;
    SortingCenter sortingCenter;
    Warehouse warehouse;

    @BeforeEach
    void setUp() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 94350342);
        warehouse = testFactory.storedWarehouse();
        testFactory.setupMockClock(clock);
    }

    @Test
    void outCourierRouteSoIsNotRecreatedAtMoscowMidnight() {
        Courier courier = testFactory.storedCourier();
        Place place = testFactory.create(TestFactory.CreateOrderParams.builder().sortingCenter(sortingCenter).build())
                .getPlacesList().get(0);

        //тайм зона по умолчанию DateTimeUtil.DEFAULT_ZONE_ID +3MSK
        Instant midnight = Instant.parse("2020-02-02T00:00:00.00+03:00");
        Instant beforeMidnight = Instant.parse("2020-02-01T23:59:59.99+03:00");
        RouteSo routeAtMidnight = routeSoCommandService.getOutgoingRoute(
                new OutCourierRouteRequest(place, courier, midnight)
        );
        RouteSo routeAtMidnight2 = routeSoCommandService.getOutgoingRoute(
                new OutCourierRouteRequest(place, courier, midnight)
        );
        RouteSo routeBeforeMidnight = routeSoCommandService.getOutgoingRoute(
                new OutCourierRouteRequest(place, courier, beforeMidnight)
        );
        assertThat(routeAtMidnight).isNotNull();
        assertThat(routeAtMidnight2).isNotNull();
        assertThat(routeBeforeMidnight).isNotNull();
        assertThat(routeAtMidnight.getId()).isEqualTo(routeAtMidnight2.getId());
        assertThat(routeAtMidnight.getId()).isNotEqualTo(routeBeforeMidnight.getId());
    }

    @Test
    void inCourierRouteSoIsNotRecreatedAtMoscowMidnight() {
        Courier courier = testFactory.storedCourier();

        //тайм зона по умолчанию DateTimeUtil.DEFAULT_ZONE_ID +3MSK
        Instant midnight = Instant.parse("2020-02-02T00:00:00.00+03:00");
        Instant beforeMidnight = Instant.parse("2020-02-01T23:59:59.99+03:00");
        RouteSo routeAtMidnight = routeSoCommandService.getIncomingRoute(
                InCourierRouteRequest.builder()
                        .courier(courier)
                        .sortingCenter(sortingCenter)
                        .time(midnight)
                        .build()
        );
        RouteSo routeAtMidnight2 = routeSoCommandService.getIncomingRoute(
                InCourierRouteRequest.builder()
                        .courier(courier)
                        .sortingCenter(sortingCenter)
                        .time(midnight)
                        .build()
        );

        RouteSo routeBeforeMidnight = routeSoCommandService.getIncomingRoute(
                InCourierRouteRequest.builder()
                        .courier(courier)
                        .sortingCenter(sortingCenter)
                        .time(beforeMidnight)
                        .build()
        );
        assertThat(routeAtMidnight).isNotNull();
        assertThat(routeAtMidnight2).isNotNull();
        assertThat(routeBeforeMidnight).isNotNull();
        assertThat(routeAtMidnight.getId()).isEqualTo(routeAtMidnight2.getId());
        assertThat(routeAtMidnight.getId()).isNotEqualTo(routeBeforeMidnight.getId());
    }

    @Test
    void outWarehouseRouteSoIsNotRecreatedAtMoscowMidnight() {
        Warehouse warehouse = testFactory.storedWarehouse();
        Place place = testFactory.create(TestFactory.CreateOrderParams.builder().sortingCenter(sortingCenter).build())
                .getPlacesList().get(0);

        //тайм зона по умолчанию DateTimeUtil.DEFAULT_ZONE_ID +3MSK
        Instant midnight = Instant.parse("2020-02-02T00:00:00.00+03:00");
        Instant beforeMidnight = Instant.parse("2020-02-01T23:59:59.99+03:00");
        RouteSo routeAtMidnight = routeSoCommandService.getOutgoingRoute(
                new OutWarehouseRouteRequest(place, warehouse, midnight)
        );
        RouteSo routeAtMidnight2 = routeSoCommandService.getOutgoingRoute(
                new OutWarehouseRouteRequest(place, warehouse, midnight)

        );
        RouteSo routeBeforeMidnight = routeSoCommandService.getOutgoingRoute(
                new OutWarehouseRouteRequest(place, warehouse, beforeMidnight)
        );
        assertThat(routeAtMidnight).isNotNull();
        assertThat(routeAtMidnight2).isNotNull();
        assertThat(routeBeforeMidnight).isNotNull();
        assertThat(routeAtMidnight.getId()).isEqualTo(routeAtMidnight2.getId());
        assertThat(routeAtMidnight.getId()).isNotEqualTo(routeBeforeMidnight.getId());
    }

    @Test
    void inWarehouseRouteSoIsNotRecreatedAtMoscowMidnight() {
        Warehouse warehouse = testFactory.storedWarehouse();

        //тайм зона по умолчанию DateTimeUtil.DEFAULT_ZONE_ID +3MSK
        Instant midnight = Instant.parse("2020-02-02T00:00:00.00+03:00");
        Instant beforeMidnight = Instant.parse("2020-02-01T23:59:59.99+03:00");
        RouteSo routeAtMidnight = routeSoCommandService.getIncomingRoute(
                InWarehouseRouteRequest.builder()
                        .warehouse(warehouse)
                        .sortingCenter(sortingCenter)
                        .time(midnight)
                        .build()
        );
        RouteSo routeAtMidnight2 = routeSoCommandService.getIncomingRoute(
                InWarehouseRouteRequest.builder()
                        .warehouse(warehouse)
                        .sortingCenter(sortingCenter)
                        .time(midnight)
                        .build()
        );
        RouteSo routeBeforeMidnight = routeSoCommandService.getIncomingRoute(
                InWarehouseRouteRequest.builder()
                        .warehouse(warehouse)
                        .sortingCenter(sortingCenter)
                        .time(beforeMidnight)
                        .build()
        );
        assertThat(routeAtMidnight).isNotNull();
        assertThat(routeAtMidnight2).isNotNull();
        assertThat(routeBeforeMidnight).isNotNull();
        assertThat(routeAtMidnight.getId()).isEqualTo(routeAtMidnight2.getId());
        assertThat(routeAtMidnight.getId()).isNotEqualTo(routeBeforeMidnight.getId());
    }


    //UTC midnight

    @Test
    void outCourierRouteSoIsNotRecreatedAtUTCMidnight() {
        Courier courier = testFactory.storedCourier();
        Place place = testFactory.create(TestFactory.CreateOrderParams.builder().sortingCenter(sortingCenter).build())
                .getPlacesList().get(0);

        //тайм зона по умолчанию DateTimeUtil.DEFAULT_ZONE_ID +3MSK
        Instant midnight = Instant.parse("2020-02-02T00:00:00.00z");
        Instant beforeMidnight = Instant.parse("2020-02-01T23:59:59.99z");
        RouteSo routeAtMidnight = routeSoCommandService.getOutgoingRoute(
                new OutCourierRouteRequest(place, courier, midnight)
        );
        RouteSo routeAtMidnight2 = routeSoCommandService.getOutgoingRoute(
                new OutCourierRouteRequest(place, courier, midnight)
        );
        RouteSo routeBeforeMidnight = routeSoCommandService.getOutgoingRoute(
                new OutCourierRouteRequest(place, courier, beforeMidnight)
        );
        assertThat(routeAtMidnight).isNotNull();
        assertThat(routeAtMidnight2).isNotNull();
        assertThat(routeBeforeMidnight).isNotNull();
        assertThat(routeAtMidnight.getId()).isEqualTo(routeAtMidnight2.getId());
        assertThat(routeAtMidnight.getId()).isEqualTo(routeBeforeMidnight.getId());
    }

    @Test
    void inCourierRouteSoIsNotRecreatedAtUTCMidnight() {
        Courier courier = testFactory.storedCourier();

        //тайм зона по умолчанию DateTimeUtil.DEFAULT_ZONE_ID +3MSK
        Instant midnight = Instant.parse("2020-02-02T00:00:00.00z");
        Instant beforeMidnight = Instant.parse("2020-02-01T23:59:59.99z");
        RouteSo routeAtMidnight = routeSoCommandService.getIncomingRoute(
                InCourierRouteRequest.builder()
                        .courier(courier)
                        .sortingCenter(sortingCenter)
                        .time(midnight)
                        .build()
        );
        RouteSo routeAtMidnight2 = routeSoCommandService.getIncomingRoute(
                InCourierRouteRequest.builder()
                        .courier(courier)
                        .sortingCenter(sortingCenter)
                        .time(midnight)
                        .build()
        );

        RouteSo routeBeforeMidnight = routeSoCommandService.getIncomingRoute(
                InCourierRouteRequest.builder()
                        .courier(courier)
                        .sortingCenter(sortingCenter)
                        .time(beforeMidnight)
                        .build()
        );
        assertThat(routeAtMidnight).isNotNull();
        assertThat(routeAtMidnight2).isNotNull();
        assertThat(routeBeforeMidnight).isNotNull();
        assertThat(routeAtMidnight.getId()).isEqualTo(routeAtMidnight2.getId());
        assertThat(routeAtMidnight.getId()).isEqualTo(routeBeforeMidnight.getId());
    }

    @Test
    void outWarehouseRouteSoIsNotRecreatedAtUTCMidnight() {
        Warehouse warehouse = testFactory.storedWarehouse();
        Place place = testFactory.create(TestFactory.CreateOrderParams.builder().sortingCenter(sortingCenter).build())
                .getPlacesList().get(0);


        //тайм зона по умолчанию DateTimeUtil.DEFAULT_ZONE_ID +3MSK
        Instant midnight = Instant.parse("2020-02-02T00:00:00.00z");
        Instant beforeMidnight = Instant.parse("2020-02-01T23:59:59.99z");
        RouteSo routeAtMidnight = routeSoCommandService.getOutgoingRoute(
                new OutWarehouseRouteRequest(place, warehouse, midnight)
        );
        RouteSo routeAtMidnight2 = routeSoCommandService.getOutgoingRoute(
                new OutWarehouseRouteRequest(place, warehouse, midnight)
        );
        RouteSo routeBeforeMidnight = routeSoCommandService.getOutgoingRoute(
                new OutWarehouseRouteRequest(place, warehouse, beforeMidnight)
        );
        assertThat(routeAtMidnight).isNotNull();
        assertThat(routeAtMidnight2).isNotNull();
        assertThat(routeBeforeMidnight).isNotNull();
        assertThat(routeAtMidnight.getId()).isEqualTo(routeAtMidnight2.getId());
        assertThat(routeAtMidnight.getId()).isEqualTo(routeBeforeMidnight.getId());
    }


    @Test
    void inWarehouseRouteSoIsNotRecreatedAtUTCMidnight() {
        Warehouse warehouse = testFactory.storedWarehouse();

        //тайм зона по умолчанию DateTimeUtil.DEFAULT_ZONE_ID +3MSK
        Instant midnight = Instant.parse("2020-02-02T00:00:00.00z");
        Instant beforeMidnight = Instant.parse("2020-02-01T23:59:59.99z");
        RouteSo routeAtMidnight = routeSoCommandService.getIncomingRoute(
                InWarehouseRouteRequest.builder()
                        .warehouse(warehouse)
                        .sortingCenter(sortingCenter)
                        .time(midnight)
                        .build()
        );
        RouteSo routeAtMidnight2 = routeSoCommandService.getIncomingRoute(
                InWarehouseRouteRequest.builder()
                        .warehouse(warehouse)
                        .sortingCenter(sortingCenter)
                        .time(midnight)
                        .build()
        );
        RouteSo routeBeforeMidnight = routeSoCommandService.getIncomingRoute(
                InWarehouseRouteRequest.builder()
                        .warehouse(warehouse)
                        .sortingCenter(sortingCenter)
                        .time(beforeMidnight)
                        .build()
        );
        assertThat(routeAtMidnight).isNotNull();
        assertThat(routeAtMidnight2).isNotNull();
        assertThat(routeBeforeMidnight).isNotNull();
        assertThat(routeAtMidnight.getId()).isEqualTo(routeAtMidnight2.getId());
        assertThat(routeAtMidnight.getId()).isEqualTo(routeBeforeMidnight.getId());
    }


    @Test
    @DisplayName("Проверка создание входящего маршрута из склада" +
            " при использовании конструктора маршрута для созданной коробки")
    @Disabled
    void incomingRouteSoCreatedIfPlaceCreated2() {
        ScOrder scOrder = testFactory.createForToday(order(sortingCenter).build()).get();
        Place place = testFactory.orderPlace(scOrder);

        RouteSo incomingWarehouseRoute = place.getInRoute();
        assertThat(incomingWarehouseRoute).isNotNull();
        assertThat(incomingWarehouseRoute.getType()).isEqualTo(RouteType.IN_DIRECT);
        assertThat(incomingWarehouseRoute.getDestinationId()).isEqualTo(Objects.requireNonNull(place.getWarehouseFrom()).getId());
        assertThat(incomingWarehouseRoute.getDestinationType()).isEqualTo(RouteDestinationType.WAREHOUSE);
        routeSoRepository.deleteAll();


        assertThat(incomingWarehouseRoute).isNotNull();
        assertThat(incomingWarehouseRoute.getType()).isEqualTo(RouteType.IN_DIRECT);
        assertThat(incomingWarehouseRoute.getDestinationId()).isEqualTo(Objects.requireNonNull(place.getWarehouseFrom()).getId());
        assertThat(incomingWarehouseRoute.getDestinationType()).isEqualTo(RouteDestinationType.WAREHOUSE);
    }

    /////
    /////
    /////
    /////       Старые тесты
    /////
    /////
    /////

    @Test
    void createRouteSoAndSetCell() {
        var outbound = testFactory.createOutbound("outbound1", OutboundStatus.CREATED,
                OutboundType.XDOC, Instant.now(clock), Instant.now(clock).plus(2, ChronoUnit.HOURS),
                warehouse.getYandexId(), sortingCenter, null);
        var optRouteId = routeSoCommandService.createRouteSoIfNotExists(outbound);
        transactionTemplate.execute(status -> {
            var route = routeSoRepository.findByIdOrThrow(optRouteId.get());
            var routeCells = route.getRouteSoSites();
            assertThat(routeCells).isNotEmpty();
            return null;
        });
    }

    @Test
    void createRouteSoRetryAndNotSetCellAgain() {
        var outbound = testFactory.createOutbound("outbound1", OutboundStatus.CREATED,
                OutboundType.XDOC, Instant.now(clock), Instant.now(clock).plus(2, ChronoUnit.HOURS),
                warehouse.getYandexId(), sortingCenter, null);
        var optFirstRouteId = routeSoCommandService.createRouteSoIfNotExists(outbound);
        var routeFirstSite = transactionTemplate.execute(status -> {
            var routeFirst = routeSoRepository.findByIdOrThrow(optFirstRouteId.get());
            log.info("Ugly hack: " + routeFirst.getRouteSoSites().size());
            return routeFirst.getRouteSoSites();
        });

        transactionTemplate.execute(status -> {
            var optSecondRouteId = routeSoCommandService.createRouteSoIfNotExists(outbound);
            var routeSecond = routeSoRepository.findByIdOrThrow(optSecondRouteId.get());
            var routeSecondSite = routeSecond.getRouteSoSites();
            assertThat(routeFirstSite).containsOnly(routeSecondSite.toArray(RouteSoSite[]::new));
            return null;
        });
    }

    @Test
    void stopReservationForShippedOutbound() {
        var outbound = flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1")
                .createOutbound(OUTBOUND_ID)
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .prepareToShip("XDOC-1")
                .shipAndGet(OUTBOUND_ID);
        transactionTemplate.execute(ts -> {
                    var routeSo = routeSoQueryService.getRouteByOutbound(outbound);
                    assertThat(routeSo.getRouteSoSites()
                            .stream()
                            .allMatch(routeSoSite -> routeSoSite.getReservedTo().equals(LocalDateTime.now(clock)))
                    )
                            .isTrue();
                    return null;
                }
        );
    }

    @Test
    void extendReservationForUnshippedOutbound() {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        testFactory.setSortingCenterProperty(sc, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        var outbound = flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1")
                .createOutbound(OUTBOUND_ID)
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .and()
                .getOutbound(OUTBOUND_ID);

        testFactory.setupMockClock(clock, clock.instant().plus(CELL_RESERVE_MARGIN + 1,
                ChronoUnit.HOURS));
        routeSoCommandService.extendReservationForXdocOutbounds();
        transactionTemplate.execute(ts -> {
                    var routeSo = routeSoQueryService.getRouteByOutbound(outbound);
                    assertThat(routeSo.getRouteSoSites()
                            .stream()
                            .allMatch(routeSoSite -> routeSoSite.getReservedTo().isAfter(LocalDateTime.now(clock)))
                    )
                            .isTrue();
                    return null;
                }
        );
    }

    @Test
    void doNotExtendReservation() {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        testFactory.setSortingCenterProperty(sc, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        var outbound = flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1")
                .createOutbound(OUTBOUND_ID)
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .and()
                .getOutbound(OUTBOUND_ID);

        routeSoCommandService.extendReservationForXdocOutbounds();
        transactionTemplate.execute(ts -> {
                    var routeSo = routeSoQueryService.getRouteByOutbound(outbound);
                    assertThat(routeSo.getRouteSoSites()
                            .stream()
                            .allMatch(routeSoSite -> routeSoSite.getReservedTo().isAfter(LocalDateTime.now(clock)))
                    )
                            .isTrue();
                    return null;
                }
        );
    }

    @Test
    void extendReservationForUnshippedOutboundsAfterLessThan5Hrs() {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        testFactory.setSortingCenterProperty(sc, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        var outbound = flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1")
                .createOutbound(OUTBOUND_ID)
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .and()
                .getOutbound(OUTBOUND_ID);

        testFactory.setupMockClock(clock, clock.instant().plus(4, ChronoUnit.HOURS)
                .plus(45, ChronoUnit.MINUTES));
        routeSoCommandService.extendReservationForXdocOutbounds();
        transactionTemplate.execute(ts -> {
                    var routeSo = routeSoQueryService.getRouteByOutbound(outbound);
                    assertThat(routeSo.getRouteSoSites()
                            .stream()
                            .allMatch(routeSoSite -> routeSoSite.getReservedTo().isAfter(LocalDateTime.now(clock)))
                    )
                            .isTrue();
                    return null;
                }
        );
    }

    @Test
        //TODO:kir флапает
    void extendReservationForUnshippedOutboundWithReservedCell() {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        Cell cell1 = testFactory.storedCell(sc, "CELL-1", CellType.COURIER, CellSubType.SHIP_XDOC);
        Cell cell2 = testFactory.storedCell(sc, "CELL-2", CellType.COURIER, CellSubType.SHIP_XDOC);
        Cell cell3 = testFactory.storedCell(sc, "CELL-3", CellType.BUFFER, CellSubType.BUFFER_XDOC);
        testFactory.setSortingCenterProperty(sc, SortingCenterPropertiesKey.XDOC_ENABLED, "true");

        var outbound = flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1")
                .createOutbound(OUTBOUND_ID)
                .externalId("REG-1")
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .and()
                .getOutbound(OUTBOUND_ID);

        var outbound2 = flow.createInbound(INBOUND_ID + "1")
                .linkPallets("XDOC-2")
                .fixInbound()
                .sortToAvailableCell("XDOC-2")
                .createOutbound(OUTBOUND_ID + "1", clock.instant().plus(1, ChronoUnit.DAYS))
                .externalId("REG-2")
                .addRegistryPallets("XDOC-2")
                .buildRegistry()
                .and()
                .getOutbound(OUTBOUND_ID + "1");

        testFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        routeSoCommandService.extendReservationForXdocOutbounds();
        transactionTemplate.execute(ts -> {
                    var routeSo1 = routeSoQueryService.getRouteByOutbound(outbound);
                    assertThat(routeSo1.getRouteSoSites()
                            .stream()
                            .findFirst()
                            .map(RouteSoSite::getCell)
                            .map(Cell::getId)
                            .orElseThrow()
                    )
                            .isEqualTo(cell1.getId());

                    var routeSo2 = routeSoQueryService.getRouteByOutbound(outbound2);
                    assertThat(routeSo2.getRouteSoSites()
                            .stream()
                            .findFirst()
                            .map(RouteSoSite::getCell)
                            .map(Cell::getId)
                            .orElseThrow()
                    )
                            .isEqualTo(cell2.getId());

                    return null;
                }
        );
    }

    @Test
    @DisplayName("Продление бронирования при второй отгрузке в другой ячейке")
    void extendReservationForUnshippedOutbounds() {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        Cell cell1 = testFactory.storedCell(sc, "CELL-1", CellType.COURIER, CellSubType.SHIP_XDOC);
        Cell cell2 = testFactory.storedCell(sc, "CELL-2", CellType.COURIER, CellSubType.SHIP_XDOC);
        testFactory.setSortingCenterProperty(sc, SortingCenterPropertiesKey.XDOC_ENABLED, "true");

        var outbound = flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1")
                .createOutbound(OUTBOUND_ID)
                .externalId("REG-1")
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .and()
                .getOutbound(OUTBOUND_ID);

        var outbound2 = flow.createInbound(INBOUND_ID + "1")
                .linkPallets("XDOC-2")
                .fixInbound()
                .sortToAvailableCell("XDOC-2")
                .createOutbound(OUTBOUND_ID + "1", clock.instant().plus(3, ChronoUnit.HOURS))
                .externalId("REG-2")
                .addRegistryPallets("XDOC-2")
                .buildRegistry()
                .and()
                .getOutbound(OUTBOUND_ID + "1");

        testFactory.setupMockClock(clock, clock.instant().plus(5, ChronoUnit.HOURS));

        routeSoCommandService.extendReservationForXdocOutbounds();
        transactionTemplate.execute(ts -> {
                    var routeSo1 = routeSoQueryService.getRouteByOutbound(outbound);
                    assertThat(routeSo1.getRouteSoSites()
                            .stream()
                            .findFirst()
                            .map(RouteSoSite::getCell)
                            .map(Cell::getId)
                            .orElseThrow()
                    )
                            .isEqualTo(cell1.getId());

                    var routeSo2 = routeSoQueryService.getRouteByOutbound(outbound2);
                    assertThat(routeSo2.getRouteSoSites()
                            .stream()
                            .findFirst()
                            .map(RouteSoSite::getCell)
                            .map(Cell::getId)
                            .orElseThrow()
                    )
                            .isEqualTo(cell2.getId());

                    return null;
                }
        );
    }

    @Test
    void extendReservationForUnshippedOutboundWithReservedCellWithMargin() {
        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        Cell cell1 = testFactory.storedCell(sc, "CELL-1", CellType.COURIER, CellSubType.SHIP_XDOC);
        Cell cell2 = testFactory.storedCell(sc, "CELL-2", CellType.COURIER, CellSubType.SHIP_XDOC);
        testFactory.setSortingCenterProperty(sc, SortingCenterPropertiesKey.XDOC_ENABLED, "true");

        var outbound = flow.createInbound(INBOUND_ID)
                .linkPallets("XDOC-1")
                .fixInbound()
                .createOutbound(OUTBOUND_ID)
                .externalId("REG-1")
                .addRegistryPallets("XDOC-1")
                .buildRegistry()
                .sortToAvailableCell("XDOC-1")
                .and()
                .getOutbound(OUTBOUND_ID);

        var outbound2 = flow.createInbound(INBOUND_ID + "1")
                .linkPallets("XDOC-2")
                .fixInbound()
                .createOutbound(OUTBOUND_ID + "1", clock.instant().plus(11, ChronoUnit.HOURS))
                .externalId("REG-2")
                .addRegistryPallets("XDOC-2")
                .buildRegistry()
                .and()
                .getOutbound(OUTBOUND_ID + "1");

        testFactory.setupMockClock(clock, clock.instant().plus(4, ChronoUnit.HOURS)
                .plus(50, ChronoUnit.MINUTES));

        routeSoCommandService.extendReservationForXdocOutbounds();
        transactionTemplate.execute(ts -> {
                    var routeSo1 = routeSoQueryService.getRouteByOutbound(outbound);
                    assertThat(routeSo1.getRouteSoSites()
                            .stream()
                            .findFirst()
                            .map(RouteSoSite::getCell)
                            .map(Cell::getId)
                            .orElseThrow()
                    )
                            .isEqualTo(cell1.getId());
                    assertThat(routeSo1.getRouteSoSites()
                            .stream()
                            .findFirst()
                            .map(RouteSoSite::getReservedTo)
                            .orElseThrow()
                    )
                            .isAfter(LocalDateTime.now(clock).plus(1, ChronoUnit.HOURS));

                    var routeSo2 = routeSoQueryService.getRouteByOutbound(outbound2);
                    assertThat(routeSo2.getRouteSoSites()
                            .stream()
                            .findFirst()
                            .map(RouteSoSite::getCell)
                            .map(Cell::getId)
                            .orElseThrow()
                    )
                            .isEqualTo(cell2.getId());

                    return null;
                }
        );
    }

}
