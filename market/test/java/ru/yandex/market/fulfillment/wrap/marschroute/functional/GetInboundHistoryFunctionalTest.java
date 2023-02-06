package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundHistoryResponse;

import java.util.Arrays;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;


class GetInboundHistoryFunctionalTest extends IntegrationTest {

    @Test
    void testPositiveScenario() throws Exception {
        String wrapRequest = "functional/get_inbound_history/wrap_request.xml";
        String marschrouteResponse = "functional/get_inbound_history/marschroute_response.json";
        String expectedWrapResponse = "functional/get_inbound_history/wrap_response.xml";

        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "521228"), HttpMethod.GET))
                .setResponsePath(marschrouteResponse);

        FunctionalTestScenarioBuilder.start(GetInboundHistoryResponse.class)
                .sendRequestToWrapQueryGateway(wrapRequest)
                .thenMockFulfillmentRequest(marschrouteInteraction)
                .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }
}
