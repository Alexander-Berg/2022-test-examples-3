package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.fulfillment.response.CreateTransferResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

@DatabaseSetup(
    connection = "wrapConnection",
    value = "classpath:fixtures/functional/create_transfer/state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
class CreateTransferTest extends AbstractFunctionalTest {

    /**
     * Сценарий #1:
     * <p>Создаем валидное перемещение c двумя товарами.</p>
     * </p>
     * В ответ получаем идентификатор идентификаторо перемещения
     */
    @Test
    void successfulCreateTransfer() throws Exception {
        String wrapRequest = "fixtures/functional/create_transfer/1/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/create_transfer/1/wrap_response.xml";


        FunctionalTestScenarioBuilder.start(CreateTransferResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(
                getInforCreateTransfersInteraction(
                    "fixtures/functional/create_transfer/1/create_transfer_request.json",
                    "fixtures/functional/create_transfer/1/create_transfer_response.json",
                    "inboundPartnerId",
                    "surplus",
                    "transferYandexId")
            )
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #2:
     * <p>Создаем перемещение c невалидным стоком (stockTo) отличный от FIT или SURPLUS.</p>
     * </p>
     * Ожидаем ошибку.
     */
    @Test
    void failCreateTransferWithInvalidStockType() throws Exception {
        String wrapRequest = "fixtures/functional/create_transfer/2/wrap_request.xml";

        FunctionalTestScenarioBuilder.start(CreateTransferResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.UNKNOWN_ERROR, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #3:
     * <p>Создаем перемещение без товаров - в ответ ожидаем получить ошибку сериализации.</p>
     * </p>
     * Взаимодействия с Infor SCE произойти не должно.
     */
    @Test
    void failCreateTransferWithOutAnyGoods() throws Exception {
        String wrapRequest = "fixtures/functional/create_transfer/3/wrap_request.xml";

        FunctionalTestScenarioBuilder.start(CreateTransferResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #4:
     * <p>Создаем перемещение c товарами у которых дублируется UnitId</p>
     * </p>
     * Ожидаем ошибку.
     */
    @Test
    void failCreateTransferWithDuplicateUnitId() throws Exception {
        String wrapRequest = "fixtures/functional/create_transfer/4/wrap_request.xml";

        FunctionalTestScenarioBuilder.start(CreateTransferResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #5:
     * <p>Создаем валидное перемещение на блокировку утилизации c двумя товарами.</p>
     * </p>
     * В ответ получаем идентификатор идентификаторо перемещения
     */
    @Test
    void successfulCreateUtilizationTransfer() throws Exception {
        String wrapRequest = "fixtures/functional/create_transfer/5/wrap_request.xml";
        String expectedWrapResponse = "fixtures/functional/create_transfer/5/wrap_response.xml";


        FunctionalTestScenarioBuilder.start(CreateTransferResponse.class)
                .sendRequestToWrapQueryGateway(wrapRequest)
                .thenMockFulfillmentRequests(
                        getInforCreateTransfersInteraction(
                                "fixtures/functional/create_transfer/5/create_transfer_request.json",
                                "fixtures/functional/create_transfer/5/create_transfer_response.json",
                                null,
                                "hold",
                                "transferYandexId")
                )
                .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
                .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
                .start();
    }

    /**
     * Сценарий #6:
     * <p>Создаем перемещение на утилизацию c невалидным стоком.</p>
     * </p>
     * Ожидаем ошибку.
     */
    @Test
    void failCreateUtilizationTransferWithInvalidStockType() throws Exception {
        String wrapRequest = "fixtures/functional/create_transfer/6/wrap_request.xml";

        FunctionalTestScenarioBuilder.start(CreateTransferResponse.class)
                .sendRequestToWrapQueryGateway(wrapRequest)
                .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.UNKNOWN_ERROR, 1))
                .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
                .start();
    }

    /**
     * Сценарий #7:
     * <p>Создаем перемещение на утилизацию c указанным идентификатором поставки.</p>
     * </p>
     * Ожидаем ошибку.
     */
    @Test
    void failCreateUtilizationTransferWithInboundIdSpecified() throws Exception {
        String wrapRequest = "fixtures/functional/create_transfer/7/wrap_request.xml";

        FunctionalTestScenarioBuilder.start(CreateTransferResponse.class)
                .sendRequestToWrapQueryGateway(wrapRequest)
                .andExpectWrapAnswerToContainErrors(ImmutableMap.of(ErrorCode.BAD_REQUEST, 1))
                .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
                .start();
    }

    private FulfillmentInteraction getInforCreateTransfersInteraction(String expectedRequestPath,
                                                                      String responsePath,
                                                                      String receiptKey,
                                                                      String type,
                                                                      String externalTransferKey) {
        return getInforCreateTransfersInteraction(
            expectedRequestPath, responsePath, receiptKey, type, externalTransferKey, HttpStatus.OK
        );
    }

    private FulfillmentInteraction getInforCreateTransfersInteraction(String expectedRequestPath,
                                                                      String responsePath,
                                                                      String receiptKey,
                                                                      String type,
                                                                      String externalTransferKey,
                                                                      HttpStatus responseStatus) {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (receiptKey != null) {
            params.add("receiptKey", receiptKey);
        }
        if (type != null) {
            params.add("type", type);
        }
        if (externalTransferKey != null) {
            params.add("externTransferKey", externalTransferKey);
        }

        return inforInteraction(fulfillmentUrl(Arrays.asList(
            clientProperties.getWarehouseKey(), "transfer", "transferCreate"),
            HttpMethod.POST,
            params))
            .setExpectedRequestPath(expectedRequestPath)
            .setResponsePath(responsePath)
            .setResponseStatus(responseStatus);
    }
}
