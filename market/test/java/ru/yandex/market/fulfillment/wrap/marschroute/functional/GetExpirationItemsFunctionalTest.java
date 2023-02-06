package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetExpirationItemsResponse;

import static java.util.Collections.singletonList;
import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class GetExpirationItemsFunctionalTest extends IntegrationTest {

    @Test
    void testPositiveScenario() throws Exception {
        testScenario("positive");
    }

    @Test
    void testNegativeScenario() throws Exception {
        testScenario("negative");
    }

    private void testScenario(String scenarioSubdirectoryName) throws Exception {
        String wrapRequestPath = "functional/get_expiration_items/" + scenarioSubdirectoryName +
            "/wrapper_request.xml";
        String marschrouteResponsePath = "functional/get_expiration_items/" + scenarioSubdirectoryName +
            "/marschroute_response.json";
        String wrapResponsePath = "functional/get_expiration_items/" + scenarioSubdirectoryName +
            "/expected_wrapper_response.xml";

        LinkedMultiValueMap<String, String> urlArguments = new LinkedMultiValueMap<>();
        urlArguments.add("limit", "100");
        urlArguments.add("offset", "0");
        urlArguments.add("sort[prod_id]", "asc");

        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(singletonList("products"), HttpMethod.GET, urlArguments))
            .setResponsePath(marschrouteResponsePath);

        FunctionalTestScenarioBuilder
            .start(GetExpirationItemsResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequestPath)
            .thenMockFulfillmentRequest(marschrouteInteraction)
            .andExpectWrapAnswerToBeEqualTo(wrapResponsePath)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
