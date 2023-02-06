package ru.yandex.market.delivery.transport_manager.repository.mappers.route;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePoint;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePointType;

class RoutePointMapperTest extends AbstractContextualTest {

    @Autowired
    RoutePointMapper routePointMapper;

    @Autowired
    RouteMapper routeMapper;

    @Test
    @DatabaseSetup("/repository/route/full_routes.xml")
    void testFindByPoints() {
        RoutePoint first = RoutePoint.builder()
            .index(0)
            .type(RoutePointType.OUTBOUND)
            .logisticsPointId(10L)
            .partnerId(1L)
            .build();
        RoutePoint second = RoutePoint.builder()
            .index(1)
            .type(RoutePointType.INBOUND)
            .logisticsPointId(20L)
            .partnerId(2L)
            .build();

        Long routeId = routePointMapper.findRouteIdByRoutePoint(List.of(first, second), 2);

        Assertions.assertThat(routeId).isEqualTo(20L);
    }

    @Test
    @DatabaseSetup("/repository/route/full_routes.xml")
    void testFindByPointsDiffPoints() {
        RoutePoint first = RoutePoint.builder()
            .index(0)
            .partnerId(1L)
            .logisticsPointId(10L)
            .type(RoutePointType.OUTBOUND)
            .build();
        RoutePoint second = RoutePoint.builder()
            .index(1)
            .partnerId(3L)
            .logisticsPointId(30L)
            .type(RoutePointType.INBOUND)
            .build();

        Long routeId = routePointMapper.findRouteIdByRoutePoint(List.of(first, second), 2);

        Assertions.assertThat(routeId).isNull();
    }

    @Test
    @DatabaseSetup("/repository/route/route_points_not_all_points.xml")
    void testFindByPointsNotFAllPointsFromRoute() {
        RoutePoint first = RoutePoint.builder()
            .index(0)
            .partnerId(1L)
            .logisticsPointId(10L)
            .type(RoutePointType.OUTBOUND)
            .build();
        RoutePoint second = RoutePoint.builder()
            .index(1)
            .partnerId(2L)
            .logisticsPointId(20L)
            .type(RoutePointType.INBOUND)
            .build();

        Long routeId = routePointMapper.findRouteIdByRoutePoint(List.of(first, second), 2);

        Assertions.assertThat(routeId).isNull();
    }


}
