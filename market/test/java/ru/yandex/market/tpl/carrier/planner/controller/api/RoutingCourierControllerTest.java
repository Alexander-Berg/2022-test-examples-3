package ru.yandex.market.tpl.carrier.planner.controller.api;

import java.time.LocalDate;

import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockRoutingTransportScheduleRule/defaultRoutingTransportScheduleRule.sql")
public class RoutingCourierControllerTest extends BasePlannerWebTest {

    @SneakyThrows
    @Test
    void shouldGetRoutingCouriers() {
        mockMvc.perform(get("/routing/couriers")
                .contentType(MediaType.APPLICATION_JSON)
                .param("depotId", "172")
                .param("date", LocalDate.of(2022, 2, 2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(Matchers.hasSize(5)))
                .andExpect(jsonPath("$[*].ref").value(Matchers.hasItem("3 газельки от Истварда-1-1-Иствард-Газель")))
                .andExpect(jsonPath("$[*].maximalStops").value(Matchers.everyItem(Matchers.notNullValue())));
    }
}
