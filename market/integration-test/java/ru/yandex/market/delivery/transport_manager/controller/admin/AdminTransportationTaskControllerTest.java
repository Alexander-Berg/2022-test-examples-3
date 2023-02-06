package ru.yandex.market.delivery.transport_manager.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.controller.routing.RoutingControllerTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

public class AdminTransportationTaskControllerTest extends AbstractContextualTest {

    @Test
    @SneakyThrows
    @DisplayName("Получение грида рейсов")
    @DatabaseSetup({
            "/repository/transportation/all_kinds_of_transportation.xml",
            "/repository/transportation_task/transportation_tasks.xml",
            "/repository/transportation_task/transportation_task_transportations.xml"
    })
    void search() {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/transportation-task/search")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andDo(RoutingControllerTest.setResponseCharesetEncoding("UTF-8"))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/transportation_task/grid.json",
                "*.*.created",
                "*.*.updated"
            ));
    }
}
