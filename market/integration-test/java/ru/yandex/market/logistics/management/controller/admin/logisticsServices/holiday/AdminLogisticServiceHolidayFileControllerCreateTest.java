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
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.uploadFileForCreate;

@DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_holidays_with_multiple_services.xml")
public class AdminLogisticServiceHolidayFileControllerCreateTest extends AbstractContextualAspectValidationTest {

    /**
     * <b>Дни с id:</b>
     * <li>(11-44, 77)  -> не меняются (т.к. отсутствуют в csv-файле)</li>
     * <li>(1, 2)       -> добавятся (т.к. отсутствуют в таблице) в новый календарь (id=1)</li>
     * <li>(3)          -> добавятся (т.к. отсутствуют в таблице) в сущ-щий календарь (id=22)</li>
     * <li>(55)         -> не меняются (т.к. is_holiday уже true)</li>
     * <li>(66)         -> is_holiday=true (т.к. уже есть в таблице с is_holiday=false)</li>
     */
    @Test
    @DisplayName("Создание выходных для сервисов через csv-файл")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_HOLIDAY_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticServices/after/create_holidays_for_multiple_services.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void createHolidaysFromFileSuccess() throws Exception {
        mockMvc.perform(
            uploadFileForCreate("data/controller/admin/logisticServices/request/upload_create_success.csv")
        ).andExpect(status().isOk());
    }
}
