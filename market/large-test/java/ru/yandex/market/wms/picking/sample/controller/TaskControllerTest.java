package ru.yandex.market.wms.picking.sample.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.yandex.market.wms.picking.utils.TestcontainersConfiguration;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class TaskControllerTest extends TestcontainersConfiguration {

    @Test
    @DatabaseSetup(value = "/testcontainers/sample/controller/task/create-happy-pass/before.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/testcontainers/sample/controller/task/create-happy-pass/after.xml",
            assertionMode = NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void createTasksHappyPassTest() throws Exception {
        mockMvc.perform(post("/sample/task/MSRMNT_STK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "testcontainers/sample/controller/task/create-happy-pass/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "testcontainers/sample/controller/task/create-happy-pass/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/sample/controller/task/assign-task-happy-pass/before.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/testcontainers/sample/controller/task/assign-task-happy-pass/after.xml",
            assertionMode = NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void assignTasksHappyPassTest() throws Exception {
        mockMvc.perform(post("/sample/task/MSRMNT_STK/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "testcontainers/sample/controller/task/assign-task-happy-pass/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "testcontainers/sample/controller/task/assign-task-happy-pass/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/testcontainers/sample/controller/task/assign-tasks-when-active-exist/immutable.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/testcontainers/sample/controller/task/assign-tasks-when-active-exist/immutable.xml",
            assertionMode = NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void assignTasksWhenActiveExists() throws Exception {
        mockMvc.perform(post("/sample/task/MSRMNT_STK/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "testcontainers/sample/controller/task/assign-tasks-when-active-exist/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "testcontainers/sample/controller/task/assign-tasks-when-active-exist/response.json")))
                .andReturn();
    }
}
