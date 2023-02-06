package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.fasterxml.jackson.databind.JavaType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.yandex.market.fulfillment.wrap.core.scenario.FunctionalTestScenario;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.GetInboundStatusNegativeScenarioWrongResponse;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.GetInboundStatusNegativeScenarioWrongWaybillStatus;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.GetInboundStatusNegativeScenarioWrongWaybillType;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.GetInboundStatusPositiveScenario;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.request.GetInboundsStatusRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundsStatusResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class GetInboundStatusFunctionalTest extends IntegrationTest {

    @Test
    void testPositiveScenario() throws Exception {
        executeScenario(new GetInboundStatusPositiveScenario(restTemplate, createMarschrouteApiUrl()));
    }

    @Test
    void testNegativeScenarioBadResponse() throws Exception {
        executeScenario(new GetInboundStatusNegativeScenarioWrongResponse(restTemplate, createMarschrouteApiUrl()));
    }

    @Test
    void testNegativeScenarioWrongWaybillType() throws Exception {
        executeScenario(new GetInboundStatusNegativeScenarioWrongWaybillType(restTemplate, createMarschrouteApiUrl()));
    }

    @Test
    void testNegativeScenarioWrongWaybillStatusCode() throws Exception {
        executeScenario(new GetInboundStatusNegativeScenarioWrongWaybillStatus(restTemplate, createMarschrouteApiUrl()));
    }

    private void executeScenario(FunctionalTestScenario<RequestWrapper<GetInboundsStatusRequest>, ResponseWrapper<GetInboundsStatusResponse>> scenario) throws Exception {
        scenario.configureMocks();

        String requestXml = scenario.getRequestContent();

        String responseXml = mockMvc.perform(post("/query-gateway")
                .contentType(MediaType.TEXT_XML_VALUE)
                .content(requestXml))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JavaType requestType = fulfillmentMapper.getTypeFactory().constructParametricType(RequestWrapper.class, GetInboundsStatusRequest.class);
        JavaType responseType = fulfillmentMapper.getTypeFactory().constructParametricType(ResponseWrapper.class, GetInboundsStatusResponse.class);

        scenario.doAssertions(
                softly,
                fulfillmentMapper.readValue(requestXml, requestType),
                fulfillmentMapper.readValue(responseXml, responseType)
        );

        scenario.verifyMocks();
    }
}
