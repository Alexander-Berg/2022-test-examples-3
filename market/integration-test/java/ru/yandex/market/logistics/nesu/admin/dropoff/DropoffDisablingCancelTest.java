package ru.yandex.market.logistics.nesu.admin.dropoff;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
@DisplayName("Отмена запроса на отключение дропоффа")
class DropoffDisablingCancelTest extends AbstractContextualTest {

    @Test
    @DisplayName("Отмена заявки")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/after/disabling_dropoff_request_cancel.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelRequest() throws Exception {
        mockMvc.perform(post("/admin/dropoff-disabling/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 1}")
            )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ошибка. Отмена заявки в терминальном статусе")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/before/disabling_dropoff_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelRequestTerminalStatus() throws Exception {
        mockMvc.perform(post("/admin/dropoff-disabling/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 3}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Request status must not be terminal"));
    }

    @Test
    @DisplayName("Ошибка. Заявка не найдена")
    @ExpectedDatabase(
        value = "/repository/dropoff/disabling/before/disabling_dropoff_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelRequestNotFound() throws Exception {
        mockMvc.perform(post("/admin/dropoff-disabling/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 5}")
            )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [DROPOFF_DISABLING_REQUEST] with ids [5]"));
    }
}
