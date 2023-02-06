package ru.yandex.market.logistics.nesu.admin.dropoff;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
@DisplayName("Получение детальной карточки запроса на отключение дропоффа")
class DropoffDisablingSubtaskDetailTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получение детальной карточки")
    void getDetail() throws Exception {
        mockMvc.perform(get("/admin/dropoff-disabling/subtask/2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/dropoff-disabling/subtask_detail.json"));
    }

    @Test
    @DisplayName("Получение подзадачи со статусом ERROR")
    void getDetailWithErrorStatus() throws Exception {
        mockMvc.perform(get("/admin/dropoff-disabling/subtask/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/dropoff-disabling/subtask_detail_error.json"));
    }

    @Test
    @DisplayName("Ошибка. Запись не найдена")
    void getDetailUnknownId() throws Exception {
        mockMvc.perform(get("/admin/dropoff-disabling/subtask/7"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [DROPOFF_DISABLING_SUBTASK] with ids [7]"));
    }
}
