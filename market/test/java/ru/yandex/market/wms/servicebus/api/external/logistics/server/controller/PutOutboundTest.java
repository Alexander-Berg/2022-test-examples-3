package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import java.util.Collections;

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

import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.internal.api.client.impl.WmsApiClientImpl;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PutOutboundTest extends IntegrationTest {
    @Autowired
    @Qualifier("logistic-api")
    protected XmlMapper xmlMapper;
    @MockBean(name = "wmsApiClientImpl")
    @Autowired
    private WmsApiClientImpl wmsApiClientImpl;

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .serializationInclusion(NON_NULL)
            .dateFormat(new StdDateFormat())
            .failOnUnknownProperties(false)
            .build()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

    @BeforeEach
    public void init() {
        Mockito.reset(wmsApiClientImpl);
    }

    @Test
    public void putOutboundHappyTest() throws Exception {
        putOutboundTest("happy");
    }

    @Test
    public void putOutboundFixLostTest() throws Exception {
        OrderDTO responseOrderDto = OrderDTO.builder()
                .externorderkey("outbound-775325")
                .orderkey("0000068632")
                .originorderkey("0000068632")
                .type("24")
                .storerkey("")
                .orderdetails(Collections.emptyList())
                .build();

        Mockito.when(wmsApiClientImpl.writeOffFixlosts(any(), any(), eq(false), any()))
                .thenAnswer(invocation -> responseOrderDto);

        mockMvc.perform(post("/api/logistic/putOutbound")
                        .contentType(MediaType.TEXT_XML)
                        .accept(MediaType.TEXT_XML)
                        .content(getFileContent("api/logistics/server/putOutbound/fixLost/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .xml(getFileContent("api/logistics/server/putOutbound/fixLost/response.xml")))
                .andReturn();

    }

    @Test
    public void putOutboundOperLostTest() throws Exception {
        mockMvc.perform(post("/api/logistic/putOutbound")
                        .contentType(MediaType.TEXT_XML)
                        .accept(MediaType.TEXT_XML)
                        .content(getFileContent("api/logistics/server/putOutbound/operLost/request.xml")))
                .andExpect(status().is5xxServerError())
                .andExpect(content()
                        .xml(getFileContent("api/logistics/server/putOutbound/operLost/response.xml")));
        verifyNoInteractions(wmsApiClientImpl);
    }

    @Test
    public void putOutboundInterWarehouseTest() throws Exception {
        putOutboundTest("interStore");
    }

    @Test
    public void putOutboundAuctionTest() throws Exception {
        putOutboundTest("auction");
    }

    @Test
    public void putOutboundResellTest() throws Exception {
        putOutboundTest("resell");
    }

    private void putOutboundTest(String testCase) throws Exception {
        OrderDTO createOrderWmsRequest = objectMapper.readValue(
                getFileContent(String.format("api/logistics/server/putOutbound/%s/apiRequest.json", testCase)),
                new TypeReference<>() {
                });
        OrderDTO createOrderWmsResponse = objectMapper.readValue(
                getFileContent(String.format("api/logistics/server/putOutbound/%s/apiResponse.json", testCase)),
                new TypeReference<>() {
                });

        Mockito.when(wmsApiClientImpl.createOrder(any())).thenAnswer((Answer) invocation -> {
            Object[] args = invocation.getArguments();
            OrderDTO order = (OrderDTO) args[0];
            Assertions.assertEquals(createOrderWmsRequest, order);
            return createOrderWmsResponse;
        });

        mockMvc.perform(post("/api/logistic/putOutbound")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent(String.format("api/logistics/server/putOutbound/%s/request.xml", testCase))))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent(String.format("api/logistics/server/putOutbound/%s/response" +
                        ".xml", testCase))))
                .andReturn();

        verify(wmsApiClientImpl, times(1)).createOrder(any());
    }
}
