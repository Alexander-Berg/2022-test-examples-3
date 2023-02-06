package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import ru.yandex.market.wms.common.spring.dto.ReceiptDto;
import ru.yandex.market.wms.receiving.client.ReceivingClient;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.internal.api.client.impl.WmsApiClientImpl;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PutInboundTest extends IntegrationTest {
    @Autowired
    @Qualifier("logistic-api")
    protected XmlMapper xmlMapper;
    @MockBean(name = "wmsApiClientImpl")
    @Autowired
    private WmsApiClientImpl wmsApiClientImpl;

    @MockBean(name = "receivingClient")
    @Autowired
    private ReceivingClient receivingClient;

    @BeforeEach
    public void init() {
        Mockito.reset(wmsApiClientImpl);
        Mockito.reset(receivingClient);
    }

    @Test
    public void putInboundHappyTest() throws Exception {
        putInboundTest("happy");
    }

    @Test
    public void putInboundInventarizationTest() throws Exception {
        putInboundTest("inventarization");
    }

    @Test
    public void putInboundCheckExReceiptKey3Test() throws Exception {
        putInboundTest("invalid-exreceipkey3");
    }

    private void putInboundTest(String testCase) throws Exception {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .serializationInclusion(NON_NULL)
                .dateFormat(new StdDateFormat())
                .failOnUnknownProperties(false)
                .build()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

        ReceiptDto createReceiptWmsRequest = objectMapper.readValue(
                getFileContent(String.format("api/logistics/server/putInbound/%s/apiRequest.json", testCase)),
                new TypeReference<ReceiptDto>() {
                });

        ReceiptDto createReceiptWmsResponse = objectMapper.readValue(
                getFileContent(String.format("api/logistics/server/putInbound/%s/apiResponse.json", testCase)),
                new TypeReference<ReceiptDto>() {
                });

        Mockito.when(receivingClient.createReceipt(any())).thenAnswer((Answer) invocation -> {
            Object[] args = invocation.getArguments();
            ReceiptDto receipt = (ReceiptDto) args[0];
            Assertions.assertEquals(createReceiptWmsRequest, receipt);
            return createReceiptWmsResponse;
        });

        mockMvc.perform(post("/api/logistic/putInbound")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent(String.format("api/logistics/server/putInbound/%s/request.xml", testCase))))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent(String.format("api/logistics/server/putInbound/%s/response.xml",
                        testCase))))
                .andReturn();

        verify(receivingClient, times(1)).createReceipt(any());
    }
}
