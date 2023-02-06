package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.model.GeoInformation;
import ru.yandex.market.logistic.api.model.fulfillment.response.UpdateOrderResponse;

import java.util.Collections;
import java.util.Optional;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;
import static ru.yandex.market.fulfillment.wrap.marschroute.service.order.request.factory.delivery.self.CreateMarketDeliveryOrderRequestFactory.DEFAULT_FALLBACK_LOCATION_ID;

class UpdateOrderFunctionalTest extends RepositoryTest {

    private static final String KLADR_ID = "123";

    @MockBean
    private GeoInformationProvider geoInformationProvider;

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
                .setFulfillmentUrl(fulfillmentUrl(asList("tracking", "EXT64006271"), HttpMethod.GET))
                .setResponsePath("functional/update_order/1/tracking.json");

        FunctionalTestScenarioBuilder.start(UpdateOrderResponse.class)
                .sendRequestToWrapQueryGateway("functional/update_order/1/request.xml")
                .thenMockFulfillmentRequest(trackingInteraction)
                .andExpectWrapAnswerToBeEqualTo("functional/update_order/1/response.xml")
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }

    /**
     * Сценарий #2:
     * <p>
     * Обновляем заказ, который был сохранен в БД с DeliveryId, отличным от доставки МарДо,
     * который отныне будет доставляться МарДо.
     * <p>
     * В результате прослойка должна сформировать запрос на обновление данных заказа с корректными данными,
     * а так же перезаписать значение deliveryId в БД на соответствующее доставке МарДо.
     */
    @Test
    @DatabaseSetup(value = "/functional/update_order/2/setup_db.xml")
    @ExpectedDatabase(value = "/functional/update_order/2/expected_db.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateOrderToBeMarketDelivery() throws Exception {
        doReturn(Optional.of(new GeoInformation(DEFAULT_FALLBACK_LOCATION_ID, "", KLADR_ID, "", null)))
                .when(geoInformationProvider).findWithKladr(DEFAULT_FALLBACK_LOCATION_ID);

        FulfillmentInteraction trackingInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("tracking", "EXT64006271"), HttpMethod.GET))
                .setResponsePath("functional/update_order/2/tracking.json");

        LinkedMultiValueMap<String, String> urlArguments = new LinkedMultiValueMap<>();
        urlArguments.add("kladr", KLADR_ID);

        FulfillmentInteraction deliveryCityInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("delivery_city"), HttpMethod.GET, urlArguments))
                .setResponsePath("functional/update_order/2/delivery_city.json");

        FulfillmentInteraction updateInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT64006271"), HttpMethod.POST))
            .setExpectedRequestPath("functional/update_order/2/update_request.json")
            .setResponsePath("functional/update_order/2/update_response.json");

        FulfillmentInteraction getYandexIdInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT64006271"), HttpMethod.GET))
            .setResponsePath("functional/update_order/get_yandex_id_response.json");

        FunctionalTestScenarioBuilder.start(UpdateOrderResponse.class)
                .sendRequestToWrapQueryGateway("functional/update_order/2/request.xml")
                .thenMockFulfillmentRequest(trackingInteraction)
                .thenMockFulfillmentRequest(getYandexIdInteraction)
                .thenMockFulfillmentRequest(getYandexIdInteraction)
                .thenMockFulfillmentRequest(deliveryCityInteraction)
                .thenMockFulfillmentRequest(updateInteraction)
                .andExpectWrapAnswerToBeEqualTo("functional/update_order/2/response.xml")
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();

        verify(geoInformationProvider).findWithKladr(DEFAULT_FALLBACK_LOCATION_ID);
    }

    /**
     * Сценарий #3:
     * <p>
     * Попытка обновить заказ, который не существует в Маршруте.
     * <p>
     * На этапе трекинга заказа из Маршрута должна вернуться ошибка,
     * которая затем должна быть возвращена пользователю прослойки.
     */
    @Test
    void updateNonExistingOrder() throws Exception {
        FulfillmentInteraction trackingInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("tracking", "EXT64006271"), HttpMethod.GET))
                .setResponsePath("functional/update_order/3/tracking.json");

        FunctionalTestScenarioBuilder.start(UpdateOrderResponse.class)
                .sendRequestToWrapQueryGateway("functional/update_order/3/request.xml")
                .thenMockFulfillmentRequest(trackingInteraction)
                .andExpectWrapAnswerToBeEqualTo("functional/update_order/3/response.xml")
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }

    /**
     * Сценарий #4:
     * <p>
     * Обновляем заказ, который был сохранен в БД с DeliveryId, отличным от доставкой почтой через Маршрут,
     * но после обновления будет доставляться именно этим способом.
     * <p>
     * В результате прослойка должна сформировать запрос на обновление данных заказа с корректными данными,
     * а так же перезаписать значение deliveryId в БД на соответствующее доставке МарДо.
     */
    @Test
    @DatabaseSetup(value = "/functional/update_order/4/setup_db.xml")
    @ExpectedDatabase(value = "/functional/update_order/4/expected_db.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateOrderToBeMarschroutePost() throws Exception {
        doReturn(Optional.of(new GeoInformation(DEFAULT_FALLBACK_LOCATION_ID, "", "77000000000", "", null)))
                .when(geoInformationProvider).findWithKladr(DEFAULT_FALLBACK_LOCATION_ID);

        FulfillmentInteraction trackingInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("tracking", "EXT64006271"), HttpMethod.GET))
                .setResponsePath("functional/update_order/4/tracking.json");

        LinkedMultiValueMap<String, String> urlArguments = new LinkedMultiValueMap<>();
        urlArguments.add("payment_type", "2");
        urlArguments.add("kladr","77000000000");
        urlArguments.add("index", "129323");
        urlArguments.add("weight", "1000");
        urlArguments.add("parcel_size", "[100, 100, 100]");
        urlArguments.add("order_sum", "500");

        FulfillmentInteraction deliveryCityInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("delivery_city"), HttpMethod.GET, urlArguments))
                .setResponsePath("functional/update_order/4/delivery_city.json");

        FulfillmentInteraction updateInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT64006271"), HttpMethod.POST))
                .setExpectedRequestPath("functional/update_order/4/update_request.json")
                .setResponsePath("functional/update_order/4/update_response.json");

        FulfillmentInteraction getYandexIdInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT64006271"), HttpMethod.GET))
            .setResponsePath("functional/update_order/get_yandex_id_response.json");

        FunctionalTestScenarioBuilder.start(UpdateOrderResponse.class)
                .sendRequestToWrapQueryGateway("functional/update_order/4/request.xml")
                .thenMockFulfillmentRequest(trackingInteraction)
                .thenMockFulfillmentRequest(getYandexIdInteraction)
                .thenMockFulfillmentRequest(getYandexIdInteraction)
                .thenMockFulfillmentRequest(deliveryCityInteraction)
                .thenMockFulfillmentRequest(updateInteraction)
                .andExpectWrapAnswerToBeEqualTo("functional/update_order/4/response.xml")
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();

        verify(geoInformationProvider).findWithKladr(DEFAULT_FALLBACK_LOCATION_ID);
    }

    /**
     * Сценарий #5:
     * <p>
     * Обновление заказа, который был в статусе, запрещающем исполнять запрос на обновление (уже был исполнен).
     * <p>
     * В результате прослойка должна сформировать запрос на обновление данных заказа с корректными данными,
     * а так же перезаписать значение deliveryId в БД на соответствующее доставке МарДо.
     */
    @Test
    @DatabaseSetup(value = "/functional/update_order/5/setup_db.xml")
    @ExpectedDatabase(value = "/functional/update_order/5/expected_db.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateOrderThatWasInIsBeingPackaged() throws Exception {
        doReturn(Optional.of(new GeoInformation(DEFAULT_FALLBACK_LOCATION_ID, "", "77000000000", "", null)))
            .when(geoInformationProvider).findWithKladr(DEFAULT_FALLBACK_LOCATION_ID);

        FulfillmentInteraction trackingInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("tracking", "EXT64006271"), HttpMethod.GET))
            .setResponsePath("functional/update_order/5/tracking.json");

        LinkedMultiValueMap<String, String> urlArguments = new LinkedMultiValueMap<>();
        urlArguments.add("payment_type", "2");
        urlArguments.add("kladr","77000000000");
        urlArguments.add("index", "129323");
        urlArguments.add("weight", "1000");
        urlArguments.add("parcel_size", "[100, 100, 100]");
        urlArguments.add("order_sum", "500");

        FulfillmentInteraction deliveryCityInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("delivery_city"), HttpMethod.GET, urlArguments))
            .setResponsePath("functional/update_order/5/delivery_city.json");

        FulfillmentInteraction updateInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT64006271"), HttpMethod.POST))
            .setExpectedRequestPath("functional/update_order/5/update_request.json")
            .setResponsePath("functional/update_order/5/update_response.json");

        FulfillmentInteraction getYandexIdInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(asList("order", "EXT64006271"), HttpMethod.GET))
            .setResponsePath("functional/update_order/get_yandex_id_response.json");

        FunctionalTestScenarioBuilder.start(UpdateOrderResponse.class)
            .sendRequestToWrapQueryGateway("functional/update_order/5/request.xml")
            .thenMockFulfillmentRequest(trackingInteraction)
            .thenMockFulfillmentRequest(getYandexIdInteraction)
            .thenMockFulfillmentRequest(getYandexIdInteraction)
            .thenMockFulfillmentRequest(deliveryCityInteraction)
            .thenMockFulfillmentRequest(updateInteraction)
            .andExpectWrapAnswerToBeEqualTo("functional/update_order/5/response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
