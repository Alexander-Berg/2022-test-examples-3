package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.CancelInboundResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class CancelInboundTest extends AbstractFunctionalTest {

    /**
     * Сценарий #1: Положительный сценарий с успешной отменой поставки.
     * <p>Проверяем, что будет выполнен запрос к инфору с отменой.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_inbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelInboundPositive() throws Exception {
        final String receiptId = "0000000013";
        assertScenario(
            "fixtures/functional/cancel_inbound/1/request.xml",
            "fixtures/functional/cancel_inbound/1/response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "receipts", receiptId), HttpMethod.DELETE))
                .setResponsePath("fixtures/functional/cancel_inbound/common/empty_response.json")
                .setResponseStatus(HttpStatus.OK)
        );
    }

    /**
     * Сценарий #2: Отмена поставки со статусом, при котором операцию уже нельзя произвести
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_inbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelInboundWithNonCancelableStatus() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_inbound/2/request.xml",
            "fixtures/functional/cancel_inbound/2/response.xml"
        );
    }

    /**
     * Сценарий #3: Попытка отменить поставку, которой нет В БД
     * <p>В БД нет поставки, которую пытаемся отменить</p>
     * <p>Проверяем, что будет возвращен статус об успешной отмене</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_inbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelInboundWithInvalidReceiptId() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_inbound/3/request.xml",
            "fixtures/functional/cancel_inbound/3/response.xml"
        );
    }

    /**
     * Сценарий #4: Попытка выполнить отмену для поставки, которая была отменена ранее
     * <p>В БД есть поставка, которая уже отменена с нашей стороны</p>
     * <p>Проверяем, что будет возвращен статус об успешной отмене</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_inbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelInboundWithAlreadyCancelledStatus() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_inbound/4/request.xml",
            "fixtures/functional/cancel_inbound/4/response.xml"
        );
    }

    /**
     * Сценарий #5: Попытка выполнить отмену для поставки (конкуррентный случай).
     * <p>В БД есть поставка, которая на момент проверки иммет еще подходящий для отмены статус,
     * но не валидный в момент обращения к инфору</p>
     * <p>Проверяем, что будет возвращена ошибка от инфора</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_inbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void concurrentCancellation() throws Exception {
        final String receiptId = "0000000013";

        assertScenario(
            "fixtures/functional/cancel_inbound/5/request.xml",
            "fixtures/functional/cancel_inbound/5/response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "receipts", receiptId), HttpMethod.DELETE))
                .setResponsePath("fixtures/functional/cancel_inbound/common/empty_response.json")
                .setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }

    /**
     * Сценарий #6: Попытка отменить поставку, которая прерывается исключением от клиента.
     * <p>Проверяем, что текст ошибки корректно пробросился до ответа обертки.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_inbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void clientRespondsWithException() throws Exception {
        final String receiptId = "0000000013";

        assertScenario(
            "fixtures/functional/cancel_inbound/6/request.xml",
            "fixtures/functional/cancel_inbound/6/response.xml",
            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "receipts", receiptId), HttpMethod.DELETE))
                .setResponsePath("fixtures/functional/common/failed_client_response.json")
                .setResponseStatus(HttpStatus.NOT_FOUND)
        );
    }

    /**
     * Сценарий #7: Попытка отменить поставку, некорректным запросом (отсутствует partnerId).
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_inbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelInboundWithoutPartnerId() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_inbound/7/request.xml",
            "fixtures/functional/cancel_inbound/7/response.xml"
        );
    }

    private void assertScenario(String wrapRequest,
                                String wrapResponse,
                                FulfillmentInteraction interaction) throws Exception {

        FunctionalTestScenarioBuilder.start(CancelInboundResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interaction)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    private void assertScenarioWithoutInteraction(String wrapRequest, String wrapResponse) throws Exception {
        FunctionalTestScenarioBuilder.start(CancelInboundResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }
}
