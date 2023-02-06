package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

class PutOutboundRegistryControllerTest extends IntegrationTest {

    @Autowired
    @Qualifier("logistic-api")
    protected XmlMapper xmlMapper;
    @MockBean(name = "wmsApiClientImpl")
    @Autowired
    private WmsApiClientImpl wmsApiClientImpl;

    private ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .serializationInclusion(NON_NULL)
            .dateFormat(new StdDateFormat())
            .failOnUnknownProperties(false)
            .build()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

    @AfterEach
    public void resetMocks() {
        Mockito.reset(wmsApiClientImpl);
    }

    @Test
    public void putOutboundRegistryHappyTest() throws Exception {
        OrderDTO fullOrder = readOrderDTOFromFile("api/logistics/server/putOutboundRegistry/happy/fullOrder.json");
        OrderDTO partialOrder =
                readOrderDTOFromFile("api/logistics/server/putOutboundRegistry/happy/partialOrder.json");

        Mockito.when(wmsApiClientImpl.getOrderByExternalOrderKey((String) any())).thenAnswer((Answer) invocation -> {
            Object[] args = invocation.getArguments();
            String extOrderKey = (String) args[0];
            Assertions.assertEquals(partialOrder.getExternorderkey(), extOrderKey);
            return partialOrder;
        });

        Mockito.when(wmsApiClientImpl.createOrder((OrderDTO) any())).thenAnswer((Answer) invocation -> {
            Object[] args = invocation.getArguments();
            OrderDTO order = (OrderDTO) args[0];
            Assertions.assertEquals(fullOrder, order);
            return fullOrder;
        });

        mockMvc.perform(post("/api/logistic/putOutboundRegistry")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/putOutboundRegistry/happy/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent("api/logistics/server/putOutboundRegistry/happy/response.xml")))
                .andReturn();

        verify(wmsApiClientImpl, times(1)).getOrderByExternalOrderKey(any());
        verify(wmsApiClientImpl, times(1)).createOrder(any());
    }

    @Test
    public void validationShouldFail() throws Exception {
        OrderDTO partialOrder = readOrderDTOFromFile("api/logistics/server/putOutboundRegistry/invalid/partialOrder" +
                ".json");
        //api response should not contain order details
        Mockito.when(wmsApiClientImpl.getOrderByExternalOrderKey((String) any())).thenAnswer((Answer) invocation -> {
            Object[] args = invocation.getArguments();
            String extOrderKey = (String) args[0];
            Assertions.assertEquals(partialOrder.getExternorderkey(), extOrderKey);
            return partialOrder;
        });

        mockMvc.perform(post("/api/logistic/putOutboundRegistry")
                .contentType(MediaType.TEXT_XML)
                .accept(MediaType.TEXT_XML)
                .content(getFileContent("api/logistics/server/putOutboundRegistry/invalid/request.xml")))
                .andExpect(status().is5xxServerError())
                .andExpect(content().xml(
                        getFileContent("api/logistics/server/putOutboundRegistry/invalid/response.xml")))
                .andReturn();

        verify(wmsApiClientImpl, times(1)).getOrderByExternalOrderKey(any());
    }

    private OrderDTO readOrderDTOFromFile(String path) throws Exception {
        return objectMapper.readValue(
                getFileContent(String.format(path)),
                new TypeReference<OrderDTO>() {
                });
    }

}
