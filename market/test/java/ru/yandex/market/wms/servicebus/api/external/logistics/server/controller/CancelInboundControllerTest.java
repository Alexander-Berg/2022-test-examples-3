package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.IntegrationTestConfig;
import ru.yandex.market.wms.servicebus.api.external.logistics.server.exception.LogisticApiExceptionHandler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
class CancelInboundControllerTest extends IntegrationTest {
    @Autowired
    private Environment env;

    @BeforeEach
    public void setupWireMock() {
        WireMock.configureFor(env.getProperty("wiremock.server.port", Integer.class));
    }

    @Test
    void shouldSuccessCancelInbound() throws Exception {
        setupCancelInbound("31435430");

        mockMvc.perform(post("/api/logistic/cancelInbound")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/cancelInbound/success/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent("api/logistics/server/cancelInbound/success/response.xml")))
                .andReturn();
    }

    private void setupCancelInbound(String receiptKey) {
        stubFor(WireMock.post(urlEqualTo(String.format("/receiving/INFOR_SCPRD_wmwhse1/inbound/%s/cancel", receiptKey)))
                .willReturn(aResponse().withStatus(200)));
    }
}
