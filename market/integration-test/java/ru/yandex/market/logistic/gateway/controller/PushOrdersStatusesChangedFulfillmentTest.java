package ru.yandex.market.logistic.gateway.controller;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.delivery.tracker.domain.model.Partner;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistics.test.integration.matchers.XmlMatcher;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PushOrdersStatusesChangedFulfillmentTest extends AbstractIntegrationTest {

    @Autowired
    private TrackerApiClient trackerApiClient;

    @Test
    public void testFulfillmentPushOrdersStatusesChangedSuccessful() throws Exception {
        String xmlResponse = performRequest(
            "fixtures/request/fulfillment/push_orders_statuses_changed/" +
                "fulfillment_push_orders_statuses_changed.xml");

        verify(trackerApiClient).pushOrdersStatusesChanged(
            argThat(partners -> Objects.equals(partners.get(0).getId(), 145L)),
            anyList());

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_orders_statuses_changed/fulfillment_push_orders_statuses_changed.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }

    @Test
    public void testSortCenterPushOrdersStatusesChangedSuccessful() throws Exception {

        String xmlResponse = performRequest(
            "fixtures/request/fulfillment/push_orders_statuses_changed/" +
                "sorting_center_push_orders_statuses_changed.xml");

        verify(trackerApiClient).pushOrdersStatusesChanged(
            argThat(partners -> Objects.equals(partners.get(0).getId(), 79L)),
            anyList());

        String expectedXml = getFileContent(
            "fixtures/response/fulfillment/push_orders_statuses_changed/" +
                "fulfillment_push_orders_statuses_changed.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }

    @Test
    public void testSeveralPartnersPushOrdersStatusesChangedSuccessful() throws Exception {

        String xmlResponse = performRequest(
            "fixtures/request/fulfillment/push_orders_statuses_changed/" +
                "universal_push_order_statuses_changed.xml");

        verify(trackerApiClient).pushOrdersStatusesChanged(
            argThat(partners ->
                Objects.equals(partners.stream().map(Partner::getId).collect(Collectors.toList()),
                    List.of(45L, 46L, 44L))),
                anyList());

        String expectedXml = getFileContent(
            "fixtures/response/fulfillment/push_orders_statuses_changed/" +
                "fulfillment_push_orders_statuses_changed.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }

    @Test
    public void testFulfillmentPushOrdersStatusesChangedInvalidTokenError() throws Exception {

        String xmlResponse = performRequest(
            "fixtures/request/fulfillment/push_orders_statuses_changed/" +
                "fulfillment_push_orders_statuses_changed_invalid_token.xml");

        verify(trackerApiClient, never())
            .pushOrdersStatusesChanged(anyListOf(Partner.class), anyList());

        String expectedXml =
            getFileContent("fixtures/response/fulfillment/push_orders_statuses_changed/fulfillment_push_orders_statuses_changed_invalid_token.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }

    private String performRequest(String contentPath) throws Exception {
        return mockMvc.perform(post("/fulfillment/query-gateway")
            .contentType(MediaType.TEXT_XML_VALUE)
            .accept(MediaType.TEXT_XML_VALUE)
            .content(getFileContent(contentPath)))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }
}
