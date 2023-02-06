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
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.deleteMultiple;

@DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_holidays.xml")
public class AdminLogisticServiceHolidayControllerDeleteMultipleTest extends AbstractContextualAspectValidationTest {

    @Test
    @DisplayName("Удалить несколько выходных для одного сервиса, когда не все выходные существуют")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_HOLIDAY_EDIT
    )
    public void deleteMultipleHolidaysForOneServiceFailedIdNotFound() throws Exception {
        mockMvc.perform(
            deleteMultiple(25L).content("{\"ids\": [11, 33, 666]}")
        ).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удалить несколько выходных для одного сервиса")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_HOLIDAY_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticServices/after/delete_holidays.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void deleteMultipleHolidaysForOneService() throws Exception {
        mockMvc.perform(
            deleteMultiple(25L).content("{\"ids\": [11, 33]}")
        ).andExpect(status().isOk());
    }
}
