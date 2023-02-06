package ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.delete;

@DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_holidays.xml")
public class AdminLogisticServiceHolidayControllerDeleteTest extends AbstractContextualAspectValidationTest {

    @Test
    @DisplayName("Удалить выходной сервиса по несуществующему id")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_HOLIDAY_EDIT
    )
    public void deleteHolidayByIdFailedIdWithNotFound() throws Exception {
        mockMvc.perform(
            delete(23L, 25L)
        ).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удалить выходной сервиса по id")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_HOLIDAY_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticServices/after/delete_holiday.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void deleteHolidayById() throws Exception {
        mockMvc.perform(
            delete(22L, 25L)
        ).andExpect(status().isOk());
    }
}
