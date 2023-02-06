package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.fasterxml.jackson.databind.JavaType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.GetStocksBaseScenario;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.request.GetStocksRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetStocksResponse;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


abstract class BaseGetStocksFunctionalTest<R> extends IntegrationTest {

    @Test
    void testGetStocks() throws Exception {
        GetStocksBaseScenario scenario = getGetStocksScenario();
        scenario.configureMocks();

        String requestXml = scenario.getRequestContent();
        ResultActions result = mockMvc.perform(post("/query-gateway")
                .contentType(MediaType.TEXT_XML_VALUE)
                .content(requestXml));
        result.andExpect(status().isOk());

        String responseXml = result
                .andReturn()
                .getResponse()
                .getContentAsString();

        JavaType requestType = fulfillmentMapper.getTypeFactory().constructParametricType(RequestWrapper.class, GetStocksRequest.class);

        scenario.doAssertions(
                softly,
                fulfillmentMapper.readValue(requestXml, requestType),
                getExpectedResponse(responseXml)
        );

        scenario.verifyMocks();
    }

    protected ResponseWrapper<GetStocksResponse> getExpectedResponse(String responseXml) throws IOException {
        JavaType responseType = fulfillmentMapper.getTypeFactory().constructParametricType(ResponseWrapper.class, GetStocksResponse.class);
        return fulfillmentMapper.readValue(responseXml, responseType);

    }

    protected abstract GetStocksBaseScenario getGetStocksScenario();

}
