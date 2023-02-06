package ru.yandex.market.wms.api.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;


public class OutboundStatusControllerTest extends IntegrationTest {

    /**
     * В БД для указанного заказа присутствует полноценная история с множеством статусов.
     * В истории присутствуют статусы, на которые нет маппинга для outbound.
     * <p>
     * В ответ должен вернуться последний статус.
     */
    @Test
    @DatabaseSetup("/outbound/statuses/1/before.xml")
    void test() throws Exception {
        performRequestAndCheck("outbound/statuses/1/request.json", "outbound/statuses/1/response.json");
    }

    /**
     * В БД нет ничего про этот заказ.
     */
    @Test
    @DatabaseSetup("/outbound/statuses/2/before.xml")
    void test2() throws Exception {
        performRequestAndCheck("outbound/statuses/2/request.json", "outbound/statuses/2/response.json");
    }

    /**
     * Заказ есть, истории нет.
     */
    @Test
    @DatabaseSetup("/outbound/statuses/3/before.xml")
    void test3() throws Exception {
        performRequestAndCheck("outbound/statuses/3/request.json", "outbound/statuses/3/response.json");
    }

    /**
     * Заказ есть, история есть, но статусов с пустым ордерлайн не завезли.
     */
    @Test
    @DatabaseSetup("/outbound/statuses/4/before.xml")
    void test4() throws Exception {
        performRequestAndCheck("outbound/statuses/4/request.json", "outbound/statuses/4/response.json");
    }

    /**
     * Заказ есть, история есть, статус с пустым ордерлайн есть, но и тот не маппится.
     */
    @Test
    @DatabaseSetup("/outbound/statuses/5/before.xml")
    void test5() throws Exception {
        performRequestAndCheck("outbound/statuses/5/request.json", "outbound/statuses/5/response.json");
    }

    /**
     * В БД для указанных заказов присутствуют полноценные истории с множеством статусов.
     * В истории присутствуют статусы, на которые нет маппинга для outbound.
     * <p>
     * В ответ должен вернуться последний статус для обоих заказов.
     */
    @Test
    @DatabaseSetup("/outbound/statuses/6/before.xml")
    void test6() throws Exception {
        performRequestAndCheck("outbound/statuses/6/request.json", "outbound/statuses/6/response.json");
    }

    /**
     * В БД нет ничего про outbound-12750263.
     */
    @Test
    @DatabaseSetup("/outbound/statuses/7/before.xml")
    void test7() throws Exception {
        performRequestAndCheck("outbound/statuses/7/request.json", "outbound/statuses/7/response.json");
    }

    /**
     * Заказы есть, истории по outbound-12750263 нет.
     */
    @Test
    @DatabaseSetup("/outbound/statuses/8/before.xml")
    void test8() throws Exception {
        performRequestAndCheck("outbound/statuses/8/request.json", "outbound/statuses/8/response.json");
    }

    /**
     * Заказы есть, история есть, но статусов по outbound-12750263 с пустым ордерлайн не завезли.
     */
    @Test
    @DatabaseSetup("/outbound/statuses/9/before.xml")
    void test9() throws Exception {
        performRequestAndCheck("outbound/statuses/9/request.json", "outbound/statuses/9/response.json");
    }

    /**
     * Заказы есть, история есть, статусы с пустым ордерлайн есть, но для outbound-12750263 статус не маппится.
     */
    @Test
    @DatabaseSetup("/outbound/statuses/10/before.xml")
    void test10() throws Exception {
        performRequestAndCheck("outbound/statuses/10/request.json", "outbound/statuses/10/response.json");
    }

    /**
     * В БД для указанных заказов присутствуют полноценные истории с множеством статусов.
     * В истории присутствуют статусы, на которые нет маппинга для outbound.
     * <p>
     * Но вот запрос передали пустой
     */
    @Test
    @DatabaseSetup("/outbound/statuses/11/before.xml")
    void test11() throws Exception {
        performRequestAndCheck("outbound/statuses/11/request.json", "outbound/statuses/11/response.json");
    }

    private void performRequestAndCheck(String requestPath, String responseFilePath) throws Exception {
        var mvcResult = mockMvc
                .perform(
                        MockMvcRequestBuilders
                                .post("/ENTERPRISE/outbound/statuses")
                                .content(FileContentUtils.getFileContent(requestPath))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JSONAssert.assertEquals(
                FileContentUtils.getFileContent(responseFilePath),
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE
        );
    }
}
