package ru.yandex.market.wrap.infor.functional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOrderResponse;

class TokenValidationFunctionalTest extends AbstractFunctionalTest {
    /**
     * Проверяем, что пользователь прислал запрос с токеном, которого нет в таблице,
     * возвращаем ответ с ошибкой валидации токена.
     */
    @Test
    void wrapReturnsErrorWhenTokenDoesNotExistInDb() throws Exception {
        String wrapRequest = "fixtures/functional/token_validation/unknown_token/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/token_validation/unknown_token/wrap_response.xml";

        FunctionalTestScenarioBuilder.start(GetOrderResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Проверяем, что пользователь прислал запрос с requestType, которые не поддерживается данным токеном,
     * возвращаем ответ с ошибкой валидации токена.
     */
    @Test
    void wrapReturnsErrorWhenTokenDoesNotSupportRequestType() throws Exception {
        String wrapRequest = "fixtures/functional/token_validation/unsupported_request_type/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/token_validation/unsupported_request_type/wrap_response.xml";

        FunctionalTestScenarioBuilder.start(GetOrderResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }
}
