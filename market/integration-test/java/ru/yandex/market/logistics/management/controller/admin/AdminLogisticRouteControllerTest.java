package ru.yandex.market.logistics.management.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DisplayName("Тесты на общие для логистических маршрутов операции")
@DatabaseSetup("/data/controller/admin/logisticRoute/prepare_data.xml")
class AdminLogisticRouteControllerTest extends AbstractContextualTest {

    @Test
    @SneakyThrows
    @DisplayName("Успешное получение сегментов маршрута")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SEGMENTS)
    void getSegments() {
        mockMvc.perform(get("/admin/lms/logistic-route/logistic-segment")
            .param("routeId", "104_102_101"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/logisticRoute/response/segments_104_102_101.json"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение пустого списка сегментов маршрута, если нет роли AUTHORITY_ROLE_LOGISTIC_SEGMENTS")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER)
    void getSegmentsWithoutRole() {
        mockMvc.perform(get("/admin/lms/logistic-route/logistic-segment")
            .param("routeId", "104_102_101"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/logisticRoute/response/empty_segments.json"));
    }
}
