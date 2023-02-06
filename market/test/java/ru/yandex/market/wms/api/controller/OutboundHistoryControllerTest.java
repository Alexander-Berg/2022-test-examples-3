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

public class OutboundHistoryControllerTest extends IntegrationTest {

    public static final String ID = "outbound-12750263";

    /**
     * В БД для указанного заказа присутствует полноценная история с множеством статусов.
     * В истории присутствуют статусы, на которые нет маппинга для outbound.
     * <p>
     * В ответ должна вернуться эта история, но с сортировкой по дате статуса по возрастанию
     * и со схлопыванием статусов.
     */
    @Test
    @DatabaseSetup("/outbound/history/1/before.xml")
    void test() throws Exception {
        performRequestAndCheck("outbound/history/1/response.json");
    }

    /**
     * В БД нет ничего про этот заказ.
     */
    @Test
    @DatabaseSetup("/outbound/history/2/before.xml")
    void test2() throws Exception {
        performRequestAndCheck("outbound/history/2/response.json");
    }

    /**
     * Заказ есть, истории нет.
     */
    @Test
    @DatabaseSetup("/outbound/history/3/before.xml")
    void test3() throws Exception {
        performRequestAndCheck("outbound/history/3/response.json");
    }

    /**
     * Заказ есть, история есть, но статусов с пустым ордерлайн не завезли.
     */
    @Test
    @DatabaseSetup("/outbound/history/4/before.xml")
    void test4() throws Exception {
        performRequestAndCheck("outbound/history/4/response.json");
    }

    /**
     * Заказ есть, история есть, статус с пустым ордерлайн есть, но и тот не маппится.
     */
    @Test
    @DatabaseSetup("/outbound/history/5/before.xml")
    void test5() throws Exception {
        performRequestAndCheck("outbound/history/5/response.json");
    }

    private void performRequestAndCheck(String responseFilePath) throws Exception {
        var mvcResult = mockMvc
                .perform(
                        MockMvcRequestBuilders
                                .get("/ENTERPRISE/outbound/history")
                                .queryParam("yandexId", ID)
                                .queryParam("partnerId", ID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        JSONAssert.assertEquals(
                FileContentUtils.getFileContent(responseFilePath),
                mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.STRICT
        );
    }
}
