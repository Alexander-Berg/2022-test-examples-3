package ru.yandex.market.wms.servicebus.api.external.logistics.client.controller;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.logistics.client.LogisticsApiClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class LogisticApiControllerTest extends IntegrationTest {
    @MockBean
    @Autowired
    private LogisticsApiClient logisticsApiClient;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(logisticsApiClient);
    }

    @Test
    public void testValidPushReferenceItems() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Iterable<Item> items = Arrays.asList(
                mapper.readValue(
                        getFileContent("api/internal/api/referenceItems/push/lgwRequest.json"),
                        Item[].class
                )
        );

        Mockito.when(logisticsApiClient.pushReferenceItems(items)).thenReturn(new ResponseEntity(HttpStatus.OK));
        Mockito.when(dbConfigService.getConfigAsBoolean("ASYNC_PUSH_REFERENCE_ITEMS")).thenReturn(false);

        mockMvc.perform(
                post("/api/referenceitems/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/api/referenceItems/push/apiRequest.json"))
        )
                .andExpect(status().isOk());
    }

    @Test
    public void testValidPushOrdersStatusesChanged() throws Exception {
        Mockito.when(logisticsApiClient.pushOrderStatusChanged(ArgumentMatchers.any()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        mockMvc.perform(
                post("/api/orderstatus/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("api/internal/api/orderStatus/push/apiRequest.json"))
        )
                .andExpect(status().isOk());
    }
}
