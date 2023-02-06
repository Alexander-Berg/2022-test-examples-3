package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.yandex.market.fulfillment.wrap.core.processing.validation.order.CreateOrderRequestValidator;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.FunctionalTestScenarios;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.model.GeoInformation;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateOrderResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateOutboundResponse;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.fulfillment.wrap.marschroute.service.order.request.factory.delivery.self.CreateMarketDeliveryOrderRequestFactory.DEFAULT_FALLBACK_LOCATION_ID;

class CreateOrderFunctionalTest extends RepositoryTest {

    static final String KLADR = "78000000000";
    static final String UNKNOWN_KLADR = "78000000001";
    @MockBean
    private GeoInformationProvider geoInformationProvider;

    @Test
    @ExpectedDatabase(value = "classpath:functional/create_order/order_info_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void createOrderWithKladr() throws Exception {
        Long locationId = 2L;
        GeoInformation geoInformation = new GeoInformation(
            locationId,
            null,
            KLADR,
            null,
            null
        );

        given(geoInformationProvider.findWithKladr(locationId)).willReturn(Optional.of(geoInformation));

        initStraightForwardTestScenario(KLADR);

        verify(geoInformationProvider).findWithKladr(locationId);
    }

    @Test
    void createOrderWithDefaultKladr() throws Exception {
        Long locationId = 2L;
        GeoInformation geoInformation = new GeoInformation(
            locationId,
            null,
            "",
            null,
            null
        );

        GeoInformation fallBackGeoInformation = new GeoInformation(
            DEFAULT_FALLBACK_LOCATION_ID,
            null,
            KLADR,
            null,
            null
        );

        given(geoInformationProvider.findWithKladr(locationId)).willReturn(Optional.of(geoInformation));
        given(geoInformationProvider.findWithKladr(DEFAULT_FALLBACK_LOCATION_ID))
            .willReturn(Optional.of(fallBackGeoInformation));

        initEmptyCityIdTestScenario();

        verify(geoInformationProvider).findWithKladr(locationId);
        verify(geoInformationProvider).findWithKladr(DEFAULT_FALLBACK_LOCATION_ID);
    }


    @Test
    void createOrderCityIdIsUnknown() throws Exception {
        Long locationId = 2L;
        GeoInformation geoInformation = new GeoInformation(
            locationId,
            null,
            UNKNOWN_KLADR,
            null,
            null
        );


        GeoInformation fallBackGeoInformation = new GeoInformation(
            DEFAULT_FALLBACK_LOCATION_ID,
            null,
            KLADR,
            null,
            null
        );

        given(geoInformationProvider.findWithKladr(locationId)).willReturn(Optional.of(geoInformation));
        given(geoInformationProvider.findWithKladr(DEFAULT_FALLBACK_LOCATION_ID))
            .willReturn(Optional.of(fallBackGeoInformation));

        initCityUnknownTestScenario(UNKNOWN_KLADR);

        verify(geoInformationProvider).findWithKladr(locationId);
        verify(geoInformationProvider).findWithKladr(DEFAULT_FALLBACK_LOCATION_ID);
    }

    /**
     * Проверяет, что в случае, когда при запросе на создание заказа отсутствует значение order.orderId.yandexId
     * - это приведет к соответствующей ошибке валидации.
     */
    @Test
    void createOrderWithMissingOrderYandexId() throws Exception {
        FunctionalTestScenarioBuilder.start(CreateOrderResponse.class)
            .sendRequestToWrapQueryGateway("functional/create_order/validation/1.xml")
            .andExpectWrapAnswerToMeetRequirements((response, assertions) -> {
                List<ErrorPair> errorCodes = response.getRequestState().getErrorCodes();

                assertions.assertThat(errorCodes).hasSize(1);
                ErrorPair errorCode = errorCodes.get(0);

                assertions.assertThat(errorCode.getCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                assertions.assertThat(errorCode.getMessage()).isEqualTo(CreateOrderRequestValidator.MISSING_YANDEX_ID_MESSAGE);
            })
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Проверяет, что в случае, если в БД уже присутствует информация о заказе с указанным yandexId -
     * не будет совершена попытка создать новые заказ, а в ответ вернуться данные из соответствующей строки в таблице
     * order_info.
     */
    @Test
    @DatabaseSetup("classpath:functional/create_order/order_info_setup.xml")
    void createExistingOrder() throws Exception {
        FunctionalTestScenarioBuilder.start(CreateOrderResponse.class)
            .sendRequestToWrapQueryGateway("functional/create_order/wrap_request.xml")
            .andExpectWrapAnswerToBeEqualTo("functional/create_order/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    private void initEmptyCityIdTestScenario() throws Exception {
        FunctionalTestScenarios.marschrouteOrderCreationEmptyCityId(
            CreateOutboundResponse.class,
            "functional/create_order/wrap_request.xml",
            "functional/create_order/marschroute_request.json",
            "functional/create_order/marschroute_response.json",
            "functional/create_order/wrap_response.xml"
        )
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    private void initStraightForwardTestScenario(String kladr) throws Exception {
        FunctionalTestScenarios.marschrouteOrderCreationWithCityCheck(
            CreateOutboundResponse.class,
            "functional/create_order/wrap_request.xml",
            "functional/create_order/marschroute_request.json",
            "functional/create_order/marschroute_response.json",
            "functional/create_order/wrap_response.xml", kladr,
            "functional/create_order/marschroute_delivery_city_success_response.json"
        )
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    private void initCityUnknownTestScenario(String kladr) throws Exception {
        FunctionalTestScenarios.marschrouteOrderCreationWithCityCheck(
            CreateOutboundResponse.class,
            "functional/create_order/wrap_request.xml",
            "functional/create_order/marschroute_request.json",
            "functional/create_order/marschroute_response.json",
            "functional/create_order/wrap_response.xml", kladr,
            "functional/create_order/marschroute_delivery_city_fail_response.json"
        )
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
