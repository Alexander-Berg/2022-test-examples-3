package ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.create;

@DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_holidays.xml")
public class AdminLogisticServiceHolidayControllerCreateTest extends AbstractContextualAspectValidationTest {

    @Test
    @DisplayName("Создать выходной для несуществующего сервиса")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_HOLIDAY_EDIT
    )
    public void createHolidayFailedLogisticServiceNotFound() throws Exception {
        mockMvc.perform(
            create(52L).content("{\"day\": \"2020-04-11\"}")
        ).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Создать выходной из несуществующего календарного дня")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_HOLIDAY_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticServices/after/create_new_calendar_day_holiday.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void createHolidayFromExistingCalendarDay() throws Exception {
        mockMvc.perform(
            create(25L).content("{\"day\": \"2020-04-11\"}")
        ).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Создать выходной из уже существующего календарного дня")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_HOLIDAY_EDIT
    )
    @DatabaseSetup(
        value = "/data/controller/admin/logisticServices/before/prepare_holiday.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticServices/after/create_existing_calendar_day_holiday.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void createHolidayFromNonExistingCalendarDay() throws Exception {
        mockMvc.perform(
            create(25L).content("{\"day\": \"2023-01-01\"}")
        ).andExpect(status().isCreated());
    }
}
