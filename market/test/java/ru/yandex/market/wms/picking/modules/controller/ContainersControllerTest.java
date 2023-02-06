package ru.yandex.market.wms.picking.modules.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class ContainersControllerTest extends IntegrationTest {
    @Test
    @DatabaseSetup("/controller/containers/unassign-happy-path/before.xml")
    @ExpectedDatabase(value = "/controller/containers/unassign-happy-path/after.xml", assertionMode = NON_STRICT)
    public void unassignContainerHappyPath() throws Exception {
        mockMvc.perform(delete("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/unassign-happy-path/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/failed-container-not-empty/immutable.xml")
    @ExpectedDatabase(value = "/controller/containers/failed-container-not-empty/immutable.xml",
            assertionMode = NON_STRICT)
    public void unassignNotEmptyContainerFails() throws Exception {
         mockMvc.perform(delete("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/failed-container-not-empty/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/containers/failed-container-not-empty/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/failed-has-attached-assignment/immutable.xml")
    @ExpectedDatabase(value = "/controller/containers/failed-has-attached-assignment/immutable.xml",
            assertionMode = NON_STRICT)
    public void unassignContainerWithAttachedAssignmentFails() throws Exception {
        mockMvc.perform(delete("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/failed-has-attached-assignment/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/containers/failed-has-attached-assignment/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/failed-container-not-attached-to-user/immutable.xml")
    @ExpectedDatabase(value = "/controller/containers/failed-container-not-attached-to-user/immutable.xml",
            assertionMode = NON_STRICT)
    public void noContainerAttachedToUser() throws Exception {
         mockMvc.perform(delete("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/failed-container-not-attached-to-user/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/containers/failed-container-not-attached-to-user/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/assign-container-happy-path/before.xml")
    @ExpectedDatabase(value = "/controller/containers/assign-container-happy-path/after.xml",
            assertionMode = NON_STRICT)
    public void assignContainerHappyPath() throws Exception {
        mockMvc.perform(post("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/assign-container-happy-path/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/assign-container-max-picking-count-exceeded/immutable.xml")
    @ExpectedDatabase(value = "/controller/containers/assign-container-max-picking-count-exceeded/immutable.xml",
            assertionMode = NON_STRICT)
    public void assignContainerMaxPickingCountExceeded() throws Exception {
        mockMvc.perform(post("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/containers/assign-container-max-picking-count-exceeded/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/containers/assign-container-max-picking-count-exceeded/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/assign-container-already-occupied/immutable.xml")
    @ExpectedDatabase(value = "/controller/containers/assign-container-already-occupied/immutable.xml",
            assertionMode = NON_STRICT)
    public void assignContainerAlreadyOccupied() throws Exception {
        mockMvc.perform(post("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/assign-container-already-occupied/request.json")))
                .andExpect(status().isNotFound())
                .andExpect(content().json(getFileContent(
                        "controller/containers/assign-container-already-occupied/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/assign-container-not-empty/immutable.xml")
    @ExpectedDatabase(value = "/controller/containers/assign-container-not-empty/immutable.xml",
            assertionMode = NON_STRICT)
    public void assignContainerNotEmpty() throws Exception {
        mockMvc.perform(post("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/assign-container-not-empty/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/containers/assign-container-not-empty/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/assign-container-when-not-exists/before.xml")
    @ExpectedDatabase(value = "/controller/containers/assign-container-when-not-exists/after.xml",
            assertionMode = NON_STRICT)
    public void assignContainerWhenNotExists() throws Exception {
        mockMvc.perform(post("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/assign-container-when-not-exists/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/assign-container-when-tote-not-exists/before.xml")
    @ExpectedDatabase(value = "/controller/containers/assign-container-when-tote-not-exists/after.xml",
            assertionMode = NON_STRICT)
    public void assignContainerWhenPickingToteNotExists() throws Exception {
        mockMvc.perform(post("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/assign-container-when-tote-not-exists/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/assign-second-container-happy-path/before.xml")
    @ExpectedDatabase(value = "/controller/containers/assign-second-container-happy-path/after.xml",
            assertionMode = NON_STRICT)
    public void assignSecondContainerHappyPath() throws Exception {
        mockMvc.perform(post("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/assign-second-container-happy-path/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/assign-fifth-container/before.xml")
    @ExpectedDatabase(value = "/controller/containers/assign-fifth-container/after.xml",
            assertionMode = NON_STRICT)
    public void assignFifthContainerAfterSecondAndThirdBeingDropped() throws Exception {
        mockMvc.perform(post("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/assign-fifth-container/request.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/containers/assign-container-after-assign/before.xml")
    public void assignContainerAfterAssignContainerCall() throws Exception {
        mockMvc.perform(post("/assign-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/containers/assign-container-after-assign/first-assign.json")))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "controller/containers/assign-container-after-assign/first-assign-response.json")))
                .andReturn();
        mockMvc.perform(post("/containers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/containers/assign-container-after-assign/second-assign.json")))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/containers/assign-container-after-assign/response.json")))
                .andReturn();
    }
}
