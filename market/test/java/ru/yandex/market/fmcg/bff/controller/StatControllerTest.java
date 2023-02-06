package ru.yandex.market.fmcg.bff.controller;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import ru.yandex.market.fmcg.bff.test.FmcgBffTest;
import ru.yandex.market.fmcg.bff.test.MockServerUtil;
import ru.yandex.market.fmcg.bff.test.TestUtil;
import ru.yandex.market.fmcg.bff.util.YasmStatistics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static ru.yandex.market.fmcg.bff.test.TestUtil.loadResourceAsString;

class StatControllerTest extends FmcgBffTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MockRestServiceServer searchMockServer;

    @Autowired
    private YasmStatistics yasmStatistics;

    @BeforeEach
    void initServer() {
        MockServerUtil.INSTANCE.reset();
        yasmStatistics.clear();
    }

    void setSearchTestData(String path, List<Parameter> params, String response) {
        MockServerUtil.INSTANCE.mockServer()
            .when(HttpRequest.request().withPath(path).withQueryStringParameters(params).withMethod("GET"))
            .respond(
                HttpResponse.response(response)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
            );
    }

    void setupSearchMockServerEmptyResponse(String path, List<Parameter> params) {
        setSearchTestData(path, params, TestUtil.loadResourceAsString("search/SearchEmptyResponse.json"));
    }

    void setupSearchMockServerResponse(String path, List<Parameter> params) {
        setSearchTestData(path, params, TestUtil.loadResourceAsString("search/SearchResponse.json"));
    }

    @SneakyThrows
    ResultActions performBffRequestPost(String url, String requestBodyName) {
        return mockMvc.perform(post(url)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(loadResourceAsString(requestBodyName)));
    }

    @SneakyThrows
    ResultActions performBffRequestGet(String url) {
        return mockMvc.perform(get(url)
            .contentType(MediaType.APPLICATION_JSON_UTF8));
    }

    void matchBffResponse(String url,
                          HttpStatus expectedStatus,
                          String response) throws Exception {
        performBffRequestGet(url)
            .andExpect(status().is(expectedStatus.value()))
            .andExpect(content().json(response));
    }

    @Test
    void emptyRequestsWithFilter() throws Exception {
        setupSearchMockServerEmptyResponse("/yandsearch", new ArrayList<Parameter>(Arrays.asList(
            new Parameter("place", "yellow_promo"),
            new Parameter("fesh", "321"),
            new Parameter("outlets", "654"))));
        setupSearchMockServerResponse("/yandsearch", new ArrayList<Parameter>(Arrays.asList(
            new Parameter("place", "yellow_promo"),
            new Parameter("fesh", "561785"),
            new Parameter("outlets", "87831330"))));
        performBffRequestPost("/apiv1/product/searchPromo?pageNum=1&pageSize=20", "StatControllerTest.request1.json");
        performBffRequestPost("/apiv1/product/searchPromo?pageNum=1&pageSize=20", "StatControllerTest.request2.json");
        performBffRequestGet("/apiv1/product/searchPromo?pageNum=1&pageSize=20&shopOutlet=321-654");
        performBffRequestGet("/apiv1/product/searchPromo?pageNum=1&pageSize=20&shopOutlet=561785-87831330");
        matchBffResponse("/stat", HttpStatus.OK, "[[searchPromo_empty_response_count_dmmm,2]]");
        searchMockServer.verify();
    }

    @Test
    void emptyRequestsForExp() throws Exception {
        // Rport exp is on: non empty response
        setupSearchMockServerResponse("/yandsearch", new ArrayList<Parameter>(Arrays.asList(
            new Parameter("place", "yellow_msku"),
            new Parameter("fesh", "561785"),
            new Parameter("outlets", "87831330"),
            new Parameter("nid", "123456"),
            new Parameter("rearr-factors", "use_new_yellow_index=1"))));
        // Report exp is on: empty response
        setupSearchMockServerEmptyResponse("/yandsearch", new ArrayList<Parameter>(Arrays.asList(
            new Parameter("place", "yellow_msku"),
            new Parameter("fesh", "321"),
            new Parameter("outlets", "654"),
            new Parameter("nid", "123456"),
            new Parameter("rearr-factors", "use_new_yellow_index=1"))));

        // Report exp is explicitly or implicitly turned off: empty responses
        setupSearchMockServerEmptyResponse("/yandsearch", new ArrayList<Parameter>(Arrays.asList(
            new Parameter("place", "yellow_msku"),
            new Parameter("fesh", "321"),
            new Parameter("outlets", "654"),
            new Parameter("nid", "123456"),
            new Parameter("rearr-factors", "use_new_yellow_index=0"))));
        setupSearchMockServerEmptyResponse("/yandsearch", new ArrayList<Parameter>(Arrays.asList(
            new Parameter("place", "yellow_msku"),
            new Parameter("fesh", "321"),
            new Parameter("outlets", "654"),
            new Parameter("nid", "123456"))));

        // valid flags: expecting status OK
        String expFlagsJson = "[{\"HANDLER\": \"MARKETAPPS\", \"CONTEXT\": {\"SUPERCHECK\": {\"TESTID\": [\"188611\"], \"rearr\": [\"use_new_yellow_search=1\"]}}}]";
        String expFlagsBase64 = Base64.getEncoder().encodeToString(expFlagsJson.getBytes());

        // Empty response without exp headers
        performBffRequestPost("/apiv1/product/searchByNid?nid=123456&pageNum=1&pageSize=20", "StatControllerTest.request1.json");
        matchBffResponse("/stat", HttpStatus.OK, "[[searchByNid_empty_response_count_dmmm,1]]");

        // Empty response with exp headers
        mockMvc.perform(post("/apiv1/product/searchByNid?nid=123456&pageNum=1&pageSize=20")
            .header("x-yandex-expflags", expFlagsBase64)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(loadResourceAsString("StatControllerTest.request1.json")));
        matchBffResponse("/stat", HttpStatus.OK, "[[searchByNid_empty_response_count_dmmm,2],[188611_searchByNid_empty_response_count_dmmm,1]]");

        // Non-empty response without exp headers
        performBffRequestPost("/apiv1/product/searchByNid?nid=123456&pageNum=1&pageSize=20", "StatControllerTest.request2.json");
        matchBffResponse("/stat", HttpStatus.OK, "[[searchByNid_empty_response_count_dmmm,2],[188611_searchByNid_empty_response_count_dmmm,1]]");
        // Non-empty response with exp headers
        mockMvc.perform(post("/apiv1/product/searchByNid?nid=123456&pageNum=1&pageSize=20")
            .header("x-yandex-expflags", expFlagsBase64)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(loadResourceAsString("StatControllerTest.request2.json")));
        matchBffResponse("/stat", HttpStatus.OK, "[[searchByNid_empty_response_count_dmmm,2],[188611_searchByNid_empty_response_count_dmmm,1]]");
        searchMockServer.verify();

        // valid flags for another test_id: expecting status OK
        String expFlags2Json = "[{\"HANDLER\": \"MARKETAPPS\", \"CONTEXT\": {\"SUPERCHECK\": {\"TESTID\": [\"188612\"], \"rearr\": [\"use_new_yellow_index=1\"]}}}]";
        String expFlags2Base64 = Base64.getEncoder().encodeToString(expFlags2Json.getBytes());

        // Empty response with exp headers 2
        mockMvc.perform(post("/apiv1/product/searchByNid?nid=123456&pageNum=1&pageSize=20")
            .header("x-yandex-expflags", expFlags2Base64)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(loadResourceAsString("StatControllerTest.request1.json")));
        matchBffResponse("/stat", HttpStatus.OK, "[[searchByNid_empty_response_count_dmmm,3],[188611_searchByNid_empty_response_count_dmmm,1],[188612_searchByNid_empty_response_count_dmmm,1]]");

    }
}
