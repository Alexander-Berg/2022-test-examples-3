package ru.yandex.market.wms.servicebus.api.internal.wms.server.controller;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.WrapInforClient;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.dto.IdentifierMappingDto;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.dto.InforUnitId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class MapSkuControllerTest extends IntegrationTest {

    @MockBean
    @Autowired
    private WrapInforClient wrapInforClient;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(wrapInforClient);
    }

    @Test
    public void shouldMapSku() throws Exception {
        Mockito.when(wrapInforClient.mapReferenceItems(Arrays.asList(
                new UnitId(null, 111111L, "exists.111111"),
                new UnitId(null, 222222L, "exists.222222"),
                new UnitId(null, 333333L, "not-exists.333333")
                )
        )).thenReturn(Arrays.asList(
                new IdentifierMappingDto(new UnitId(null, 111111L, "exists.111111"), new InforUnitId("ROV0000001",
                        111111L)),
                new IdentifierMappingDto(new UnitId(null, 222222L, "exists.222222"), new InforUnitId("ROV0000002",
                        222222L))
        ));

        MvcResult mockResult = mockMvc.perform(post("/wms/mapSku")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("api/internal/wms/mapSkus/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("api/internal/wms/mapSkus/response.json")))
                .andReturn();
    }


}
