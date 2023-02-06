package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class CancelOutboundTest extends AbstractFunctionalTest {

    /**
     * Сценарий #1: Отмена изъятия со статусом, при котором операцию еще можно произвести (до 51).
     * <p>Проверяем, что будет выполнен запрос к инфору с отменой.</p>
     */
    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_outbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOutboundWithCancelableStatus() throws Exception {
        assertScenario(
            "fixtures/functional/cancel_outbound/1/wrap_request.xml",
            "fixtures/functional/cancel_outbound/1/wrap_response.xml",
            getOkInteraction("0000000029")
        );
    }

    /**
     * Сценарий #1.1: Отмена изъятия утилизации со статусом, при котором операцию еще можно произвести (до 51).
     * <p>Проверяем, что будет выполнен запрос к инфору с отменой.</p>
     */
    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_outbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOutboundUtilizationWithCancelableStatus() throws Exception {
        assertScenario(
            "fixtures/functional/cancel_outbound/1_01/wrap_request.xml",
            "fixtures/functional/cancel_outbound/1_01/wrap_response.xml",
            getOkInteraction("0000002901")
        );
    }

    /**
     * Сценарий #2: Отмена изъятия со статусом, при котором операцию уже нельзя произвести (>= 51)
     * <p>В БД несколько заказов и история для запрашиваемого заказа с 2-мя статусами: 10->51.</p>
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_outbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOutboundWithNonCancelableStatus() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_outbound/2/wrap_request.xml",
            "fixtures/functional/cancel_outbound/2/wrap_response.xml"
        );
    }

    /**
     * Сценарий #3: Попытка отменить несуществующее изъятие
     * <p>В БД несколько заказов</p>
     * <p>Проверяем, что будет возвращена ошибка при попытке отменить несуществующий заказ</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_outbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOutboundWithInvalidYandexId() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_outbound/3/wrap_request.xml",
            "fixtures/functional/cancel_outbound/3/wrap_response.xml"
        );
    }

    /**
     * Сценарий #4: Попытка выполнить отмену для изъятия, которое было отменено ранее извне
     * <p>В БД есть заказ, который уже отменен с нашей стороны</p>
     * <p>Проверяем, что будет возвращен статус об успешной отмене</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_outbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOutboundWithAlreadyExternallyCancelledStatus() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_outbound/4/wrap_request.xml",
            "fixtures/functional/cancel_outbound/4/wrap_response.xml"
        );
    }

    /**
     * Сценарий #5: Попытка выполнить отмену для изъятия, которое было отменено ранее складом
     * <p>В БД есть заказ, который уже отменен складом</p>
     * <p>Проверяем, что будет возвращен статус об успешной отмене</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_outbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOutboundWithInternallyCancelledStatus() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_outbound/5/wrap_request.xml",
            "fixtures/functional/cancel_outbound/5/wrap_response.xml"
        );
    }

    /**
     * Сценарий #6: Попытка выполнить отмену для изъятия (конкуррентный случай).
     * <p>В БД есть заказ, который на момент проверки иммет еще подходящий для отмены статус,
     * но не валидный в момент обращения к инфору</p>
     * <p>Проверяем, что будет возвращена ошибка от инфора</p>
     */
    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_outbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void concurrentCancellation() throws Exception {
        final String orderId = "0000000029";

        assertScenario(
            "fixtures/functional/cancel_outbound/6/wrap_request.xml",
            "fixtures/functional/cancel_outbound/6/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments", orderId, "cancel"), HttpMethod.POST))
                .setResponsePath("fixtures/functional/cancel_outbound/common/empty_response.json")
                .setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }

    /**
     * Сценарий №7 Попытка отменить изъятие, которая прерывается исключением от клиента.
     * <p>Проверяем, что текст ошибки корректно пробросился до ответа обертки.</p>
     */
    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_outbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void clientRespondsWithException() throws Exception {
        final String orderId = "0000000029";

        assertScenario(
            "fixtures/functional/cancel_outbound/7/wrap_request.xml",
            "fixtures/functional/cancel_outbound/7/wrap_response.xml",
            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "shipments", orderId, "cancel"), HttpMethod.POST))
                .setResponsePath("fixtures/functional/common/failed_client_response.json")
                .setResponseStatus(HttpStatus.NOT_FOUND)
        );
    }

    /**
     * Сценарий #8: Попытка выполнить отмену для изъятия c
     * orderType={@link ru.yandex.market.wrap.infor.model.OrderType#STANDARD}.
     * <p>В БД есть заказ orderType=STANDARD</p>
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     */
    @DatabaseSetup(
        value = "classpath:fixtures/functional/cancel_outbound/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOutboundWithOutboundFitOrderType() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_outbound/8/wrap_request.xml",
            "fixtures/functional/cancel_outbound/8/wrap_response.xml"
        );
    }

    private void assertScenarioWithoutInteraction(String wrapRequest, String wrapResponse) throws Exception {
        FunctionalTestScenarioBuilder.start(AbstractResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    private void assertScenario(String wrapRequest,
                                String wrapResponse,
                                FulfillmentInteraction interaction) throws Exception {


        FunctionalTestScenarioBuilder.start(AbstractResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interaction)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    private FulfillmentInteraction getOkInteraction(String orderId) {
        return inforInteraction(fulfillmentUrl(
            Arrays.asList(clientProperties.getWarehouseKey(), "shipments", orderId, "cancel"), HttpMethod.POST))
            .setResponsePath("fixtures/functional/cancel_outbound/common/empty_response.json")
            .setResponseStatus(HttpStatus.OK);
    }

}
