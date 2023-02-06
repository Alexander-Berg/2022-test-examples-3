package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.response.CancelInboundResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class SwitchInforClientTest extends AbstractFunctionalTest {


    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_inbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void thatClientUrlMatchesFirstToken() throws Exception {
        final String receiptId = "0000000013";
        assertScenario(
            "fixtures/functional/wms_client/1/request.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "receipts", receiptId), HttpMethod.DELETE))
                .setResponsePath("fixtures/functional/cancel_inbound/common/empty_response.json")
                .setResponseStatus(HttpStatus.OK),
            "http://localhost/scprd/test1"
        );
    }

    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_inbound/common/state.xml",
        connection = "secondWmsConnection"
    )
    @Test
    void thatClientUrlMatchesSecondToken() throws Exception {
        final String receiptId = "0000000013";
        assertScenario(
            "fixtures/functional/wms_client/2/request.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "receipts", receiptId), HttpMethod.DELETE))
                .setResponsePath("fixtures/functional/cancel_inbound/common/empty_response.json")
                .setResponseStatus(HttpStatus.OK),
            "http://localhost/scprd/test2"
        );
    }

    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_inbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void whenReceiveUnknownTokenThanResponseWithError() throws Exception {
        FunctionalTestScenarioBuilder.start(CancelInboundResponse.class)
            .sendRequestToWrapQueryGateway("fixtures/functional/wms_client/3/request.xml")
            .andExpectWrapAnswerToBeEqualTo("fixtures/functional/wms_client/3/response.xml")
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, "http://localhost/scprd/test1")
            .start();
    }

    private void assertScenario(String wrapRequest,
                                FulfillmentInteraction interaction,
                                String url) throws Exception {

        FunctionalTestScenarioBuilder.start(CancelInboundResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interaction)
            .build(mockMvc, restTemplate, fulfillmentMapper, url)
            .start();
    }
}
