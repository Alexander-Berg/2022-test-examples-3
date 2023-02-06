package ru.yandex.market.delivery.transport_manager.repository.mappers.route;

import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.Route;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePoint;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePointPair;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePointType;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RouteStatus;

@DatabaseSetup("/repository/route/full_routes.xml")
class RouteMapperTest extends AbstractContextualTest {

    @Autowired
    private RouteMapper mapper;

    private static final List<Route> ROUTES = List.of(route10(), route20(), route30());

    @Test
    void getByIds() {
        softly.assertThat(mapper.getByIds(List.of(10L, 20L, 30L)))
            .containsExactlyInAnyOrder(ROUTES.get(0), ROUTES.get(1), ROUTES.get(2));
    }

    private static Route route10() {
        return Route.builder()
            .id(10L)
            .status(RouteStatus.DRAFT)
            .pointPairs(List.of())
            .created(LocalDateTime.of(2021, 11, 1, 12, 30))
            .updated(LocalDateTime.of(2021, 11, 2, 12, 30))
            .build();
    }

    private static Route route20() {
        return Route.builder()
            .id(20L)
            .status(RouteStatus.ACTIVE)
            .pointPairs(List.of(
                new RoutePointPair()
                    .setOutboundPoint(
                        RoutePoint.builder()
                            .id(21L)
                            .routeId(20L)
                            .index(0)
                            .partnerId(1L)
                            .logisticsPointId(10L)
                            .type(RoutePointType.OUTBOUND)
                            .build()
                    )
                    .setInboundPoint(
                        RoutePoint.builder()
                            .id(22L)
                            .routeId(20L)
                            .index(1)
                            .partnerId(2L)
                            .logisticsPointId(20L)
                            .type(RoutePointType.INBOUND)
                            .build()
                    )
            ))
            .created(LocalDateTime.of(2021, 11, 1, 12, 30))
            .updated(LocalDateTime.of(2021, 11, 2, 12, 30))
            .name("testname")
            .build();
    }

    private static Route route30() {
        return Route.builder()
            .id(30L)
            .status(RouteStatus.ACTIVE)
            .pointPairs(List.of(
                new RoutePointPair()
                    .setOutboundPoint(
                        RoutePoint.builder()
                            .id(31L)
                            .routeId(30L)
                            .index(0)
                            .partnerId(1L)
                            .logisticsPointId(10L)
                            .type(RoutePointType.OUTBOUND)
                            .build()
                    )
                    .setInboundPoint(
                        RoutePoint.builder()
                            .id(33L)
                            .routeId(30L)
                            .index(2)
                            .partnerId(3L)
                            .logisticsPointId(30L)
                            .type(RoutePointType.INBOUND)
                            .build()
                    ),
                new RoutePointPair()
                    .setOutboundPoint(
                        RoutePoint.builder()
                            .id(32L)
                            .routeId(30L)
                            .index(1)
                            .partnerId(2L)
                            .logisticsPointId(20L)
                            .type(RoutePointType.OUTBOUND)
                            .build()
                    )
                    .setInboundPoint(
                        RoutePoint.builder()
                            .id(34L)
                            .routeId(30L)
                            .index(3)
                            .partnerId(3L)
                            .logisticsPointId(30L)
                            .type(RoutePointType.INBOUND)
                            .build()
                    )
            ))
            .created(LocalDateTime.of(2021, 11, 1, 12, 30))
            .updated(LocalDateTime.of(2021, 11, 2, 12, 30))
            .build();
    }
}
