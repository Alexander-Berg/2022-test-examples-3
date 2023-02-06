package ru.yandex.market.logistic.gateway.controller;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.config.ExecutorTestConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(
    classes = ExecutorTestConfiguration.class,
    properties = {"tvm.internal.log-only-mode=false", "tvm.unsecured-methods=/ping"}
)
@ActiveProfiles(profiles = "security-test")
public class TvmSecurityTest extends AbstractIntegrationTest {

    @Test
    public void testSuccessfulRequest() throws Exception {
        mockMvc.perform(baseRequestBuilder())
            .andExpect(status().is4xxClientError())
            .andExpect(content().json("{"
                + "\"error\":\"SERVICE_TICKET_NOT_PRESENT\","
                + "\"requestUri\":\"/fulfillment/getOrdersStatus\","
                + "\"remoteAddress\":\"127.0.0.1\""
                + "}"));
    }

    private MockHttpServletRequestBuilder baseRequestBuilder() {
        return post("/fulfillment/getOrdersStatus")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/fulfillment/get_orders_status/fulfillment_get_orders_status.json"));
    }
}
