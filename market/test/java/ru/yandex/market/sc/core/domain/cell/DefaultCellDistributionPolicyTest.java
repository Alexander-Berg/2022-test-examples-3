package ru.yandex.market.sc.core.domain.cell;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.policy.DefaultCellDistributionPolicy;
import ru.yandex.market.sc.core.domain.cell.policy.OrderCellParams;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.RouteCommandService;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.route_so.RouteSoQueryService;
import ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSite;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
class DefaultCellDistributionPolicyTest {

    @Autowired
    DefaultCellDistributionPolicy defaultCellDistributionPolicy;
    @Autowired
    RouteRepository routeRepository;
    @Autowired
    RouteCommandService routeCommandService;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    CellCommandService cellCommandService;
    @Autowired
    TestFactory testFactory;
    @MockBean
    Clock clock;
    @Autowired
    RouteSoQueryService routeSoQueryService;
    @Autowired
    CellQueryService cellQueryService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    XDocFlow flow;

    SortingCenter sortingCenter;
    Warehouse warehouse;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        warehouse = testFactory.storedWarehouse("123");
        testFactory.setupMockClock(clock);
    }

    @Test
    void createNewCellForXdoc() {
        var outbound = testFactory.createOutbound("outbound1", OutboundStatus.CREATED,
                OutboundType.XDOC, Instant.now(clock), Instant.now(clock).plus(2, ChronoUnit.HOURS),
                warehouse.getYandexId(), sortingCenter, null);
        Optional<Cell> cell = transactionTemplate.execute(ts ->
                defaultCellDistributionPolicy.findOrCreateCellForRouteSo(outbound, sortingCenter,
                        DateTimeUtil.toLocalDateTime(outbound.getFromTime()),
                        DateTimeUtil.toLocalDateTime(outbound.getToTime()),
                        warehouse.getYandexId()));
        assertThat(cell).isNotEmpty();
        assertThat(cell.get().getType()).isEqualTo(CellType.COURIER);
        assertThat(cell.get().getSubtype()).isEqualTo(CellSubType.SHIP_XDOC);
        assertThat(cell.get().isDeleted()).isFalse();
        assertThat(cell.get().getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
    }

    @Test
    void getExistingCellForXdoc() {
        var xdocCell = testFactory.storedCell(sortingCenter, "xdoc cell", CellType.COURIER, CellSubType.SHIP_XDOC);
        var outbound = testFactory.createOutbound("outbound1", OutboundStatus.CREATED,
                OutboundType.XDOC, Instant.now(clock), Instant.now(clock).plus(2, ChronoUnit.HOURS),
                warehouse.getYandexId(), sortingCenter, null);
        Optional<Cell> cell = transactionTemplate.execute(ts ->
                defaultCellDistributionPolicy.findOrCreateCellForRouteSo(outbound, sortingCenter,
                        DateTimeUtil.toLocalDateTime(outbound.getFromTime()),
                        DateTimeUtil.toLocalDateTime(outbound.getToTime()),
                        warehouse.getYandexId()));
        assertThat(cell).contains(xdocCell);
    }

    @Test
    @Transactional
    void shouldNotDesignateAlreadyDesignatedCell() {
        //Зона нужна, чтобы сбить совпадение в id для cell и site
        testFactory.storedZone(sortingCenter, "1");
        var xdocCell = testFactory.storedCell(sortingCenter, "xdoc cell", CellType.COURIER, CellSubType.SHIP_XDOC);

        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .sortingCenter(sortingCenter)
                .toTime(Instant.now(clock))
                .fromTime(Instant.now(clock))
                .type(OutboundType.XDOC)
                .logisticPointToExternalId(warehouse.getYandexId())
                .partnerToExternalId(warehouse.getPartnerId())
                .externalId("outbound1")
                .build());

        var anotherOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .sortingCenter(sortingCenter)
                .toTime(Instant.now(clock))
                .fromTime(Instant.now(clock))
                .type(OutboundType.XDOC)
                .logisticPointToExternalId(warehouse.getYandexId())
                .partnerToExternalId(warehouse.getPartnerId())
                .externalId("outbound2")
                .build());

        RouteSo routeSo = routeSoQueryService.getRouteByOutbound(outbound);
        RouteSo anotherRouteSo = routeSoQueryService.getRouteByOutbound(anotherOutbound);

        Cell cell = routeSo.getRouteSoSites().stream()
                .map(RouteSoSite::getCell)
                .findFirst()
                .orElseThrow();

        Cell anotherCell = anotherRouteSo.getRouteSoSites().stream()
                .map(RouteSoSite::getCell)
                .findFirst()
                .orElseThrow();

        assertThat(cell).isEqualTo(xdocCell);
        assertThat(anotherCell).isNotEqualTo(xdocCell);
    }

    @Test
    void getManuallyDesignatedCellWarehouse() {
        var xdocCell = testFactory.storedCell(sortingCenter, "xdoc cell", CellType.COURIER, CellSubType.SHIP_XDOC,
                warehouse.getYandexId());
        var outbound = testFactory.createOutbound("outbound1", OutboundStatus.CREATED,
                OutboundType.XDOC, Instant.now(clock), Instant.now(clock).plus(2, ChronoUnit.HOURS),
                warehouse.getYandexId(), sortingCenter, null);
        Optional<Cell> cell = transactionTemplate.execute(ts ->
                defaultCellDistributionPolicy.findOrCreateCellForRouteSo(outbound, sortingCenter,
                        DateTimeUtil.toLocalDateTime(outbound.getFromTime()),
                        DateTimeUtil.toLocalDateTime(outbound.getToTime()),
                        warehouse.getYandexId()));
        assertThat(cell).contains(xdocCell);
    }

    @Test
    @Transactional
    void cantDesignateSameCellWithinSameTime() {
        testFactory.storedWarehouse();
        testFactory.storedUser(flow.getSortingCenter(), TestFactory.USER_UID_LONG);
        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .fixInbound()
                .sortToAvailableCell("XDOC-1")
                .createOutbound("out-1")
                .buildRegistry("XDOC-1")
                .and()
                .createInbound("in-2")
                .linkPallets("XDOC-2")
                .fixInbound()
                .sortToAvailableCell("XDOC-2");
        testFactory.setupMockClock(clock, clock.instant().plus(8, ChronoUnit.HOURS));

        flow.createOutbound("out-2")
                .addRegistryPallets("XDOC-2")
                .externalId("reg-123")
                .buildRegistry();

        Outbound outbound1 = flow.getOutbound("out-1");
        Outbound outbound2 = flow.getOutbound("out-2");

        RouteSoSite rss1 =
                routeSoQueryService.getRouteByOutbound(outbound1).getRouteSoSites().stream().findFirst().orElseThrow();
        RouteSoSite rss2 =
                routeSoQueryService.getRouteByOutbound(outbound2).getRouteSoSites().stream().findFirst().orElseThrow();

        assertThat(rss1.getCell()).isNotEqualTo(rss2.getCell());
    }

    @Test
    void sortInAdvanceCantPickUsedDesignatedCell() {
        int daysInAdvance = 1;
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.COURIER_SORT_IN_ADVANCE_DAYS, Objects.toString(daysInAdvance)
        );
        var courier = testFactory.storedCourier();
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER, courier.getId());
        var futureDate = LocalDate.now(clock).plusDays(daysInAdvance);
        var order = testFactory.createOrder(sortingCenter)
                .updateCourier(courier).updateShipmentDate(futureDate).get();
        assertThat(testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                .orElseThrow().allowNextRead().getCells(futureDate)
        ).isEqualTo(List.of(cell));
        transactionTemplate.execute(ts -> {
            assertThat(defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                    order, futureDate, futureDate, false
            )).isEmpty();
            return null;
        });
    }

    @Test
    void doNotUseDeletedManuallyDesignatedCellCourier() {
        var courier = testFactory.storedCourier();
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER, courier.getId());
        cellCommandService.deleteCell(sortingCenter, cell.getId());
        var orderCellParams = courierCellParams(courier, LocalDate.now(clock));
        transactionTemplate.execute(ts -> {
            assertThat(defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                    orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), false
            )).isEmpty();
            return null;
        });
    }

    @Test
    void doNotUseDeletedManuallyDesignatedCellWarehouse() {
        var localWarehouse = testFactory.storedWarehouse();
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, localWarehouse.getYandexId());
        var orderCellParams = warehouseCellParams(localWarehouse, LocalDate.now(clock));
        cellCommandService.deleteCell(sortingCenter, cell.getId());
        transactionTemplate.execute(ts -> {
            assertThat(defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                    orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), false
            )).isEmpty();
            return null;
        });
    }


    @Test
    void doNotUseCourierCellWithOrdersForRouteWithDifferentCourier() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.COURIER);
        var order = testFactory.createForToday(
                order(sortingCenter).externalId("nn").build()
        ).updateCourier(testFactory.storedCourier(1L)).accept().sort().get();
        var orderCellParams = courierCellParams(
                testFactory.storedCourier(2L), LocalDate.now(clock).plusDays(1));
        transactionTemplate.execute(ts -> {
            Cell actual = defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                    orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), true
            ).stream().findFirst().orElseThrow();
            assertThat(actual).isNotEqualTo(cell);
            assertThat(actual.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
            return null;
        });
    }

    @Test
    void doNotUseCellFromOtherRouteWhenOrderMistakelyInCell() {
        //place1 с route1 лежит в cell1, place2 с route2 каким-то ошибочным образом лежит в cell1
        //не назначать cell1 на route2
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        var place1 = testFactory.createForToday(
                order(sortingCenter).externalId("o1").build()
        ).updateCourier(testFactory.storedCourier(1L)).accept().sort().getPlace();
        var place2 = testFactory.createForToday(
                order(sortingCenter).externalId("o2").build()
        ).updateCourier(testFactory.storedCourier(2L)).accept().sort().getPlace();
        var place3 = testFactory.createForToday(
                order(sortingCenter).externalId("o3").build()
        ).updateCourier(testFactory.storedCourier(3L)).getPlace();
        var orderCellParams = courierCellParams(place3.getCourier(), LocalDate.now(clock).plusDays(1));

        //незконный способ положить заказ в ячейку - где-то в коде есть бага, которая позволяет положить заказ не в
        // ту ячейку
        transactionTemplate.execute(ts -> {
            Place actualPlace2 = testFactory.updated(place2);
            actualPlace2.setMutableState(actualPlace2.getMutableState().withCell(place1.getCell()), null,
                    Instant.now(clock));
            return null;
        });

        AtomicReference<Cell> routeCell = new AtomicReference<>();
        transactionTemplate.execute(ts -> {
            routeCell.set(defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                            orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), true)
                    .stream().findFirst().orElseThrow());
            return null;
        });
        assertThat(routeCell.get()).isNotEqualTo(place2.getCell());
    }

    @Test
    @Disabled
    void useSameCellForOrderFromRoute() {
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        var courier = testFactory.storedCourier(1L);

        var place1 = testFactory.createForToday(
                order(sortingCenter).externalId("o1").build()
        ).updateCourier(courier).accept().sort().getPlace();
        var place2 = testFactory.createForToday(
                order(sortingCenter).externalId("o2").build()
        ).updateCourier(courier).getPlace();
        var orderCellParams2 = courierCellParams(place2.getCourier(), LocalDate.now(clock).plusDays(1));
        transactionTemplate.execute(ts -> {
            Cell actual2 = defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                    orderCellParams2.getOrder(), LocalDate.now(clock), LocalDate.now(clock), true
            ).stream().findFirst().orElseThrow();
            assertThat(actual2).isEqualTo(place1.getCell());
            return null;
        });
    }

    private ScOrder courierCellParams(Courier courier, LocalDate date) {
        return testFactory.createOrder(sortingCenter)
                .updateCourier(courier).updateShipmentDate(date).get();
    }

    private ScOrder warehouseCellParams(Warehouse warehouse, LocalDate date) {
        return testFactory.create(
                        order(sortingCenter)
                                .externalId("odef")
                                .warehouseReturnId(warehouse.getYandexId())
                                .build()
                )
                .updateShipmentDate(date).accept().makeReturn().get();
    }

    @Test
    void doNotUseReturnCellWithOrdersForRoute() {
        var cell = testFactory.storedCell(sortingCenter, "1", CellType.RETURN);
        testFactory.createForToday(order(sortingCenter).externalId("nn").build())
                .accept().sort().ship().makeReturn().accept().sort().get();
        var newWarehouse = testFactory.storedWarehouse("new one");
        var orderCellParams = warehouseCellParams(newWarehouse, LocalDate.now(clock).plusDays(1));
        transactionTemplate.execute(ts -> {
            Cell actual = defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                    orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), true
            ).stream().findFirst().orElseThrow();
            assertThat(actual).isNotEqualTo(cell);
            assertThat(actual.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
            return null;
        });
    }

    @Test
    void designateCellForReturnRoute() {
        testFactory.storedCell(sortingCenter, "10", CellType.RETURN);
        var designatedCell = testFactory.storedCell(sortingCenter, "20", CellType.RETURN,
                warehouse.getYandexId());
        testFactory.storedCell(sortingCenter, "30", CellType.RETURN);

        var orderCellParams = warehouseCellParams(warehouse, LocalDate.now(clock));
        LocalDate futureDate = LocalDate.now(clock).plusDays(1L);
        var cell = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                orderCellParams.getOrder(), futureDate, futureDate, false
        ).stream().findFirst().orElseThrow());
        assertThat(cell).isEqualTo(designatedCell);
    }

    @Test
    void designateCellForCourierRoute() {
        var courier = testFactory.storedCourier(1L);
        testFactory.storedCell(sortingCenter, "10", CellType.COURIER);
        var designatedCell = testFactory.storedCell(sortingCenter, "20", CellType.COURIER, 1L);
        testFactory.storedCell(sortingCenter, "30", CellType.COURIER);

        var orderCellParams = courierCellParams(courier, LocalDate.now(clock));
        LocalDate futureDate = LocalDate.now(clock).plusDays(1L);
        var cell = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                orderCellParams.getOrder(), futureDate, futureDate, false
        ).stream().findFirst().orElseThrow());
        assertThat(cell).isEqualTo(designatedCell);
    }

    @Test
    void designateMultipleCellsForReturnRoute() {
        testFactory.storedCell(sortingCenter, "10", CellType.RETURN);
        var designatedCell1 = testFactory.storedCell(sortingCenter, "20", CellType.RETURN,
                warehouse.getYandexId());
        var designatedCell2 = testFactory.storedCell(sortingCenter, "21", CellType.RETURN,
                warehouse.getYandexId());
        testFactory.storedCell(sortingCenter, "30", CellType.RETURN);

        var orderCellParams = warehouseCellParams(warehouse, LocalDate.now(clock));
        LocalDate futureDate = LocalDate.now(clock).plusDays(1L);
        var cells = transactionTemplate.execute(
                ts -> new HashSet<>(defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                        orderCellParams.getOrder(), futureDate, futureDate, false
                ))
        );
        assertThat(cells).isEqualTo(Set.of(designatedCell1, designatedCell2));
    }

    @Test
    void designateMultipleCellsForCourierRoute() {
        var courier = testFactory.storedCourier(1L);
        testFactory.storedCell(sortingCenter, "10", CellType.COURIER);
        var designatedCell1 = testFactory.storedCell(sortingCenter, "20", CellType.COURIER, 1L);
        var designatedCell2 = testFactory.storedCell(sortingCenter, "21", CellType.COURIER, 1L);
        testFactory.storedCell(sortingCenter, "30", CellType.COURIER);

        var orderCellParams = courierCellParams(courier, LocalDate.now(clock));
        LocalDate futureDate = LocalDate.now(clock).plusDays(1L);
        var cells = transactionTemplate.execute(
                ts -> new HashSet<>(defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                        orderCellParams.getOrder(), futureDate, futureDate, false
                ))
        );
        assertThat(cells).isEqualTo(Set.of(designatedCell1, designatedCell2));
    }

    @Test
    void useCellWithOrdersForReturnRoute() {
        testFactory.storedCell(sortingCenter, "10", CellType.RETURN);
        var designatedCell = testFactory.storedCell(sortingCenter, "20",
                CellType.RETURN, warehouse.getYandexId());
        testFactory.storedCell(sortingCenter, "30", CellType.RETURN);
        var place = testFactory.create(order(sortingCenter).warehouseReturnId(warehouse.getYandexId()).build())
                .cancel().accept().sort().getPlace();
        assertThat(place.getCell()).isEqualTo(designatedCell);

        LocalDate futureDate = LocalDate.now(clock).plusDays(1L);
        transactionTemplate.execute(ts -> {
            assertThat(
                    defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                            place, futureDate, futureDate, null, false, false
                    ).stream().findFirst().orElseThrow().getId()
            ).isEqualTo(designatedCell.getId());
            return null;
        });
    }


    @Test
    void createIfNoCellsAvailable() {
        var orderCellParams = warehouseCellParams(warehouse, LocalDate.now(clock));
        Cell cell1 = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                        orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), true)
                .stream().findFirst().orElseThrow());
        assertThatIsNotActive(cell1);

        var bufferCellParams = bufferCellParams();
        Cell cell2 = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateFreeBufferCell(
                bufferCellParams, true).orElseThrow());
        assertThatIsNotActive(cell2);
    }

    private OrderCellParams bufferCellParams() {
        return testFactory.createOrder(sortingCenter).get();
    }


    @Test
    void doNotCreateNewCellIfNotNeededAvailable() {
        var orderCellParams = warehouseCellParams(warehouse, LocalDate.now(clock));
        Optional<Cell> cellO = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), false
        ).stream().findFirst());
        assertThat(cellO).isEmpty();

        var bufferCellParams = bufferCellParams();
        cellO = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateFreeBufferCell(
                bufferCellParams, false));
        assertThat(cellO).isEmpty();
    }

    @Test
    void createIfAllCellsDesignated() {
        LocalDate date = LocalDate.now(clock);
        Cell cell1 = testFactory.storedActiveCell(sortingCenter, CellType.COURIER);
        Courier courier1 = testFactory.storedCourier(1L);
        testFactory.storedOutgoingCourierRoute(date, sortingCenter, courier1, cell1);
        Courier courier2 = testFactory.storedCourier(2L);
        var orderCellParams = courierCellParams(courier2, date);

        Cell cell2 = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                        orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), true)
                .stream().findFirst().orElseThrow());
        assertThatIsNotActive(cell2);
        assertThat(cell2).isNotEqualTo(cell1);
    }


    @Test
    void findNotDesignatedCell() {
        Cell expected = testFactory.storedActiveCell(sortingCenter, CellType.COURIER);
        var orderCellParams = courierCellParams(testFactory.storedCourier(), LocalDate.now(clock));
        var route = testFactory.findOutgoingCourierRoute(orderCellParams).orElseThrow().allowReading();
        Cell actual = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOutgoingCellFromRoute(
                orderCellParams.getOrder(), route).orElseThrow());
        assertThat(actual).isEqualTo(expected);
    }


    @Test
    void findNotDesignatedCellLexicographicallyFirst() {
        testFactory.storedCell(sortingCenter, "ЧА-10", CellType.COURIER);
        Cell expected = testFactory.storedCell(sortingCenter, "ЧА-2", CellType.COURIER);
        testFactory.storedCell(sortingCenter, "ЧБ-2", CellType.COURIER);
        var order = courierCellParams(testFactory.storedCourier(), LocalDate.now(clock));
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                        .orElseThrow().allowReading();
        assertThat(route.getCells(LocalDate.now(clock))).isEqualTo(List.of(expected));
    }

    @Test
    void manyDifferentCellsOnRoute() {
        var cell1 = testFactory.storedCell(sortingCenter, "def", CellType.RETURN, CellSubType.DEFAULT);
        var cell2 = testFactory.storedCell(sortingCenter, "dam", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        var order1 = warehouseCellParams(warehouse, LocalDate.now(clock));
        var order2 = warehouseDamagedCellParams(warehouse, LocalDate.now(clock));
        var route = testFactory.findOutgoingWarehouseRoute(order1).orElseThrow().allowReading();
        assertThat(route.allowNextRead().getCells(LocalDate.now(clock))).isEqualTo(List.of(cell1, cell2));
        //todo: тут тестируется только старый флоу
        Cell actual1 = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOutgoingCellFromRoute(
                order1, route
        ).orElseThrow());
        Cell actual2 = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOutgoingCellFromRoute(
                order2, route
        ).orElseThrow());
        assertThat(actual1).isEqualTo(cell1);
        assertThat(actual2).isEqualTo(cell2);
    }

    @Test
    void createReturnDamagedCell() {
        var orderCellParams = warehouseDamagedCellParams(testFactory.storedWarehouse(), LocalDate.now(clock));
        Cell actual = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), true
        ).stream().findFirst().orElseThrow());
        assertThat(actual).isNotNull();
        assertThat(actual.getType()).isEqualTo(CellType.RETURN);
        assertThat(actual.getSubtype()).isEqualTo(CellSubType.RETURN_DAMAGED);
        assertThatIsNotActive(actual);
    }

    private void assertThatIsNotActive(Cell cell) {
        assertThat(cell).isNotNull();
        if (cell.getType() != CellType.RETURN) {
            assertThat(cell.getScNumber()).isNull();
        } else {
            assertThat(cell.getScNumber()).isEqualTo("RETURN NEW " + cell.getId());
        }
        assertThat(cell.getStatus()).isEqualTo(CellStatus.NOT_ACTIVE);
    }

    private ScOrder warehouseDamagedCellParams(Warehouse warehouse, LocalDate date) {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED, "true");
        return testFactory.create(
                        order(sortingCenter)
                                .externalId("odam")
                                .warehouseReturnId(warehouse.getYandexId())
                                .warehouseCanProcessDamagedOrders(true)
                                .build()
                )
                .updateShipmentDate(date).accept().markOrderAsDamaged().get();
    }


    @Test
    void createClientReturnCell() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        var orderCellParams = warehouseClientReturnCellParams(LocalDate.now(clock));
        Cell actual = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), true
        ).stream().findFirst().orElseThrow());
        assertThat(actual).isNotNull();
        assertThat(actual.getType()).isEqualTo(CellType.RETURN);
        assertThat(actual.getSubtype()).isEqualTo(CellSubType.CLIENT_RETURN);
        assertThatIsNotActive(actual);
    }

    private OrderLike warehouseClientReturnCellParams(LocalDate date) {
        return testFactory.create(
                        order(sortingCenter)
                                .isClientReturn(true)
                                .shipmentDate(date)
                                .build()
                )
                .accept().get();
    }


    @Test
    void createDroppedOrdersCell() {
        var orderCellParams = droppedOrderCellParam(LocalDate.now(clock));
        Cell actual = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateFreeBufferCell(
                orderCellParams.getOrder(), true).orElseThrow());
        assertThat(actual).isNotNull();
        assertThat(actual.getType()).isEqualTo(CellType.BUFFER);
        assertThat(actual.getSubtype()).isEqualTo(CellSubType.DROPPED_ORDERS);
        assertThatIsNotActive(actual);
    }

    private OrderLike droppedOrderCellParam(LocalDate date) {
        return testFactory.createOrder(sortingCenter).updateShipmentDate(date).get();
    }

    @Test
    void findReturnDamagedCell() {
        Cell expected = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.RETURN_DAMAGED);
        var orderCellParams = warehouseDamagedCellParams(testFactory.storedWarehouse(), LocalDate.now(clock));
        var route = testFactory.findOutgoingWarehouseRoute(orderCellParams).orElseThrow().allowReading();

        //todo: тут тестируется только старый флоу
        Cell actual = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOutgoingCellFromRoute(
                orderCellParams.getOrder(), route
        ).orElseThrow());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void findClientReturnCell() {
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        Cell expected = testFactory.storedCell(sortingCenter, "1", CellType.RETURN, CellSubType.CLIENT_RETURN);
        var orderCellParams = warehouseClientReturnCellParams(LocalDate.now(clock));
        var route = testFactory.findOutgoingWarehouseRoute(orderCellParams).orElseThrow().allowReading();

        //todo: тут тестируется только старый флоу
        Cell actual = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOutgoingCellFromRoute(
                orderCellParams.getOrder(), route).orElseThrow());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void findDroppedOrdersCell() {
        Cell expected = testFactory.storedCell(sortingCenter, "1", CellType.BUFFER, CellSubType.DROPPED_ORDERS);
        var orderCellParams = droppedOrderCellParam(LocalDate.now(clock));
        Cell actual = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateFreeBufferCell(
                orderCellParams.getOrder(), true).orElseThrow());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void useFreeCellsForDamagedOrders() {
        for (int i = 0; i < 20; i++) {
            testFactory.storedCell(sortingCenter, "ПВ-" + i, CellType.RETURN, CellSubType.RETURN_DAMAGED);
        }
        var orderCellParams = warehouseDamagedCellParams(testFactory.storedWarehouse(), LocalDate.now(clock));
        var actual = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), false
        ).stream().findFirst().orElseThrow());
        assertThat(actual).isNotNull();
    }

    @Test
    void useCellWithDamagedOrdersForReturnRouteNoDesignedCell() {
        var expectedCell = testFactory.storedCell(sortingCenter,
                "ПВ-20",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED,
                warehouse.getYandexId());
        var order = testFactory.create(order(sortingCenter)
                        .warehouseReturnId(warehouse.getYandexId())
                        .warehouseCanProcessDamagedOrders(true).build())
                .cancel().accept().markOrderAsDamaged().sort(expectedCell.getId()).get();
        LocalDate futureDate = LocalDate.now(clock).plusDays(1L);
        transactionTemplate.execute(ts -> {
            var actual = defaultCellDistributionPolicy.findOrCreateOutgoingCell(order,
                            futureDate, futureDate, false)
                    .stream().findFirst().orElseThrow();
            assertThat(actual).isEqualTo(expectedCell);
            return null;
        });
    }


    @Test
    void dontUseEmptyCellWithWarehouseIdForOtherWarehouse() {
        var warehouseWithCell = testFactory.storedWarehouse("w-1");
        var warehouseCell = testFactory.storedCell(sortingCenter, CellType.RETURN, warehouseWithCell, "B-1");

        var orderCellParams = warehouseDamagedCellParams(
                testFactory.storedWarehouse("w-2"), LocalDate.now(clock));
        Cell actual = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), true
        ).stream().findFirst().orElseThrow());
        assertThat(actual).isNotEqualTo(warehouseCell);
    }

    @Test
    void dontUseEmptyCellWithCourierIdForOtherCourier() {
        var courier = testFactory.storedCourier(1L);
        var courierCell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER, courier.getId());

        var orderCellParams = courierCellParams(
                testFactory.storedCourier(2L), LocalDate.now(clock));
        Cell actual = transactionTemplate.execute(ts -> defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                orderCellParams.getOrder(), LocalDate.now(clock), LocalDate.now(clock), true
        ).stream().findFirst().orElseThrow());
        assertThat(actual).isNotEqualTo(courierCell);
    }

    @Test
    void dontUseDeletedCell() {
        Cell destinationCellDeleted = testFactory.storedCell(sortingCenter, "123123", CellType.BUFFER,
                CellSubType.BUFFER_XDOC, warehouse.getYandexId());
        testFactory.deleteCellForce(destinationCellDeleted);
        Cell destinationCell = testFactory.storedCell(sortingCenter, "123123", CellType.BUFFER,
                CellSubType.BUFFER_XDOC, warehouse.getYandexId());

        assertThat(defaultCellDistributionPolicy.findCellForDestinationOrCreateDefault(sortingCenter,
                warehouse.getYandexId()))
                .isEqualTo(destinationCell);
    }

    @Test
    @Transactional
    void useBufferCell() {
        Cell bufferCell = testFactory.storedCell(sortingCenter, "123123", CellType.BUFFER,
                CellSubType.BUFFER_XDOC);

        assertThat(defaultCellDistributionPolicy.findCellForDestinationOrCreateDefault(
                sortingCenter,
                warehouse.getYandexId()))
                .isEqualTo(bufferCell);
    }

    @Test
    void doNotAssignCellForFutureDatesForMiddleMile() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);
        Map<Integer, Boolean> ignoreCellCreationForDayInFuture = Map.of(0, true, 1, true, 2, false, 3, false, 4, false);
        ignoreCellCreationForDayInFuture.forEach((dayInFuture, assignCell) ->
                transactionTemplate.execute(ts -> {
                    LocalDate date = LocalDate.now(clock).plusDays(dayInFuture);
                    ScOrder order = testFactory.createOrder(sortingCenter).updateShipmentDate(date).get();
                    assertThat(defaultCellDistributionPolicy.findOrCreateOutgoingCell(
                                    order, date, date
                                    , true)
                            .size() > 0).isEqualTo(assignCell);
                    return null;
                }));
    }

    @Test
    void reusePreviouslyAssignedCell() {
        var cellList = new ArrayList<Cell>();
        int numberOfCellsToCreate = 10;
        for (int i = 0; i < numberOfCellsToCreate; i++) {
            cellList.add(testFactory.storedCell(sortingCenter, "O-" + i, CellType.COURIER));
        }

        var currentDate = LocalDate.now(clock);
        var order1 = testFactory.createForToday(order(sortingCenter).externalId("o1").build()).get();
        var firstAssignedCell = cellList.get(0);
        assertThat(
                testFactory.findOutgoingCourierRoute(order1)
                        .orElseThrow().allowNextRead().getCells(LocalDate.now(clock)))
                .isEqualTo(List.of(firstAssignedCell));

        var order2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build()).get();
        List<Cell> foundCells = transactionTemplate.execute(ts ->
                defaultCellDistributionPolicy.findOrCreateOutgoingCell(order2, currentDate.plusDays(1),
                        currentDate.plusDays(1), null, true, false)
        );

        assertThat(foundCells).isEqualTo(List.of(firstAssignedCell));
    }

    @Test
    void avoidCellPreviouslyAssignedToOthers() {
        int numberOfCellsToCreate = 10;
        var cellList = new ArrayList<Cell>();
        var currentDate = LocalDate.now(clock);
        var courier = testFactory.storedCourier(777);
        for (int i = 0; i < numberOfCellsToCreate; i++) {
            cellList.add(testFactory.storedCell(sortingCenter, "O-" + i, CellType.COURIER));
            testFactory.storedOutgoingCourierRoute(currentDate.minusDays(i), sortingCenter, courier, true, cellList.get(i));
        }
        var order = testFactory.createOrder(sortingCenter)
                .updateShipmentDate(currentDate)
                .updateCourier(testFactory.defaultCourier()).get();
        var unusedCellToBeSelected = testFactory.storedCell(sortingCenter, "O-" + System.nanoTime(), CellType.COURIER);
        List<Cell> foundCells = transactionTemplate.execute(ts ->
                defaultCellDistributionPolicy.findOrCreateOutgoingCell(order, currentDate, currentDate, true)
        );
        assertThat(foundCells).hasSize(1);
        assertThat(foundCells.get(0)).isEqualTo(unusedCellToBeSelected);
    }

    @Test
    void useCellFromPool() {
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);
        var cell3 = testFactory.storedCell(sortingCenter, "c3", CellType.COURIER);
        var cell4 = testFactory.storedCell(sortingCenter, "c4", CellType.RETURN);

        var courier = testFactory.storedCourier(1L);
        var courierWithPool = testFactory.storedCourier(2);
        var ds = testFactory.storedDeliveryService("ds-1");

        testFactory.storedPool(1L, sortingCenter, "pool1", RouteDestinationType.DELIVERY_SERVICE,
                ds.getId(), Set.of(cell3, cell4)
        );

        var place1 = testFactory.createForToday(
                order(sortingCenter).externalId("o1").build()
        ).updateCourier(courier).accept().sort().getPlace();
        assertThat(place1.getCell()).isNotNull();
        assertThat(place1.getCell()).isIn(cell1, cell2);

        var place2 = testFactory.createForToday(
                order(sortingCenter).externalId("o2").deliveryService(ds).build()
        ).updateCourier(courierWithPool).accept().sort().getPlace();
        assertThat(place2.getCell()).isNotNull();
        assertThat(place2.getCell()).isEqualTo(cell3);

        var place3 = testFactory.createForToday(
                order(sortingCenter).externalId("o3").deliveryService(ds).build()
        ).updateCourier(courierWithPool).accept().sort().getPlace();
        assertThat(place3.getCell()).isNotNull();
        assertThat(place3.getCell()).isEqualTo(cell3);
    }

    @Test
    @DisplayName("Проверка неназначения ячейки из пула для посторонних СД")
    void dontUseCellFromPoolForOthers() {
        var cell1 = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var cell2 = testFactory.storedCell(sortingCenter, "c2", CellType.COURIER);

        testFactory.storedPool(1L, sortingCenter, "pool1", RouteDestinationType.DELIVERY_SERVICE,
                System.nanoTime(), Set.of(cell1)
        );
        testFactory.storedPool(2L, sortingCenter, "pool2", RouteDestinationType.DELIVERY_SERVICE,
                System.nanoTime() + 1, Set.of(cell2)
        );
        var courier = testFactory.storedCourier(1L);
        var order = testFactory.createOrder(TestFactory.CreateOrderParams.builder()
                .externalId("o1")
                .shipmentDate(LocalDate.now(clock))
                .sortingCenter(sortingCenter)
                .build()
        ).updateCourier(courier).get();

        assertThat(testFactory.findRoutesCell(cell1)).isEmpty();
        assertThat(testFactory.findRoutesCell(cell2)).isEmpty();
        assertThat(testFactory.findRouteCell(testFactory.findOutgoingRoute(order).get(), order)).isPresent();
    }

    @Test
    void dontAssignImpermanentCellOnRoute() {
        testFactory.setSortingCenterProperty(
                sortingCenter, SortingCenterPropertiesKey.IMPERMANENT_ENABLED, true);
        var localWarehouse = testFactory.storedWarehouse("warehouse-merchant", WarehouseType.SHOP);
        var impermanenceCell = testFactory.storedCell(
                sortingCenter, "impermanence-cell", CellType.RETURN, CellSubType.IMPERMANENT);
        var o1 = testFactory.create(order(sortingCenter, "o1")
                        .warehouseReturnId(localWarehouse.getYandexId()).build())
                .updateShipmentDate(LocalDate.now(clock))
                .accept()
                .makeReturn()
                .accept()
                .get();

        var route = testFactory.findOutgoingRoute(o1).orElseThrow();
        assertThat(route.allowNextRead().getRouteCells())
                .noneMatch(rc -> rc.getCell().equals(impermanenceCell));
    }

}
