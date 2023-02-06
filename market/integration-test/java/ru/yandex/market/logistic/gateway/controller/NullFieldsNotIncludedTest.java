package ru.yandex.market.logistic.gateway.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemReference;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetReferenceItemsRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetReferenceItemsResponse;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.sync.GetReferenceItemsRequestExecutor;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NullFieldsNotIncludedTest extends AbstractIntegrationTest {

    @MockBean
    private GetReferenceItemsRequestExecutor getReferenceItemsRequestExecutorMock;

    @Test
    public void checkResponseIsWithoutNullFields() throws Exception {

        ItemReference itemReference = new ItemReference(
            new UnitId(null, 436353L, "pingpong-01-01-2297"),
            new Korobyte(4, 10, 4, BigDecimal.valueOf(0.1), null, null),
            null,
            new LinkedHashSet<>(Arrays.asList(
                new Barcode("pingpong-01-01-2297", null, BarcodeSource.UNKNOWN),
                new Barcode("pingpong-01-01-2299", null, BarcodeSource.UNKNOWN),
                new Barcode("60377104", null, BarcodeSource.UNKNOWN),
                new Barcode("2867018669104", null, BarcodeSource.PARTNER))),
            null);

        GetReferenceItemsResponse getReferenceItemsResponse =
            new GetReferenceItemsResponse(Collections.singletonList(itemReference));

        when(getReferenceItemsRequestExecutorMock.tryExecute(any(), anySet())).thenReturn(getReferenceItemsResponse);

        final MvcResult mvcResult = mockMvc.perform(post("/fulfillment/getReferenceItems")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(new GetReferenceItemsRequest(1, 1, null, new Partner(145L)))))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(
                getFileContent("fixtures/response/fulfillment/get_reference_items/fulfillment_get_reference_items_without_null_fields.json"), true))
            .andReturn();
    }
}
