package ru.yandex.market.checkout.checkouter.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class StatusDocumentationTest extends AbstractWebTestBase {


    @Test
    public void testCytoscapeResponce() throws Exception {
        String result = mockMvc.perform(get("/documentation/status?version=123"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertTrue(result.contains("cytoscape"));
    }


}
