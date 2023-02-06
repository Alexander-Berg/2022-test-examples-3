package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.ff.base.MvcIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.util.FileContentUtils.getFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

/**
 * Функциональный тест для контроллера коллбэков трекера статусов (Delivery Tracker)
 * Ручка /tracker
 */
class DeliveryTrackerCallbackControllerTest  extends MvcIntegrationTest {

    /**
     * Проверяем успешное обновление статусов для заявок
     * <p>
     * В БД:
     * <ul>
     *      <li> shop_request: Заявка с id=0 type=SUPPLY в статусе 3
     *      <li> request_status_history: для заявки с id=0 записи статусов 0, 1, 2, 3
     * </ul>
     * В запросе:
     * <ul>
     *      <li> для заявки 0 новые статусы (9, 6, 7)
     * </ul>
     * <p>
     * В базе данных должна появиться таска, в которой описаны все новые статусы для обновления
     */
    @Test
    @DatabaseSetup("classpath:controller/tracker/before-request-update-statuses.xml")
    @ExpectedDatabase(
            value = "classpath:controller/tracker/after-creating-task-update-statuses.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldUpdateStatusForOrder() throws Exception {
        mockMvc.perform(
                        post("/tracker/notify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(extractFileContent("controller/tracker/status-updates.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/tracker/result.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/tracker/before-request-update-statuses-slow.xml")
    @ExpectedDatabase(
            value = "classpath:controller/tracker/after-creating-task-update-statuses-slow.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void shouldUpdateStatusForOrderSlow() throws Exception {
        mockMvc.perform(
                post("/tracker/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(extractFileContent("controller/tracker/status-updates.json")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/tracker/result.json")));
    }

}

