package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetTransferHistoryResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class GetTransferHistoryTest extends AbstractFunctionalTest {

    private static final Boolean withDetails = true;

    /**
     * Сценарий #1:
     * <p>Получаем историю статусов перемещения по partnerId.</p>
     * <p>
     * Ожидаем получить валидную историю статусов перемещения
     */
    @Test
    void successfulGetTransferHistory() throws Exception {
        String wrapRequest = "fixtures/functional/get_transfer_history/1/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_transfer_history/1/wrap_response.xml";

        FunctionalTestScenarioBuilder.start(GetTransferHistoryResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(
                getInforGetTransferHistoryInteraction(
                    "fixtures/functional/get_transfer_history/1/get_transfer_history_request.json",
                    "fixtures/functional/get_transfer_history/1/get_transfer_history_response.json",
                    withDetails)
            )
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }


    /**
     * Сценарий #2:
     * <p>Пытаемся получить историю статусов перемещения по partnerId, при этом не передаем partnerId</p>
     * <p>
     * Ожидаем получить ошибку сериализации
     */
    @Test
    void failGetTransferHistoryWithInvalidPartnerId() throws Exception {
        String wrapRequest = "fixtures/functional/get_transfer_history/2/wrap_request.xml";

        FunctionalTestScenarioBuilder.start(GetTransferHistoryResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #3:
     * <p>Получаем историю статусов перемещения по partnerId,
     * при этом INFOR возвращает трансфер с пустой историей</p>
     * <p>
     * Ожидаем получить ошибку сериализации
     */
    @Test
    void failGetTransferHistoryWithOutHistory() throws Exception {
        String wrapRequest = "fixtures/functional/get_transfer_history/3/wrap_request.xml";

        FunctionalTestScenarioBuilder.start(GetTransferHistoryResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(
                getInforGetTransferHistoryInteraction(
                    "fixtures/functional/get_transfer_history/3/get_transfer_history_request.json",
                    "fixtures/functional/get_transfer_history/3/get_transfer_history_response.json",
                    withDetails)
            )
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.UNKNOWN_ERROR, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #4:
     * <p>Получаем историю статусов перемещения по partnerId. В истории первым идет -1 статус.</p>
     * <p>
     * Ожидаем получить валидную историю статусов перемещения без -1 статуса.
     */
    @Test
    void successfulGetTransferHistoryWhenErrorStatusIsFirstInHistory() throws Exception {
        String wrapRequest = "fixtures/functional/get_transfer_history/4/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_transfer_history/4/wrap_response.xml";

        FunctionalTestScenarioBuilder.start(GetTransferHistoryResponse.class)
                .sendRequestToWrapQueryGateway(wrapRequest)
                .thenMockFulfillmentRequests(
                        getInforGetTransferHistoryInteraction(
                                "fixtures/functional/get_transfer_history/4/get_transfer_history_request.json",
                                "fixtures/functional/get_transfer_history/4/get_transfer_history_response.json",
                                withDetails)
                )
                .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
                .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
                .start();
    }

    /**
     * Сценарий #5:
     * <p>Получаем историю статусов перемещения по partnerId. В истории есть только -1 статус.</p>
     * <p>
     * Ожидаем получить валидную историю статусов перемещения, то есть только -1 статус.
     */
    @Test
    void successfulGetTransferHistoryWhenCreatedWithError() throws Exception {
        String wrapRequest = "fixtures/functional/get_transfer_history/5/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_transfer_history/5/wrap_response.xml";

        FunctionalTestScenarioBuilder.start(GetTransferHistoryResponse.class)
                .sendRequestToWrapQueryGateway(wrapRequest)
                .thenMockFulfillmentRequests(
                        getInforGetTransferHistoryInteraction(
                                "fixtures/functional/get_transfer_history/5/get_transfer_history_request.json",
                                "fixtures/functional/get_transfer_history/5/get_transfer_history_response.json",
                                withDetails)
                )
                .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
                .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
                .start();
    }

    private FulfillmentInteraction getInforGetTransferHistoryInteraction(String expectedRequestPath,
                                                                         String responsePath,
                                                                         Boolean withDetails) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (withDetails != null) {
            params.add("withDetails", withDetails.toString());
        }

        return inforInteraction(fulfillmentUrl(Arrays.asList(
            clientProperties.getWarehouseKey(), "transfer", "getTransfers"),
            HttpMethod.POST,
            params))
            .setExpectedRequestPath(expectedRequestPath)
            .setResponsePath(responsePath)
            .setResponseStatus(HttpStatus.OK);
    }
}
