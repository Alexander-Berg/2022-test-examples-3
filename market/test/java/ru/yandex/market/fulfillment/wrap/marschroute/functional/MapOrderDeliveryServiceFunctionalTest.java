package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import java.util.Collections;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.fulfillment.wrap.marschroute.service.MarschrouteDeliveryCityService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.model.GeoInformation;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateOrderResponse;

import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

public class MapOrderDeliveryServiceFunctionalTest extends RepositoryTest {
    static final String KLADR = "78000000001";
    @MockBean
    private GeoInformationProvider geoInformationProvider;

    @MockBean
    private MarschrouteDeliveryCityService deliveryService;

    /**
     * Для заказа со службой доставки id которой нет в маппинге
     * ничего не должно измениться
     */
    @Test
    @DatabaseSetup("classpath:functional/map_delivery_service/ds_mapping_setup.xml")
    @ExpectedDatabase(value = "classpath:functional/map_delivery_service/1/ds_mapping_expected.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void deliveryIdShouldNotChange() throws Exception {

        Long locationId = 2L;
        GeoInformation geoInformation = new GeoInformation(
                locationId,
                null,
                KLADR,
                null,
                null
        );

        given(geoInformationProvider.findWithKladr(locationId)).willReturn(Optional.of(geoInformation));
        given(deliveryService.isCityIdUnknown(KLADR)).willReturn(false);

        FulfillmentInteraction createOrderInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("order"), HttpMethod.PUT))
                .setResponsePath("functional/map_delivery_service/marschroute_response.json");

        FunctionalTestScenarioBuilder.start(CreateOrderResponse.class)
                .sendRequestToWrapQueryGateway("functional/map_delivery_service/1/wrap_request.xml")
                .thenMockFulfillmentRequest(createOrderInteraction)
                .andExpectWrapAnswerToBeEqualTo("functional/map_delivery_service/wrap_response.xml")
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }

    /**
     * При выполнении маппинга службы доставки должна заполнятся таблица delivery_mapping_history
     *
     */
    @Test
    @DatabaseSetup("classpath:functional/map_delivery_service/ds_mapping_setup.xml")
    @ExpectedDatabase(value = "classpath:functional/map_delivery_service/2/ds_mapping_expected.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void deliveryIdShouldBeMapped() throws Exception {

        Long locationId = 2L;
        GeoInformation geoInformation = new GeoInformation(
                locationId,
                null,
                KLADR,
                null,
                null
        );

        given(geoInformationProvider.findWithKladr(locationId)).willReturn(Optional.of(geoInformation));
        given(deliveryService.isCityIdUnknown(KLADR)).willReturn(false);

        FulfillmentInteraction createOrderInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Collections.singletonList("order"), HttpMethod.PUT))
                .setResponsePath("functional/map_delivery_service/marschroute_response.json");

        FunctionalTestScenarioBuilder.start(CreateOrderResponse.class)
                .sendRequestToWrapQueryGateway("functional/map_delivery_service/2/wrap_request.xml")
                .thenMockFulfillmentRequest(createOrderInteraction)
                .andExpectWrapAnswerToBeEqualTo("functional/map_delivery_service/wrap_response.xml")
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();
    }
}
