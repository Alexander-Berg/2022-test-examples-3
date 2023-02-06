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


public class OrderStatusHistoryControllerTest extends IntegrationTest {

    /**
     * Сценарий #1:
     * <p>
     * В теле запроса отсутствует orderId.partnerId.
     * <p>
     * Ожидаем получить ответ с ошибкой 9400 BAD REQUEST и сообщением об отсутствующем partnerId.
     */
    @Test
    void noPartnerIdAvailable() throws Exception {
        executeXMLScenario("order-history/1/request.xml", "order-history/1/response.xml");
        executeJSONScenario("order-history/1/request.json", "order-history/1/response.json",
                status().isInternalServerError());
    }

    /**
     * Сценарий #2:
     * <p>
     * В БД отсутствует информация по статусам заказа с идентификатором из запроса.
     * <p>
     * В ответ должна вернуться ошибка с информацией о том, что нам не удалось найти заказ с указанным ID.
     */
    @Test
    @DatabaseSetup("/order-history/2/before.xml")
    void noHistoryAvailable() throws Exception {
        executeXMLScenario("order-history/2/request.xml", "order-history/2/response.xml");
        executeJSONScenario("order-history/2/request.json", "order-history/2/response.json",
                status().isInternalServerError());
    }


    /**
     * Сценарий #3:
     * В БД для указанного заказа присутствует только 1 статус.
     * В ответ должна вернуться история, состоящая из 1 статуса.
     */
    @Test
    @DatabaseSetup("/order-history/3/before.xml")
    void singleStatusesHistory() throws Exception {
        executeXMLScenario("order-history/3/request.xml", "order-history/3/response.xml");
        executeJSONScenario("order-history/3/request.json", "order-history/3/response.json",
                status().isOk());
    }

    /**
     * Сценарий #4:
     * В БД для указанного заказа присутствует полноценная история с множеством статусов.
     * <p>
     * В ответ должна вернуться эта история, но с сортировкой по дате статуса по возрастанию
     * и со схлопыванием статусов.
     */
    @Test
    @DatabaseSetup("/order-history/4/before.xml")
    void multipleStatusesHistory() throws Exception {
        executeXMLScenario("order-history/4/request.xml", "order-history/4/response.xml");
        executeJSONScenario("order-history/4/request.json", "order-history/4/response.json",
                status().isOk());
    }

    /**
     * Сценарий #6:
     * В БД для указанного заказа присутствует только 1 статус
     * для заказа с 47-ым статусом (нагрузочное тестирование).
     * <p>
     * В ответ должна вернуться история, состоящая из 1 статуса.
     */
    @Test
    @DatabaseSetup("/order-history/6/before.xml")
    void singleStatusesHistoryOfOrderForLoadTesting() throws Exception {
        executeXMLScenario("order-history/6/request.xml", "order-history/6/response.xml");
        executeJSONScenario("order-history/6/request.json", "order-history/6/response.json",
                status().isOk());
    }

    /**
     * Сценарий #7:
     * В БД для указанного заказа присутствует 2 статуса: 0 -> -1 -> -3
     * <p>
     * В ответ должна вернуться история, состоящая из 1 статуса.
     */
    @Test
    @DatabaseSetup("/order-history/7/before.xml")
    public void statusesHistoryOfOrderWithOutOfPickingLotStatus() throws Exception {
        executeXMLScenario("order-history/7/request.xml", "order-history/7/response.xml");
        executeJSONScenario("order-history/7/request.json", "order-history/7/response.json",
                status().isOk());
    }

    /**
     * Сценарий #8:
     * В БД для указанного заказа присутствует полноценная история с множеством статусов.
     * При этом сортировка по serialKey и по addDate отличается,
     * то есть для максимального addDate минимальный serialKey.
     * <p>
     * В ответ должна вернуться эта история, но с сортировкой по ДАТЕ статуса по возрастанию
     * и со схлопыванием статусов.
     */
    @Test
    @DatabaseSetup("/order-history/8/before.xml")
    void multipleStatusesHistoryWithDifferentAddDateAndSerialKeySortOrder() throws Exception {
        executeXMLScenario("order-history/8/request.xml", "order-history/8/response.xml");
        executeJSONScenario("order-history/8/request.json", "order-history/8/response.json",
                status().isOk());
    }

    /**
     * Сценарий #9:
     * В БД для указанного заказа присутствует 1 статус и 2 возвратных статуса.
     * В ответ должна вернуться история, состоящая из 1 статуса и 2 возвратных статусов.
     */
    @Test
    @DatabaseSetup("/order-history/9/before.xml")
    void returnOrderStatusesHistory() throws Exception {
        executeXMLScenario("order-history/9/request.xml", "order-history/9/response.xml");
        executeJSONScenario("order-history/9/request.json", "order-history/9/response.json",
                status().isOk());
    }

    /**
     * Сценарий #10:
     * 95 статус есть, нет ни 65 ни 68. Ожидаем что добавится 120 статус перед 130
     * YM_FAKE_ORDER_PARCEL_MIXIN - включен
     */
    @Test
    @DatabaseSetup("/order-history/settings/fake-parcel-enabled.xml")
    @DatabaseSetup("/order-history/10/before.xml")
    void fakeOrderStatusesHistoryWithFake() throws Exception {
        executeXMLScenario("order-history/10/request.xml", "order-history/10/response.xml");
        executeJSONScenario("order-history/10/request.json", "order-history/10/response.json",
                status().isOk());
    }

