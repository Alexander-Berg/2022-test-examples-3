package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import ru.yandex.market.logistic.api.model.fulfillment.Inbound;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.wms.receiving.client.ReceivingClient;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.logistics.server.service.PutReferenceItemsService;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class CreateInboundTest extends IntegrationTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .serializationInclusion(NON_NULL)
            .dateFormat(new StdDateFormat())
            .failOnUnknownProperties(false)
            .build()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

    @MockBean
    @Autowired
    private ReceivingClient receivingClient;
    @MockBean
    @Autowired
    private PutReferenceItemsService putReferenceItemsService;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(receivingClient, putReferenceItemsService);
    }

    @Test
    public void createInboundSuccessfully() throws Exception {
        Inbound expectedInbound = objectMapper.readValue(
                getFileContent("api/logistics/server/createInbound/wms-api.request.json"),
                new TypeReference<Inbound>() { });

        ResourceId createInboundResponse = objectMapper.readValue(
                getFileContent("api/logistics/server/createInbound/wms-api.response.json"),
                new TypeReference<ResourceId>() { });

        Mockito.when(receivingClient.createInbound(any())).thenAnswer(invocation -> {
            Inbound actualInbound = invocation.getArgument(0);
            assertEquals(expectedInbound, actualInbound);
            return createInboundResponse;
        });

        mockMvc.perform(post("/api/logistic/createInbound")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/createInbound/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent("api/logistics/server/createInbound/response.xml")))
                .andReturn();

        verify(receivingClient, times(1)).createInbound(any());
        verify(putReferenceItemsService).saveInbound(anyList(), eq(true));
    }
}
