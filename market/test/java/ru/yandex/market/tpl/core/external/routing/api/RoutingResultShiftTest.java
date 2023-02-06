package ru.yandex.market.tpl.core.external.routing.api;

import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RoutingResultShiftTest {

    @Test
    void getClientReturnIds() {
        //given
        var response = buildTestRoutingResponse();

        //when
        var resultShift = new RoutingResultShift(0, response);

        //then
        Assertions.assertThat(resultShift.getClientReturnIds()).hasSize(4);
        Assertions.assertThat(resultShift.getClientReturnIds()).containsExactlyInAnyOrder(11L, 12L, 21L, 22L);
    }

    private List<RoutingResponseRoutePoint> buildTestRoutingResponse() {
        var responseItem11 = new RoutingResponseItem(
                null, List.of(), List.of(
                new RoutingResponseItemTask(11L, RoutingRequestItemType.CLIENT_RETURN),
                new RoutingResponseItemTask(110L, RoutingRequestItemType.CLIENT)
        ), null);
        var responseItem12 = new RoutingResponseItem(
                null, List.of(), List.of(
                new RoutingResponseItemTask(12L, RoutingRequestItemType.CLIENT_RETURN),
                new RoutingResponseItemTask(120L, RoutingRequestItemType.DROPOFF_CARGO_RETURN)
        ), null);
        var responseItem21 = new RoutingResponseItem(
                null, List.of(), List.of(
                new RoutingResponseItemTask(21L, RoutingRequestItemType.CLIENT_RETURN),
                new RoutingResponseItemTask(210L, RoutingRequestItemType.DROPSHIP)
        ), null);
        var responseItem22 = new RoutingResponseItem(
                null, List.of(), List.of(
                new RoutingResponseItemTask(22L, RoutingRequestItemType.CLIENT_RETURN),
                new RoutingResponseItemTask(220L, RoutingRequestItemType.LAVKA)
        ), null);


        var responseRoutePoint1 = new RoutingResponseRoutePoint(
                Instant.now(), Instant.now(),
                null, List.of(responseItem11, responseItem12), null
        );
        var responseRoutePoint2 = new RoutingResponseRoutePoint(
                Instant.now(), Instant.now(),
                null, List.of(responseItem21, responseItem22), null
        );

        List<RoutingResponseRoutePoint> response = List.of(responseRoutePoint1, responseRoutePoint2);
        return response;
    }
}
