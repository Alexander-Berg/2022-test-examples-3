package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.time.LocalTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.CreateRoutingTransportScheduleRuleDto;
import ru.yandex.mj.generated.server.model.RoutingTransportScheduleDto;
import ru.yandex.mj.generated.server.model.RoutingTransportVehicleTypeDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RoutingScheduleRuleTransportTest extends BasePlannerWebTest {

    private final ObjectMapper objectMapper;

    @Sql("classpath:mockRoutingTransportScheduleRule/defaultRoutingTransportScheduleRule.sql")
    @SneakyThrows
    @Test
    void shouldGetRoutingScheduleRule() {
        mockMvc.perform(get(
                "/internal/delivery-services/{deliveryServiceId}/routing-transport-schedule-rule",
                        223462
                )
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(Matchers.hasSize(2)));
    }

    @Sql("classpath:mockRoutingTransportScheduleRule/defaultRoutingTransportScheduleRule.sql")
    @SneakyThrows
    @Test
    void shouldCreateRoutingScheduleRule() {
        RoutingTransportScheduleDto schedule = new RoutingTransportScheduleDto();
        schedule.setVehicleType(RoutingTransportVehicleTypeDto.CAR);
        schedule.setShiftStartTime(LocalTime.of(0, 0).toString());
        schedule.setShiftEndTime(LocalTime.of(0, 0).toString());
        schedule.setEndDayOffset(1);

        CreateRoutingTransportScheduleRuleDto dto = new CreateRoutingTransportScheduleRuleDto();
        dto.setTransportTypeId(1L);
        dto.setName("5 газелей от Альтики");
        dto.setSchedule(schedule);
        dto.setCount(5);
        dto.setDepotId(172L);
        mockMvc.perform(MockMvcRequestBuilders.post(
                "/internal/delivery-services/{deliveryServiceId}/routing-transport-schedule-rule",
                        223463
                )       .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

    }
}
