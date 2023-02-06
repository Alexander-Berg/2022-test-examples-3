package ru.yandex.market.wrap.infor.functional;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOrderResponse;

import static ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl.fulfillmentUrl;

class GetOrderTest extends AbstractFunctionalTest {

    /**
     * Сценарий #1:
     * <p>Передаем запрос на получение информации о заказе с 3-мя коробками</p>
     * <p>Получаем корректный ответ со списком из 3 Place'ов</p>
     */
    @Test
    void getOrderWithPacks() throws Exception {
        assertAnswer(
            "fixtures/functional/get_order/1/wrap_request.xml",
            "fixtures/functional/get_order/1/wrap_response.xml",
            "0000000410",
            "fixtures/functional/get_order/1/infor_response.json"
        );
    }

    /**
     * Сценарий #2:
     * <p>Передаем запрос на получение информации о заказе с одной коробкой</p>
     * <p>Получаем корректный ответ с одним Place'ом</p>
     */
    @Test
    void getOrderWithSinglePacks() throws Exception {
        assertAnswer(
            "fixtures/functional/get_order/2/wrap_request.xml",
            "fixtures/functional/get_order/2/wrap_response.xml",
            "0000000410",
            "fixtures/functional/get_order/2/infor_response.json"
        );
    }

    /**
     * Сценарий #3:
     * <p>Передаем запрос на получение информации о заказе с пустым списком pack-ов</p>
     * <p>Получаем корректный ответ c пустым списком Place'ов</p>
     */
    @Test
    void getOrderWithEmptyPacksList() throws Exception {
        assertAnswer(
            "fixtures/functional/get_order/3/wrap_request.xml",
            "fixtures/functional/get_order/3/wrap_response.xml",
            "0000000410",
            "fixtures/functional/get_order/3/infor_response.json"
        );
    }

    /**
     * Сценарий #4:
     * <p>Передаем запрос на получение информации о заказе с полем orderPacks = null</p>
     * <p>Получаем корректный ответ c пустым списком Place'ов</p>
     */
    @Test
    void getOrderWithNullPacks() throws Exception {
        assertAnswer(
            "fixtures/functional/get_order/4/wrap_request.xml",
            "fixtures/functional/get_order/4/wrap_response.xml",
            "0000000410",
            "fixtures/functional/get_order/4/infor_response.json"
        );
    }

    /**
     * Сценарий #5:
     * <p>Передаем запрос на получение информации о заказе без заданного partnerId</p>
     * <p>В ответ получаем ошибку</p>
     */
    @Test
    void getOrderWithoutPartnerId() throws Exception {
        FunctionalTestScenarioBuilder.start(GetOrderResponse.class)
            .sendRequestToWrapQueryGateway("fixtures/functional/get_order/5/wrap_request.xml")
            .andExpectWrapAnswerToBeEqualTo("fixtures/functional/get_order/5/wrap_response.xml")
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #6:
     * <p>Передаем запрос на получение информации о заказе с 3-мя коробками, 2 из которых с дублированными id.
     * Флаг по схлопыванию плейсов включен в БД.</p>
     *
     * <p>Получаем корректный ответ со списком из 2 Place'ов без дубликатов</p>
     */
    @Test
    @DatabaseSetup("classpath:fixtures/functional/get_order/6/state.xml")
    void getOrderWithDuplicatedPacksAndTurnedFlagOn() throws Exception {
        assertAnswer(
            "fixtures/functional/get_order/duplicates/wrap_request.xml",
            "fixtures/functional/get_order/6/wrap_response.xml",
            "0000000410",
            "fixtures/functional/get_order/duplicates/infor_response.json"
        );
    }

    /**
     * Сценарий #7:
     * <p>Передаем запрос на получение информации о заказе с 3-мя коробками, 2 из которых с дублированными id.
     * Флаг по схлопыванию плейсов в БД выключен.</p>
     *
     * <p>Получаем корректный ответ со списком из 3 Place'ов с дубликатами</p>
     */
    @Test
    @DatabaseSetup("classpath:fixtures/functional/get_order/7/state.xml")
    void getOrderWithDuplicatedPacksAndTurnedFlagOff() throws Exception {
        assertAnswer(
            "fixtures/functional/get_order/duplicates/wrap_request.xml",
            "fixtures/functional/get_order/7/wrap_response.xml",
            "0000000410",
            "fixtures/functional/get_order/duplicates/infor_response.json"
        );
    }

    /**
     * Сценарий #8:
     * <p>Передаем запрос на получение информации о заказе с 3-мя коробками, 2 из которых с дублированными id.
     * Флаг по схлопыванию плейсов в БД не задан.</p>
     *
     * <p>Получаем корректный ответ со списком из 3 Place'ов с дубликатами</p>
     */
    @Test
    void getOrderWithDuplicatedPacksAndAbsentFlag() throws Exception {
        assertAnswer(
            "fixtures/functional/get_order/duplicates/wrap_request.xml",
            "fixtures/functional/get_order/8/wrap_response.xml",
            "0000000410",
            "fixtures/functional/get_order/duplicates/infor_response.json"
        );
    }

    /**
     * Сценарий #9:
     * <p>Передаем запрос на получение информации о заказе нулевым списком коробок.
     * Флаг по схлопыванию плейсов включен в БД.</p>
     *
     * <p>Получаем корректный ответ с пустым списком плейсов</p>
     */
    @Test
    @DatabaseSetup("classpath:fixtures/functional/get_order/9/state.xml")
    void getOrderWithNullPacksListAndTurnedFlagOn() throws Exception {
        assertAnswer(
            "fixtures/functional/get_order/9/wrap_request.xml",
            "fixtures/functional/get_order/9/wrap_response.xml",
            "0000000410",
            "fixtures/functional/get_order/9/infor_response.json"
        );
    }

    /**
     * Сценарий #10:
     * <p>Передаем запрос на получение информации о заказе с недостающими товаром</p>
     * <p>Получаем корректный ответ с одним undefinedCount</p>
     */
    @Test
    void getOrderWithUndefinedCount() throws Exception {
        assertAnswer(
                "fixtures/functional/get_order/10/wrap_request.xml",
                "fixtures/functional/get_order/10/wrap_response.xml",
                "0000000410",
                "fixtures/functional/get_order/10/infor_response.json"
        );
    }


    private void assertAnswer(String wrapRequestPath, String wrapResponsePath,
                              String orderId, String inforResponsePath) throws Exception {

        FunctionalTestScenarioBuilder.start(GetOrderResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequestPath)
            .thenMockFulfillmentRequests(getInforShipmentsInteraction(orderId, inforResponsePath))
            .andExpectWrapAnswerToBeEqualTo(wrapResponsePath)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();

    }

    private FulfillmentInteraction getInforShipmentsInteraction(String orderId, String responsePath) {


        return inforInteraction(fulfillmentUrl(Arrays.asList(
            clientProperties.getWarehouseKey(), "shipments", orderId), HttpMethod.GET))
            .setResponsePath(responsePath)
            .setResponseStatus(HttpStatus.OK);
    }
}
