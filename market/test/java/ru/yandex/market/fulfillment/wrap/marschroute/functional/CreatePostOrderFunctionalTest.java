package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.model.GeoInformation;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateOrderResponse;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;
import static ru.yandex.market.fulfillment.wrap.marschroute.service.order.request.factory.delivery.self.CreateMarketDeliveryOrderRequestFactory.DEFAULT_FALLBACK_LOCATION_ID;

class CreatePostOrderFunctionalTest extends RepositoryTest {

    @MockBean
    private GeoInformationProvider geoInformationProvider;

    /**
     * Сценарий #1:
     * <p>
     * Пытаемся создать почтовый заказ.
     * <p>
     * Удается найти город с комбинацией кладр + индекс
     * <p>
     * В результате в cityId должно проставится значение КЛАДР от полученного города.
     */
    @Test
    void createPostOrderByIndexAndKladr() throws Exception {
        int scenarioNumber = 1;

        LinkedMultiValueMap<String, String> urlArguments = new LinkedMultiValueMap<>();
        urlArguments.add("payment_type", "2");
        urlArguments.add("kladr", "77000000000");
        urlArguments.add("index", "129323");
        urlArguments.add("weight", "1000");
        urlArguments.add("parcel_size", "[100, 100, 100]");
        urlArguments.add("order_sum", "500");

        FulfillmentInteraction deliveryCityInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("delivery_city"), HttpMethod.GET, urlArguments))
            .setResponsePath("functional/create_post_order/1/delivery_city.json");

        FulfillmentInteraction createOrderInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("order"), HttpMethod.PUT))
            .setExpectedRequestPath("functional/create_post_order/1/marschroute_request.json")
            .setResponsePath("functional/create_post_order/1/marschroute_response.json");

        executeScenario(scenarioNumber, deliveryCityInteraction, createOrderInteraction);
    }

    /**
     * Сценарий #2:
     * <p>
     * Пытаемся создать почтовый заказ.
     * <p>
     * Не удалось найти город с комбинацией индекс + кладр.
     * В результате мы попытались найти город по комбинации индекс + наименование и этом увенчалось успехом.
     * <p>
     * В результате в cityId должно проставится значение КЛАДР от полученного города.
     */
    @Test
    void createPostOrderByIndexAndName() throws Exception {
        int scenarioNumber = 2;

        LinkedMultiValueMap<String, String> firstDeliveryCityArguments = new LinkedMultiValueMap<>();
        firstDeliveryCityArguments.add("payment_type", "2");
        firstDeliveryCityArguments.add("kladr", "77000000000");
        firstDeliveryCityArguments.add("index", "129323");
        firstDeliveryCityArguments.add("weight", "1000");
        firstDeliveryCityArguments.add("parcel_size", "[100, 100, 100]");
        firstDeliveryCityArguments.add("order_sum", "500");

        FulfillmentInteraction firstDeliveryCityRequest = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("delivery_city"), HttpMethod.GET, firstDeliveryCityArguments))
            .setResponsePath("functional/create_post_order/" + scenarioNumber + "/first_delivery_city.json");

        LinkedMultiValueMap<String, String> secondDeliveryCityArguments = new LinkedMultiValueMap<>();
        secondDeliveryCityArguments.add("payment_type", "2");
        secondDeliveryCityArguments.add("index", "129323");
        secondDeliveryCityArguments.add("weight", "1000");
        secondDeliveryCityArguments.add("parcel_size", "[100, 100, 100]");
        secondDeliveryCityArguments.add("order_sum", "500");
        secondDeliveryCityArguments.add("name", "Москва");

        FulfillmentInteraction secondDeliveryCityRequest = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("delivery_city"), HttpMethod.GET, secondDeliveryCityArguments))
            .setResponsePath("functional/create_post_order/" + scenarioNumber + "/second_delivery_city.json");

        FulfillmentInteraction createOrder = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("order"), HttpMethod.PUT))
            .setExpectedRequestPath("functional/create_post_order/" + scenarioNumber + "/marschroute_request.json")
            .setResponsePath("functional/create_post_order/" + scenarioNumber + "/marschroute_response.json");

        executeScenario(scenarioNumber, firstDeliveryCityRequest, secondDeliveryCityRequest, createOrder);
    }


    /**
     * Сценарий #3:
     * <p>
     * Пытаемся создать почтовый заказ.
     * <p>
     * Не удалось найти город с комбинацией индекс + кладр.
     * После этого нам так же не удалось найти опций доставки по комбинации индекс + наименование.
     * <p>
     * В ответ должна вернуться ошибка.
     */
    @Test
    void failedToFindDeliveryOptions() throws Exception {
        int scenarioNumber = 3;

        LinkedMultiValueMap<String, String> firstDeliveryCityArguments = new LinkedMultiValueMap<>();
        firstDeliveryCityArguments.add("payment_type", "2");
        firstDeliveryCityArguments.add("kladr", "77000000000");
        firstDeliveryCityArguments.add("index", "129323");
        firstDeliveryCityArguments.add("weight", "1000");
        firstDeliveryCityArguments.add("parcel_size", "[100, 100, 100]");
        firstDeliveryCityArguments.add("order_sum", "500");

        FulfillmentInteraction firstDeliveryCityRequest = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("delivery_city"), HttpMethod.GET, firstDeliveryCityArguments))
            .setResponsePath("functional/create_post_order/" + scenarioNumber + "/first_delivery_city.json");

        LinkedMultiValueMap<String, String> secondDeliveryCityArguments = new LinkedMultiValueMap<>();
        secondDeliveryCityArguments.add("payment_type", "2");
        secondDeliveryCityArguments.add("index", "129323");
        secondDeliveryCityArguments.add("weight", "1000");
        secondDeliveryCityArguments.add("parcel_size", "[100, 100, 100]");
        secondDeliveryCityArguments.add("order_sum", "500");
        secondDeliveryCityArguments.add("name", "Москва");

        FulfillmentInteraction secondDeliveryCityRequest = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("delivery_city"), HttpMethod.GET, secondDeliveryCityArguments))
            .setResponsePath("functional/create_post_order/" + scenarioNumber + "/second_delivery_city.json");

        executeScenario(scenarioNumber, firstDeliveryCityRequest, secondDeliveryCityRequest);
    }

    /**
     * Сценарий #4:
     * <p>
     * При поиске города доставки по index + kladr было возвращено несколько городов.
     * <p>
     * Должны упать с ошибкой о том, что было найдено несколько городов.
     */
    @Test
    void multipleCitiesFoundDuringIndexKladrSearch() throws Exception {
        int scenarioNumber = 4;

        LinkedMultiValueMap<String, String> firstDeliveryCityArguments = new LinkedMultiValueMap<>();
        firstDeliveryCityArguments.add("payment_type", "2");
        firstDeliveryCityArguments.add("kladr", "77000000000");
        firstDeliveryCityArguments.add("index", "129323");
        firstDeliveryCityArguments.add("weight", "1000");
        firstDeliveryCityArguments.add("parcel_size", "[100, 100, 100]");
        firstDeliveryCityArguments.add("order_sum", "500");

        FulfillmentInteraction deliveryCityRequest = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("delivery_city"), HttpMethod.GET, firstDeliveryCityArguments))
            .setResponsePath("functional/create_post_order/" + scenarioNumber + "/delivery_city.json");

        executeScenario(scenarioNumber, deliveryCityRequest);
    }


    /**
     * Сценарий #5:
     * <p>
     * При поиске города доставки по index + kladr не удалось найти локаций.
     * При поиске города доставки по index + name было найдено несколько городов.
     * <p>
     * Должны упать с ошибкой о том, что было найдено несколько городов.
     */
    @Test
    void multipleCitiesFoundDuringIndexNameSearch() throws Exception {
        int scenarioNumber = 5;

        LinkedMultiValueMap<String, String> firstDeliveryCityArguments = new LinkedMultiValueMap<>();
        firstDeliveryCityArguments.add("payment_type", "2");
        firstDeliveryCityArguments.add("kladr", "77000000000");
        firstDeliveryCityArguments.add("index", "129323");
        firstDeliveryCityArguments.add("weight", "1000");
        firstDeliveryCityArguments.add("parcel_size", "[100, 100, 100]");
        firstDeliveryCityArguments.add("order_sum", "500");

        FulfillmentInteraction firstDeliveryCityRequest = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("delivery_city"), HttpMethod.GET, firstDeliveryCityArguments))
            .setResponsePath("functional/create_post_order/" + scenarioNumber + "/first_delivery_city.json");

        LinkedMultiValueMap<String, String> secondDeliveryCityArguments = new LinkedMultiValueMap<>();
        secondDeliveryCityArguments.add("payment_type", "2");
        secondDeliveryCityArguments.add("index", "129323");
        secondDeliveryCityArguments.add("weight", "1000");
        secondDeliveryCityArguments.add("parcel_size", "[100, 100, 100]");
        secondDeliveryCityArguments.add("order_sum", "500");
        secondDeliveryCityArguments.add("name", "Москва");

        FulfillmentInteraction secondDeliveryCityRequest = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("delivery_city"), HttpMethod.GET, secondDeliveryCityArguments))
            .setResponsePath("functional/create_post_order/" + scenarioNumber + "/second_delivery_city.json");

        executeScenario(scenarioNumber, firstDeliveryCityRequest, secondDeliveryCityRequest);
    }

    private void executeScenario(int scenarioNumber, FulfillmentInteraction... interactions) throws Exception {
        doReturn(Optional.of(new GeoInformation(DEFAULT_FALLBACK_LOCATION_ID, "", "77000000000", "", null)))
            .when(geoInformationProvider).findWithKladr(DEFAULT_FALLBACK_LOCATION_ID);

        FunctionalTestScenarioBuilder.start(CreateOrderResponse.class)
            .sendRequestToWrapQueryGateway("functional/create_post_order/" + scenarioNumber + "/wrap_request.xml")
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo("functional/create_post_order/" + scenarioNumber + "/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();

        verify(geoInformationProvider).findWithKladr(DEFAULT_FALLBACK_LOCATION_ID);
    }
}
