package ru.yandex.market.logistic.gateway.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.delivery.tracker.domain.model.Partner;
import ru.yandex.market.delivery.tracker.domain.model.ResourceId;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistics.test.integration.matchers.XmlMatcher;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PushOrdersStatusesChangedDeliveryTest extends AbstractIntegrationTest {

    @Autowired
    private TrackerApiClient trackerApiClient;

    @Test
    public void testDeliveryPushOrdersStatusesChangedSuccessful() throws Exception {

        String xmlResponse = mockMvc.perform(post("/delivery/query-gateway")
            .contentType(MediaType.TEXT_XML_VALUE)
            .accept(MediaType.TEXT_XML_VALUE)
            .content(getFileContent("fixtures/request/delivery/push_orders_statuses_changed/delivery_push_orders_statuses_changed.xml")))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        verify(trackerApiClient).pushOrdersStatusesChanged(anyListOf(Partner.class), anyListOf(ResourceId.class));

        String expectedXml = getFileContent("fixtures/response/delivery/push_order_statuses_changed/delivery_push_orders_statuses_changed.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }

    @Test
    public void testDeliveryPushOrdersStatusesChangedInvalidTokenError() throws Exception {
        String xmlResponse = mockMvc.perform(post("/delivery/query-gateway")
            .contentType(MediaType.TEXT_XML_VALUE)
            .accept(MediaType.TEXT_XML_VALUE)
            .content(getFileContent("fixtures/request/delivery/push_orders_statuses_changed/delivery_push_orders_statuses_changed_invalid_token.xml")))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();

        verify(trackerApiClient, never())
            .pushOrdersStatusesChanged(anyListOf(Partner.class), anyListOf(ResourceId.class));

        String expectedXml =
            getFileContent("fixtures/response/delivery/push_order_statuses_changed/delivery_push_orders_statuses_changed_invalid_token.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }
}
