package ru.yandex.market.wms.api.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.common.spring.utils.XmlAssertUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class OrderStatusControllerTest extends IntegrationTest {

    /**
     * Сценарий #0:
     * На пустой запрос должны ответить с ошибкой 400.
     */
    @Test
    public void getOrderStatusesIncorrectRequest() throws Exception {
        mockMvc.perform(post("/ENTERPRISE/order-status")
                .contentType(MediaType.TEXT_XML)
                .content(getFileContent("order-statuses/0/request.xml")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/ENTERPRISE/order-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("order-statuses/0/request.json")))
                .andExpect(status().isBadRequest());
    }

    /**
     * Сценарий #1:
     * <p>
     * Запрашиваем актуальный статус для 1 идентификатора.
     * Информация по этому идентификатору отсутствует в БД.
     * <p>
     * В ответ должно вернуться тело с ошибкой
     */
    @Test
    public void getOrderStatusesForNotExistingOrder() throws Exception {
        executeScenario("order-statuses/1/request.xml", "order-statuses/1/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/1/request.json", "order-statuses/1/response.json",
                status().isInternalServerError(), MediaType.APPLICATION_JSON);
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
    @DatabaseSetup("/order-statuses/2/before.xml")
    void singleStatusAvailableForSingleId() throws Exception {
        executeScenario("order-statuses/2/request.xml", "order-statuses/2/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/2/request.json", "order-statuses/2/response.json",
                status().isOk(), MediaType.APPLICATION_JSON);
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
    @DatabaseSetup("/order-statuses/3/before.xml")
    void multipleStatusesAvailableForSingleId() throws Exception {
        executeScenario("order-statuses/3/request.xml", "order-statuses/3/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/3/request.json", "order-statuses/3/response.json",
                status().isOk(), MediaType.APPLICATION_JSON);
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
    @DatabaseSetup("/order-statuses/4/before.xml")
    void singleStatusAvailableForMultipleIds() throws Exception {
        executeScenario("order-statuses/4/request.xml", "order-statuses/4/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/4/request.json", "order-statuses/4/response.json",
                status().isOk(), MediaType.APPLICATION_JSON);
    }

    /**
     * Сценарий #5:
     * <p>
     * Запрашиваем актуальные статусы по множеству идентификаторов.
     * Информация по этому набору идентификаторов отсутствует в БД.
     * <p>
     * В ответ должна вернуться ошибка.
     */
    @Test
    void noStatusesAvailableForMultiple() throws Exception {
        executeScenario("order-statuses/5/request.xml", "order-statuses/5/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/5/request.json", "order-statuses/5/response.json",
                status().isInternalServerError(), MediaType.APPLICATION_JSON);
    }

    /**
     * Сценарий #7:
     * <p>
     * Запрашиваем актуальные статусы по множеству идентификаторов.
     * В БД присутствует ровно по несколько статусов для каждого из них.
     */
    @Test
    @DatabaseSetup("/order-statuses/7/before.xml")
    void multipleStatusesAvailableForMultipleIds() throws Exception {
        executeScenario("order-statuses/7/request.xml", "order-statuses/7/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/7/request.json", "order-statuses/7/response.json",
                status().isOk(), MediaType.APPLICATION_JSON);
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
    @DatabaseSetup("/order-statuses/8/before.xml")
    void singleStatusAvailableOfOrderForLoadTesting() throws Exception {
        executeScenario("order-statuses/8/request.xml", "order-statuses/8/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/8/request.json", "order-statuses/8/response.json",
                status().isOk(), MediaType.APPLICATION_JSON);
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
    @DatabaseSetup("/order-statuses/9/before.xml")
    void multipleStatusesAvailableForSingleIdWithAddDateAndSerialKeySortDifferent() throws Exception {
        executeScenario("order-statuses/9/request.xml", "order-statuses/9/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/9/request.json", "order-statuses/9/response.json",
                status().isOk(), MediaType.APPLICATION_JSON);
    }

    /**
     * Сценарий #10:
     * <p>
     * Запрашиваем актуальный статус для 1 идентификатора.
     * По этому идентификатору присутствует более 1 статуса в БД и вовзратные статусы.
     * При этом возвратный статус с самой актуальной датой.
     * <p>
     * В ответ должна вернуться информация с возвратным статусом.
     */
    @Test
    @DatabaseSetup("/order-statuses/10/before.xml")
    void orderHasReturnStatuses() throws Exception {
        executeScenario("order-statuses/10/request.xml", "order-statuses/10/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/10/request.json", "order-statuses/10/response.json",
                status().isOk(), MediaType.APPLICATION_JSON);
    }

    /**
     * Сценарий #11:
     * <p>
     * Запрашиваем актуальный статус для 1 идентификатора.
     * По этому идентификатору присутствует только вовзратные статусы.
     * <p>
     * В ответ должна вернуться информация с возвратным статусом.
     */
    @Test
    @DatabaseSetup("/order-statuses/11/before.xml")
    void orderHasOnlyReturnStatuses() throws Exception {
        executeScenario("order-statuses/11/request.xml", "order-statuses/11/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/11/request.json", "order-statuses/11/response.json",
                status().isOk(), MediaType.APPLICATION_JSON);
    }

    /**
     * Сценарий #13:
     * <p>
     * Запрашиваем актуальный статус для 1 заказа со статусом 02 (разделение заказа).
     * По этому идентификатору присутствует ровно 1 статус в БД.
     * <p>
     * В ответ должна вернуться информация со статусом из БД.
     */
    @Test
    @DatabaseSetup("/order-statuses/13/before.xml")
    void singleStatusAfterSplitOrder() throws Exception {
        executeScenario("order-statuses/13/request.xml", "order-statuses/13/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/13/request.json", "order-statuses/13/response.json",
                status().isOk(), MediaType.APPLICATION_JSON);
    }

    @Test
    @DatabaseSetup("/order-statuses/new-history/db.xml")
    void newStatusHistory() throws Exception {
        executeScenario("order-statuses/new-history/request.xml", "order-statuses/new-history/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/new-history/request.json", "order-statuses/new-history/response.json",
                status().isOk(), MediaType.APPLICATION_JSON);
    }

    @Test
    @DatabaseSetup("/order-statuses/new-history-split/db.xml")
    void newStatusHistoryForSpitOrder() throws Exception {
        executeScenario("order-statuses/new-history-split/request.xml",
                "order-statuses/new-history-split/response.xml",
                status().isOk(), MediaType.TEXT_XML);
        executeScenario("order-statuses/new-history-split/request.json",
                "order-statuses/new-history-split/response.json",
                status().isOk(), MediaType.APPLICATION_JSON);
    }

    private void executeScenario(String requestFilePath, String responseFilePath, ResultMatcher responseStatus,
                                 MediaType mediaType) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/order-status")
                        .contentType(mediaType)
                        .content(getFileContent(requestFilePath)))
                .andDo(print())
                .andExpect(responseStatus)
                .andReturn();

        final String contentAsString = mvcResult.getResponse().getContentAsString();

        if (mediaType == MediaType.TEXT_XML) {
            XmlAssertUtils.assertXmlValuesAreEqual(contentAsString,
                    getFileContent(responseFilePath));
        } else {
            JsonAssertUtils.assertFileNonExtensibleEquals(responseFilePath, contentAsString);
        }
    }
}
