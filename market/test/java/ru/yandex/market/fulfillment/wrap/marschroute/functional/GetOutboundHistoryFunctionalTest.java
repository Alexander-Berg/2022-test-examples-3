package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOutboundHistoryResponse;

import java.util.Arrays;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class GetOutboundHistoryFunctionalTest extends RepositoryTest {

    /**
     * Сценарий #1:
     * <p>
     * Получаем историю статусов по Outbound'у с FIT стока, чей заказ уже был успешно выполнен.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_outbound_history/1/setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbound_history/1/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getFitOutboundHistory() throws Exception {
        executeScenario(1);
    }

    /**
     * Сценарий #2:
     * <p>
     * Получаем историю статусов по Outbound'у с FIT стока,
     * чей заказ был повторно пересобран после первой неудачной попытки.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_outbound_history/2/setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbound_history/2/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getFitReassembledOutboundHistory() throws Exception {
        executeScenario(2);
    }


    /**
     * Сценарий #3:
     * <p>
     * Получаем историю статусов по Outbound, информация о котором отсутствует в БД.
     * <p>
     * В ответ должны получить ошибку о том, что Outbound не найден.
     */
    @Test
    void getMissingInDatabaseOutboundHistory() throws Exception {
        FunctionalTestScenarioBuilder.start(GetOutboundHistoryResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbound_history/3/wrap_request.xml")
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbound_history/3/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Сценарий #4:
     * <p>
     * Получаем историю статусов по Outbound, информация о котором присутствует, но отсутствует в Маршруте.
     * <p>
     * В ответ должны получить Маршрутовский message внутри нашей ошибки.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_outbound_history/4/setup.xml")
    void getMissingInMarschrouteOutboundHistory() throws Exception {
        executeScenario(4);
    }

    private void executeScenario(int scenarioNumber) throws Exception {
        FulfillmentInteraction trackingInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("tracking", "partner_id"), HttpMethod.GET))
            .setResponsePath("functional/get_outbound_history/" + scenarioNumber + "/tracking_response.json");

        FunctionalTestScenarioBuilder.start(GetOutboundHistoryResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbound_history/" + scenarioNumber + "/wrap_request.xml")
            .thenMockFulfillmentRequest(trackingInteraction)
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbound_history/" + scenarioNumber + "/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Сценарий #5:
     * <p>
     * Получаем историю статусов по !FIT Outbound'у, статусы которого пока еще не были сохранены в БД.
     * <p>
     * В ответ должны получить историю с актуальным статусом от самого Маршрута.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_outbound_history/5/setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbound_history/5/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getNonFitOutboundHistoryOnEmptyDatabase() throws Exception {
        FulfillmentInteraction waybillInfoInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner_id"), HttpMethod.GET))
            .setResponsePath("functional/get_outbound_history/5/marschroute_waybill_response.json");

        FunctionalTestScenarioBuilder.start(GetOutboundHistoryResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbound_history/5/wrap_request.xml")
            .thenMockFulfillmentRequest(waybillInfoInteraction)
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbound_history/5/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Сценарий #6:
     * <p>
     * Получаем историю статусов по !FIT Outbound'у, один из статусов которого уже был сохранен в БД,
     * а новый будет получен в момент запроса.
     * <p>
     * В ответ должна вернуться история с обоими статусами.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_outbound_history/6/setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbound_history/6/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getNonFitOutboundHistoryOnNonEmptyDatabase() throws Exception {
        FulfillmentInteraction waybillInfoInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner_id"), HttpMethod.GET))
            .setResponsePath("functional/get_outbound_history/6/marschroute_waybill_response.json");

        FunctionalTestScenarioBuilder.start(GetOutboundHistoryResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbound_history/6/wrap_request.xml")
            .thenMockFulfillmentRequest(waybillInfoInteraction)
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbound_history/6/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }


    /**
     * Сценарий #7:
     * <p>
     * Получаем историю статусов по !FIT Outbound'у, статусы которого пока еще не были сохранены в БД.
     * В истории от маршрута возвращается статус 221.
     * <p>
     * В ответ должны получить историю с актуальным статусом от самого Маршрута.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_outbound_history/7/setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbound_history/7/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getNonFitOutboundHistoryOnEmptyDatabaseWith221Status() throws Exception {
        FulfillmentInteraction waybillInfoInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner_id"), HttpMethod.GET))
            .setResponsePath("functional/get_outbound_history/7/marschroute_waybill_response.json");

        FunctionalTestScenarioBuilder.start(GetOutboundHistoryResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbound_history/7/wrap_request.xml")
            .thenMockFulfillmentRequest(waybillInfoInteraction)
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbound_history/7/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Сценарий #8:
     * <p>
     * Получаем историю статусов по !FIT Outbound'у, статусы которого пока еще не были сохранены в БД.
     * В истории от маршрута возвращается статус 222.
     * <p>
     * В ответ должны получить историю с актуальным статусом от самого Маршрута.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_outbound_history/8/setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbound_history/8/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getNonFitOutboundHistoryOnEmptyDatabaseWith222Status() throws Exception {
        FulfillmentInteraction waybillInfoInteraction = FulfillmentInteraction.createMarschrouteInteraction()
            .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner_id"), HttpMethod.GET))
            .setResponsePath("functional/get_outbound_history/8/marschroute_waybill_response.json");

        FunctionalTestScenarioBuilder.start(GetOutboundHistoryResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbound_history/8/wrap_request.xml")
            .thenMockFulfillmentRequest(waybillInfoInteraction)
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbound_history/8/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

    /**
     * Проверяет, что при если WaybillInfo содержит поле dispatch_doc_id,
     * то история будет содержать статусы для partner_id из запроса и для dispatch_doc_id.
     * Также маппинг partner_id -> dispatch_doc_id будет сохранен в базу.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_outbound_history/9/setup.xml")
    @ExpectedDatabase(value = "classpath:functional/get_outbound_history/9/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getDefectOutboundHistoryWhenWaybillInfoHasDispatchDocId() throws Exception {
        FulfillmentInteraction[] interactions = new FulfillmentInteraction[]{
            FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "partner_id"), HttpMethod.GET))
                .setResponsePath("functional/get_outbound_history/9/waybill.json"),
            FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(fulfillmentUrl(Arrays.asList("waybill", "dispatch_partner_id"), HttpMethod.GET))
                .setResponsePath("functional/get_outbound_history/9/waybill-d.json")
        };

        FunctionalTestScenarioBuilder.start(GetOutboundHistoryResponse.class)
            .sendRequestToWrapQueryGateway("functional/get_outbound_history/9/wrap_request.xml")
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo("functional/get_outbound_history/9/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }

}
