package ru.yandex.market.sc.internal.domain.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.registry.OrderForRegistry;
import ru.yandex.market.sc.core.domain.registry.ScRegistryService;
import ru.yandex.market.sc.core.domain.route.model.RouteDocumentType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.DAMAGED_ORDERS_ENABLED;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static ru.yandex.market.sc.core.test.TestFactory.ordersRegistryFilter;

@EmbeddedDbIntTest
class PartnerReportServiceMockTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    PartnerReportService partnerReportService;

    @MockBean
    ScRegistryService scRegistryService;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter(100L, "Новый СЦ");
    }

    @Test
    void getOrderRegistryNotDamagedOrdersOnly() {
        testFactory.setSortingCenterProperty(sortingCenter, DAMAGED_ORDERS_ENABLED, true);
        var notDamagedOrder = testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().ship().makeReturn().accept().sort().ship().get();
        var damagedOrdersCell = testFactory.storedCell(sortingCenter,
                "ПЗ-1",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED);
        testFactory.createForToday(
                order(sortingCenter, "2").warehouseCanProcessDamagedOrders(true).build()
        ).accept().markOrderAsDamaged().sort(damagedOrdersCell.getId()).ship().get();

        var route = testFactory.findPossibleOutgoingWarehouseRoute(notDamagedOrder).orElseThrow();

        partnerReportService.getReturnRegistry(testFactory.getRouteIdForSortableFlow(route), sortingCenter, ordersRegistryFilter(RouteDocumentType.NORMAL));

        verify(scRegistryService).generateRegistry(
                any(),
                argThat(arg -> {
                    assertThat(arg).containsOnly(
                            new OrderForRegistry(1, notDamagedOrder.getExternalId(),
                                    notDamagedOrder.getAssessedCost().doubleValue(), "1/1")
                    );
                    return true;
                })
        );
    }

    @Test
    void getOrderRegistryDamagedOrdersOnly() {
        testFactory.setSortingCenterProperty(sortingCenter, DAMAGED_ORDERS_ENABLED, true);
        testFactory.createForToday(order(sortingCenter, "1").build())
                .accept().sort().ship().makeReturn().accept().sort().ship().get();
        var damagedOrdersCell = testFactory.storedCell(sortingCenter,
                "ПЗ-1",
                CellType.RETURN,
                CellSubType.RETURN_DAMAGED);
        var damagedOrder = testFactory.createForToday(
                order(sortingCenter, "2").warehouseCanProcessDamagedOrders(true).build()
        ).accept().markOrderAsDamaged().sort(damagedOrdersCell.getId()).ship().get();

        var route = testFactory.findPossibleOutgoingWarehouseRoute(damagedOrder).orElseThrow();

        partnerReportService.getReturnRegistry(testFactory.getRouteIdForSortableFlow(route),
                sortingCenter, ordersRegistryFilter(RouteDocumentType.ONLY_DAMAGED));

        verify(scRegistryService).generateRegistry(
                any(),
                argThat(arg -> {
                    assertThat(arg).containsOnly(
                            new OrderForRegistry(1, damagedOrder.getExternalId(),
                                    damagedOrder.getAssessedCost().doubleValue(), "1/1")
                    );
                    return true;
                })
        );
    }

    @Test
    void getOrderRegistryMultiOrderNotAllPlacesPresent() {
        var order = testFactory.createForToday(order(sortingCenter, "1")
                .places("1", "2")
                .build())
                .cancel()
                .acceptPlaces("1").sortPlaces("1").ship()
                .get();

        var route = testFactory.findPossibleOutgoingWarehouseRoute(order).orElseThrow();

        partnerReportService.getReturnRegistry(testFactory.getRouteIdForSortableFlow(route),
                sortingCenter, ordersRegistryFilter(RouteDocumentType.NORMAL));

        verify(scRegistryService).generateRegistry(
                any(),
                argThat(arg -> {
                    assertThat(arg).containsOnly(
                            new OrderForRegistry(1, order.getExternalId(),
                                    order.getAssessedCost().doubleValue(), "1/2")
                    );
                    return true;
                })
        );
    }

}
