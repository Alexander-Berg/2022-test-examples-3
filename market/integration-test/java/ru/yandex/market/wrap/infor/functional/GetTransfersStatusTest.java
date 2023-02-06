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
import ru.yandex.market.logistic.api.model.fulfillment.response.GetTransfersStatusResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

/**
 * Тесты проверки логики получения статусов перемещений.
 *
 * @author moisandrew 24.05.19.
 */
class GetTransfersStatusTest extends AbstractFunctionalTest {

    private static final Boolean withDetails = false;

    /**
     * Сценарий #1:
     * <p>Получаем статусы перемещений по списку partnerId (трансферов)</p>
     * <p>
     */
    @Test
    void successfulGetTransfersStatusWithMultiTransfers() throws Exception {
        String wrapRequest = "fixtures/functional/get_transfers_status/1/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_transfers_status/1/wrap_response.xml";

        FunctionalTestScenarioBuilder.start(GetTransfersStatusResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(
                getInforGetTransfersStatusInteraction(
                    "fixtures/functional/get_transfers_status/1/get_transfers_status_request.json",
                    "fixtures/functional/get_transfers_status/1/get_transfers_status_response.json",
                    withDetails)
            )
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #2:
     * <p>Получаем статусы перемещений по списку, содержащему только один partnerId (одно перемещение)</p>
     * <p>
     */
    @Test
    void failGetTransfersStatusWithSingleTransfer() throws Exception {
        String wrapRequest = "fixtures/functional/get_transfers_status/2/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_transfers_status/2/wrap_response.xml";

        FunctionalTestScenarioBuilder.start(GetTransfersStatusResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(
                getInforGetTransfersStatusInteraction(
                    "fixtures/functional/get_transfers_status/2/get_transfers_status_request.json",
                    "fixtures/functional/get_transfers_status/2/get_transfers_status_response.json",
                    withDetails)
            )
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #3:
     * <p>Получаем статусы перемещений по списку c отсутствием partnerId </p>
     * <p>
     * Ожидается ошибка сериализации.
     */
    @Test
    void failGetTransfersStatusWithInvalidPartnerId() throws Exception {
        String wrapRequest = "fixtures/functional/get_transfers_status/3/wrap_request.xml";

        FunctionalTestScenarioBuilder.start(GetTransfersStatusResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    private FulfillmentInteraction getInforGetTransfersStatusInteraction(String expectedRequestPath,
                                                                         String responsePath,
                                                                         Boolean withDetails) {
        return getInforGetTransfersStatusInteraction(expectedRequestPath, responsePath, withDetails, HttpStatus.OK);
    }

    private FulfillmentInteraction getInforGetTransfersStatusInteraction(String expectedRequestPath,
                                                                         String responsePath,
                                                                         Boolean withDetails,
                                                                         HttpStatus responseStatus) {

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
            .setResponseStatus(responseStatus);
    }
}
