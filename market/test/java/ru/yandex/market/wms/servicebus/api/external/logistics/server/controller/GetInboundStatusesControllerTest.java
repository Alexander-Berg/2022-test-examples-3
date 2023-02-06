package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = IntegrationTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@Import(LogisticApiExceptionHandler.class)
class GetInboundStatusesControllerTest extends IntegrationTest {
    @Autowired
    private Environment env;

    @BeforeEach
    public void setupWireMock() {
        WireMock.configureFor(env.getProperty("wiremock.server.port", Integer.class));
    }

    @Test
    public void getStatus() throws Exception {
        //when
        setupResponse();

        //then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/logistic/v2/getInboundStatuses")
                        .header(HttpHeaders.CONTENT_TYPE, APPLICATION_XML_VALUE)
                        .content(FileContentUtils.getFileContent("api/inbound/request/get-inbound-statuses.xml")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML_VALUE))
                .andExpect(
                        content().xml(
                                FileContentUtils.getFileContent("api/inbound/response/get-inbound-statuses.xml")
                        ));
    }

    private void setupResponse() {
        stubFor(post(urlEqualTo("/receiving/INFOR_SCPRD_wmwhse1/inbound/get-inbound-statuses"))
                .withRequestBody(containing("a-1611242344"))
                .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FileContentUtils.getFileContent("api/inbound/get-inbound-statuses.json"))
                )
        );
    }
}
