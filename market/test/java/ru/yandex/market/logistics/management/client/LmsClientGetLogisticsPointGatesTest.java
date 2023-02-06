package ru.yandex.market.logistics.management.client;

import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

@DisplayName("Ворота логистических точек")
public class LmsClientGetLogisticsPointGatesTest extends AbstractClientTest {

    @Test
    @DisplayName("Получение ворот логистической точки")
    void getLogisticsPointGates() {
        mockServer.expect(requestTo(uri + "/externalApi/logisticsPoints/1/gates"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/logistics_points/gates.json"))
            );

        List<LogisticsPointGateResponse> response = client.getLogisticsPointGates(1L);
        softly.assertThat(response).hasSize(1);
        softly.assertThat(response.get(0)).usingRecursiveComparison().isEqualTo(LogisticsPointGateResponse.newBuilder()
            .id(1000L)
            .gateNumber("1-1")
            .enabled(true)
            .types(EnumSet.of(GateTypeResponse.INBOUND))
            .schedule(Set.of(
                new ScheduleDayResponse(
                    5L,
                    1,
                    LocalTime.of(12, 0, 0),
                    LocalTime.of(18, 0, 0),
                    true
                ),
                new ScheduleDayResponse(
                    6L,
                    2,
                    LocalTime.of(12, 0, 0),
                    LocalTime.of(15, 0, 0),
                    true
                )
            ))
        );
    }
}
