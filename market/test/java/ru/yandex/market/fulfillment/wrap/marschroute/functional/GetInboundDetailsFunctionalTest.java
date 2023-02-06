package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundDetailsResponse;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;


class GetInboundDetailsFunctionalTest extends IntegrationTest {

    /**
     * Сценарий, когда пользователь запрашивает детали существующей поставки и
     * они были обнаружены на стороне Маршрута и поставка уже была оприходована.
     */
    @Test
    void testPositiveScenario() throws Exception {
        String requestContentPath = "functional/get_inbound_details/positive/wrapper_request.xml";
        String waybillInfoPath = "functional/get_inbound_details/positive/marschroute_waybill_info_response.json";
        String waybillAdditionalInfoPath = "functional/get_inbound_details/positive/marschroute_response.json";
        String expectedWrapResponsePath = "functional/get_inbound_details/positive/wrapper_response.xml";

        FulfillmentInteraction waybill = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("waybill", "123"), HttpMethod.GET))
            .setResponsePath(waybillInfoPath);

        FulfillmentInteraction waybillAdditional = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("waybill", "123", "additional"), HttpMethod.GET))
            .setResponsePath(waybillAdditionalInfoPath);

        FunctionalTestScenarioBuilder
            .start(GetInboundDetailsResponse.class)
            .sendRequestToWrapQueryGateway(requestContentPath)
            .thenMockFulfillmentRequest(waybill)
            .thenMockFulfillmentRequest(waybillAdditional)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponsePath)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Сценарий, когда пользователь запрашивает детали поставки, которая не существует в Маршруте.
     */
    @Test
    void testScenarioWhenInboundDoesNotExist() throws Exception {
        String requestContentPath = "functional/get_inbound_details/missing_inbound/wrapper_request.xml";
        String waybillInfoPath = "functional/get_inbound_details/missing_inbound/marschroute_response.json";

        executeInvalidInboundScenario(requestContentPath, waybillInfoPath, (response, assertions) -> {
            List<ErrorPair> errorCodes = response.getRequestState().getErrorCodes();

            assertions.assertThat(errorCodes)
                .as("Assert that error codes contain only 1 value")
                .hasSize(1);

            ErrorPair errorCode = errorCodes.get(0);

            assertions.assertThat(errorCode.getCode())
                .as("Assert that error code belongs to unknown error")
                .isEqualTo(ErrorCode.UNKNOWN_ERROR);

            assertions.assertThat(errorCode.getMessage())
                .as("Assert that error message contains marschroute message")
                .contains("Накладая не найдена");

            assertions.assertThat(errorCode.getDescription())
                .as("Assert that error description contains marschroute code")
                .contains("401");
        });
    }

    /**
     * Сценарий, когда пользователь запрашивает детали существующей поставки,
     * которая на текущий момент не была оприходована.
     */
    @Test
    void testScenarioWhenInboundIsNotYetAccepted() throws Exception {
        String requestContentPath = "functional/get_inbound_details/not_yet_accepted/wrapper_request.xml";
        String waybillInfoPath = "functional/get_inbound_details/not_yet_accepted/marschroute_response.json";

        executeInvalidInboundScenario(requestContentPath, waybillInfoPath, (response, assertions) -> {
            List<ErrorPair> errorCodes = response.getRequestState().getErrorCodes();

            assertions.assertThat(errorCodes)
                .as("Assert that error codes contain only 1 value")
                .hasSize(1);

            ErrorPair errorCode = errorCodes.get(0);

            assertions.assertThat(errorCode.getCode())
                .as("Assert that error code belongs to unknown error")
                .isEqualTo(ErrorCode.UNKNOWN_ERROR);

            assertions.assertThat(errorCode.getDescription())
                .as("Assert that description contains current status code")
                .contains("122");
        });
    }

    private void executeInvalidInboundScenario(String requestContentPath,
                                               String waybillInfoPath,
                                               BiConsumer<ResponseWrapper<GetInboundDetailsResponse>, SoftAssertions> requirements) throws Exception {

        FulfillmentInteraction waybill = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("waybill", "123"), HttpMethod.GET))
            .setResponsePath(waybillInfoPath);

        FunctionalTestScenarioBuilder
            .start(GetInboundDetailsResponse.class)
            .sendRequestToWrapQueryGateway(requestContentPath)
            .thenMockFulfillmentRequest(waybill)
            .andExpectWrapAnswerToMeetRequirements(requirements)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
