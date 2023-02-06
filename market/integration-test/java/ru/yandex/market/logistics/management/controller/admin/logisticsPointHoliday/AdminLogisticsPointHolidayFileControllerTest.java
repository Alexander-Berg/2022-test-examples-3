package ru.yandex.market.logistics.management.controller.admin.logisticsPointHoliday;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.controller.MediaTypes;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.controller.admin.partnerHoliday.AdminPartnerHolidayFileControllerTest.multipartFile;

@DisplayName("Скачивание шаблона и загрузка csv-файла выходных дней ПВЗ")
@DatabaseSetup("/data/controller/admin/logisticsPointHoliday/before/prepare_data.xml")
class AdminLogisticsPointHolidayFileControllerTest extends AbstractContextualTest {

    private static final String URL = "/admin/lms/logistics-point-holiday";

    @Test
    @DisplayName("Скачивание шаблона csv-файла выходных дней ПВЗ")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT})
    void downloadTemplateSuccess() throws Exception {
        mockMvc.perform(get(URL + "/download/template"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=logistics-point-holidays-template.csv"
            ))
            .andExpect(content().contentType(MediaTypes.TEXT_CSV_UTF8))
            .andExpect(TestUtil.fileContent(
                "data/controller/admin/logisticsPointHoliday/response/logistics-point-holidays-template.csv"
            ));
    }

    @Test
    @DisplayName("Загрузка файла и создание выходных дней ПВЗ")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsPointHoliday/after/upload_add.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvSuccess() throws Exception {
        performCreate("data/controller/admin/logisticsPointHoliday/request/upload_add_success.csv")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Загрузка файла и замена выходных дней ПВЗ, которые попадают в интервал")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsPointHoliday/after/upload_replace_all.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceAllSuccess() throws Exception {
        performUpdate("data/controller/admin/logisticsPointHoliday/request/upload_replace_all_success.csv")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Загрузка файла выходных дней, которые не попадают в интервал")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsPointHoliday/after/upload_replace_out_of_date_interval.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceOutOfDateInterval() throws Exception {
        performUpdate("data/controller/admin/logisticsPointHoliday/request/upload_replace_out_of_date_interval.csv")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Загрузка файла выходных дней, которые попадают в интервал и которые нет")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_HOLIDAY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsPointHoliday/after/upload_replace_out_and_in_date_interval.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceOutAndInDateInterval() throws Exception {
        performUpdate("data/controller/admin/logisticsPointHoliday/request/upload_replace_out_and_in_date_interval.csv")
            .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions performCreate(String jsonPath) throws Exception {
        return perform("/upload/add?parentId=1", jsonPath);
    }

    @Nonnull
    private ResultActions performUpdate(String jsonPath) throws Exception {
        return perform("/upload/replace?parentId=1", jsonPath);
    }

    @Nonnull
    private ResultActions perform(String endpoint, String jsonPath) throws Exception {
        return mockMvc.perform(
            multipart(URL + endpoint)
                .file(
                    multipartFile(
                        TestUtil.pathToJson(jsonPath)
                    )
                )
        );
    }
}
