package ru.yandex.market.delivery.transport_manager.controller.distribution_center;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class DistributionCenterControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("Контроллер РЦ: успешный пуш состояния")
    @ExpectedDatabase(
        value = "/controller/distribution_center/put_state/database/push_state_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void pushStateSuccess() throws Exception {
        mockMvc.perform(put("/distribution_center/1/state")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/distribution_center/put_state/push_state_success.json")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Контроллер РЦ: ошибка валидации")
    void pushStateValidationError() throws Exception {
        mockMvc.perform(put("/distribution_center/1/state")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/distribution_center/put_state/push_state_validation_error.json")))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Контроллер РЦ: успешный пуш состояния sortables")
    @ExpectedDatabase(
        value = "/controller/distribution_center/put_state/database/push_sortables_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void pushSortablesSuccess() throws Exception {
        mockMvc.perform(put("/distribution_center/1/state")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/distribution_center/put_state/push_sortables_success.json")))
            .andExpect(status().isOk());
    }
}
