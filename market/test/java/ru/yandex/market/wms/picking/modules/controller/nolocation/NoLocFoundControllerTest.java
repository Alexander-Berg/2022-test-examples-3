package ru.yandex.market.wms.picking.modules.controller.nolocation;

import java.util.function.Function;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpringBootTest(classes = IntegrationTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
public class NoLocFoundControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup(value = "/nolocfound/no-loc-found-before.xml")
    @ExpectedDatabase(value = "/nolocfound/insertion-no-loc-found-after.xml", assertionMode = NON_STRICT)
    public void insertTest() throws Exception {
        commonQuery(MockMvcRequestBuilders::post, "kekll");
    }

    @Test
    @DatabaseSetup(value = "/nolocfound/no-loc-found-before.xml")
    @ExpectedDatabase(value = "/nolocfound/deletion-no-loc-found-after.xml", assertionMode = NON_STRICT)
    public void deleteTest() throws Exception {
        commonQuery(MockMvcRequestBuilders::delete, "kekl");
    }

    @Test
    @DatabaseSetup(value = "/controller/no-location-found/prepare.xml")
    @DatabaseSetup(value = "/controller/no-location-found/noloc.xml")
    @ExpectedDatabase(value = "/controller/no-location-found/prepare.xml", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/controller/no-location-found/noloc.xml", assertionMode = NON_STRICT)
    public void getNoLocsInAreaTest() throws Exception {
        mockMvc.perform(get("/no-loc-found/areas")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/no-location-found/get-areas/areas.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/no-location-found/prepare.xml")
    @ExpectedDatabase(value = "/controller/no-location-found/prepare.xml", assertionMode = NON_STRICT)
    public void getEmptyNoLocsInAreaTest() throws Exception {
        mockMvc.perform(get("/no-loc-found/areas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/no-location-found/prepare.xml")
    @DatabaseSetup(value = "/controller/no-location-found/noloc.xml")
    @ExpectedDatabase(value = "/controller/no-location-found/prepare.xml", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/controller/no-location-found/noloc.xml", assertionMode = NON_STRICT)
    public void getNoLocsByArea() throws Exception {
        mockMvc.perform(get("/no-loc-found/areas/A1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(
                        getFileContent("controller/no-location-found/get-locs-by-area/locs.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/no-location-found/prepare.xml")
    @DatabaseSetup(value = "/controller/no-location-found/noloc.xml")
    @ExpectedDatabase(value = "/controller/no-location-found/prepare.xml", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/controller/no-location-found/noloc.xml", assertionMode = NON_STRICT)
    public void getNoLocsByUnknownArea() throws Exception {
        mockMvc.perform(get("/no-loc-found/areas/A999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/no-location-found/prepare.xml")
    @ExpectedDatabase(value = "/controller/no-location-found/prepare.xml", assertionMode = NON_STRICT)
    public void getEmptyNoLocsByArea() throws Exception {
        mockMvc.perform(get("/no-loc-found/areas/A3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/no-location-found/prepare.xml")
    @DatabaseSetup(value = "/controller/no-location-found/noloc.xml")
    @ExpectedDatabase(value = "/controller/no-location-found/prepare.xml", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/controller/no-location-found/after-update-status.xml", assertionMode = NON_STRICT)
    public void updateNoLocStatus() throws Exception {
        mockMvc.perform(put("/no-loc-found/locations/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/no-location-found/update-status.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/no-location-found/prepare.xml")
    @DatabaseSetup(value = "/controller/no-location-found/noloc.xml")
    @ExpectedDatabase(value = "/controller/no-location-found/prepare.xml", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/controller/no-location-found/noloc.xml", assertionMode = NON_STRICT)
    public void updateNoLocBadStatus() throws Exception {
        mockMvc.perform(put("/no-loc-found/locations/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/no-location-found/update-bad-status.json")))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/no-location-found/prepare.xml")
    @DatabaseSetup(value = "/controller/no-location-found/noloc.xml")
    @ExpectedDatabase(value = "/controller/no-location-found/prepare.xml", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/controller/no-location-found/noloc.xml", assertionMode = NON_STRICT)
    public void updateNoLocBadID() throws Exception {
        mockMvc.perform(put("/no-loc-found/locations/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/no-location-found/update-status.json")))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andReturn();
    }

    private void commonQuery(
            Function<String,
            MockHttpServletRequestBuilder> buildersSupplier,
            String loc
    ) throws Exception {
        mockMvc.perform(buildersSupplier.apply("/no-loc-found")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\n" +
                        "\t\"location\": \"%s\"\n" +
                        "}", loc)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/no-location-found/prepare.xml")
    @DatabaseSetup(value = "/controller/no-location-found/noloc.xml")
    @ExpectedDatabase(value = "/controller/no-location-found/prepare.xml", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/controller/no-location-found/noloc.xml", assertionMode = NON_STRICT)
    public void printNoLoc() throws Exception {
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/reporter/report/locLabel/print"))
                .withRequestBody(
                        new EqualToJsonPattern(
                            getFileContent("controller/no-location-found/reporter-request.json"),
                            true,
                            true
                        )
                )
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(getFileContent("controller/no-location-found/reporter-response.json"))
                )
        );

        mockMvc.perform(post("/no-loc-found/print")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/no-location-found/print-request.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }
}
