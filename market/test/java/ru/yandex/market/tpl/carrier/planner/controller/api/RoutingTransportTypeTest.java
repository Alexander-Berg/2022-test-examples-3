package ru.yandex.market.tpl.carrier.planner.controller.api;

import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockRoutingTransportScheduleRule/defaultRoutingTransportScheduleRule.sql")
public class RoutingTransportTypeTest extends BasePlannerWebTest {

    @SneakyThrows
    @Test
    void shouldGetRoutingTransportTypes() {
        mockMvc.perform(get("/internal/routing-transport-type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(2)));
    }
}
