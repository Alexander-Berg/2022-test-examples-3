package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/repository/logistic-point-availability/before/prepare_data.xml")
@DatabaseSetup("/repository/logistic-point-availability/before/schedule_prepare_data.xml")
class LogisticPointAvailabilityScheduleDeleteTest extends AbstractContextualTest {
    @Test
    @DisplayName("Удаление слота отгрузки для конфигурации доступности склада для магазинов")
    void delete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/admin/logistic-point-availability/schedule/1"))
            .andExpect(status().isOk());

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/admin/logistic-point-availability/schedule")
                .params(toParams(Map.of("logisticPointAvailabilityId", "1")))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/admin/logistic-point-availability/schedule/search_result_no_data_found.json"
            ));
    }

    @Test
    @DisplayName("Удаление слота отгрузки для конфигурации доступности склада для магазинов "
        + "по несуществующему идентификатору")
    void deleteUnknownId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/admin/logistic-point-availability/schedule/2"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTIC_POINT_AVAILABILITY_SCHEDULE_DAY] with ids [2]"));
    }
}
