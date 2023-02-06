package ru.yandex.market.delivery.transport_manager.converter.routing;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.dto.routing.RouteItemDto;
import ru.yandex.market.delivery.transport_manager.dto.routing.RoutePointDto;
import ru.yandex.market.delivery.transport_manager.dto.routing.RoutingResultDto;
import ru.yandex.market.delivery.transport_manager.dto.routing.UserRoutingResultDto;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingLocationType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResponseItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResponseRoutePoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultShift;

class RoutingConverterTest {
    @Test
    void convert() {
        RoutingResultDto actual = RoutingConverter.convert(new RoutingResult(
            null,
            RoutingRequest.builder()
                .users(Set.of(
                        RoutingCourier.builder()
                                .id(1L)
                                .depotId(0)
                                .partnerId(10001L)
                                .build(),
                        RoutingCourier.builder()
                                .id(2L)
                                .depotId(0)
                                .partnerId(10002L)
                                .build()
                ))
                .build(),
            null,
            RoutingProfileType.CUSTOM,
            RoutingMockType.REAL,
            Map.of(
                1L, new RoutingResultShift(1L, List.of(
                    new RoutingResponseRoutePoint(
                        Instant.MAX,
                        Instant.MAX,
                        null,
                        List.of(
                            new RoutingResponseItem("", List.of(1L, 2L), null, null),
                            new RoutingResponseItem("", List.of(3L), null, null)
                        ),
                        RoutingLocationType.delivery
                    ),
                    new RoutingResponseRoutePoint(
                        Instant.MAX,
                        Instant.MAX,
                        null,
                        List.of(
                            new RoutingResponseItem("", List.of(4L), null, null)
                        ),
                        RoutingLocationType.delivery
                    )
                )),
                2L, new RoutingResultShift(2L, List.of(
                    new RoutingResponseRoutePoint(
                        Instant.MAX,
                        Instant.MAX,
                        null,
                        List.of(
                            new RoutingResponseItem("", List.of(5L), null, null)
                        ),
                        RoutingLocationType.delivery
                    )))
            ),
            null,
            null
        ));

        RoutingResultDto expected = new RoutingResultDto(
            Map.of(
                1L, new UserRoutingResultDto(List.of(
                    new RoutePointDto(List.of(new RouteItemDto(List.of(1L, 2L)), new RouteItemDto(List.of(3L)))),
                    new RoutePointDto(List.of(new RouteItemDto(List.of(4L))))
                )),
                2L, new UserRoutingResultDto(List.of(
                    new RoutePointDto(List.of(new RouteItemDto(List.of(5L))))
                ))
            ),
            Map.of(
                1L, 10001L,
                2L, 10002L
            )
        );

        Assertions.assertEquals(expected, actual);
    }
}
