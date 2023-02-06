package ru.yandex.market.wrap.infor.functional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateOrderResponse;

class SwitchWmsDatasourceTest extends AbstractFunctionalTest {

    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/wms_datasource/first_db_state.xml",
        connection = "wmsConnection"
    )
    @DatabaseSetup(
        value = "classpath:fixtures/functional/wms_datasource/second_db_state.xml",
        connection = "secondWmsConnection"
    )
    void whenReceiveFirstDbTokenGetOrderFromFirstDb() throws Exception {
        executeScenario(
            "fixtures/functional/wms_datasource/wrap_request_with_first_db_token.xml",
            "fixtures/functional/wms_datasource/wrap_response_with_first_db_order.xml"
        );
    }

    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/wms_datasource/first_db_state.xml",
        connection = "wmsConnection"
    )
    @DatabaseSetup(
        value = "classpath:fixtures/functional/wms_datasource/second_db_state.xml",
        connection = "secondWmsConnection"
    )
    void whenReceiveSecondDbTokenGetOrderFromSecondDb() throws Exception {
        executeScenario(
            "fixtures/functional/wms_datasource/wrap_request_with_second_db_token.xml",
            "fixtures/functional/wms_datasource/wrap_response_with_second_db_order.xml"
        );
    }

    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/wms_datasource/first_db_state.xml",
        connection = "wmsConnection"
    )
    @DatabaseSetup(
        value = "classpath:fixtures/functional/wms_datasource/second_db_state.xml",
        connection = "secondWmsConnection"
    )
    void whenReceiveUnknownTokenResponseWithError() throws Exception {
        executeScenario(
            "fixtures/functional/wms_datasource/wrap_request_with_unknown_token.xml",
            "fixtures/functional/wms_datasource/wrap_response_with_token_error.xml"
        );
    }

    private void executeScenario(String wrapRequest,
                                 String wrapResponse,
                                 FulfillmentInteraction... interactions) throws Exception {
        FunctionalTestScenarioBuilder.start(CreateOrderResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }
}
