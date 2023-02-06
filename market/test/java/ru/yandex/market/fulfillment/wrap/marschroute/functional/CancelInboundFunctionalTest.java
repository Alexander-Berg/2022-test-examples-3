package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.fulfillment.response.CancelInboundResponse;

class CancelInboundFunctionalTest extends IntegrationTest {

    /**
     * Позитивный сценарий с успешной отменой поставки.
     */
    @Test
    void testPositiveScenario() throws Exception {
        String wrapRequestPath = "functional/cancel_inbound/positive/wrap_request.xml";
        String marschrouteResponse = "functional/cancel_inbound/positive/marschroute_response.json";
        String expectedWrapResponse = "functional/cancel_inbound/positive/wrap_response.xml";

        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("waybill", "853537"), HttpMethod.DELETE))
            .setResponsePath(marschrouteResponse);

        FunctionalTestScenarioBuilder
            .start(CancelInboundResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequestPath)
            .thenMockFulfillmentRequest(marschrouteInteraction)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }


    /**
     * Негативный сценарий, когда накладная поставки не была найдена в Маршруте.
     */
    @Test
    void testNegativeScenario() throws Exception {
        String wrapRequestPath = "functional/cancel_inbound/negative/wrap_request.xml";
        String marschrouteResponsePath = "functional/cancel_inbound/negative/marschroute_response.json";

        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("waybill", "853537"), HttpMethod.DELETE))
            .setResponsePath(marschrouteResponsePath);

        FunctionalTestScenarioBuilder
            .start(CancelInboundResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequestPath)
            .thenMockFulfillmentRequest(marschrouteInteraction)
            .andExpectWrapAnswerToMeetRequirements((response, assertions) -> {
                List<ErrorPair> errorCodes = response.getRequestState().getErrorCodes();

                assertions.assertThat(errorCodes)
                    .as("Assert that error codes contain 1 error")
                    .hasSize(1);

                ErrorPair errorCode = errorCodes.get(0);

                assertions.assertThat(errorCode.getCode())
                    .as("Assert that it is unknown error")
                    .isEqualTo(ErrorCode.UNKNOWN_ERROR);

                assertions.assertThat(errorCode.getMessage())
                    .as("Assert that message contains marschroute message")
                    .contains("Накладная не может быть отменена");

                assertions.assertThat(errorCode.getDescription())
                    .as("Assert that description contains marschroute error code")
                    .contains("408");
            })
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
