package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import java.text.MessageFormat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

@SpringBootTest(classes = IntegrationTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@Import(LogisticApiExceptionHandler.class)
class GetOrderControllerTest extends IntegrationTest {

    private static final String RESPONSES_PATH = "api/logistics/server/getOrder/response/";
    private static final String EXPECTED_PATH = "api/logistics/server/getOrder/expected/";
    private static final String REQUEST_PATTERN_PATH = "api/logistics/server/getOrder/request-pattern.xml";

    @DisplayName("Request order from api, convert to order response and compare with original warp infor response")
    @ParameterizedTest(name = "Order number: {arguments}")
    @ValueSource(strings = {"0000000410", "0000000879", "0000000881", "0000001047", "0000081213"})
    void shouldGetOrderInformation(String orderKey) throws Exception {
        setupOrder(orderKey);
        checkSuccessResponse(orderKey);
    }

    @DisplayName("Request order from api with fake parcels, convert and compare with original response")
    @ParameterizedTest(name = "Order number: {arguments}")
    @ValueSource(strings = {"0000000001"})
    void shouldGetOrderInformationWithFakes(String orderKey) throws Exception {
        setupOrder(orderKey);
        checkSuccessResponse(orderKey);
    }

    private void setupOrder(String orderKey) {
        stubFor(get(urlEqualTo("/api/INFOR_SCPRD_wmwhse1/shipments/" + orderKey))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FileContentUtils.getFileContent(RESPONSES_PATH + orderKey + ".json"))
                )
        );
    }

    private void checkSuccessResponse(String orderKey) throws Exception {
        mockMvc.perform(post("/api/logistic/getOrder")
                        .contentType(MediaType.TEXT_XML)
                        .content(request(orderKey)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(FileContentUtils.getFileContent(EXPECTED_PATH + orderKey + ".xml")));
    }

    @DisplayName("Request absent order and respond 200 with error body")
    @ParameterizedTest(name = "Order number: {arguments}")
    @ValueSource(strings = {"000000087990"})
    void shouldProcessError(String orderKey) throws Exception {
        setupAbsentOrder(orderKey);
        checkErrorResponse(orderKey);
    }

    private void setupAbsentOrder(String orderKey) {
        stubFor(get(urlEqualTo("/api/INFOR_SCPRD_wmwhse1/shipments/" + orderKey))
                .willReturn(aResponse()
                        .withStatus(404)));
    }

    private void checkErrorResponse(String orderKey) throws Exception {
        mockMvc.perform(post("/api/logistic/getOrder")
                        .contentType(MediaType.TEXT_XML)
                        .content(request(orderKey)))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(FileContentUtils.getFileContent(EXPECTED_PATH + orderKey + ".xml")));
    }

    private String request(String orderKey) {
        String fileContent = FileContentUtils.getFileContent(REQUEST_PATTERN_PATH);
        return MessageFormat.format(fileContent, orderKey);
    }
}
