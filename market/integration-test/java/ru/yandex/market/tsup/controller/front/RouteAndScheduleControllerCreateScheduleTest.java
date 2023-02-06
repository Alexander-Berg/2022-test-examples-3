package ru.yandex.market.tsup.controller.front;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleDto;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tsup.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("LineLength")
public class RouteAndScheduleControllerCreateScheduleTest extends AbstractContextualTest {
    @Autowired
    private TransportManagerClient transportManagerClient;

    @SneakyThrows
    @Test
    void shouldCreateSchedule() {
        Mockito.when(transportManagerClient.findOrCreateRouteSchedule(Mockito.any()))
                .thenReturn(RouteScheduleDto.builder()
                        .id(234L)
                        .build());

        mockMvc.perform(post("/routes/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(IntegrationTestUtils.extractFileContent(
                        "fixture/route/schedule/create_route_schedule.json"
                ))
        )
                .andExpect(status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent(
                        "fixture/route/schedule/create_route_schedule_response.json"
                ));
    }

    @SneakyThrows
    @Test
    void shouldCreateScheduleWithMergedPointsTimes() {
        Mockito.when(transportManagerClient.findOrCreateRouteSchedule(Mockito.any()))
                .thenReturn(RouteScheduleDto.builder()
                        .id(234L)
                        .build());

        mockMvc.perform(post("/routes/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(IntegrationTestUtils.extractFileContent(
                                "fixture/route/schedule/create_route_schedule_with_merged_points_times.json"
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent(
                        "fixture/route/schedule/create_route_schedule_response.json"
                ));
    }

    @SneakyThrows
    @Test
    void shouldCreateScheduleFailed() {
        Mockito.when(transportManagerClient.findOrCreateRouteSchedule(Mockito.any()))
            .thenReturn(RouteScheduleDto.builder()
                .id(234L)
                .build());

        mockMvc.perform(post("/routes/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(IntegrationTestUtils.extractFileContent(
                    "fixture/route/schedule/create_route_schedule_failed.json"
                ))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Неправильно задано время " +
                "транзита между точками 1 и 0 или время слотов на этих точках"));

    }

    @SneakyThrows
    @Test
    void shouldCreateScheduleWithMergedPointsTimesFailed() {
        Mockito.when(transportManagerClient.findOrCreateRouteSchedule(Mockito.any()))
                .thenReturn(RouteScheduleDto.builder()
                        .id(234L)
                        .build());

        mockMvc.perform(post("/routes/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(IntegrationTestUtils.extractFileContent(
                                "fixture/route/schedule" +
                                        "/create_route_schedule_with_merged_points_times_failed.json"
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неправильно задано время транзита " +
                        "между точками 3 и 1 или время слотов на этих точках"));

        mockMvc.perform(post("/routes/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(IntegrationTestUtils.extractFileContent(
                                "fixture/route/schedule/create_route_schedule_with_merged_points_times_failed_transition_time.json"
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неправильно задано время транзита " +
                        "между схлопнутыми точками 1 и 0 или время слотов на этих точках"));

        mockMvc.perform(post("/routes/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(IntegrationTestUtils.extractFileContent(
                                "fixture/route/schedule/create_route_schedule_with_merged_points_times_failed_start_time.json"
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неправильно задано время транзита " +
                        "между схлопнутыми точками 1 и 0 или время слотов на этих точках"));

        mockMvc.perform(post("/routes/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(IntegrationTestUtils.extractFileContent(
                                "fixture/route/schedule/create_route_schedule_with_merged_points_times_failed_operation_type.json"
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Неправильно задано время транзита между схлопнутыми точками 1 и 0 " +
                                "или время слотов на этих точках - транзитное время не может быть 0"));

    }

    @SneakyThrows
    @DisplayName("MAGISTRALDUTY-286")
    @Test
    void routePointDefaultValidationWorking() {
        mockMvc.perform(post("/routes/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(IntegrationTestUtils.extractFileContent(
                                "fixture/route/schedule/create_route_schedule_failed_route_point_property.json"
                        ))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("routePoint[1] has violations: transitionTime must not be null"));
    }

    @SneakyThrows
    @Test
    void shouldUpdateSchedule() {
        Mockito.when(transportManagerClient.updateRouteSchedule(Mockito.any()))
                .thenReturn(RouteScheduleDto.builder()
                        .id(234L)
                        .build());

        mockMvc.perform(put("/routes/schedule/234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(IntegrationTestUtils.extractFileContent(
                                "fixture/route/schedule/update_route_schedule.json"
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent(
                        "fixture/route/schedule/create_route_schedule_response.json"
                ));
    }
}