    /**
     * Сценарий #10.1:
     * 95 статус есть, нет ни 65 ни 68. Ожидаем что добавится 120 статус перед 130
     * YM_FAKE_ORDER_PARCEL_MIXIN - выключен
     */
    @Test
    @DatabaseSetup("/order-history/10/before.xml")
    void fakeOrderStatusesHistoryWithoutFake() throws Exception {
        executeXMLScenario("order-history/10/request.xml", "order-history/10/response-no-fake.xml");
        executeJSONScenario("order-history/10/request.json", "order-history/10/response-no-fake.json",
                status().isOk());
    }

    /**
     * Сценарий #11:
     * 95 статус есть, есть 65. Ожидаем, что добавится 120 статус перед 130
     * YM_FAKE_ORDER_PARCEL_MIXIN - включен
     */
    @Test
    @DatabaseSetup("/order-history/settings/fake-parcel-enabled.xml")
    @DatabaseSetup("/order-history/11/before.xml")
    void fakeOrderStatusesHistoryHas65Status() throws Exception {
        executeXMLScenario("order-history/11/request.xml", "order-history/11/response.xml");
        executeJSONScenario("order-history/11/request.json", "order-history/11/response.json",
                status().isOk());
    }

    /**
     * Сценарий #12:
     * 95 статус есть, есть 68. Ожидаем что не добавиться еще раз 120 статус перед 130
     * YM_FAKE_ORDER_PARCEL_MIXIN - включен
     */
    @Test
    @DatabaseSetup("/order-history/settings/fake-parcel-enabled.xml")
    @DatabaseSetup("/order-history/12/before.xml")
    void fakeOrderStatusesHistoryHas68Status() throws Exception {
        executeXMLScenario("order-history/12/request.xml", "order-history/12/response.xml");
        executeJSONScenario("order-history/12/request.json", "order-history/12/response.json",
                status().isOk());
    }

    /**
     * Сценарий #12.1:
     * 95 статус есть, есть 68. Ожидаем что не добавиться еще раз 120 статус перед 130
     * YM_FAKE_ORDER_PARCEL_MIXIN - включен
     */
    @Test
    @DatabaseSetup("/order-history/12/before.xml")
    void fakeOrderStatusesHistoryHas68StatusSettingDisabled() throws Exception {
        executeXMLScenario("order-history/12/request.xml", "order-history/12/response.xml");
        executeJSONScenario("order-history/12/request.json", "order-history/12/response.json",
                status().isOk());
    }

    /**
     * Сценарий #13:
     * нет 95 статуса, есть 65 и 68. Ожидаем что не добавится 120
     * YM_FAKE_ORDER_PARCEL_MIXIN - включен
     */
    @Test
    @DatabaseSetup("/order-history/settings/fake-parcel-enabled.xml")
    @DatabaseSetup("/order-history/13/before.xml")
    void fakeOrderStatusesHistoryNo95Status() throws Exception {
        executeXMLScenario("order-history/13/request.xml", "order-history/13/response.xml");
        executeJSONScenario("order-history/13/request.json", "order-history/13/response.json",
                status().isOk());
    }

    /**
     * Сценарий #13.1:
     * нет 95 статуса, есть 65 и 68. Ожидаем что не добавится 120
     * YM_FAKE_ORDER_PARCEL_MIXIN - выключен
     */
    @Test
    @DatabaseSetup("/order-history/13/before.xml")
    void fakeOrderStatusesHistoryNo95StatusSettingDisabled() throws Exception {
        executeXMLScenario("order-history/13/request.xml", "order-history/13/response.xml");
        executeJSONScenario("order-history/13/request.json", "order-history/13/response.json",
                status().isOk());
    }

    @Test
    @DatabaseSetup("/order-history/new-history/db.xml")
    void newStatusHistory() throws Exception {
        executeXMLScenario("order-history/new-history/request.xml", "order-history/new-history/response.xml");
        executeJSONScenario("order-history/new-history/request.json", "order-history/new-history/response.json",
                status().isOk());
    }

    @Test
    @DatabaseSetup("/order-history/new-history-split/db.xml")
    void newStatusHistoryForSplitOrder() throws Exception {
        executeXMLScenario("order-history/new-history-split/request.xml",
                "order-history/new-history-split/response.xml");
        executeJSONScenario("order-history/new-history-split/request.json",
                "order-history/new-history-split/response.json",
                status().isOk());

        // request without yandexId
        executeXMLScenario("order-history/new-history-split/request-no-yandex-id.xml",
                "order-history/new-history-split/response.xml");
        executeJSONScenario("order-history/new-history-split/request-no-yandex-id.json",
                "order-history/new-history-split/response.json",
                status().isOk());
    }

    private void executeXMLScenario(String requestFilePath, String responseFilePath) throws Exception {
        executeScenario(
                requestFilePath,
                responseFilePath,
                status().isOk(),
                MediaType.TEXT_XML
        );
    }

    private void executeJSONScenario(String requestFilePath, String responseFilePath,
                                     ResultMatcher responseStatus) throws Exception {
        executeScenario(
                requestFilePath,
                responseFilePath,
                responseStatus,
                MediaType.APPLICATION_JSON
        );
    }

    private void executeScenario(String requestFilePath, String responseFilePath, ResultMatcher responseStatus,
                                 MediaType mediaType) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/order-status-history")
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
