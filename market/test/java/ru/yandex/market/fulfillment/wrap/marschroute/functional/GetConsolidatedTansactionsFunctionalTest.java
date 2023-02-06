package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetConsolidatedTransactionsResponse;

class GetConsolidatedTansactionsFunctionalTest extends RepositoryTest {

    /**
     * Сценарий 1:
     * Запрашиваем транзакции по заказу за интервал, в котором не было транзакций.
     * <p>
     * В ответ должны получить пустой список транзакций.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_consolidated_transactions/1/database.xml")
    void byIntervalWithZeroTransactions() throws Exception {
        executeScenario(
            "functional/get_consolidated_transactions/1/request.xml",
            "functional/get_consolidated_transactions/1/response.xml"
        );
    }

    /**
     * Сценарий 2:
     * Запрашиваем транзакции за интервал, в котором были транзакции.
     * <p>
     * В ответ должны получить корректный список транзакций.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_consolidated_transactions/2/database.xml")
    void byIntervalWithExistingTransactions() throws Exception {
        executeScenario(
            "functional/get_consolidated_transactions/2/request.xml",
            "functional/get_consolidated_transactions/2/response.xml"
        );
    }


    /**
     * Сценарий 3:
     * Запрашиваем транзакции не указав  интервал.
     * <p>
     * В ответ должны получить ошибку о некорректном запросе.
     */
    @Test
    void withoutOrderIdAndInterval() throws Exception {
        executeScenario(
            "functional/get_consolidated_transactions/3/request.xml",
            "functional/get_consolidated_transactions/3/response.xml"
        );
    }

    /**
     * Сценарий 4:
     * Запрашиваем транзакции за интервал, чья протяженность больше недели.
     * <p>
     * В ответ должны получить ошибку о некорректном запросе.
     */
    @Test
    void withIntervalLengthMoreThanOneWeek() throws Exception {
        executeScenario(
            "functional/get_consolidated_transactions/4/request.xml",
            "functional/get_consolidated_transactions/4/response.xml"
        );
    }

    /**
     * Сценарий 5:
     * Запрос с ограничением по limit=2/offset=1.
     * <p>
     * В БД - 4 записи.
     * Будут возвращены вторая и третья записи.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_consolidated_transactions/common/database.xml")
    void withValidPagination() throws Exception {
        executeScenario(
            "functional/get_consolidated_transactions/5/request.xml",
            "functional/get_consolidated_transactions/5/response.xml"
        );
    }

    /**
     * Сценарий 6:
     * Запрос только с фильтрацией по интервалу с limit=2 без offset.
     * <p>
     * В БД - 4 записи.
     * Проверяем, что offset будет установлен как 0 и будут вовращены первые 2 записи.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_consolidated_transactions/common/database.xml")
    void byIntervalWithLimitOnly() throws Exception {
        executeScenario(
            "functional/get_consolidated_transactions/6/request.xml",
            "functional/get_consolidated_transactions/6/response.xml"
        );
    }

    /**
     * Сценарий 7:
     * Запрос только с указаным параметром offset.
     * <p>
     * Будет возвращена ошибка.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_consolidated_transactions/common/database.xml")
    void withOffsetOnly() throws Exception {
        executeScenario(
            "functional/get_consolidated_transactions/7/request.xml",
            "functional/get_consolidated_transactions/7/response.xml"
        );
    }

    /**
     * Сценарий 8:
     * Запрос с ограничением по limit=10/offset=100500.
     * <p>
     * В БД - 4 записи.
     * Будет возвращен пустой список.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_consolidated_transactions/common/database.xml")
    void withOffsetWhichIsMoreThenSizeOfDb() throws Exception {
        executeScenario(
            "functional/get_consolidated_transactions/8/request.xml",
            "functional/get_consolidated_transactions/8/response.xml"
        );
    }

    private void executeScenario(String request, String response) throws Exception {
        FunctionalTestScenarioBuilder
            .start(GetConsolidatedTransactionsResponse.class)
            .sendRequestToWrapQueryGateway(request)
            .andExpectWrapAnswerToBeEqualTo(response)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
