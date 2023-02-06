package ru.yandex.market.wrap.infor.functional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOrdersStatusResponse;

class GetOrdersStatusTest extends AbstractFunctionalTest {

    /**
     * Сценарий #1:
     * <p>
     * Запрашиваем актуальный статус для 1 идентификатора.
     * Информация по этому идентификатору отсутствует в БД.
     * <p>
     * В ответ должно вернуться тело с пустым набором статусов.
     */
    @Test
    void noStatusesAvailableForSingleId() throws Exception {
        executeScenario(
            "fixtures/functional/get_orders_status/1/request.xml",
            "fixtures/functional/get_orders_status/1/response.xml"
        );
    }

    /**
     * Сценарий #2:
     * <p>
     * Запрашиваем актуальный статус для 1 идентификатора.
     * По этому идентификатору присутствует ровно 1 статус в БД.
     * <p>
     * В ответ должна вернуться информация со статусом из БД.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_orders_status/2/state.xml",
        connection = "wmsConnection"
    )
    void singleStatusAvailableForSingleId() throws Exception {
        executeScenario(
            "fixtures/functional/get_orders_status/2/request.xml",
            "fixtures/functional/get_orders_status/2/response.xml"
        );
    }

    /**
     * Сценарий #3:
     * <p>
     * Запрашиваем актуальный статус для 1 идентификатора.
     * По этому идентификатору присутствует более 1 статуса в БД.
     * <p>
     * В ответ должна вернуться информация с самым актуальным из статусов из БД.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_orders_status/3/state.xml",
        connection = "wmsConnection"
    )
    void multipleStatusesAvailableForSingleId() throws Exception {
        executeScenario(
            "fixtures/functional/get_orders_status/3/request.xml",
            "fixtures/functional/get_orders_status/3/response.xml"
        );
    }

    /**
     * Сценарий #4:
     * <p>
     * Запрашиваем актуальные статусы по множеству идентификаторов.
     * В БД присутствует ровно по 1 статусу для каждого из них.
     * <p>
     * В ответ должны вернуться соответствующие статусы из бд.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_orders_status/4/state.xml",
        connection = "wmsConnection"
    )
    void singleStatusAvailableForMultipleIds() throws Exception {
        executeScenario(
            "fixtures/functional/get_orders_status/4/request.xml",
            "fixtures/functional/get_orders_status/4/response.xml"
        );
    }

    /**
     * Сценарий #5:
     * <p>
     * Запрашиваем актуальные статусы по множеству идентификаторов.
     * Информация по этому набору идентификаторов отсутствует в БД.
     * <p>
     * В ответ должен вернуться пустой ответ.
     */
    @Test
    void noStatusesAvailableForMultiple() throws Exception {
        executeScenario(
            "fixtures/functional/get_orders_status/5/request.xml",
            "fixtures/functional/get_orders_status/5/response.xml"
        );
    }

    /**
     * Сценарий #6:
     * Задействована ручка /delivery/get-orders-status, которая пригодна для трекания статусов СД.
     * <p>
     * В ответ клиент должен получить ошибку с информацией о том, что эта ручка не активна для WMS.
     */
    @Test
    void deliveryHandlerIsUsed() throws Exception {
        String wrapRequest = "fixtures/functional/get_orders_status/6/request.xml";
        String wrapResponse = "fixtures/functional/get_orders_status/6/response.xml";

        FunctionalTestScenarioBuilder.start(GetOrdersStatusResponse.class)
            .sendRequestToWrap("/delivery/get-orders-status", HttpMethod.POST, wrapRequest)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, deliveryMapper, clientProperties.getUrl())
            .start();
    }

    /**
     * Сценарий #7:
     * <p>
     * Запрашиваем актуальные статусы по множеству идентификаторов.
     * В БД присутствует ровно по несколько статусов для каждого из них.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_orders_status/7/state.xml",
        connection = "wmsConnection"
    )
    void multipleStatusesAvailableForMultipleIds() throws Exception {
        executeScenario(
            "fixtures/functional/get_orders_status/7/request.xml",
            "fixtures/functional/get_orders_status/7/response.xml"
        );
    }

    /**
     * Сценарий #8:
     * <p>
     * Запрашиваем актуальный статус для 1 заказа со статусом 47 (нагрузочное тестирование).
     * По этому идентификатору присутствует ровно 1 статус в БД.
     * <p>
     * В ответ должна вернуться информация со статусом из БД.
     */
    @Test
    @DatabaseSetup(
        value = "classpath:fixtures/functional/get_orders_status/8/state.xml",
        connection = "wmsConnection"
    )
    void singleStatusAvailableOfOrderForLoadTesting() throws Exception {
        executeScenario(
            "fixtures/functional/get_orders_status/8/request.xml",
            "fixtures/functional/get_orders_status/8/response.xml"
        );
    }

    /**
     * Сценарий #9:
     * <p>
     * Запрашиваем актуальный статус для 1 идентификатора.
     * По этому идентификатору присутствует более 1 статуса в БД.
     * При этом сортировка по serialKey и по addDate отличается,
     * то есть для максимального addDate минимальный serialKey.
     * <p>
     * В ответ должна вернуться информация с самым актуальным из статусов (по addDate) из БД.
     */
    @Test
    @DatabaseSetup(
            value = "classpath:fixtures/functional/get_orders_status/9/state.xml",
            connection = "wmsConnection"
    )
    void multipleStatusesAvailableForSingleIdWithAddDateAndSerialKeySortDifferent() throws Exception {
        executeScenario(
                "fixtures/functional/get_orders_status/9/request.xml",
                "fixtures/functional/get_orders_status/9/response.xml"
        );
    }

    private void executeScenario(String wrapRequest, String expectedWrapResponse) throws Exception {
        FunctionalTestScenarioBuilder.start(GetOrdersStatusResponse.class)
            .sendRequestToWrap("/fulfillment/get-orders-status", HttpMethod.POST, wrapRequest)
            .andExpectWrapAnswerToBeEqualTo(expectedWrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }
}
