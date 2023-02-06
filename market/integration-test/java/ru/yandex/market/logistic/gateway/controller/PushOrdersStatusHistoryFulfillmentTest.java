package ru.yandex.market.logistic.gateway.controller;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.delivery.tracker.api.client.TrackerApiClient;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.model.Partner;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistics.test.integration.matchers.XmlMatcher;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class PushOrdersStatusHistoryFulfillmentTest extends AbstractIntegrationTest {

    @Autowired
    private TrackerApiClient trackerApiClient;

    @Test
    public void testFulfillmentPushOrdersStatusHistorySuccessful() throws Exception {
        String xmlResponse = performRequest("fixtures/request/fulfillment/push_orders_status_history/" +
            "fulfillment_push_orders_status_history.xml");

        verify(trackerApiClient).pushTracksHistory(
            argThat(partners -> Objects.equals(partners.get(0).getId(), 145L)),
            argThat(apiVersion -> Objects.equals(apiVersion, ApiVersion.FF)),
            anyList()
        );

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_orders_status_history/" +
            "fulfillment_push_orders_status_history.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }

    @Test
    public void testSeveralPartnersPushOrdersStatusHistorySuccessful() throws Exception {
        String xmlResponse = performRequest("fixtures/request/fulfillment/push_orders_status_history/" +
            "universal_fulfillment_push_orders_status_history.xml");

        verify(trackerApiClient).pushTracksHistory(
            argThat(partners ->
                Objects.equals(
                    partners.stream().map(Partner::getId).collect(Collectors.toList()),
                    List.of(45L, 46L, 44L)
                )
            ),
            argThat(apiVersion -> Objects.equals(apiVersion, ApiVersion.FF)),
            anyList());

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_orders_status_history/" +
            "fulfillment_push_orders_status_history.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();
    }

    @Test
    public void testFulfillmentPushPushOrdersStatusHistoryInvalidTokenError() throws Exception {
        String xmlResponse = performRequest("fixtures/request/fulfillment/push_orders_status_history/" +
            "fulfillment_push_orders_status_history_invalid_token.xml");

        verify(trackerApiClient, never()).pushTracksHistory(
            anyListOf(Partner.class),
            argThat(apiVersion -> Objects.equals(apiVersion, ApiVersion.FF)),
            anyList()
        );

        String expectedXml = getFileContent("fixtures/response/fulfillment/push_orders_status_history/" +
            "fulfillment_push_orders_status_history_invalid_token.xml");

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
