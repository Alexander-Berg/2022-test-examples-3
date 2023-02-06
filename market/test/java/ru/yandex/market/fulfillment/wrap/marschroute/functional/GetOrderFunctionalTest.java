package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOrderResponse;

import java.util.Arrays;
import java.util.LinkedList;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class GetOrderFunctionalTest extends IntegrationTest {

    /**
     * Проверяем, что пользователь прислал корректный запрос,
     * который был успешно обработан на стороне Маршрута.
     */
    @Test
    void testPositiveScenario() throws Exception {
        String wrapRequest = "functional/get_order/positive/wrap_request.xml";
        String marschrouteResponse = "functional/get_order/positive/marschroute_response.json";
        String marschrouteAdditionalResponse = "functional/get_order/positive/marschroute_additional_response.json";
        String expectedWrapResponse = "functional/get_order/positive/wrap_response.xml";

        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("order", "555555"), HttpMethod.GET))
            .setResponsePath(marschrouteResponse);

        FulfillmentInteraction marschrouteInteractionAdditional = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("order", "555555", "additional"), HttpMethod.GET))
            .setResponsePath(marschrouteAdditionalResponse);

        LinkedList<FulfillmentInteraction> fulfillmentInteractionList = new LinkedList<>();
        fulfillmentInteractionList.add(marschrouteInteraction);
        fulfillmentInteractionList.add(marschrouteInteractionAdditional);

        FunctionalTestScenarioBuilder.start(GetOrderResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequest(fulfillmentInteractionList)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Проверяем, что пользователь прислал корректный запрос,
     * который был успешно обработан на стороне Маршрута.
     *
     * Однако Маршрут не прислал коробки (т.е. упаковал в одну)
     */
    @Test
    void testPositiveScenarioWithoutShipments() throws Exception {
        String wrapRequest = "functional/get_order/positive/positive_without_shipments/wrap_request.xml";
        String marschrouteResponse = "functional/get_order/positive/positive_without_shipments/marschroute_response.json";
        String marschrouteAdditionalResponse = "functional/get_order/positive/positive_without_shipments/marschroute_additional_response.json";
        String expectedWrapResponse = "functional/get_order/positive/positive_without_shipments/wrap_response.xml";

        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("order", "555555"), HttpMethod.GET))
            .setResponsePath(marschrouteResponse);

        FulfillmentInteraction marschrouteInteractionAdditional = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("order", "555555", "additional"), HttpMethod.GET))
            .setResponsePath(marschrouteAdditionalResponse);

        LinkedList<FulfillmentInteraction> fulfillmentInteractionList = new LinkedList<>();
        fulfillmentInteractionList.add(marschrouteInteraction);
        fulfillmentInteractionList.add(marschrouteInteractionAdditional);

        FunctionalTestScenarioBuilder.start(GetOrderResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequest(fulfillmentInteractionList)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Проверяем, что пользователь прислал корректный запрос,
     * который был успешно обработан на стороне Маршрута.
     *
     * Однако Маршрут прислал одну коробку. В этом случае почему-то мы именуем плейс как заказ, а не как шипмент
     */
    @Test
    void testPositiveScenarioWithSingleShipment() throws Exception {
        String wrapRequest = "functional/get_order/positive/positive_with_single_shipment/wrap_request.xml";
        String marschrouteResponse = "functional/get_order/positive/positive_with_single_shipment/marschroute_response.json";
        String marschrouteAdditionalResponse = "functional/get_order/positive/positive_with_single_shipment/marschroute_additional_response.json";
        String expectedWrapResponse = "functional/get_order/positive/positive_with_single_shipment/wrap_response.xml";

        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("order", "555555"), HttpMethod.GET))
            .setResponsePath(marschrouteResponse);

        FulfillmentInteraction marschrouteInteractionAdditional = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("order", "555555", "additional"), HttpMethod.GET))
            .setResponsePath(marschrouteAdditionalResponse);

        LinkedList<FulfillmentInteraction> fulfillmentInteractionList = new LinkedList<>();
        fulfillmentInteractionList.add(marschrouteInteraction);
        fulfillmentInteractionList.add(marschrouteInteractionAdditional);

        FunctionalTestScenarioBuilder.start(GetOrderResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequest(fulfillmentInteractionList)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Проверяет, что в случае отсутствия yandex_id и partner_id при получении заказа -
     * запрос не пройдет валидацию.
     */
    @Test
    void orderIdValidationOnBothIdsMissing() throws Exception {
        FunctionalTestScenarioBuilder.start(GetOrderResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_order/negative/missing_both.xml")
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Проверяем ситуацию, когда товар отсутствует на складе.
     */
    @Test
    void testAbsentItemScenario() throws Exception {
        String wrapRequest = "functional/get_order/positive/absent_item/wrap_request.xml";
        String marschrouteResponse = "functional/get_order/positive/absent_item/marschroute_response.json";
        String marschrouteAdditionalResponse = "functional/get_order/positive/absent_item/marschroute_additional_response.json";
        String expectedWrapResponse = "functional/get_order/positive/absent_item/wrap_response.xml";

        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("order", "555555"), HttpMethod.GET))
                .setResponsePath(marschrouteResponse);

        FulfillmentInteraction marschrouteInteractionAdditional = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("order", "555555", "additional"), HttpMethod.GET))
                .setResponsePath(marschrouteAdditionalResponse);

        LinkedList<FulfillmentInteraction> fulfillmentInteractionList = new LinkedList<>();
        fulfillmentInteractionList.add(marschrouteInteraction);
        fulfillmentInteractionList.add(marschrouteInteractionAdditional);

        FunctionalTestScenarioBuilder.start(GetOrderResponse.class)
                .sendRequestToWrapQueryGateway(wrapRequest)
                .thenMockFulfillmentRequest(fulfillmentInteractionList)
                .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }

}
