package ru.yandex.market.wms.servicebus.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.IntegrationTestConfig;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpringBootTest(classes = IntegrationTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class WmsClusterPagematchControllerTest extends IntegrationTest {

    @Test
    public void getClusterPagematch() throws Exception {
        initUrlTextStub("/receiving/pagematch", "pagematch pagematch wms-receiving");
        initUrlTextStub("/autostart/pagematch", "pagematch pagematch wms-autostart");

        MvcResult mockResult = mockMvc.perform(MockMvcRequestBuilders.get("/cluster-pagematch"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(getFileContent("controller/pagematch.txt")))
                .andReturn();
    }

    private void initUrlTextStub(String url, String response) {
        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "plain/text")
                        .withBody(response)));
    }
}
