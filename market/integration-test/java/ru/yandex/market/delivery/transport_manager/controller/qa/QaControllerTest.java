package ru.yandex.market.delivery.transport_manager.controller.qa;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

class QaControllerTest extends AbstractContextualTest {

    @Test
    @SneakyThrows
    @DisplayName("Поиск рейса по перемещению и наоборот")
    @DatabaseSetup({
        "/repository/route/full_routes.xml",
        "/repository/route_schedule/full_schedules.xml",
        "/repository/trip/before/trips_and_transportations.xml"
    })
    void findTripAndTransportations() {
        mockMvc.perform(get("/qa/trip/search")
            .param("tripId", "2")
            .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/trip/search_trip_success.json"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Поиск dcUnits")
    @DatabaseSetup("/repository/distribution_unit_center/distribution_center_units.xml")
    void findDcUnits() {
        mockMvc.perform(get("/qa/dc_unit/search")
                        .param("dcUnitIds", "1, 2")
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/distribution_center/search/search_success.json"));
    }
}
