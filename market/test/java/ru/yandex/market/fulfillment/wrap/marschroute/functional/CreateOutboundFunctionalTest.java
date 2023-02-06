package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.FunctionalTestScenarios;
import ru.yandex.market.fulfillment.wrap.marschroute.service.outbound.creation.MarschrouteCreateOutboundService;
import ru.yandex.market.logistic.api.model.fulfillment.Outbound;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateOutboundResponse;

import java.util.Collections;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class CreateOutboundFunctionalTest extends RepositoryTest {

    @SpyBean
    private MarschrouteCreateOutboundService createOutboundService;

    @Test
    @DatabaseSetup(value = "classpath:repository/empty_dataset.xml", type = DatabaseOperation.DELETE_ALL)
    @ExpectedDatabase(value = "classpath:repository/create_fit_outbound_result.xml", assertionMode = NON_STRICT_UNORDERED)
    void createFitStockOutbound() throws Exception {
        FunctionalTestScenarios.marschrouteMarketOrderCreation(
            CreateOutboundResponse.class,
            "functional/create_outbound/fit/wrap_request.xml",
            "functional/create_outbound/fit/marschroute_request.json",
            "functional/create_outbound/fit/marschroute_response.json",
            "functional/create_outbound/fit/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/empty_dataset.xml", type = DatabaseOperation.DELETE_ALL)
    @ExpectedDatabase(value = "classpath:repository/create_fit_outbound_result.xml", assertionMode = NON_STRICT_UNORDERED)
    void createFitStockOutboundWithCombinedAddress() throws Exception {
        FunctionalTestScenarios.marschrouteMarketOrderCreation(
            CreateOutboundResponse.class,
            "functional/create_outbound/fit/combined_address_wrap_request.xml",
            "functional/create_outbound/fit/combined_address_marschroute_request.json",
            "functional/create_outbound/fit/marschroute_response.json",
            "functional/create_outbound/fit/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/empty_dataset.xml", type = DatabaseOperation.DELETE_ALL)
    @ExpectedDatabase(value = "classpath:repository/create_outbound_result.xml", assertionMode = NON_STRICT_UNORDERED)
    void createQuarantineStockOutbound() throws Exception {
        String wrapRequest = "functional/create_outbound/not_fit/wrap_request.xml";
        String marschrouteRequest = "functional/create_outbound/not_fit/marschroute_request.json";
        String marschrouteResponse = "functional/create_outbound/not_fit/marschroute_response.json";
        String wrapResponse = "functional/create_outbound/not_fit/wrap_response.xml";

        FulfillmentInteraction interaction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Collections.singletonList("shipment"), HttpMethod.PUT))
            .setExpectedRequestPath(marschrouteRequest)
            .setResponsePath(marschrouteResponse);

        FunctionalTestScenarioBuilder.start(CreateOutboundResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequest(interaction)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    @Test
    void createFitStockOutboundWithPartnerIdSavingFailed() throws Exception {
        doReturn(new ResourceId("503", "EXT66172145"))
            .when(createOutboundService).createOutbound(any(Outbound.class));

        doThrow(RuntimeException.class).when(createOutboundService)
            .persistOutboundsId(anyCollectionOf(ResourceId.class));

        FunctionalTestScenarioBuilder.start(CreateOutboundResponse.class)
            .sendRequestToWrapQueryGateway("functional/create_outbound/fit/wrap_request.xml")
            .andExpectWrapAnswerToBeEqualTo("functional/create_outbound/fit/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();

        verify(createOutboundService).createOutbound(any(Outbound.class));
        verify(createOutboundService).persistOutboundsId(anyCollectionOf(ResourceId.class));
    }

    /**
     * Проверяет сценарий, когда изъятие уже было создано ранее и мы не должны создавать еще одно
     */
    @Test
    @DatabaseSetup(value = "classpath:repository/fit_outbound_exist.xml")
    @ExpectedDatabase(value = "classpath:repository/fit_outbound_exist.xml", assertionMode = NON_STRICT_UNORDERED)
    void skipCreatingOutbound() throws Exception {
        FunctionalTestScenarioBuilder
            .start(CreateOutboundResponse.class)
            .sendRequestToWrapQueryGateway("functional/create_outbound/fit/wrap_request.xml")
            .andExpectWrapAnswerToBeEqualTo("functional/create_outbound/fit/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
