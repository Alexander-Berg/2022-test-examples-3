package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.IntegrationTestConfig;
import ru.yandex.market.wms.servicebus.api.external.logistics.server.exception.LogisticApiExceptionHandler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = IntegrationTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@Import(LogisticApiExceptionHandler.class)
public class GetOrderStatusHistoryControllerTest extends IntegrationTest {

    @Test
    public void getOrderStatusHistory() throws Exception {
        setupResponse();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/logistic/order-status-history")
                        .header(HttpHeaders.CONTENT_TYPE, APPLICATION_XML_VALUE)
                        .content(FileContentUtils.getFileContent(
                                "api/logistics/server/getOrderStatusHistory/request.xml")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML_VALUE))
                .andDo(print())
                .andExpect(
                        content().xml(
                                FileContentUtils.getFileContent(
                                        "api/logistics/server/getOrderStatusHistory/response.xml")
                        ));
    }

    private void setupResponse() {
        stubFor(post(urlEqualTo("/api/INFOR_SCPRD_wmwhse1/order-status-history"))
                .withRequestBody(containing("a-1611242345"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FileContentUtils.getFileContent(
                                "api/logistics/server/getOrderStatusHistory/wms-api-response.json"))
                )
        );
    }
}
