package ru.yandex.market.wms.receiving.controller;

import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class LocControllerTest extends ReceivingIntegrationTest {

    @Test
    @DatabaseSetup("/controller/loc-controller/before.xml")
    public void checkTable_Ok_02() throws Exception {
        mockMvc.perform(post("/locs/check-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/loc-controller/request/check-table-ok-02.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/loc-controller/response/got-table-ok-02.json")));
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before.xml")
    public void checkTable_Ok_STAGE02() throws Exception {
        mockMvc.perform(post("/locs/check-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/loc-controller/request/check-table-ok-STAGE02.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "controller/loc-controller/response/got-table-ok-STAGE02.json")));
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before.xml")
    public void checkTable_Ok_stage02() throws Exception {
        mockMvc.perform(post("/locs/check-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/loc-controller/request/check-table-ok-stage02-lowercase" +
                                ".json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "controller/loc-controller/response/got-table-ok-stage02-lowercase.json")));
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before.xml")
    public void checkTable_Ok_DAMAGE02() throws Exception {
        mockMvc.perform(post("/locs/check-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/loc-controller/request/check-table-ok-DAMAGE02.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "controller/loc-controller/response/got-table-ok-DAMAGE02.json")));
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before.xml")
    public void checkTable_NotFound_03() throws Exception {
        mockMvc.perform(post("/locs/check-table")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/loc-controller/request/check-table-not-found-03.json")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/table-params-before.xml")
    public void tableParametersNotFound() throws Exception {
        mockMvc.perform(get("/locs/table-parameters")
                        .param("tableId", "STAGE09")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/loc-controller/response/got-table-params-no-loc.json")
                ));
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/table-params-before.xml")
    public void tableParametersEmptyRequest() throws Exception {
        mockMvc.perform(get("/locs/table-parameters")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/table-params-before.xml")
    public void tableParametersOk() throws Exception {
        mockMvc.perform(get("/locs/table-parameters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("tableId", "stage01"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/loc-controller/response/got-table-params-ok.json")
                ));
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/table-params-before.xml")
    public void tableParametersOk2() throws Exception {
        mockMvc.perform(get("/locs/table-parameters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("tableId", "stage03"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/loc-controller/response/got-table-params-ok-2.json")
                ));
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/table-params-before.xml")
    public void tableParametersOk3() throws Exception {
        mockMvc.perform(get("/locs/table-parameters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("tableId", "stage04"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/loc-controller/response/got-table-params-ok-3.json")
                ));
    }


    @Test
    @DatabaseSetup("/controller/loc-controller/before.xml")
    public void checkPrinterOkTest() throws Exception {
        mockMvc.perform(post("/locs/check-printer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/loc-controller/request/check-printer-ok.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before.xml")
    public void checkPrinterFailTest() throws Exception {
        mockMvc.perform(
                        post("/locs/check-printer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/loc-controller/request/check-printer-fail.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before.xml")
    public void findPrinterOkTest() throws Exception {
        mockMvc.perform(get("/locs/find-printer?printerName=p01"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/loc-controller/response/check-printer-ok.json")
                ));
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before.xml")
    public void findPrinterFailTest() throws Exception {
        mockMvc.perform(get("/locs/find-printer?printerName=p02"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before-2.xml")
    public void getLocationTypeByLocTest() throws Exception {
        mockMvc.perform(get("/locs/pack"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/loc-controller/response/location-type-response-ok.json")
                ));
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before-2.xml")
    public void getLocationTypeByLocNotFoundTest() throws Exception {
        mockMvc.perform(get("/locs/randomloc"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before-2.xml")
    public void getLocationTypeByLocEnumNotFoundTest() throws Exception {
        mockMvc.perform(get("/locs/stage"))
                .andExpect(status().isInternalServerError())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Unknown LocationType: ERROR500",
                        Objects.requireNonNull(result.getResolvedException()).getMessage()));
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before-2.xml")
    public void getVerifiedGateTypeByLocTest() throws Exception {
        mockMvc.perform(get("/locs/pickto/pack"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/loc-controller/response/location-type-response-ok.json")
                ));
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before-2.xml")
    public void getVerifiedGateTypeByLocWrongLocTest() throws Exception {
        mockMvc.perform(get("/locs/pickto/randomloc"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before-2.xml")
    public void getVerifiedGateTypeByLocWrongTypeTest() throws Exception {
        mockMvc.perform(get("/locs/randomtype/pack"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DatabaseSetup("/controller/loc-controller/before-2.xml")
    public void getVerifiedGateTypeByLocWrongBothTest() throws Exception {
        mockMvc.perform(get("/locs/randomtype/randomloc"))
                .andExpect(status().is4xxClientError());
    }
}
