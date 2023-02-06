package ru.yandex.market.logistic.gateway.controller;

import java.util.Collections;

import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemExpiration;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetExpirationItemsRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetExpirationItemsResponse;
import ru.yandex.market.logistic.gateway.service.executor.fulfillment.sync.GetExpirationItemsRequestExecutor;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmptyListIncludedTest extends AbstractIntegrationTest {

    @MockBean
    private GetExpirationItemsRequestExecutor getExpirationItemsRequestExecutor;

    @Test
    public void checkResponseContainsEmptyList() throws Exception {

        GetExpirationItemsRequest request = new GetExpirationItemsRequest(
            1, 1, null, new Partner(145L)
        );

         ItemExpiration itemExpiration = new ItemExpiration(
             new UnitId(null, 436353L, "pingpong-01-01-2297"),
             Collections.emptyList()
         );

        GetExpirationItemsResponse response =
            new GetExpirationItemsResponse(Collections.singletonList(itemExpiration));

        when(getExpirationItemsRequestExecutor.tryExecute(any(), anySet())).thenReturn(response);

        mockMvc.perform(post("/fulfillment/getExpirationItems")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(
                getFileContent("fixtures/response/fulfillment/get_expiration_items/fulfillment_get_expiration_items_with_empty_list.json"), true))
            .andReturn();
    }

}
