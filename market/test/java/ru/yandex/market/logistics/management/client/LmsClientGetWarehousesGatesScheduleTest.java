package ru.yandex.market.logistics.management.client;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateCustomScheduleResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGatesCustomScheduleResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGatesScheduleResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDateTimeResponse;
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

public class LmsClientGetWarehousesGatesScheduleTest extends AbstractClientTest {

    private static final LocalDate DATE_FROM = LocalDate.of(2019, 11, 4);
    private static final LocalDate DATE_TO = LocalDate.of(2019, 11, 11);

    @Test
    void getWarehousesGatesScheduleTest() {
        mockServer.expect(requestTo(
            getBuilder(uri, "/export/partners/1/warehousesGatesSchedule")
                .queryParam("from", DATE_FROM)
                .queryParam("to", DATE_TO)
                .toUriString()
        ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_warehouses_gates_schedule.json")));

        List<LogisticsPointGatesScheduleResponse> warehousesGatesSchedule =
            client.getWarehousesGatesScheduleByPartnerId(1L, DATE_FROM, DATE_TO);

        softly.assertThat(warehousesGatesSchedule).as("Response should not be empty").isNotEmpty();
        softly.assertThat(warehousesGatesSchedule).as("Should have one warehouse").hasSize(1);
        softly.assertThat(warehousesGatesSchedule)
            .as("Should have correct data")
            .containsExactlyInAnyOrder(
                LogisticsPointGatesScheduleResponse.newBuilder()
                    .schedule(
                        getSchedule(
                            IntStream.range(4, 9), LocalTime.of(10, 0), LocalTime.of(18, 0)
                        ))
                    .gates(getGates(ImmutableMap.of(1L, "gate11", 2L, "gate12")))
                    .build()
            );
    }

    @Test
    void getWarehousesGatesScheduleV2Test() {
        mockServer.expect(requestTo(
            getBuilder(uri, "/export/partners/1/warehousesGatesCustomSchedule")
                .queryParam("from", DATE_FROM)
                .queryParam("to", DATE_TO)
                .toUriString()
        ))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonResource("data/controller/partner_warehouses_gates_schedule_v2_mini.json")));

        List<LogisticsPointGatesCustomScheduleResponse> warehousesGatesSchedule =
            client.getWarehousesGatesCustomScheduleByPartnerId(1L, DATE_FROM, DATE_TO);

        softly.assertThat(warehousesGatesSchedule).as("Response should not be empty").isNotEmpty();
        softly.assertThat(warehousesGatesSchedule).as("Should have one warehouse").hasSize(1);
        softly.assertThat(warehousesGatesSchedule)
            .as("Should have correct data")
            .containsExactlyInAnyOrder(
                LogisticsPointGatesCustomScheduleResponse.newBuilder()
                    .gates(List.of(LogisticsPointGateCustomScheduleResponse.newBuilder()
                        .id(4L)
                        .gateNumber("gate32")
                        .enabled(false)
                        .types(EnumSet.of(GateTypeResponse.INBOUND, GateTypeResponse.OUTBOUND))
                        .schedule(List.of(ScheduleDateTimeResponse.newBuilder()
                            .date(LocalDate.of(2021, 6, 22))
                            .from(LocalTime.of(12, 0))
                            .to(LocalTime.of(16, 0))
                            .build()))
                        .build()))
                    .logisticsPointId(3L)
                    .build()
            );
    }

    private List<ScheduleDateTimeResponse> getSchedule(IntStream days, LocalTime from, LocalTime to) {
        return days.mapToObj(
            day -> ScheduleDateTimeResponse.newBuilder()
                .date(LocalDate.of(2019, 11, day))
                .from(from)
                .to(to).build()
        ).collect(Collectors.toList());
    }

    private Set<LogisticsPointGateResponse> getGates(Map<Long, String> gates) {
        return gates.entrySet().stream()
            .map(gate -> LogisticsPointGateResponse.newBuilder()
                .id(gate.getKey())
                .gateNumber(gate.getValue())
                .enabled(false)
                .types(EnumSet.of(GateTypeResponse.INBOUND, GateTypeResponse.OUTBOUND))
                .build()
            )
            .collect(Collectors.toSet());
    }
}
