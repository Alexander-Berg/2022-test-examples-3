package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundsStatusResponse;

import java.util.Arrays;

import static java.util.Collections.singletonList;
import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class GetOutboundsStatusFunctionalTest extends RepositoryTest {

    @Test
    @DatabaseSetup(value = "classpath:functional/get_outbounds_status/setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbounds_status/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getOutboundsStatus() throws Exception {
        FulfillmentInteraction orders = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(
                singletonList("orders"),
                HttpMethod.GET,
                ImmutableMap.of("filter[order_id]", singletonList("partner-1"))
            ))
            .setResponsePath("functional/get_outbounds_status/marschroute_orders_response.json");

        FulfillmentInteraction waybill = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner-2"), HttpMethod.GET))
            .setResponsePath("functional/get_outbounds_status/marschroute_waybill_response.json");

        FunctionalTestScenarioBuilder.start(GetOutboundsStatusResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbounds_status/wrap_request.xml")
            .thenMockFulfillmentRequest(orders)
            .thenMockFulfillmentRequest(waybill)
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbounds_status/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    @Test
    @DatabaseSetup(value = "classpath:functional/get_outbounds_status/setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbounds_status/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getCreated221OutboundStatus() throws Exception {
        FulfillmentInteraction orders = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(
                singletonList("orders"),
                HttpMethod.GET,
                ImmutableMap.of("filter[order_id]", singletonList("partner-1"))
            ))
            .setResponsePath("functional/get_outbounds_status/marschroute_orders_response.json");

        FulfillmentInteraction waybill = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner-2"), HttpMethod.GET))
            .setResponsePath("functional/get_outbounds_status/created_221/marschroute_waybill_response.json");

        FunctionalTestScenarioBuilder.start(GetOutboundsStatusResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbounds_status/wrap_request.xml")
            .thenMockFulfillmentRequest(orders)
            .thenMockFulfillmentRequest(waybill)
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbounds_status/created_221/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    @Test
    @DatabaseSetup(value = "classpath:functional/get_outbounds_status/setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbounds_status/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getCreated222OutboundStatus() throws Exception {
        FulfillmentInteraction orders = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(
                singletonList("orders"),
                HttpMethod.GET,
                ImmutableMap.of("filter[order_id]", singletonList("partner-1"))
            ))
            .setResponsePath("functional/get_outbounds_status/marschroute_orders_response.json");

        FulfillmentInteraction waybill = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner-2"), HttpMethod.GET))
            .setResponsePath("functional/get_outbounds_status/created_222/marschroute_waybill_response.json");

        FunctionalTestScenarioBuilder.start(GetOutboundsStatusResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbounds_status/wrap_request.xml")
            .thenMockFulfillmentRequest(orders)
            .thenMockFulfillmentRequest(waybill)
       .andExpectWrapAnswerToBeEqualTo("functional/get_outbounds_status/created_222/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    @Test
    @DatabaseSetup(value = "classpath:functional/get_outbounds_status/setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbounds_status/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getCancelled220_OutboundStatus() throws Exception {
        FulfillmentInteraction orders = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(
                singletonList("orders"),
                HttpMethod.GET,
                ImmutableMap.of("filter[order_id]", singletonList("partner-1"))
            ))
            .setResponsePath("functional/get_outbounds_status/marschroute_orders_response.json");

        FulfillmentInteraction waybill = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner-2"), HttpMethod.GET))
            .setResponsePath("functional/get_outbounds_status/cancelled_220/marschroute_waybill_response.json");

        FunctionalTestScenarioBuilder.start(GetOutboundsStatusResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbounds_status/wrap_request.xml")
            .thenMockFulfillmentRequest(orders)
            .thenMockFulfillmentRequest(waybill)
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbounds_status/cancelled_220/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Проверяет, что если при трекинге одного из изъятий не было обнаружено
     * - будет успешно возвращена информация о всех найденных.
     */
    @Test
    @DatabaseSetup(value = "classpath:functional/get_outbounds_status/empty_response/state.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbounds_status/empty_response/state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getOutboundsStatusWithMissingInfos() throws Exception {
        FulfillmentInteraction orders = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(
                singletonList("orders"),
                HttpMethod.GET,
                ImmutableMap.of("filter[order_id]", singletonList("partner-1"))
            ))
            .setResponsePath("functional/get_outbounds_status/empty_response/orders.json");

        FulfillmentInteraction waybill = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner-2"), HttpMethod.GET))
            .setResponsePath("functional/get_outbounds_status/empty_response/waybill.json");

        FunctionalTestScenarioBuilder.start(GetOutboundsStatusResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbounds_status/empty_response/wrap_request.xml")
            .thenMockFulfillmentRequest(orders)
            .thenMockFulfillmentRequest(waybill)
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbounds_status/empty_response/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Проверяет, что при необходимости запроса waybill будет проверена база outbound_dispatch_info
     * на наличие маппинга partner_id -> dispatch_partner_id.
     * Если маппинг есть, то для запроса будет использован dispatch_partner_id,
     * если маппинга нет - будет запрос по partner_id, и если в ответе будет dispatch_doc_id -
     * то маппинг будет сохранен и сделан новый запрос по dispatch_doc_id
     */
    @Test
    @DatabaseSetup(value = "classpath:functional/get_outbounds_status/dispatch/db_setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbounds_status/dispatch/db_expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getDefectOutboundStatusWhenWaybillInfoHasDispatchDocId() throws Exception {
        FulfillmentInteraction[] interactions = new FulfillmentInteraction[]{
            FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner-101"), HttpMethod.GET))
                .setResponsePath("functional/get_outbounds_status/dispatch/waybill-101.json"),
            FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner-101-d"), HttpMethod.GET))
                .setResponsePath("functional/get_outbounds_status/dispatch/waybill-101-d.json"),
            FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner-102-d"), HttpMethod.GET))
                .setResponsePath("functional/get_outbounds_status/dispatch/waybill-102-d.json")
        };

        FunctionalTestScenarioBuilder.start(GetOutboundsStatusResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbounds_status/dispatch/wrap_request.xml")
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbounds_status/dispatch/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    @Test
    void outboundIdValidationOnPartialResourceId() throws Exception {
        FunctionalTestScenarioBuilder.start(GetOutboundsStatusResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbounds_status/negative/wrap_request.xml")
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 5))
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
