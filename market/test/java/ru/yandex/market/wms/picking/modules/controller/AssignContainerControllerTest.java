package ru.yandex.market.wms.picking.modules.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class AssignContainerControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/controller/assign-container/1/before_assign.xml")
    @ExpectedDatabase(value = "/controller/assign-container/1/after_assign.xml", assertionMode = NON_STRICT)
    public void assignContainerHappyPath() throws Exception {
        mockMvc.perform(post("/assign-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/assign-container/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/assign-container/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/assign-container/2/before_assign.xml")
    @ExpectedDatabase(value = "/controller/assign-container/2/after_assign.xml", assertionMode = NON_STRICT)
    public void assignExistingContainerHappyPath() throws Exception {
        mockMvc.perform(post("/assign-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/assign-container/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/assign-container/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/assign-container/3/before_assign.xml")
    @ExpectedDatabase(value = "/controller/assign-container/3/before_assign.xml", assertionMode = NON_STRICT)
    public void assignContainerOtherAssignmentAlreadyExists() throws Exception {
        mockMvc.perform(post("/assign-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/assign-container/request.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/assign-container/4/before_assign.xml")
    @ExpectedDatabase(value = "/controller/assign-container/4/before_assign.xml", assertionMode = NON_STRICT)
    public void assignContainerExtraGoodsInContainer() throws Exception {
        mockMvc.perform(post("/assign-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/assign-container/request.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/assign-container/5/before_assign.xml")
    @ExpectedDatabase(value = "/controller/assign-container/5/before_assign.xml", assertionMode = NON_STRICT)
    public void assignOccupiedContainer() throws Exception {
        mockMvc.perform(post("/assign-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/assign-container/request.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/assign-container/6/before_assign.xml")
    @ExpectedDatabase(value = "/controller/assign-container/6/before_assign.xml", assertionMode = NON_STRICT)
    public void assignAlreadyOccupiedContainerByUser() throws Exception {
        mockMvc.perform(post("/assign-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/assign-container/request.json")))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/assign-container/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/assign-container/7/before_assign.xml")
    @ExpectedDatabase(value = "/controller/assign-container/7/before_assign.xml", assertionMode = NON_STRICT)
    public void assignAlreadyOccupiedContainerByAnotherUser() throws Exception {
        mockMvc.perform(post("/assign-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/assign-container/request.json")))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/assign-container/8/before_assign.xml")
    public void assignContainerWithDroppedPickDetails() throws Exception {
        mockMvc.perform(post("/assign-container")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/assign-container/8/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent("controller/assign-container/8/response.json")))
                .andReturn();
    }
}
