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

class CancelOrderTest extends AbstractFunctionalTest {

    /**
     * Сценарий #1: Отмена заказа со статусом, при котором операцию еще можно произвести (до 51).
     * <p>Проверяем, что будет выполнен запрос к инфору с отменой.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOrderWithCancelableStatus() throws Exception {
        assertScenario(
            "fixtures/functional/cancel_order/1/wrap_request.xml",
            "fixtures/functional/cancel_order/1/wrap_response.xml",
            getOkInteraction("0000000029")
        );
    }

    /**
     * Сценарий #2: Отмена заказа со статусом, при котором операцию уже нельзя произвести (>= 51)
     * <p>В БД несколько заказов и история для запрашиваемого заказа с 2-мя статусами: 10->51.</p>
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOrderWithNonCancelableStatus() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_order/2/wrap_request.xml",
            "fixtures/functional/cancel_order/2/wrap_response.xml"
        );
    }

    /**
     * Сценарий #3: Попытка отменить несуществующий заказ
     * <p>В БД несколько заказов</p>
     * <p>Проверяем, что будет возвращена ошибка при попытке отменить несуществующий заказ</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOrderWithInvalidYandexId() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_order/3/wrap_request.xml",
            "fixtures/functional/cancel_order/3/wrap_response.xml"
        );
    }

    /**
     * Сценарий #4: Попытка выполнить отмену для заказа, которы был отменен ранее извне
     * <p>В БД есть заказ, который уже отменен с нашей стороны</p>
     * <p>Проверяем, что будет возвращен статус об успешной отмене</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOrderWithAlreadyExternallyCancelledStatus() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_order/4/wrap_request.xml",
            "fixtures/functional/cancel_order/4/wrap_response.xml"
        );
    }

    /**
     * Сценарий #5: Попытка выполнить отмену для заказа, которы был отменен ранее складом
     * <p>В БД есть заказ, который уже отменен складом</p>
     * <p>Проверяем, что будет возвращен статус об успешной отмене</p>
     * <p>Взаимодействий с инфором нет.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOrderWithInternallyCancelledStatus() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_order/5/wrap_request.xml",
            "fixtures/functional/cancel_order/5/wrap_response.xml"
        );
    }

    /**
     * Сценарий #6: Попытка выполнить отмену для заказа (конкурретный случай).
     * <p>В БД есть заказ, который на момент проверки иммет еще подходящий для отмены статус,
     * но не валидный в момент обращения к инфору</p>
     * <p>Проверяем, что будет возвращена ошибка от инфора</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void concurrentCancellation() throws Exception {
        final String orderId = "0000000029";

        assertScenario(
            "fixtures/functional/cancel_order/6/wrap_request.xml",
            "fixtures/functional/cancel_order/6/wrap_response.xml",
            inforInteraction(fulfillmentUrl(
                Arrays.asList(clientProperties.getWarehouseKey(), "shipments", orderId, "cancel"), HttpMethod.POST))
                .setResponsePath("fixtures/functional/cancel_order/common/empty_response.json")
                .setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }

    /**
     * Сценарий №7 Попытка отменить заказ прерывается исключением от клиента.
     * <p>Проверяем, что текст ошибки корректно пробросился до ответа обертки.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void clientRespondsWithException() throws Exception {
        final String orderId = "0000000029";

        assertScenario(
            "fixtures/functional/cancel_order/7/wrap_request.xml",
            "fixtures/functional/cancel_order/7/wrap_response.xml",
            inforInteraction(fulfillmentUrl(Arrays.asList(
                clientProperties.getWarehouseKey(), "shipments", orderId, "cancel"), HttpMethod.POST))
                .setResponsePath("fixtures/functional/common/failed_client_response.json")
                .setResponseStatus(HttpStatus.NOT_FOUND)
        );
    }

    /**
     * Сценарий #8: Попытка выполнить отмену для заказа c
     * orderType={@link ru.yandex.market.wrap.infor.model.OrderType#OUTBOUND_FIT}.
     * <p>В БД есть заказ orderType=OUTBOUND_FIT</p>
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOrderWithOutboundFitOrderType() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_order/8/wrap_request.xml",
            "fixtures/functional/cancel_order/8/wrap_response.xml"
        );
    }

    /**
     * Сценарий #9: Попытка выполнить отмену для заказа c
     * orderType={@link ru.yandex.market.wrap.infor.model.OrderType#OUTBOUND_DEFECT}.
     * <p>В БД есть заказ orderType=OUTBOUND_DEFECT</p>
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOrderWithOutboundDefectOrderType() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_order/9/wrap_request.xml",
            "fixtures/functional/cancel_order/9/wrap_response.xml"
        );
    }

    /**
     * Сценарий #10: Попытка выполнить отмену для заказа c
     * orderType={@link ru.yandex.market.wrap.infor.model.OrderType#OUTBOUND_EXPIRED}.
     * <p>В БД есть заказ orderType=OUTBOUND_EXPIRED</p>
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOrderWithOutboundExpiredOrderType() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_order/10/wrap_request.xml",
            "fixtures/functional/cancel_order/10/wrap_response.xml"
        );
    }

    /**
     * Сценарий #11: Попытка выполнить отмену для заказа c
     * orderType={@link ru.yandex.market.wrap.infor.model.OrderType#LOAD_TESTING}.
     * <p>В БД есть заказ orderType=LOAD_TESTING</p>
     * <p>Проверяем, что будет выполнен запрос к инфору с отменой.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOrderWithLoadTestingStatus() throws Exception {
        assertScenario(
            "fixtures/functional/cancel_order/11/wrap_request.xml",
            "fixtures/functional/cancel_order/11/wrap_response.xml",
            getOkInteraction("0000000036")
        );
    }

    /**
     * Сценарий #12: Попытка выполнить отмену для заказа c
     * orderType={@link ru.yandex.market.wrap.infor.model.OrderType#OUTBOUND_SURPLUS}.
     * <p>В БД есть заказ orderType=OUTBOUND_SURPLUS</p>
     * <p>Проверяем, что будет возвращена ошибка BAD_REQUEST.</p>
     */
    @DatabaseSetup(
        value = "/fixtures/functional/cancel_order/common/state.xml",
        connection = "wmsConnection"
    )
    @Test
    void cancelOrderWithOutboundSurplusOrderType() throws Exception {
        assertScenarioWithoutInteraction(
            "fixtures/functional/cancel_order/12/wrap_request.xml",
            "fixtures/functional/cancel_order/12/wrap_response.xml"
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
            .setResponsePath("fixtures/functional/cancel_order/common/empty_response.json")
            .setResponseStatus(HttpStatus.OK);
    }

}
