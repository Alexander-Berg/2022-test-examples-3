package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOrdersStatusResponse;

import java.util.Collections;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;


class GetOrdersStatusFunctionalTest extends IntegrationTest {

    /**
     * Сценарий для ручки /fulfillment/...
     * На стороне Маршрута нашлись все заказы из запроса и они возвращаются нам в ответе.
     */
    @Test
    void testFulfillmentPositiveScenario() throws Exception {
        String wrapRequest = "functional/get_orders_status/fulfillment/positive_wrap_request.xml";
        String marschrouteResponse = "functional/get_orders_status/fulfillment/positive_marschroute_response.json";
        String wrapResponse = "functional/get_orders_status/fulfillment/positive_wrap_response.xml";

        executeScenario(wrapRequest, marschrouteResponse, wrapResponse, fulfillmentMapper, "fulfillment");
    }

    /**
     * Сценарий для ручки /delivery/...
     * На стороне Маршрута нашлись все заказы из запроса и они возвращаются нам в ответе.
     */
    @Test
    void testDeliveryPositiveScenario() throws Exception {
        String wrapRequest = "functional/get_orders_status/delivery/positive_wrap_request.xml";
        String marschrouteResponse = "functional/get_orders_status/delivery/positive_marschroute_response.json";
        String wrapResponse = "functional/get_orders_status/delivery/positive_wrap_response.xml";

        executeScenario(wrapRequest, marschrouteResponse, wrapResponse, deliveryMapper, "delivery");
    }

    /**
     * Сценарий для ручки /fulfillment/...
     * На стороне Маршрута не нашлось заказов с указанными id и нам возвращается пустой ответ.
     */
    @Test
    void testFulfillmentEmptyResponseScenario() throws Exception {
        String wrapRequest = "functional/get_orders_status/fulfillment/positive_wrap_request.xml";
        String marschrouteResponse = "functional/get_orders_status/empty_marschroute_response.json";
        String wrapResponse = "functional/get_orders_status/empty_wrap_response.xml";

        executeScenario(wrapRequest, marschrouteResponse, wrapResponse, fulfillmentMapper, "fulfillment");
    }

    /**
     * Сценарий для ручки /delivery/...
     * На стороне Маршрута не нашлось заказов с указанными id и нам возвращается пустой ответ.
     */
    @Test
    void testDeliveryEmptyResponseScenario() throws Exception {
        String wrapRequest = "functional/get_orders_status/delivery/positive_wrap_request.xml";
        String marschrouteResponse = "functional/get_orders_status/empty_marschroute_response.json";
        String wrapResponse = "functional/get_orders_status/empty_wrap_response.xml";

        executeScenario(wrapRequest, marschrouteResponse, wrapResponse, deliveryMapper, "delivery");
    }

    private void executeScenario(String wrapRequest,
                                 String marschrouteResponse,
                                 String wrapResponse,
                                 ObjectMapper mapper,
                                 String urlPrefix) throws Exception {
        LinkedMultiValueMap<String, String> urlArguments = new LinkedMultiValueMap<>();
        urlArguments.add("filter[order_id][]", "EXT62137374");
        urlArguments.add("filter[order_id][]", "EXT62136198");
        urlArguments.add("filter[order_id][]", "EXT62125658");
        urlArguments.add("filter[order_id][]", "EXT62119624");

        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("orders"), HttpMethod.GET, urlArguments))
                .setResponsePath(marschrouteResponse);

        FunctionalTestScenarioBuilder.start(GetOrdersStatusResponse.class)
                .sendRequestToWrap("/" + urlPrefix + "/get-orders-status", HttpMethod.POST, wrapRequest)
                .thenMockFulfillmentRequest(marschrouteInteraction)
                .andExpectWrapAnswerToBeEqualTo(wrapResponse)
                .build(mockMvc, restTemplate, mapper, apiUrl, apiKey)
                .start();
    }
}
