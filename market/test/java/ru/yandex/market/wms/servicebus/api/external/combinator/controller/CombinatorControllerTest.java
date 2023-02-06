package ru.yandex.market.wms.servicebus.api.external.combinator.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.servicebus.model.request.OrderMaxParcelDimensionsRequest;
import ru.yandex.market.wms.common.spring.servicebus.model.response.OrderMaxParcelDimensionsResponse;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.combinator.client.CombinatorClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class CombinatorControllerTest extends IntegrationTest {
    @MockBean
    @Autowired
    private CombinatorClient combinatorClient;

    @Test
    public void testGetOrderMaxDimensions() throws Exception {
        Mockito.when(combinatorClient.getOrderMaxDimensions(OrderMaxParcelDimensionsRequest.builder()
                        .externalOrderKey("00012345")
                        .build()))
                .thenReturn(OrderMaxParcelDimensionsResponse.builder()
                        .length(100)
                        .width(200)
                        .height(300)
                        .dimSum(600)
                        .weight(1000)
                        .build());

        mockMvc.perform(post("/wms/getOrderMaxDimensions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/wms/getOrderMaxDimensions/request.json")))
                .andDo(print())
                .andExpect(content().json(
                        getFileContent("api/internal/wms/getOrderMaxDimensions/response.json")))
                .andExpect(status().isOk());
    }

}

