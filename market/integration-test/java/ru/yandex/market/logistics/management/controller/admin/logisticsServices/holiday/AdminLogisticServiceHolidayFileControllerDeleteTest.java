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
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.uploadFileForDelete;

@DatabaseSetup("/data/controller/admin/logisticServices/before/prepare_holidays_with_multiple_services.xml")
public class AdminLogisticServiceHolidayFileControllerDeleteTest extends AbstractContextualAspectValidationTest {

    @Test
    @DisplayName("Удаление выходных для сервиса с несуществующим id через csv-файл")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_HOLIDAY_EDIT
    )
    public void deleteHolidaysFromFileFailedServiceNotFound() throws Exception {
        mockMvc.perform(
            uploadFileForDelete("data/controller/admin/logisticServices/request/upload_delete_service_not_found.csv")
        ).andExpect(status().isNotFound());
    }

    /**
     * <b>Дни с id:</b>
     * <li>(11, 22, 44, 55)  -> is_holiday = false</li>
     * <li>(33, 77)          -> не меняются (т.к. отсутствуют в csv-файле)</li>
     * <li>(66)              -> не меняется (т.к. is_holiday уже false)</li>
     * <li>(1)               -> добавится (т.к. есть в csv-файле, но не в таблице) в новый календарь (id=1)</li>
     * <li>(2)               -> добавится (т.к. есть в csv-файле, но не в таблице) в сущ-щий календарь (id=11)</li>
     */
    @Test
    @DisplayName("Удаление выходных для сервисов через csv-файл")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_HOLIDAY_EDIT
    )
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticServices/after/delete_holidays_for_multiple_services.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void deleteHolidaysFromFileSuccess() throws Exception {
        mockMvc.perform(
            uploadFileForDelete("data/controller/admin/logisticServices/request/upload_delete_success.csv")
        ).andExpect(status().isOk());
    }
}
