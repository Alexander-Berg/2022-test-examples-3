package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import java.util.Arrays;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class CancelOrderFunctionalTest extends IntegrationTest {

    /**
     * Проверяем, что пользователь прислал корректный запрос,
     * который был успешно обработан на стороне Маршрута.
     */
    @Test
    void positiveScenario() throws Exception {
        FulfillmentInteraction interaction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("order", "853537"), HttpMethod.DELETE))
            .setResponsePath("functional/cancel_order/positive/marschroute_response.json");

        FunctionalTestScenarioBuilder.start(AbstractResponse.class)
            .sendRequestToWrapQueryGateway("functional/cancel_order/positive/wrap_request.xml")
            .thenMockFulfillmentRequest(interaction)
            .andExpectWrapAnswerToBeEqualTo("functional/cancel_order/positive/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Проверяет, что в случае отсутствия yandex_id при отмене заказа -
     * запрос не пройдет валидацию.
     */
    @Test
    void orderIdValidationOnMissingYandexId() throws Exception {
        testOrderIdValidation("functional/cancel_order/negative/missing_yandex_id.xml");
    }

    /**
     * Проверяет, что в случае отсутствия partner_id при отмене заказа -
     * запрос не пройдет валидацию.
     */
    @Test
    void orderIdValidationOnMissingPartnerId() throws Exception {
        testOrderIdValidation("functional/cancel_order/negative/missing_partner_id.xml");
    }

    /**
     * Проверяет, что в случае отсутствия yandex_id и partner_id при отмене заказа -
     * запрос не пройдет валидацию.
     */
    @Test
    void orderIdValidationOnBothIdsMissing() throws Exception {
        testOrderIdValidation("functional/cancel_order/negative/missing_both.xml");
    }

    private void testOrderIdValidation(String requestPath) throws Exception {
        FunctionalTestScenarioBuilder.start(AbstractResponse.class)
            .sendRequestToWrapQueryGateway(requestPath)
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
