package ru.yandex.market.logistic.gateway.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LogisticApiControllerTest extends AbstractIntegrationTest {

    @Test
    public void badRequestType() throws Exception {
        mockMvc.perform(post("/delivery/query-gateway")
            .contentType(MediaType.TEXT_XML_VALUE)
            .accept(MediaType.TEXT_XML_VALUE)
            .content(getFileContent("fixtures/request/delivery/delivery_bad_request_type.xml")))
            .andExpect(status().is4xxClientError())
            .andExpect(content()
                .string("Can't find PartnerMethod by apiType='DELIVERY' and requestName='theMostUselessRequest'"));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/marschroute_delivery_token.xml",
        connection = "dbUnitDatabaseConnection"
    )
    public void applicationXmlContentTypeDeliveryRequest() throws Exception {
        mockMvc.perform(post("/delivery/query-gateway")
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .content(getFileContent("fixtures/request/delivery/push_orders_statuses_changed" +
                "/delivery_push_orders_statuses_changed.xml")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_XML_VALUE));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/marschroute_delivery_token.xml",
        connection = "dbUnitDatabaseConnection"
    )
    public void textXmlContentTypeDeliveryRequest() throws Exception {
        mockMvc.perform(post("/delivery/query-gateway")
            .contentType(MediaType.TEXT_XML_VALUE)
            .content(getFileContent("fixtures/request/delivery/push_orders_statuses_changed" +
                "/delivery_push_orders_statuses_changed.xml")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.TEXT_XML_VALUE));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/marschroute_fulfillment_token.xml",
        connection = "dbUnitDatabaseConnection"
    )
    public void applicationXmlContentTypeFulfillmentRequest() throws Exception {
        mockMvc.perform(post("/fulfillment/query-gateway")
            .contentType(MediaType.APPLICATION_XML_VALUE)
            .content(getFileContent("fixtures/request/fulfillment/push_orders_statuses_changed" +
                "/fulfillment_push_orders_statuses_changed.xml")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_XML_VALUE));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/state/marschroute_fulfillment_token.xml",
        connection = "dbUnitDatabaseConnection"
    )
    public void textXmlContentTypeFulfillmentRequest() throws Exception {
        mockMvc.perform(post("/fulfillment/query-gateway")
            .contentType(MediaType.TEXT_XML_VALUE)
            .content(getFileContent("fixtures/request/fulfillment/push_orders_statuses_changed" +
                "/fulfillment_push_orders_statuses_changed.xml")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.TEXT_XML_VALUE));
    }
}
