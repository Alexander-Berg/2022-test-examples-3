package ru.yandex.market.delivery.transport_manager.admin.controller;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

class AdminDefectTransportationTaskControllerTest extends AbstractContextualTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @SneakyThrows
    void getDefectTransportationTaskDto() {
        mockMvc.perform(
                get("/admin/transportation-task-defect/new")
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/transportation_task/defect/new.json"));
    }

    @ExpectedDatabase(
        value = "/repository/transportation_task/after/defect_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    @SneakyThrows
    void createTransportationTask() {
        mockMvc.perform(
                post("/admin/transportation-task-defect")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent("controller/transportation_task/defect/request.json"))
            )
            .andExpect(status().isOk());
    }
}
