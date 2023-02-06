package ru.yandex.market.delivery.transport_manager.repository.mappers.route;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.Route;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePoint;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePointPair;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePointType;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RouteStatus;
import ru.yandex.market.delivery.transport_manager.service.route.RouteFindOrCreateService;

public class RouteCreationTest extends AbstractContextualTest {

    @Autowired
    RouteFindOrCreateService routeFindOrCreateService;

    @Test
    @DisplayName("Успешное создание маршрута")
    @ExpectedDatabase(
        value = "/repository/route/after/create_simple_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void createRouteSuccess() throws Exception {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
        RoutePoint firstPoint = RoutePoint.builder()
            .type(RoutePointType.OUTBOUND)
            .routeId(1L)
            .index(0)
            .partnerId(1L)
            .logisticsPointId(101L)
            .build();
        RoutePoint secondPoint = RoutePoint.builder()
            .type(RoutePointType.INBOUND)
            .routeId(1L)
            .index(1)
            .partnerId(2L)
            .logisticsPointId(102L)
            .build();
        var relation = new RoutePointPair(firstPoint, secondPoint);
        Route route = Route.builder()
            .name("Название")
            .status(RouteStatus.ACTIVE)
            .pointPairs(List.of(relation))
            .build();
        routeFindOrCreateService.findOrCreate(route);
    }
}
