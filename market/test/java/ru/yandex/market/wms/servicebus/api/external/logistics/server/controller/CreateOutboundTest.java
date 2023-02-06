package ru.yandex.market.wms.servicebus.api.external.logistics.server.controller;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.dto.IdentifierMappingDto;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.dto.InforUnitId;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.impl.WrapInforClientImpl;
import ru.yandex.market.wms.servicebus.api.internal.api.client.impl.WmsApiClientImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class CreateOutboundTest extends IntegrationTest {
    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean(name = "wmsApiClientImpl")
    @Autowired
    private WmsApiClientImpl wmsApiClientImpl;

    @MockBean(name = "wrapInforClientImpl")
    @Autowired
    private WrapInforClientImpl wrapInforClientImpl;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(wmsApiClientImpl);
    }

    @Test
    public void happy() throws Exception {
        objectMapper.registerModule(new JavaTimeModule());
        OrderDTO createOrderWmsResponse = objectMapper.readValue(
                getFileContent("api/logistics/server/createOutbound/happy/api.response.json"),
                new TypeReference<OrderDTO>() {
                });

        Mockito.when(wmsApiClientImpl.createOrder((OrderDTO) any()))
                .thenAnswer((Answer) invocation -> createOrderWmsResponse);

        Mockito.when(wrapInforClientImpl.mapReferenceItems(anyList())).thenAnswer((Answer) invocation -> {
            List<UnitId> unitIds = invocation.getArgument(0);
            List<IdentifierMappingDto> result = new ArrayList<>();
            for (UnitId id : unitIds) {
                IdentifierMappingDto element = new IdentifierMappingDto();
                element.setInforUnitId(new InforUnitId(id.getId(), id.getVendorId()));
                element.setUnitId(new ru.yandex.market.logistic.api.model.fulfillment.UnitId(id.getId(),
                        id.getVendorId(), id.getArticle()));
                result.add(element);
            }
            return result;
        });

        mockMvc.perform(post("/api/logistic/createOutbound")
                        .contentType(MediaType.TEXT_XML)
                        .accept(MediaType.TEXT_XML)
                        .content(getFileContent("api/logistics/server/createOutbound/happy/request.xml")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().xml(getFileContent("api/logistics/server/createOutbound/happy/response.xml")))
                .andReturn();

        verify(wmsApiClientImpl, times(1)).createOrder(any());
    }
}
