package ru.yandex.market.wms.picking.modules.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class AdminControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup(value = "/controller/admin/happy/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/admin/happy/after.xml",
            assertionMode = NON_STRICT, connection = "wmwhseConnection")
    public void setMaxAssignmentHappyPath() throws Exception {
        mockMvc.perform(post("/admin/max-assignment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/admin/happy/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/admin/not_found/before.xml", connection = "wmwhseConnection")
    public void setMaxAssignmentPutawayZoneNotFound() throws Exception {
        mockMvc.perform(post("/admin/max-assignment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/admin/not_found/request.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/admin/happy/before.xml", connection = "wmwhseConnection")
    public void getMaxAssignmentHappyPath() throws Exception {
        mockMvc.perform(get("/admin/max-assignment")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/admin/happy/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/admin/happy/before.xml", connection = "wmwhseConnection")
    public void getMaxAssignmentWithFilter() throws Exception {
        mockMvc.perform(get("/admin/max-assignment")
                .param("filter", "ZONE_ID==ZONE_2")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/admin/happy/response_filtered.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/admin/happy/before.xml", connection = "wmwhseConnection")
    public void getMaxAssignmentWithOffsetAndLimit() throws Exception {
        mockMvc.perform(get("/admin/max-assignment")
                .param("limit", "1")
                .param("offset", "1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/admin/happy/response_limit_offset.json")))
                .andReturn();
    }
}
