package ru.yandex.market.wms.picking.modules.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickingOldAssignmentsControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup(value = "/old-assignments/1/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/old-assignments/1/after.xml",
            assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getOldAssignments() throws Exception {
        mockMvc.perform(get("/old-assignments"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("old-assignments/1/response.json")))
            .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/old-assignments/2/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/old-assignments/2/after.xml",
            assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getOldAssignmentsEmptyList() throws Exception {
        mockMvc.perform(get("/old-assignments"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("old-assignments/2/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/old-assignments/1/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/old-assignments/1/after.xml",
            assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getByPostOldAssignments() throws Exception {
        mockMvc.perform(get("/old-assignments"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("old-assignments/1/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/old-assignments/2/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/old-assignments/2/after.xml",
            assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void getByPostOldAssignmentsEmptyList() throws Exception {
        mockMvc.perform(post("/old-assignments"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("old-assignments/2/response.json")))
                .andReturn();
    }

}
