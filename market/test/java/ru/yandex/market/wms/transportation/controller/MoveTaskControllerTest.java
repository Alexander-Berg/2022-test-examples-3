package ru.yandex.market.wms.transportation.controller;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

class MoveTaskControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/controller/move-tasks/common-state.xml")
    @DatabaseSetup("/controller/move-tasks/start-task-successful/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/common-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/move-tasks/start-task-successful/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void startTaskSuccessfulTest() throws Exception {
        mockMvc.perform(post("/usertasks/move/start-and-assign-next")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/move-tasks/start-task-successful/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/move-tasks/start-task-successful/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/common-state.xml")
    @DatabaseSetup("/controller/move-tasks/finish-task-successful/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/common-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/move-tasks/finish-task-successful/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void finishTaskSuccessfulTest() throws Exception {
        mockMvc.perform(post("/usertasks/move/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/move-tasks/finish-task-successful/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/common-state.xml")
    @DatabaseSetup("/controller/move-tasks/decline-task-successful/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/common-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/move-tasks/decline-task-successful/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void declineTaskSuccessfulTest() throws Exception {
        mockMvc.perform(post("/usertasks/move/decline")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/move-tasks/decline-task-successful/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/assign-and-start-successful/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/assign-and-start-successful/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignAndStartSuccessful() throws Exception {
        mockMvc.perform(post("/usertasks/move/assign-and-start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/move-tasks/assign-and-start-successful/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/move-tasks/assign-and-start-successful/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/assign-and-start-unfinished-tos-exists/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/assign-and-start-unfinished-tos-exists/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignAndStartUnfinishedTosExists() throws Exception {
        mockMvc.perform(post("/usertasks/move/assign-and-start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/move-tasks/assign-and-start-unfinished-tos-exists/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/move-tasks/assign-and-start-unfinished-tos-exists/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/assign-and-start-tos-not-found/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/assign-and-start-tos-not-found/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignAndStartTosNotFound() throws Exception {
        mockMvc.perform(post("/usertasks/move/assign-and-start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/move-tasks/assign-and-start-tos-not-found/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/move-tasks/assign-and-start-tos-not-found/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/assign-and-start-few-tos-found/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/assign-and-start-few-tos-found/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignAndStartFewTosFound() throws Exception {
        mockMvc.perform(post("/usertasks/move/assign-and-start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/move-tasks/assign-and-start-few-tos-found/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/move-tasks/assign-and-start-few-tos-found/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/check-and-finish-successful/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/check-and-finish-successful/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkAndFinishSuccessful() throws Exception {
        mockMvc.perform(post("/usertasks/move/check-and-finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/move-tasks/check-and-finish-successful/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/check-and-finish-wrong-loc/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/check-and-finish-wrong-loc/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkAndFinishWrongLoc() throws Exception {
        mockMvc.perform(post("/usertasks/move/check-and-finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/move-tasks/check-and-finish-wrong-loc/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/move-tasks/check-and-finish-wrong-loc/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/check-and-finish-wrong-user/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/check-and-finish-wrong-user/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkAndFinishWrongUser() throws Exception {
        mockMvc.perform(post("/usertasks/move/check-and-finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/move-tasks/check-and-finish-wrong-user/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/move-tasks/check-and-finish-wrong-user/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/check-and-finish-to-not-found/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/check-and-finish-to-not-found/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkAndFinishToNotFound() throws Exception {
        mockMvc.perform(post("/usertasks/move/check-and-finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/move-tasks/check-and-finish-to-not-found/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/move-tasks/check-and-finish-to-not-found/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/check-and-finish-already-moved/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/check-and-finish-already-moved/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkAndFinishWithAlreadyMovedBalances() throws Exception {
        mockMvc.perform(post("/usertasks/move/check-and-finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/move-tasks/check-and-finish-already-moved/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/common-state.xml")
    @DatabaseSetup("/controller/move-tasks/assing-task-successful/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/common-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/move-tasks/assing-task-successful/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignTaskSuccessfulTest() throws Exception {
        mockMvc.perform(post("/usertasks/move/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/move-tasks/assing-task-successful/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/move-tasks/assing-task-successful/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/common-state.xml")
    @DatabaseSetup("/controller/move-tasks/assing-task-successful/initial-state.xml")
    @ExpectedDatabase(value = "/controller/move-tasks/common-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/move-tasks/assing-task-successful/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignTaskByUnitId() throws Exception {
        mockMvc.perform(post("/usertasks/move/assign-unit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/move-tasks/assing-task-successful/unit-request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/move-tasks/assing-task-successful/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/common-state.xml")
    @DatabaseSetup(value = "/controller/move-tasks/start-tasks-by-keys/initial-state.xml",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/controller/move-tasks/start-tasks-by-keys/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void startTasks() throws Exception {
        mockMvc.perform(post("/usertasks/move/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/move-tasks/start-tasks-by-keys/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/move-tasks/start-tasks-by-keys/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/common-state.xml")
    @DatabaseSetup(value = "/controller/move-tasks/start-tasks-wrong-user/initial-state.xml",
            type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/controller/move-tasks/start-tasks-wrong-user/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void startTasksByWrongUser() throws Exception {
        mockMvc.perform(post("/usertasks/move/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/move-tasks/start-tasks-wrong-user/request.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/common-state.xml")
    @DatabaseSetup(value = "/controller/move-tasks/rollback-task/initial-state.xml", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/controller/move-tasks/rollback-task/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void rollbackTask() throws Exception {
        mockMvc.perform(post("/usertasks/move/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/move-tasks/rollback-task/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/common-state.xml")
    @DatabaseSetup(value = "/controller/move-tasks/rollback-task/initial-state.xml", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/controller/move-tasks/rollback-task/final-state-peer-loc.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void rollbackTaskToSameZone() throws Exception {
        mockMvc.perform(post("/usertasks/move/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/move-tasks/rollback-task/request-peer-loc.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/common-state.xml")
    @DatabaseSetup(value = "/controller/move-tasks/rollback-task/initial-state.xml", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/controller/move-tasks/rollback-task/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void rollbackTaskToWrongLocation() throws Exception {
        mockMvc.perform(post("/usertasks/move/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/move-tasks/rollback-task/request-wrong-location.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/controller/move-tasks/common-state.xml")
    @DatabaseSetup(value = "/controller/move-tasks/rollback-task/initial-state.xml", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/controller/move-tasks/rollback-task/initial-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void rollbackTaskOfWrongUnit() throws Exception {
        mockMvc.perform(post("/usertasks/move/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/move-tasks/rollback-task/request-wrong-unit.json")))
                .andExpect(status().isBadRequest());
    }
}
