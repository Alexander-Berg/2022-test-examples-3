package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.api.model.common.ResourceId;
import ru.yandex.market.wms.receiving.client.ReceivingClient;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.internal.api.client.impl.WmsApiClientImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

class PutInboundRegistryControllerTest extends IntegrationTest {

    @Autowired
    @Qualifier("logistic-api")
    protected XmlMapper xmlMapper;
    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean(name = "wmsApiClientImpl")
    @Autowired
    private WmsApiClientImpl wmsApiClientImpl;

    @MockBean(name = "receivingClient")
    @Autowired
    private ReceivingClient receivingClient;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(wmsApiClientImpl);
        Mockito.reset(receivingClient);
    }

    @Test
    void validationShouldFail() throws Exception {
        mockMvc.perform(post("/api/logistic/putInboundRegistry")
                        .contentType(MediaType.TEXT_XML)
                        .accept(MediaType.TEXT_XML)
                        .content(getFileContent("api/logistics/server/putInboundRegistry/invalid/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(
                        getFileContent("api/logistics/server/putInboundRegistry/invalid/response.xml")))
                .andReturn();
    }

    @Test
    void shouldPassWithoutItems() throws Exception {
        when(receivingClient.putInboundRegistry(any()))
                .thenReturn(ResourceId.builder().setPartnerId("1").setYandexId("2").build());

        mockMvc.perform(post("/api/logistic/putInboundRegistry")
                        .contentType(MediaType.TEXT_XML)
                        .accept(MediaType.TEXT_XML)
                        .content(getFileContent("api/logistics/server/putInboundRegistry/valid-no-items/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(
                        getFileContent("api/logistics/server/putInboundRegistry/valid-no-items/response.xml")))
                .andReturn();
    }
}
