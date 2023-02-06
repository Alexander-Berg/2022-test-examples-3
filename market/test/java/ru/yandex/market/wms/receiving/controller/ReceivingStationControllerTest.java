package ru.yandex.market.wms.receiving.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class ReceivingStationControllerTest extends ReceivingIntegrationTest {

    @Test
    void checkPostTablesBadReqTableParam0Null() throws Exception {
        checkPostBadRequest("post-bad-request-table-loc-null.json");
    }

    @Test
    void checkPostTablesBadReqTableParam1Null() throws Exception {
        checkPostBadRequest("post-bad-request-table-conv-enabled-null.json");
    }

    @Test
    void checkPostTablesBadReqTablesWrongEnumValue() throws Exception {
        checkPostBadRequest("post-bad-request-table-container-sett.json");
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/tables/db-before.xml")
    void checkPostTablesBadReqLocNotPresent() throws Exception {
        checkPostBadRequest("post-bad-request-table-not-rcp.json");
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/tables/db-before.xml")
    void checkPostTablesBadReqTablesFewSameLocs() throws Exception {
        checkPostBadRequest("post-bad-request-table-few-same-locs.json");
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/tables/db-before.xml")
    void checkPostTablesBadReqNestingWithDefaultContainer() throws Exception {
        checkPostBadRequest("post-bad-request-table-nesting-with-default-container.json");
    }

    void checkPostBadRequest(String requestContentFile) throws Exception {
        mockMvc.perform(post("/admin/tables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receiving-station/tables/" + requestContentFile)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/tables/db-before.xml")
    @ExpectedDatabase(value = "/controller/receiving-station/tables/db-after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkPostTablesHappyPath() throws Exception {
        checkPostHappyPath("post-happy-path.json");
    }

    void checkPostHappyPath(String requestContentFile) throws Exception {
        mockMvc.perform(post("/admin/tables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receiving-station/tables/" + requestContentFile)))
                .andExpect(status().isOk());
    }

    @Test
    void checkGetBadReqMaxLimit() throws Exception {
        checkGetErrorRequest("limit", "101", HttpStatus.BAD_REQUEST);
    }

    @Test
    void checkGetBadReqWrongLimit() throws Exception {
        checkGetErrorRequest("limit", "somewrongtext", HttpStatus.BAD_REQUEST);
    }

    @Test
    void checkGetErrorReqWrongFilter() throws Exception {
        checkGetErrorRequest("filter", "somewrongtext", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void checkGetErrorReqWrongCursor() throws Exception {
        checkGetErrorRequest("cursor", "somewrongtext", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void checkGetLimitZero() throws Exception {
        checkGetErrorRequest("limit", "0", HttpStatus.BAD_REQUEST);
    }

    void checkGetErrorRequest(String paramName, String paramValue, HttpStatus errorStatus) throws Exception {
        mockMvc.perform(get("/admin/tables")
                .param(paramName, paramValue)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(errorStatus.value()));
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/tables/db-before-get.xml")
    void checkGetNoParams() throws Exception {
        mockMvc.perform(get("/admin/tables")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/receiving-station/tables/get-all-no-params.json")));
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/tables/db-before-get-empty-rs-table.xml")
    void checkGetEmptyRsTable() throws Exception {
        mockMvc.perform(get("/admin/tables")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/receiving-station/tables/get-all-empty-rs-table.json")));
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/tables/db-before-get.xml")
    void checkGetFilter() throws Exception {
        mockMvc.perform(get("/admin/tables")
                .param("filter", "defaultContainerSettings==WITH_DEFAULT_CONTAINER,tableId==TABLE2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/receiving-station/tables/get-all-filter.json")));
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/tables/db-before-get.xml")
    void checkGetFilterSubstr() throws Exception {
        mockMvc.perform(get("/admin/tables")
                .param("filter", "tableId==TABL%")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/receiving-station/tables/get-all-no-params.json")));
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/tables/db-before-get.xml")
    void checkGetFilterNoExistingTable() throws Exception {
        mockMvc.perform(get("/admin/tables")
                .param("filter", "tableId==TABLENOTEXIST")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/receiving-station/tables/get-all-empty-result.json")));
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/tables/db-before-get.xml")
    void checkGetLimitAndCursor() throws Exception {
        String cursor = "";
        for (int i = 0; i < 3; i++) {
            ResultActions result = mockMvc.perform(get("/admin/tables")
                    .param("cursor", cursor)
                    .param("limit", "2")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(content().json(getFileContent(String.format(
                            "controller/receiving-station/tables/get-all-limit-%s.json", i + 1))));

            JSONObject obj = new JSONObject(result.andReturn().getResponse().getContentAsString());
            assertions.assertThat(obj).isNotNull();

            if (obj.has("cursor")) {
                cursor = obj.getString("cursor");
            }
        }
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/default-table/db-before.xml")
    @ExpectedDatabase(value = "/controller/receiving-station/default-table/db-before.xml", assertionMode =
            DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkPostDefaultTablesSameParams() throws Exception {
        MockHttpServletRequestBuilder request = post("/admin/default-table")
                .content(getFileContent("controller/receiving-station/default-table/same-params/request.json"))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/default-table/db-before.xml")
    @ExpectedDatabase(value = "/controller/receiving-station/default-table/new-params/after.xml", assertionMode =
            DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkPostDefaultTablesNewParams() throws Exception {
        MockHttpServletRequestBuilder request = post("/admin/default-table")
                .content(getFileContent("controller/receiving-station/default-table/new-params/request.json"))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receiving-station/default-table/db-before.xml")
    @ExpectedDatabase(value = "/controller/receiving-station/default-table/db-before.xml", assertionMode =
            DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkPostDefaultTablesBadReqIncompatibleSettings() throws Exception {
        MockHttpServletRequestBuilder request = post("/admin/default-table")
                .content(getFileContent("controller/receiving-station/default-table/incompatible/request.json"))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                        getFileContent("controller/receiving-station/default-table/incompatible/response.json")
                ));
    }
}
