package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/logistic-point-availability/before/prepare_data.xml")
class LogisticPointAvailabilityDeleteTest extends AbstractContextualTest {
    @Test
    @DisplayName("Удаление конфигурации доступности складов для магазинов")
    void delete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/admin/logistic-point-availability/1"))
            .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/logistic-point-availability/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление конфигурации доступности складов для магазинов по несуществующему идентификатору")
    void deleteUnknownId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/admin/logistic-point-availability/4"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTIC_POINT_AVAILABILITY] with ids [4]"));
    }
}
