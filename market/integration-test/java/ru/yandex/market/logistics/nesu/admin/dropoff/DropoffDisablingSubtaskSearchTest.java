package ru.yandex.market.logistics.nesu.admin.dropoff;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
@DisplayName("Получение подзадач заявки на отключение дропоффа")
public class DropoffDisablingSubtaskSearchTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получение подзадач заявки")
    void getSubtasks() throws Exception {
        mockMvc.perform(get("/admin/dropoff-disabling/subtask").param("requestId", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/dropoff-disabling/subtasks.json"));
    }

    @Test
    @DisplayName("Пустой список подзадач")
    void emptySubtasksList() throws Exception {
        mockMvc.perform(get("/admin/dropoff-disabling/subtask").param("requestId", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/dropoff-disabling/empty.json"));
    }
}
