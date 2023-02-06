package ru.yandex.market.logistics.iris.controller;

import org.junit.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LgwResponseControllerTest extends AbstractContextualTest {

    @Test
    public void receivePutReferenceItemsResultSuccess() throws Exception {
        mockMvc.perform(
            post("/lgw/put_reference_items_result")
                .content("{" +
                    "\"createdItems\": [{\"id\": \"created_id\", \"vendorId\": 1, \"article\": \"created_article\"}]," +
                    "\"errorItems\": [{\"unitId\": {\"id\": \"error_id\", \"vendorId\": 1, " +
                    "\"article\": \"error_article\"}, \"errorCode\": {\"code\": 9999, \"message\": \"error\"}}]}")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
