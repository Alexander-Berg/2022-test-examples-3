package ru.yandex.market.delivery.transport_manager.admin.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup({
    "/repository/route/full_routes.xml",
    "/repository/route_schedule/full_schedules.xml",
    "/repository/trip/before/trips_and_transportations.xml"
})
class AdminRouteScheduleControllerTest extends AbstractContextualTest {

    @Test
    @SneakyThrows
    @DisplayName("Получение детальной каточки расписания маршрута")
    void getRouteSchedule() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/route-schedule/100")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/route_schedule/detail.json", true));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение грида точек расписания маршрута")
    void getPoints() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/route-schedule/points")
            .param("id", "100")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/route_schedule/points_grid.json", true));
    }
}
