package ru.yandex.market.sc.core.domain.route.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.SortableFlowSwitcherExtension;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@EmbeddedDbTest
class RouteFinishJdbcRepositoryTest {

    @Autowired
    TestFactory testFactory;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void testFinishWithoutRouteSo() {
        var place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingRoute(place).orElseThrow();
        testFactory.shipPlace(place);
        route = testFactory.getRoute(route.getId());

        assertThat(route.getRouteFinishes()).hasSize(1);
        assertThat(route.getRouteFinishes().stream().findAny().orElseThrow().getRouteId()).isEqualTo(route.getId());
        Long routeSoId = route.getRouteFinishes().stream().findAny().orElseThrow().getRouteSoId();

        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            var routeSo = place.getOutRoute();

            assertThat(routeSoId).isEqualTo(routeSo.getId());
        } else {
            assertThat(routeSoId).isNull();
        }
    }

    @Test
    void testFinishWithRouteSo() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.UPDATE_SORTABLE_ROUTES_IN_PLACE, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ROUTE_SO_FINISH_ENABLED, true);

        var place = testFactory.createOrderForToday(sortingCenter)
                .accept().sort().getPlace();
        var route = testFactory.findOutgoingRoute(place).orElseThrow();
        var routeSo = place.getOutRoute();
        testFactory.shipPlace(place);
        route = testFactory.getRoute(route.getId());
        assertThat(route.getRouteFinishes()).hasSize(1);
        assertThat(route.getRouteFinishes().stream().findAny().orElseThrow().getRouteId()).isEqualTo(route.getId());
        assertThat(route.getRouteFinishes().stream().findAny().orElseThrow().getRouteSoId()).isEqualTo(routeSo.getId());
    }

}
