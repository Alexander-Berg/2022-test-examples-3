package ru.yandex.market.wms.picking.modules.sample.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class MoveItemControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/sample/controller/move-item/happy-path/before.xml")
    @ExpectedDatabase(value = "/sample/controller/move-item/happy-path/after.xml", assertionMode = NON_STRICT)
    public void moveItemHappyPath() throws Exception {
        mockMvc.perform(post("/sample/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("sample/controller/move-item/happy-path/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/move-item/no-assignment-number/before.xml")
    @ExpectedDatabase(value = "/sample/controller/move-item/no-assignment-number/after.xml", assertionMode = NON_STRICT)
    public void moveItemWhenContainerHasNoAssignmentNumber() throws Exception {
        mockMvc.perform(post("/sample/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("sample/controller/move-item/no-assignment-number/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/move-item/wrong-container/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/move-item/wrong-container/immutable-state.xml",
            assertionMode = NON_STRICT)
    public void moveItemFromWrongContainer() throws Exception {
        mockMvc.perform(post("/sample/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("sample/controller/move-item/wrong-container/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("sample/controller/move-item/wrong-container/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/move-item/container-not-attached/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/move-item/container-not-attached/immutable-state.xml",
            assertionMode = NON_STRICT)
    public void moveItemWhenContainerNotAttachedToUser() throws Exception {
        mockMvc.perform(post("/sample/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("sample/controller/move-item/container-not-attached/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .json(getFileContent("sample/controller/move-item/container-not-attached/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/move-item/task-not-assigned/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/move-item/task-not-assigned/immutable-state.xml",
            assertionMode = NON_STRICT)
    public void moveItemWhenTaskNotAssignedToUser() throws Exception {
        mockMvc.perform(post("/sample/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("sample/controller/move-item/task-not-assigned/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .json(getFileContent("sample/controller/move-item/task-not-assigned/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/move-item/assignment-numbers-not-matched/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/move-item/assignment-numbers-not-matched/immutable-state.xml",
            assertionMode = NON_STRICT)
    public void moveItemWhenTaskAndContainerAssignmentNumbersNotMatched() throws Exception {
        mockMvc.perform(post("/sample/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "sample/controller/move-item/assignment-numbers-not-matched/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "sample/controller/move-item/assignment-numbers-not-matched/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/sample/controller/move-item/assignment-processed/immutable-state.xml")
    @ExpectedDatabase(value = "/sample/controller/move-item/assignment-processed/immutable-state.xml",
            assertionMode = NON_STRICT)
    public void moveItemWhenTaskAlreadyProcessed() throws Exception {
        mockMvc.perform(post("/sample/move-item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("sample/controller/move-item/assignment-processed/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .json(getFileContent("sample/controller/move-item/assignment-processed/response.json")))
                .andReturn();
    }
}
