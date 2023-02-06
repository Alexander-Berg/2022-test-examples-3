package ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.controller.MediaTypes;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.logisticsServices.holiday.LogisticsServicesHolidaysHelper.getTemplate;
import static ru.yandex.market.logistics.management.util.TestUtil.fileContent;

public class AdminLogisticServiceHolidayFileControllerGetTemplateTest extends AbstractContextualAspectValidationTest {

    @Test
    @DisplayName("Скачивание шаблона csv-файла выходных дней сервисов")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_HOLIDAY_EDIT
    )
    public void getTemplateSuccess() throws Exception {
        mockMvc.perform(getTemplate())
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=logistic-service-holidays-template.csv"
            ))
            .andExpect(content().contentType(MediaTypes.TEXT_CSV_UTF8))
            .andExpect(fileContent(
                "data/controller/admin/logisticServices/response/logistic-service-holidays-template.csv"
            ));
    }
}
