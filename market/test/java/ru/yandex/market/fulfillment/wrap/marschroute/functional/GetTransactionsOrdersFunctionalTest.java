package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundDetailsResponse;

class GetTransactionsOrdersFunctionalTest extends RepositoryTest {

    /**
     * Сценарий 1:
     * Запрашиваем транзакции по заказу за интервал, в котором не было транзакций.
     * <p>
     * В ответ должны получить пустой список транзакций.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_transactions_orders/1/database.xml")
    void byIntervalWithZeroTransactions() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/1/request.xml",
            "functional/get_transactions_orders/1/response.xml"
        );
    }

    /**
     * Сценарий 2:
     * Запрашиваем транзакции за интервал, в котором были транзакции.
     * <p>
     * В ответ должны получить корректный список транзакций.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_transactions_orders/2/database.xml")
    void byIntervalWithExistingTransactions() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/2/request.xml",
            "functional/get_transactions_orders/2/response.xml"
        );
    }

    /**
     * Сценарий 3:
     * Запрашиваем транзакции по идентификатору заказа, по которому не было транзакций.
     * <p>
     * В ответ должны получить пустой список транзакций.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_transactions_orders/3/database.xml")
    void byOrderIdWhenNoTransactionsAvailable() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/3/request.xml",
            "functional/get_transactions_orders/3/response.xml"
        );
    }

    /**
     * Сценарий 4:
     * Запрашиваем транзакции по идентификатору заказа, по которому существуют транзакции.
     * <p>
     * В ответ должны получить пустой список транзакций.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_transactions_orders/4/database.xml")
    void byOrderIdWhenTransactionsExist() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/4/request.xml",
            "functional/get_transactions_orders/4/response.xml"
        );
    }

    /**
     * Сценарий 5:
     * Запрашиваем транзакции по идентификатору заказа и интервалу одновременно.
     * <p>
     * В ответ должны получить ошибку о некорректном запросе.
     */
    @Test
    void byOrderIdAndIntervalSimultaneously() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/5/request.xml",
            "functional/get_transactions_orders/5/response.xml"
        );
    }

    /**
     * Сценарий 6:
     * Запрашиваем транзакции не указав ни идентификатор заказа, ни интервал.
     * <p>
     * В ответ должны получить ошибку о некорректном запросе.
     */
    @Test
    void withoutOrderIdAndInterval() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/6/request.xml",
            "functional/get_transactions_orders/6/response.xml"
        );
    }

    /**
     * Сценарий 8:
     * Запрашиваем транзакции за интервал, чья протяженность больше недели.
     * <p>
     * В ответ должны получить ошибку о некорректном запросе.
     */
    @Test
    void withIntervalLengthMoreThanOneWeek() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/7/request.xml",
            "functional/get_transactions_orders/7/response.xml"
        );
    }

    /**
     * Сценарий 9:
     * Запрашиваем транзакции за интервал, в котором были транзакции c пагинацией по limit=2/offset=1.
     * Запрос по интервалу возвращает последние 3 элемента.
     * <p>
     * В БД - 4 элемента.
     * В ответ должны получить корректный список из 2-х транзакций.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_transactions_orders/common/database.xml")
    void byIntervalWithValidLimitOffset() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/9/request.xml",
            "functional/get_transactions_orders/9/response.xml"
        );
    }

    /**
     * Сценарий 10:
     * Запрашиваем транзакции за интервал, в котором были транзакции c пагинацией по limit=4/offset=0.
     * Запрос по интервалу возвращает первых 2 элемента, а по пагинации - все.
     * <p>
     * В БД - 4 элемента.
     * В ответ должны получить корректный список из 2-х транзакций.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_transactions_orders/common/database.xml")
    void byIntervalWithValidLimitOffset2() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/10/request.xml",
            "functional/get_transactions_orders/10/response.xml"
        );
    }

    /**
     * Сценарий 11:
     * Запрашиваем транзакции за интервал, в котором были транзакции c пагинацией только по limit=2.
     * <p>
     * В БД - 4 элемента.
     * Проверяем, что offset по умолчанию будет задан 0 и будут возвращены первые 2 элемента.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_transactions_orders/common/database.xml")
    void byIntervalWithLimitOnly() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/11/request.xml",
            "functional/get_transactions_orders/11/response.xml"
        );
    }


    /**
     * Сценарий 12:
     * Запрашиваем транзакции за интервал, в котором были транзакции c пагинацией только по offset.
     * <p>
     * В БД - 4 элемента.
     * В ответ ошибка.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_transactions_orders/common/database.xml")
    void byIntervalWithOffsetOnly() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/12/request.xml",
            "functional/get_transactions_orders/12/response.xml"
        );
    }

    /**
     * Сценарий 13:
     * Запрашиваем транзакции по идентификатору заказа,
     * по которому существуют транзакции c пагинацией по limit=2/offset=1.
     * <p>
     * В БД - 4 элемента.
     * С указанным id - БД 2 транзакции.
     * В ответ должны получить корректный список из 1-ой транзакции.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_transactions_orders/common/database.xml")
    void byOrderIdWithValidLimitOffset() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/13/request.xml",
            "functional/get_transactions_orders/13/response.xml"
        );
    }

    /**
     * Сценарий 14:
     * Запрашиваем транзакции по идентификатору заказа,
     * по которому существуют транзакции c пагинацией только по limit.
     * <p>
     * В БД - 4 элемента.
     * Проверяем, что offset по умолчанию будет задан 0 и будут возвращены первые 2 элемента.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_transactions_orders/common/database.xml")
    void byOrderIdWithLimitOnly() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/14/request.xml",
            "functional/get_transactions_orders/14/response.xml"
        );
    }

    /**
     * Сценарий 15:
     * Запрашиваем транзакции по идентификатору заказа,
     * по которому существуют транзакции c пагинацией только по offset.
     * <p>
     * В БД - 4 элемента.
     * В ответе ошибка.
     */
    @Test
    @DatabaseSetup("classpath:functional/get_transactions_orders/common/database.xml")
    void byOrderIdWithOffsetOnly() throws Exception {
        executeScenario(
            "functional/get_transactions_orders/15/request.xml",
            "functional/get_transactions_orders/15/response.xml"
        );
    }

    private void executeScenario(String request, String response) throws Exception {
        FunctionalTestScenarioBuilder
            .start(GetInboundDetailsResponse.class)
            .sendRequestToWrapQueryGateway(request)
            .andExpectWrapAnswerToBeEqualTo(response)
            .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
            .start();
    }
}
