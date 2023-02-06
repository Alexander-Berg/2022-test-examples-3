package ru.yandex.market.sc.core.domain.route.repository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.cell.model.ApiCellForRouteBaseDto;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellForRouteDto;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.route.RouteFacade;
import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@EmbeddedDbTest
class RouteMapperTest {

    @Autowired
    RouteFacade routeFacade;
    @Autowired
    TestFactory testFactory;
    @MockBean
    Clock clock;

    private SortingCenter sortingCenter;
    private Cell cell;
    private TestFactory.CourierWithDs courierWithDs;

    @BeforeEach
    void init() {
        TestFactory.setupMockClock(clock);
        sortingCenter = testFactory.storedSortingCenter();
        courierWithDs = testFactory.magistralCourier();
    }

    @Test
    void doNotRequireResortingOnReturnCellWithLot() {
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.CALCULATE_CELL_PREPARED_FOR_LOT_ENABLED, "true");
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        var place = testFactory.createOrderForToday(sortingCenter)
                .cancel()
                .accept()
                .sortToLot()
                .getPlace();
        var route = testFactory.findOutgoingWarehouseRoute(place).orElseThrow();

        var routeDto = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter);

        for (ApiCellForRouteBaseDto cell : routeDto.getCells()) {
            ApiCellForRouteDto cellDto = routeFacade.getCellForRoute(sortingCenter, cell.id(), testFactory.getRouteIdForSortableFlow(route));
            assertThat(cellDto.getActions()).contains(ApiCellForRouteDto.Action.SHIP_ALL);
        }
    }

    @Test
    void dropoffSeesShipAllButtonOnCourierRouteByDefault() {
        var order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();

        var routeDto = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter);

        for (ApiCellForRouteBaseDto cell : routeDto.getCells()) {
            ApiCellForRouteDto cellDto = routeFacade.getCellForRoute(sortingCenter, cell.id(), testFactory.getRouteIdForSortableFlow(route));
            assertThat(cellDto.getActions()).contains(ApiCellForRouteDto.Action.SHIP_ALL);
        }
    }

    @Test
    void dropoffMissesShipAllButtonOnCourierRouteIfFlagEnabled() {
        testFactory.setSortingCenterProperty(sortingCenter.getId(), SortingCenterPropertiesKey.ALWAYS_RESORT_DIRECT, "true");

        var order = testFactory.createOrderForToday(sortingCenter).accept().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();

        var routeDto = routeFacade.getApiOutgoingRouteDto(
                LocalDate.now(clock), testFactory.getRouteIdForSortableFlow(route), sortingCenter);

        for (ApiCellForRouteBaseDto cell : routeDto.getCells()) {
            ApiCellForRouteDto cellDto = routeFacade.getCellForRoute(sortingCenter, cell.id(), testFactory.getRouteIdForSortableFlow(route));
            assertThat(cellDto.getActions()).doesNotContain(ApiCellForRouteDto.Action.SHIP_ALL);
        }
    }

    @Test
    void doNotFlushLastMileShipAllMonitoringAtLastMile() {
        RouteSoMigrationHelper.allowRouteReading();
        cell = testFactory.storedCell(sortingCenter, "c1", CellType.COURIER);
        var routeId = prepareOrderAndCheckMiddleMileMonitoringNotFlushingReturnRouteForResorting(
                lastMileOrder(sortingCenter), sortingCenter, true, false);

        routeId = testFactory.getRouteIdForSortableFlow(routeId);
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        ApiCellForRouteDto cellDto = routeFacade.getCellForRoute(sortingCenter, cell.getId(), routeId);

        assertThat(cellDto.getActions()).doesNotContain(ApiCellForRouteDto.Action.SHIP_ALL);
    }

    @Test
    void flushMiddleMileShipAllMonitoringAtMiddleMile() {
        RouteSoMigrationHelper.allowRouteReading();
        cell = testFactory.storedMagistralCell(sortingCenter, "c1", CellSubType.DEFAULT,
                courierWithDs.courier().getId());
        var routeId = prepareOrderAndCheckMiddleMileMonitoringNotFlushingReturnRouteForResorting(
                middleMileOrder(sortingCenter), sortingCenter, false, false);
        routeId = testFactory.getRouteIdForSortableFlow(routeId);
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        ApiCellForRouteDto cellDto = routeFacade.getCellForRoute(sortingCenter, cell.getId(), routeId);

        assertThat(cellDto.getActions()).doesNotContain(ApiCellForRouteDto.Action.SHIP_ALL);
    }

    @Test
    void flushMiddleMileShipAllMonitoringAtReturnFlow() {
        RouteSoMigrationHelper.allowRouteReading();
        testFactory.storedWarehouse(ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
        testFactory.setSortingCenterProperty(sortingCenter.getId(),
                SortingCenterPropertiesKey.ALWAYS_RESORT_RETURNS, "false");
        cell = testFactory.storedCell(sortingCenter, "c1", CellType.RETURN, CellSubType.CLIENT_RETURN);
        var routeId = prepareOrderAndCheckMiddleMileMonitoringNotFlushingReturnRouteForResorting(
                clientReturn(sortingCenter), sortingCenter, false, true);
        routeId = testFactory.getRouteIdForSortableFlow(routeId);
        RouteSoMigrationHelper.revokeRouteReadingPermission();

        ApiCellForRouteDto cellDto = routeFacade.getCellForRoute(sortingCenter, cell.getId(), routeId);

        assertThat(cellDto.getActions()).doesNotContain(ApiCellForRouteDto.Action.SHIP_ALL);
    }

    @VisibleForTesting
    long prepareOrderAndCheckMiddleMileMonitoringNotFlushingReturnRouteForResorting(
            TestFactory.CreateOrderParams orderParams, SortingCenter sortingCenter,
            boolean returnRouteLastMile, boolean returnRouteToWarehouse
    ) {
        var orderOld = testFactory.createForToday(orderParams).accept().sort().get();

        var routeOld = testFactory.findOutgoingRoute(orderOld).orElseThrow();
        routeOld.allowNextRead();
        assertThat(routeOld.getCells(LocalDate.now(clock))).isEqualTo(List.of(cell));

        ApiCellForRouteDto cellDto = routeFacade.getCellForRoute(sortingCenter, cell.getId(), testFactory.getRouteIdForSortableFlow(routeOld));
        assertThat(cellDto.getActions()).contains(ApiCellForRouteDto.Action.SHIP_ALL);
        TestFactory.setupMockClock(clock, clock.instant().plus(1, ChronoUnit.DAYS));

        var orderNew = testFactory.createForToday(
                order(sortingCenter).deliveryService(
                                testFactory.storedDeliveryService("2", returnRouteLastMile)
                        ).dsType(returnRouteLastMile
                                ? DeliveryServiceType.LAST_MILE_COURIER
                                : DeliveryServiceType.TRANSIT).externalId("o2")
                        .isClientReturn(returnRouteToWarehouse)
                        .build()
        ).accept().sort().get();
        var routeNew = testFactory.findOutgoingRoute(orderNew).orElseThrow();
        if (routeNew.getCells(LocalDate.now(clock)).stream().noneMatch(c -> Objects.equals(c.getId(), cell.getId()))) {
            testFactory.addRouteCell(routeNew, cell, LocalDate.now(clock));
        }
        return routeNew.getId();
    }

    private TestFactory.CreateOrderParams middleMileOrder(SortingCenter sortingCenter) {
        return order(sortingCenter).externalId("o1").dsType(DeliveryServiceType.TRANSIT)
                .deliveryService(courierWithDs.deliveryService()).build();
    }

    private TestFactory.CreateOrderParams lastMileOrder(SortingCenter sortingCenter) {
        return order(sortingCenter).externalId("o1").dsType(DeliveryServiceType.LAST_MILE_COURIER).build();
    }

    private TestFactory.CreateOrderParams clientReturn(SortingCenter sortingCenter) {
        return order(sortingCenter).externalId("o1").isClientReturn(true).build();
    }

}
