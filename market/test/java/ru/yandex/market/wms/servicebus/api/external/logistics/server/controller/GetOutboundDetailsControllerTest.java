package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.IntegrationTestConfig;
import ru.yandex.market.wms.servicebus.api.external.logistics.server.exception.LogisticApiExceptionHandler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpringBootTest(classes = IntegrationTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@Import(LogisticApiExceptionHandler.class)
public class GetOutboundDetailsControllerTest extends IntegrationTest {

    private static final String OUTBOUND_ID = "0000000001";

    @Test
    void getOutboundDetailsPositive() throws Exception {
        String resourcesPath = "api/logistics/server/getOutboundDetails/positive/";

        setupDetails(resourcesPath + "get_shipments_response.json");

        mockMvc.perform(post("/api/logistic/getOutboundDetails")
                        .contentType(MediaType.TEXT_XML)
                        .accept(MediaType.TEXT_XML)
                        .content(getFileContent(resourcesPath + "request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(
                        getFileContent(resourcesPath + "response.xml")))
                .andReturn();
    }

    @Test
    void getOutboundDetailsNotFound() throws Exception {
        String resourcesPath = "api/logistics/server/getOutboundDetails/not_found/";

        setupDetailsNotFound();

        mockMvc.perform(post("/api/logistic/getOutboundDetails")
                        .contentType(MediaType.TEXT_XML)
                        .accept(MediaType.TEXT_XML)
                        .content(getFileContent(resourcesPath + "request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(
                        getFileContent(resourcesPath + "response.xml")))
                .andReturn();
    }

    private void setupDetails(String filename) {
        final String testUrl = "/api/INFOR_SCPRD_wmwhse1/outbound/details/" + OUTBOUND_ID;
        stubFor(get(urlEqualTo(testUrl))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FileContentUtils.getFileContent(filename))));
    }

    private void setupDetailsNotFound() {
        stubFor(get(urlEqualTo("/api/INFOR_SCPRD_wmwhse1/outbound/details/" + OUTBOUND_ID))
                .willReturn(aResponse()
                        .withStatus(404)));
    }
}
