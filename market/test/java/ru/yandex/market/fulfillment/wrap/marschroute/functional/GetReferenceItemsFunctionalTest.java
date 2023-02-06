package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetReferenceItemsResponse;

import static java.util.Collections.singletonList;
import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class GetReferenceItemsFunctionalTest extends IntegrationTest {

    @Test
    void testPositiveScenario() throws Exception {
        String wrapRequestPath = "functional/get_reference_item/positive/wrapper_request.xml";
        String marschrouteResponsePath = "functional/get_reference_item/positive/marschroute_response.json";
        String wrapResponsePath = "functional/get_reference_item/positive/expected_wrapper_response.xml";

        LinkedMultiValueMap<String, String> urlArguments = new LinkedMultiValueMap<>();
        urlArguments.add("limit", "100");
        urlArguments.add("offset", "0");
        urlArguments.add("sort[prod_id]", "asc");

        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(singletonList("products"), HttpMethod.GET, urlArguments))
            .setResponsePath(marschrouteResponsePath);

        FunctionalTestScenarioBuilder
            .start(GetReferenceItemsResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequestPath)
            .thenMockFulfillmentRequest(marschrouteInteraction)
            .andExpectWrapAnswerToBeEqualTo(wrapResponsePath)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
