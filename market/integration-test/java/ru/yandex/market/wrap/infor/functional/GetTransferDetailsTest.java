package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetTransferDetailsResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

@DatabaseSetup(
    connection = "wrapConnection",
    value = "classpath:fixtures/functional/get_transfer_details/state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
class GetTransferDetailsTest extends AbstractFunctionalTest {

    private static final Boolean withDetails = true;

    /**
     * Сценарий #1:
     * <p>Получаем детали перемещения оп partnerId.</p>
     * <p>
     * Ожидаем получить валидный список деталей перемещения
     */
    @Test
    void successfulGetTransferDetails() throws Exception {
        String wrapRequest = "fixtures/functional/get_transfer_details/1/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_transfer_details/1/wrap_response.xml";

        FunctionalTestScenarioBuilder.start(GetTransferDetailsResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(
                getInforGetTransferDetailsInteraction(
                    "fixtures/functional/get_transfer_details/1/get_transfer_details_request.json",
                    "fixtures/functional/get_transfer_details/1/get_transfer_details_response.json",
                    withDetails)
            )
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #2:
     * <p>Получаем детали перемещения оп partnerId, когда в инфоре несколько деталей для одной sku.</p>
     * <p>
     * Ожидаем получить валидный список деталей перемещения
     */
    @Test
    void successfulGetTransferDetailsWithMultipleDetailsForOneSku() throws Exception {
        String wrapRequest = "fixtures/functional/get_transfer_details/2/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/get_transfer_details/2/wrap_response.xml";

        FunctionalTestScenarioBuilder.start(GetTransferDetailsResponse.class)
                .sendRequestToWrapQueryGateway(wrapRequest)
                .thenMockFulfillmentRequests(
                        getInforGetTransferDetailsInteraction(
                                "fixtures/functional/get_transfer_details/2/get_transfer_details_request.json",
                                "fixtures/functional/get_transfer_details/2/get_transfer_details_response.json",
                                withDetails)
                )
                .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
                .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
                .start();
    }

    private FulfillmentInteraction getInforGetTransferDetailsInteraction(String expectedRequestPath,
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
