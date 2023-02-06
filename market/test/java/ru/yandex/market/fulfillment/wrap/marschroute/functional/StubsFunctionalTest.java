package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import org.junit.jupiter.api.Test;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateIntakeResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateRegisterResponse;

class StubsFunctionalTest extends IntegrationTest {

    @Test
    void testCreateRegisterStub() throws Exception {
        executeScenario(
                "functional/create_register/request.xml",
                "functional/create_register/response.xml",
                CreateRegisterResponse.class
        );
    }

    @Test
    void testCreateIntakeStub() throws Exception {
        executeScenario(
                "functional/create_intake/request.xml",
                "functional/create_intake/response.xml",
                CreateIntakeResponse.class
        );
    }

    @Test
    void testCreateReturnRegisterStub() throws Exception {
        executeScenario(
                "functional/create_return_register/request.xml",
                "functional/create_return_register/response.xml",
                AbstractResponse.class
        );
    }

    private <T extends AbstractResponse> void executeScenario(String wrapRequest, String expectedWrapResponse, Class<T> responseClass) throws Exception {
        FunctionalTestScenarioBuilder.start(responseClass)
                .sendRequestToWrapQueryGateway(wrapRequest)
                .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }
}
