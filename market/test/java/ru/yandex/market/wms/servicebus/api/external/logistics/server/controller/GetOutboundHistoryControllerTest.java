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

import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.IntegrationTestConfig;
import ru.yandex.market.wms.servicebus.api.external.logistics.server.exception.LogisticApiExceptionHandler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpringBootTest(
        classes = IntegrationTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@Import(LogisticApiExceptionHandler.class)
public class GetOutboundHistoryControllerTest extends IntegrationTest {

    @Autowired
    private Environment env;

    @BeforeEach
    public void setupWireMock() {
        WireMock.configureFor(env.getProperty("wiremock.server.port", Integer.class));

        setupOutboundHistoryResponse();
    }

    /**
     * Тест на happy path.
     * Все данные валидны, все хорошо.
     */
    @Test
    void test() throws Exception {
        perform(
                "api/logistics/server/getOutboundHistory/ok/1/request.xml",
                "api/logistics/server/getOutboundHistory/ok/1/response.xml"
        );
    }

    /**
     * В запросе передали ключи без префикса outbound.
     */
    @Test
    void test2() throws Exception {
        perform(
                "api/logistics/server/getOutboundHistory/ok/2/request.xml",
                "api/logistics/server/getOutboundHistory/ok/2/response.xml"
        );
    }

    private void perform(String requestFileName, String responseFileName) throws Exception {
        mockMvc
                .perform(
                        post("/api/logistic/getOutboundHistory")
                                .contentType(MediaType.TEXT_XML)
                                .accept(MediaType.TEXT_XML)
                                .content(getFileContent(requestFileName))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent(responseFileName)))
                .andReturn();
    }

    private void setupOutboundHistoryResponse() {
        var mappingBuilder = get(urlPathEqualTo("/api/INFOR_SCPRD_ENTERPRISE/outbound/history"))
                .withQueryParam("yandexId", equalTo("outbound-12750263"))
                .withQueryParam("partnerId", equalTo("outbound-12750263"));

        stubFor(mappingBuilder.willReturn(
                aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(FileContentUtils.getFileContent(
                                "api/logistics/server/getOutboundHistory/ok/api-response.json"
                        ))
        ));
    }
}
