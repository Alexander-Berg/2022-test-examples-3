package ru.yandex.market.wms.autostart.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class StationToCarrierControllerTest extends AutostartIntegrationTest {

    @Test
    @DatabaseSetup("/controller/station-to-carrier/stations.xml")
    @DatabaseSetup("/controller/station-to-carrier/carriers.xml")
    @DatabaseSetup("/controller/station-to-carrier/common.xml")
    public void testGetAllStationsToCarrier() throws Exception {
        ResultActions result = mockMvc.perform(get("/station-to-carrier")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/station-to-carrier/get-all/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/station-to-carrier/carriers.xml")
    public void testGetAllCarriers() throws Exception {
        ResultActions result = mockMvc.perform(get("/station-to-carrier/carrier")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/station-to-carrier/get-carriers/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/station-to-carrier/stations.xml")
    public void testGetAllStations() throws Exception {
        ResultActions result = mockMvc.perform(get("/station-to-carrier/station")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/station-to-carrier/get-stations/get-all-stations-response.json")));
    }

    @Test
    @DatabaseSetup("/controller/station-to-carrier/stations.xml")
    public void testGetConsolidationStations() throws Exception {
        ResultActions result = mockMvc.perform(get("/station-to-carrier/station/cons")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/station-to-carrier/get-stations/get-cons-stations-response.json")));
    }

    @Test
    @DatabaseSetup("/controller/station-to-carrier/stations.xml")
    public void testGetSortStations() throws Exception {
        ResultActions result = mockMvc.perform(get("/station-to-carrier/station/sort")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/station-to-carrier/get-stations/get-sort-stations-response.json")));
    }

    @Test
    @ExpectedDatabase(value = "/controller/station-to-carrier/put-cons-stations-to-carrier/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testPutConsStationsToCarrier() throws Exception {
        ResultActions result = mockMvc.perform(put("/station-to-carrier/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/station-to-carrier/put-cons-stations-to-carrier/request.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(value = "/controller/station-to-carrier/put-sort-stations-to-carrier/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testPutSortStationsToCarrier() throws Exception {
        ResultActions result = mockMvc.perform(put("/station-to-carrier/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/station-to-carrier/put-sort-stations-to-carrier/request.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/station-to-carrier/put-cons-remove-sort/before.xml")
    @ExpectedDatabase(value = "/controller/station-to-carrier/put-cons-remove-sort/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testAddConsRemoveSortToCarrier() throws Exception {
        ResultActions result = mockMvc.perform(put("/station-to-carrier/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/station-to-carrier/put-cons-remove-sort/request.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/station-to-carrier/put-duplicate-stations-to-carrier/immutable.xml")
    @ExpectedDatabase(value = "/controller/station-to-carrier/put-duplicate-stations-to-carrier/immutable.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testTryAddAlreadyLinkedStationsToCarrier() throws Exception {
        ResultActions result = mockMvc.perform(put("/station-to-carrier/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/station-to-carrier/put-duplicate-stations-to-carrier/request.json")));

        result.andExpect(status().isBadRequest()).andExpect(content().json(getFileContent(
                "controller/station-to-carrier/put-duplicate-stations-to-carrier/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/station-to-carrier/put-sort-remove-cons/before.xml")
    @ExpectedDatabase(value = "/controller/station-to-carrier/put-sort-remove-cons/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testAddSortRemoveConsToCarrier() throws Exception {
        ResultActions result = mockMvc.perform(put("/station-to-carrier/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/station-to-carrier/put-sort-remove-cons/request.json")));

        result.andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/station-to-carrier/delete-by-carrier-and-type/before.xml")
    @ExpectedDatabase(value = "/controller/station-to-carrier/delete-by-carrier-and-type/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testDeleteByCarrierAndType() throws Exception {
        mockMvc.perform(delete("/station-to-carrier/carrier/10/cons"))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/station-to-carrier/delete-by-carrier/before.xml")
    @ExpectedDatabase(value = "/controller/station-to-carrier/delete-by-carrier/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testDeleteByCarrier() throws Exception {
        mockMvc.perform(delete("/station-to-carrier/carrier/10"))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/station-to-carrier/common.xml")
    @ExpectedDatabase(value = "/controller/station-to-carrier/activate/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testActivation() throws Exception {
        mockMvc.perform(put("/station-to-carrier/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"isActive\" : false }"))
                .andExpect(status().isOk());
    }
}
