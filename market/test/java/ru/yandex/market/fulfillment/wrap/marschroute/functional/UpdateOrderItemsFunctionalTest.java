package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.logistic.api.model.fulfillment.response.UpdateOrderResponse;

import static java.util.Arrays.asList;
import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

public class UpdateOrderItemsFunctionalTest extends IntegrationTest {

    /**
     * Сценарий #1:
     * <p>
     * Попытка обновления заказа, статус которого запрещает исполнять запрос на обновление (уже был исполнен).
     * <p>
     * В результате исполнения запроса должны получить соответствующую ошибку.
     */
    @Test
    void updateDeliveredOrder() throws Exception {
        FulfillmentInteraction trackingInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("tracking", "EXT153160833"), HttpMethod.GET))
            .setResponsePath("functional/update_order_items/1/tracking.json");

        FunctionalTestScenarioBuilder.start(UpdateOrderResponse.class)
            .sendRequestToWrapQueryGateway("functional/update_order_items/1/request.xml")
            .thenMockFulfillmentRequest(trackingInteraction)
            .andExpectWrapAnswerToBeEqualTo("functional/update_order_items/1/response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Сценарий #2:
     * <p>
     * Попытка обновить заказ, который не существует в Маршруте.
     * <p>
     * На этапе трекинга заказа из Маршрута должна вернуться ошибка,
     * которая затем должна быть возвращена пользователю прослойки.
     */
    @Test
    void updateNonExistingOrder() throws Exception {
        FulfillmentInteraction trackingInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("tracking", "EXT153160833"), HttpMethod.GET))
            .setResponsePath("functional/update_order_items/2/tracking.json");

        FunctionalTestScenarioBuilder.start(UpdateOrderResponse.class)
            .sendRequestToWrapQueryGateway("functional/update_order_items/2/request.xml")
            .thenMockFulfillmentRequest(trackingInteraction)
            .andExpectWrapAnswerToBeEqualTo("functional/update_order_items/2/response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Сценарий #3:
     * <p>
     * Попытка обновить заказ, с просроченной датой отправки.
     * <p>
     * На этапе обновления заказа Маршрут должен вернуть ошибку,
     * которая затем должна быть возвращена пользователю прослойки.
     */
    @Test
    void updateExpiredOrder() throws Exception {
        FulfillmentInteraction trackingInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("tracking", "EXT153099794"), HttpMethod.GET))
            .setResponsePath("functional/update_order_items/3/tracking.json");

        FulfillmentInteraction getOrderInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT153099794"), HttpMethod.GET))
            .setResponsePath("functional/update_order_items/3/order.json");

        FulfillmentInteraction updateInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT153099794"), HttpMethod.POST))
            .setExpectedRequestPath("functional/update_order_items/3/update_request.json")
            .setResponsePath("functional/update_order_items/3/update_response.json");

        FulfillmentInteraction getYandexIdInteraction = getOrderInteraction;


        FunctionalTestScenarioBuilder.start(UpdateOrderResponse.class)
            .sendRequestToWrapQueryGateway("functional/update_order_items/3/request.xml")
            .thenMockFulfillmentRequest(trackingInteraction)
            .thenMockFulfillmentRequest(getYandexIdInteraction)
            .thenMockFulfillmentRequest(getYandexIdInteraction)
            .thenMockFulfillmentRequest(getOrderInteraction)
            .thenMockFulfillmentRequest(updateInteraction)
            .andExpectWrapAnswerToBeEqualTo("functional/update_order_items/3/response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Сценарий #4:
     * <p>
     * Обновление позиций заказа.
     */
    @Test
    void updateOrderItems() throws Exception {
        FulfillmentInteraction trackingInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("tracking", "EXT153160833"), HttpMethod.GET))
            .setResponsePath("functional/update_order_items/4/tracking.json");

        FulfillmentInteraction getOrderInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT153160833"), HttpMethod.GET))
            .setResponsePath("functional/update_order_items/4/order.json");

        FulfillmentInteraction updateInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT153160833"), HttpMethod.POST))
            .setExpectedRequestPath("functional/update_order_items/4/update_request.json")
            .setResponsePath("functional/update_order_items/4/update_response.json");

        FulfillmentInteraction getYandexIdInteraction = getOrderInteraction;


        FunctionalTestScenarioBuilder.start(UpdateOrderResponse.class)
            .sendRequestToWrapQueryGateway("functional/update_order_items/4/request.xml")
            .thenMockFulfillmentRequest(trackingInteraction)
            .thenMockFulfillmentRequest(getYandexIdInteraction)
            .thenMockFulfillmentRequest(getYandexIdInteraction)
            .thenMockFulfillmentRequest(getOrderInteraction)
            .thenMockFulfillmentRequest(updateInteraction)
            .andExpectWrapAnswerToBeEqualTo("functional/update_order_items/4/response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Сценарий #5:
     * <p>
     * Обновление позиций заказа в волне.
     */
    @Test
    void updateOrderItemsWhichReceivedIsBeingPackagedStatus() throws Exception {
        FulfillmentInteraction trackingInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("tracking", "EXT153160833"), HttpMethod.GET))
            .setResponsePath("functional/update_order_items/5/tracking.json");

        FulfillmentInteraction getOrderInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT153160833"), HttpMethod.GET))
            .setResponsePath("functional/update_order_items/5/order.json");

        FulfillmentInteraction updateInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT153160833"), HttpMethod.POST))
            .setExpectedRequestPath("functional/update_order_items/5/update_request.json")
            .setResponsePath("functional/update_order_items/5/update_response.json");

        FulfillmentInteraction getYandexIdInteraction = getOrderInteraction;

        FunctionalTestScenarioBuilder.start(UpdateOrderResponse.class)
            .sendRequestToWrapQueryGateway("functional/update_order_items/5/request.xml")
            .thenMockFulfillmentRequest(trackingInteraction)
            .thenMockFulfillmentRequest(getYandexIdInteraction)
            .thenMockFulfillmentRequest(getYandexIdInteraction)
            .thenMockFulfillmentRequest(getOrderInteraction)
            .thenMockFulfillmentRequest(updateInteraction)
            .andExpectWrapAnswerToBeEqualTo("functional/update_order_items/5/response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

}
