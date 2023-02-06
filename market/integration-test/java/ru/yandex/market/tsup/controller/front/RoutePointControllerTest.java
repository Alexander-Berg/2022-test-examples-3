package ru.yandex.market.tsup.controller.front;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.carrier.api.RoutePointApiClient;
import ru.yandex.mj.generated.client.carrier.model.PointDto;
import ru.yandex.mj.generated.client.carrier.model.PointStatusDto;
import ru.yandex.mj.generated.client.carrier.model.PointTypeDto;
import ru.yandex.mj.generated.client.carrier.model.TimestampDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class RoutePointControllerTest extends AbstractContextualTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoutePointApiClient routePointClient;

    void setUpResponse(List<PointDto> runPoints) {
        ExecuteCall<List<PointDto>, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(runPoints));
        Mockito.when(routePointClient.internalRunsRunIdRoutePointsPost(Mockito.any(), Mockito.any()))
                .thenReturn(call);
    }

    @Test
    @SneakyThrows
    void shouldHappyPass() {
        setUpResponse(getCarrierResponse());
        mockMvc.perform(post("/runDetails/{runId}/actualTimes", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extractFileContent("fixture/route_point/route_points_request.json"))
                )
                .andDo(print())
                .andExpect(content().json(extractFileContent("fixture/route_point/route_points_response.json")));
    }

    private List<PointDto> getCarrierResponse() {
        return List.of(
                new PointDto()
                        .id(4L)
                        .routePointId(2L)
                        .type(PointTypeDto.COLLECT_DROPSHIP)
                        .latitude(BigDecimal.ONE)
                        .longitude(BigDecimal.TEN)
                        .address("Нью-Йорк")
                        .partnerName("Нью-Йорк")
                        .status(PointStatusDto.FINISHED)
                        .expectedArrivalTimestamp(Instant.parse("1989-12-31T19:00:00Z").atOffset(ZoneOffset.UTC))
                        .defaultExpectedArrivalTimestamp(new TimestampDto()
                                .timestamp(Instant.parse("1989-12-31T19:00:00Z")
                                        .atOffset(ZoneOffset.of("+3")))
                                .timezoneName("Europe/Moscow"))
                        .localExpectedArrivalTimestamp(new TimestampDto()
                                .timestamp(Instant.parse("1989-12-31T19:00:00Z")
                                        .atOffset(ZoneOffset.of("+3")))
                                .timezoneName("Europe/Moscow"))
                        .arrivalTimestamp(Instant.parse("1989-12-31T19:00:00Z").atOffset(ZoneOffset.UTC))
                        .defaultArrivalTimestamp(new TimestampDto()
                                .timestamp(Instant.parse("1989-12-31T19:00:00Z")
                                        .atOffset(ZoneOffset.of("+3")))
                                .timezoneName("Europe/Moscow"))
                        .localArrivalTimestamp(new TimestampDto()
                                .timestamp(Instant.parse("1989-12-31T19:00:00Z")
                                        .atOffset(ZoneOffset.of("+3")))
                                .timezoneName("Europe/Moscow"))
                        .departureTimestamp(Instant.parse("1989-12-31T19:20:00Z").atOffset(ZoneOffset.UTC))
                        .localDepartureTimestamp(new TimestampDto()
                                .timestamp(Instant.parse("1989-12-31T19:20:00Z")
                                        .atOffset(ZoneOffset.of("+3")))
                                .timezoneName("Europe/Moscow"))
                        .isArrivalTimestampEditable(true)
                        .photos(List.of()),
                new PointDto()
                        .id(5L)
                        .routePointId(3L)
                        .type(PointTypeDto.ORDER_RETURN)
                        .latitude(BigDecimal.ONE)
                        .longitude(BigDecimal.TEN)
                        .address("Нью-Йорк")
                        .partnerName("Нью-Йорк")
                        .status(PointStatusDto.IN_PROGRESS)
                        .expectedArrivalTimestamp(Instant.parse("1989-12-31T19:00:00Z").atOffset(ZoneOffset.UTC))
                        .defaultExpectedArrivalTimestamp(new TimestampDto()
                                .timestamp(Instant.parse("1989-12-31T19:00:00Z")
                                        .atOffset(ZoneOffset.of("+3")))
                                .timezoneName("Europe/Moscow"))
                        .localExpectedArrivalTimestamp(new TimestampDto()
                                .timestamp(Instant.parse("1989-12-31T19:00:00Z")
                                        .atOffset(ZoneOffset.of("+3")))
                                .timezoneName("Europe/Moscow"))
                        .defaultArrivalTimestamp(new TimestampDto()
                                .timezoneName("Europe/Moscow"))
                        .localArrivalTimestamp(new TimestampDto()
                                .timezoneName("Europe/Moscow"))
                        .isArrivalTimestampEditable(false)
                        .photos(List.of())
        );
    }
}
