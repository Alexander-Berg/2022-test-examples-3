package ru.yandex.market.delivery.transport_manager.controller.admin;

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

public class AdminTripControllerTest extends AbstractContextualTest {

    @Test
    @SneakyThrows
    @DisplayName("Получение грида рейсов")
    @DatabaseSetup({
        "/repository/route/full_routes.xml",
        "/repository/route_schedule/full_schedules.xml",
        "/repository/trip/before/trips_and_transportations.xml"
    })
    void search() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/trip/search")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/trip/grid.json", true));
    }
}
