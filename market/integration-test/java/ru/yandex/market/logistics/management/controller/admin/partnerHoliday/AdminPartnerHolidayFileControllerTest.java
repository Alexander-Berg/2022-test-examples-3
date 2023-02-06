package ru.yandex.market.logistics.management.controller.admin.partnerHoliday;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
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

@DisplayName("Скачивание шаблона и загрузка csv-файла выходных дней партнера")
@DatabaseSetup("/data/controller/admin/partnerHoliday/before/prepare_data.xml")
public class AdminPartnerHolidayFileControllerTest extends AbstractContextualTest {

    private static final String URL = "/admin/lms/partner-holiday";

    @Test
    @DisplayName("Скачивание шаблона csv-файла выходных дней партнера")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT})
    void downloadTemplateSuccess() throws Exception {
        mockMvc.perform(get(URL + "/download/template"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=partner-holidays-template.csv"
            ))
            .andExpect(content().contentType(MediaTypes.TEXT_CSV_UTF8))
            .andExpect(
                TestUtil.fileContent("data/controller/admin/partnerHoliday/response/partner-holidays-template.csv")
            );
    }

    @Test
    @DisplayName("Загрузка файла и создание выходных дней партнера")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerHoliday/after/upload_add.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvSuccess() throws Exception {
        performCreate("data/controller/admin/partnerHoliday/request/upload_add_success.csv")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Загрузка файла и замена выходных дней партнера, которые попадают в интервал")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerHoliday/after/upload_replace_all.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceAllSuccess() throws Exception {
        performUpdate("data/controller/admin/partnerHoliday/request/upload_replace_all_success.csv")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Загрузка файла выходных дней, которые не попадают в интервал")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerHoliday/after/upload_replace_out_of_date_interval.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceOutOfDateInterval() throws Exception {
        performUpdate("data/controller/admin/partnerHoliday/request/upload_replace_out_of_date_interval.csv")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Загрузка файла выходных дней, которые попадают в интервал и которые нет")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_HOLIDAY_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerHoliday/after/upload_replace_out_and_in_date_interval.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void uploadCsvReplaceOutAndInDateInterval() throws Exception {
        performUpdate("data/controller/admin/partnerHoliday/request/upload_replace_out_and_in_date_interval.csv")
            .andExpect(status().isOk());
    }

    @NotNull
    @SneakyThrows
    public static MockMultipartFile multipartFile(String content) {
        return new MockMultipartFile(
            "request",
            "originalFileName.csv",
            MediaTypes.TEXT_CSV_UTF8_VALUE,
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );
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
