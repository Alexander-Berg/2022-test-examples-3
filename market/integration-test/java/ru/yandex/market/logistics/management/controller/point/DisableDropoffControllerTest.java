package ru.yandex.market.logistics.management.controller.point;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

class DisableDropoffControllerTest extends AbstractContextualAspectValidationTest {

    private static final String URI = "/externalApi/logisticsPoints";

    @Test
    @DisplayName("Успешное отключение дропофа")
    @DatabaseSetup("/data/controller/point/before/disabling_dropoff.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/disabling_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successDisableDropoff() throws Exception {
        long dropoffId = 101L;
        mockMvc.perform(put(URI + "/disable-dropoff/" + dropoffId))
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(("data/controller/point/disabling_dropoff_result.json"))));
    }

    @Test
    @DisplayName("Успешное отключение неактивного дропофа")
    @DatabaseSetup("/data/controller/point/before/disabling_dropoff.xml")
    @DatabaseSetup(
        value = "/data/controller/point/before/disabling_inactive_dropoff.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/controller/point/after/disabling_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successDisableInactiveDropoff() throws Exception {
        long dropoffId = 101L;
        mockMvc.perform(put(URI + "/disable-dropoff/" + dropoffId))
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(("data/controller/point/disabling_dropoff_result.json"))));
    }

    @Test
    @DisplayName("Ошибка отключения дропофа. Лог. точка не дропофф")
    @DatabaseSetup("/data/controller/point/before/disabling_dropoff.xml")
    void logPointIsNotDropoff() throws Exception {
        long dropoffId = 111L;
        mockMvc.perform(put(URI + "/disable-dropoff/" + dropoffId))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("LogisticPoint with id=111 is not dropoff"));
    }
}
