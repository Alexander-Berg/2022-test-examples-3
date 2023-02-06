package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;
import ru.yandex.market.wms.ordermanagement.client.OrderManagementClient;
import ru.yandex.market.wms.servicebus.IntegrationTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class CreateOrderTest extends IntegrationTest {
    @Autowired
    @Qualifier("logistic-api")
    protected XmlMapper xmlMapper;
    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean(name = "orderManagementClient")
    @Autowired
    private OrderManagementClient orderManagementClient;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(orderManagementClient);
    }

    @Test
    public void happy() throws Exception {
        OrderDTO createOrderWmsRequest = objectMapper.readValue(
                getFileContent("api/logistics/server/createOrder/happy/api.request.json"),
                new TypeReference<OrderDTO>() {
                });
        OrderDTO createOrderWmsResponse = objectMapper.readValue(
                getFileContent("api/logistics/server/createOrder/happy/api.response.json"),
                new TypeReference<OrderDTO>() {
                });

        Mockito.when(orderManagementClient.createOrder(any())).thenAnswer((Answer) invocation -> {
            Object[] args = invocation.getArguments();
            OrderDTO order = (OrderDTO) args[0];
            Assertions.assertEquals(order, createOrderWmsRequest, "Request to WMS API is mismatched");
            return createOrderWmsResponse;
        });

        mockMvc.perform(post("/api/logistic/createOrder")
                .contentType(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/createOrder/happy/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent("api/logistics/server/createOrder/happy/response.xml")))
                .andReturn();

        verify(orderManagementClient, times(1)).createOrder(any());
    }
}
