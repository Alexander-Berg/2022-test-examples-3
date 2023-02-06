package ru.yandex.market.checkout.checkouter.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.market.checkout.application.AbstractWebTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SwaggerApiDocsControllerTest extends AbstractWebTestBase {

    @Test
    public void docGeneration() throws Exception {
        final MvcResult result = mockMvc.perform(get("/v2/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        final String rawResult = result.getResponse().getContentAsString();
        Assertions.assertNotNull(rawResult);
        Assertions.assertTrue(rawResult.startsWith("{\"swagger\":\"2.0\""));
        Assertions.assertTrue(rawResult.contains("\"value\":[{\"name\":\"name\",\"value\":\"USER_NOT_PAID\"}," +
                "{\"name\":\"description\",\"value\":\"CANCELLED: Покупатель не оплатил заказ\"}," +
                "{\"name\":\"id\",\"value\":\"1\"}]"));
        Assertions.assertTrue(rawResult.contains("\"value\":[{\"name\":\"name\",\"value\":\"DELIVERY\"}," +
                "{\"name\":\"description\",\"value\":\"CARGO: Доставка курьером\"}," +
                "{\"name\":\"id\",\"value\":\"0\"}]"));
    }
}
