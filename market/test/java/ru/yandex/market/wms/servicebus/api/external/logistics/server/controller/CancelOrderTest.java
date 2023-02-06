package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import java.io.IOException;

import com.github.tomakehurst.wiremock.client.WireMock;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import wiremock.org.eclipse.jetty.http.HttpStatus;

import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.IntegrationTestConfig;
import ru.yandex.market.wms.servicebus.api.external.logistics.server.exception.LogisticApiExceptionHandler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpringBootTest(classes = IntegrationTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@Import(LogisticApiExceptionHandler.class)
public class CancelOrderTest extends IntegrationTest {
    @Autowired
    private Environment env;

    private static MockWebServer apiServer;

    @BeforeAll
    static void setUp() throws IOException {
        apiServer = new MockWebServer();
        apiServer.start(9999);
    }

    @AfterAll
    static void tearDown() throws IOException {
        apiServer.shutdown();
    }

    @BeforeEach
    public void setupWireMock() {
        WireMock.configureFor(env.getProperty("wiremock.server.port", Integer.class));
    }

    @Test
    public void testNotCancellable() throws Exception {
        setupCancelOrder("outbound-1626852630004029", HttpStatus.BAD_REQUEST_400,
                "Cannot cancel order");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/logistic/cancelOrder")
                .contentType(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/cancelOrder/bad_request/request.xml")))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers
                        .content()
                        .string(getFileContent("api/logistics/server/cancelOrder/bad_request/response.xml"))
                );
    }

    private void setupCancelOrder(String externalOrderKey, int httpStatus, String responseBody) {
        stubFor(WireMock
                .post(urlEqualTo(String.format("/api/INFOR_SCPRD_wmwhse1/shipments/external-order-key/%s/cancel",
                        externalOrderKey)))
                .willReturn(aResponse().withStatus(httpStatus).withBody(responseBody)));
    }
}
